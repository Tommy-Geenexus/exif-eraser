/*
 * Copyright (c) 2018-2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.none.tom.exiferaser.selection.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.none.tom.exiferaser.EXTENSION_JPEG
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.di.DispatcherIo
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.isNullOrEmpty
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.getExtensionFromMimeTypeOrEmpty
import com.none.tom.exiferaser.selection.openInputStreamOrThrow
import com.none.tom.exiferaser.selection.openOutputStreamOrThrow
import com.none.tom.exiferaser.selection.queryOrThrow
import com.none.tom.exiferaser.selection.toProgress
import com.none.tom.exiferaser.supportedMimeTypes
import com.squareup.wire.AnyMessage
import com.tomg.exifinterfaceextended.ExifInterfaceExtended
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber

@ExperimentalContracts
@Singleton
class ImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @DispatcherIo private val dispatcher: CoroutineDispatcher
) {

    suspend fun removeMetadataBulk(
        selection: List<UserImageSelectionProto>,
        treeUri: Uri = Uri.EMPTY,
        displayNameSuffix: String = String.Empty,
        preserveOrientation: Boolean = false
    ): Flow<Result> {
        return flow {
            selection.forEachIndexed { index, imageSelection ->
                emit(
                    removeMetaData(
                        selection = imageSelection,
                        treeUri = treeUri,
                        displayNameSuffix = displayNameSuffix,
                        preserveOrientation = preserveOrientation
                    )
                )
                emit(Result.Handled(progress = (index + 1).toProgress(selection.size)))
            }
            emit(Result.HandledAll)
        }
            .buffer()
            .flowOn(dispatcher)
    }

    suspend fun removeMetadataSingle(
        selection: UserImageSelectionProto,
        treeUri: Uri = Uri.EMPTY,
        displayNameSuffix: String = String.Empty,
        preserveOrientation: Boolean = false
    ): Flow<Result> {
        return flow {
            emit(
                removeMetaData(
                    selection = selection,
                    treeUri = treeUri,
                    displayNameSuffix = displayNameSuffix,
                    preserveOrientation = preserveOrientation
                )
            )
            emit(Result.Handled(progress = PROGRESS_MAX))
            emit(Result.HandledAll)
        }
            .buffer()
            .flowOn(dispatcher)
    }

    suspend fun packDocumentTreeToAnyMessageOrNull(treeUri: Uri): AnyMessage? {
        return withContext(dispatcher) {
            runCatching {
                val childDocumentsUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                context.contentResolver.queryOrThrow(childDocumentsUri).use { cursor ->
                    if (cursor.moveToFirst()) {
                        packChildDocumentsToAnyMessageOrNull(cursor, treeUri)
                    } else {
                        throw IllegalStateException("Empty cursor")
                    }
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                null
            }
        }
    }

    @WorkerThread
    private fun packChildDocumentsToAnyMessageOrNull(
        cursor: Cursor,
        treeUri: Uri
    ): AnyMessage? {
        return runCatching {
            val selection = mutableListOf<UserImageSelectionProto>()
            while (!cursor.isAfterLast) {
                val columnName = DocumentsContract.Document.COLUMN_DOCUMENT_ID
                val documentId = cursor.getString(cursor.getColumnIndexOrThrow(columnName))
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                if (supportedMimeTypes.contains(getMimeTypeOrNull(documentUri))) {
                    selection.add(UserImageSelectionProto(documentUri.toString()))
                }
                if (!cursor.moveToNext() && !cursor.isAfterLast) {
                    throw IllegalStateException("Failed to move cursor to next row")
                }
            }
            when {
                selection.size > 2 -> {
                    AnyMessage.pack(UserImagesSelectionProto(user_images_selection = selection))
                }
                selection.size == 1 -> {
                    AnyMessage.pack(selection.first())
                }
                else -> {
                    throw IllegalStateException("No child documents found")
                }
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            null
        }
    }

    @WorkerThread
    private fun removeMetaData(
        selection: UserImageSelectionProto,
        treeUri: Uri = Uri.EMPTY,
        displayNameSuffix: String = String.Empty,
        preserveOrientation: Boolean = false
    ): Result.Report {
        var uri = Uri.EMPTY
        var modifiedUri = Uri.EMPTY
        var displayName = String.Empty
        var mimeType = String.Empty
        var extension = String.Empty
        var containsMetadata = false
        var imageSaved = false
        var containsIccProfile = false
        var containsExif = false
        var containsPhotoshopImageResources = false
        var containsXmp = false
        var containsExtendedXmp = false
        runCatching {
            uri = selection.image_path.toUri()
            mimeType = getMimeTypeOrNull(uri).orEmpty()
            extension = mimeType.getExtensionFromMimeTypeOrEmpty()
            val exifInterfaceExtended: ExifInterfaceExtended
            context.contentResolver.openInputStreamOrThrow(uri).use { source ->
                exifInterfaceExtended = ExifInterfaceExtended(source)
                containsIccProfile = exifInterfaceExtended.hasIccProfile()
                containsExif = exifInterfaceExtended.hasAttributes(true)
                containsPhotoshopImageResources = exifInterfaceExtended.hasPhotoshopImageResources()
                containsXmp = exifInterfaceExtended.hasXmp()
                containsExtendedXmp = exifInterfaceExtended.hasExtendedXmp()
                containsMetadata = containsIccProfile ||
                    containsExif ||
                    containsPhotoshopImageResources ||
                    containsXmp ||
                    containsExtendedXmp
            }
            displayName = getDisplayNameOrNull(uri).orEmpty()
                .replaceAfter(delimiter = '.', replacement = "")
                .trimEnd { c -> c == '.' }
            if (displayNameSuffix.isNotEmpty()) {
                val displayNameWithSuffix = displayName + '_' + displayNameSuffix
                if (FileUtilsKt.isValidExtFilename(displayNameWithSuffix)) {
                    displayName = displayNameWithSuffix
                }
            }
            if (!containsMetadata) {
                return@runCatching
            }
            modifiedUri = createDocument(
                uri = uri,
                defaultTreeUri = treeUri,
                displayName = displayName,
                mimeType = mimeType,
                extension = extension
            )
            if (modifiedUri.isNotNullOrEmpty()) {
                context.contentResolver.openInputStreamOrThrow(uri).use { source ->
                    context.contentResolver.openOutputStreamOrThrow(modifiedUri).use { sink ->
                        exifInterfaceExtended.saveExclusive(source, sink, preserveOrientation)
                        imageSaved = true
                    }
                }
            }
        }.getOrElse { exception ->
            Timber.e(exception)
        }
        return Result.Report(
            summary = Summary(
                displayName = displayName,
                extension = extension,
                mimeType = mimeType,
                imageModified = containsMetadata,
                imageSaved = imageSaved,
                imageUri = if (imageSaved) modifiedUri else uri,
                containsExif = containsExif,
                containsIccProfile = containsIccProfile,
                containsPhotoshopImageResources = containsPhotoshopImageResources,
                containsXmp = containsXmp,
                containsExtendedXmp = containsExtendedXmp
            )
        )
    }

    @WorkerThread
    private fun createDocument(
        uri: Uri,
        defaultTreeUri: Uri = Uri.EMPTY,
        displayName: String = String.Empty,
        mimeType: String = String.Empty,
        extension: String = String.Empty
    ): Uri? {
        return runCatching {
            if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
                throw IllegalArgumentException("Invalid uri")
            }
            if (DocumentFile.isDocumentUri(context, uri)) {
                if (displayName.isEmpty() || mimeType.isEmpty()) {
                    throw IllegalArgumentException("Invalid displayName or mimeType")
                }
                var treeUri: Uri? = null
                if (defaultTreeUri.isNotNullOrEmpty()) {
                    val file = DocumentFile.fromTreeUri(context, defaultTreeUri)
                    if (file != null && file.isDirectory && file.canWrite()) {
                        treeUri = file.uri
                    }
                }
                if (treeUri.isNullOrEmpty()) {
                    val file = DocumentFile.fromSingleUri(context, uri)
                    if (file != null && file.isFile && file.exists()) {
                        treeUri = file.uri
                    }
                }
                if (treeUri.isNotNullOrEmpty()) {
                    DocumentsContract.createDocument(
                        context.contentResolver,
                        treeUri,
                        mimeType,
                        displayName
                    )
                } else {
                    throw IllegalStateException("Failed to resolve tree uri")
                }
            } else {
                if (displayName.isEmpty() || extension.isEmpty()) {
                    throw IllegalArgumentException("Invalid displayName or extension")
                }
                getExternalPicturesFileProviderUriOrNull(
                    displayName = displayName + '_' + context.getString(R.string.modified),
                    extension = extension
                )
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            null
        }
    }

    @WorkerThread
    private fun getDisplayNameOrNull(uri: Uri): String? {
        return runCatching {
            context.contentResolver.queryOrThrow(uri).use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnName = MediaStore.Images.ImageColumns.DISPLAY_NAME
                    cursor.getString(cursor.getColumnIndexOrThrow(columnName))
                } else {
                    throw IllegalStateException("Empty cursor")
                }
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            null
        }
    }

    @WorkerThread
    private fun getMimeTypeOrNull(uri: Uri): String? {
        return runCatching {
            context.contentResolver.getType(uri)
        }.getOrElse { exception ->
            Timber.e(exception)
            null
        }
    }

    @WorkerThread
    fun getExternalPicturesFileProviderUriOrNull(
        fileProviderPackage: String = context.getString(R.string.file_provider_package),
        displayName: String,
        extension: String = EXTENSION_JPEG
    ): Uri? {
        return runCatching {
            FileProvider.getUriForFile(
                context,
                fileProviderPackage,
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    displayName + extension
                )
            )
        }.getOrElse { exception ->
            Timber.e(exception)
            null
        }
    }
}
