/*
 * Copyright (c) 2018-2020, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import android.annotation.SuppressLint
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
import com.none.tom.exiferaser.isNotEmpty
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.supportedMimeTypes
import com.squareup.wire.AnyMessage
import com.tomg.exifinterfaceextended.ExifInterfaceExtended
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber

@Suppress("TooManyFunctions")
class ImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contentResolver: ContentResolver,
    @DispatcherIo private val dispatcher: CoroutineDispatcher
) {

    @Suppress("MagicNumber")
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
                emit(Result.Handled(progress = ((index + 1) * 100) / selection.size))
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

    suspend fun getDocumentTreeAsMessageOrNull(treeUri: Uri): AnyMessage? {
        return withContext(dispatcher) {
            runCatching {
                val childDocumentsUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                queryOrThrow(childDocumentsUri).use { cursor ->
                    if (cursor.moveToFirst()) {
                        getChildDocumentsAsMessageOrNull(cursor, treeUri)
                    } else {
                        null
                    }
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                null
            }
        }
    }

    @WorkerThread
    private fun getChildDocumentsAsMessageOrNull(
        cursor: Cursor,
        treeUri: Uri
    ): AnyMessage? {
        return runCatching {
            val selection = mutableListOf<UserImageSelectionProto>()
            while (!cursor.isAfterLast) {
                val columnName = DocumentsContract.Document.COLUMN_DOCUMENT_ID
                val documentId = cursor.getString(cursor.getColumnIndexOrThrow(columnName))
                val documentUri =
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
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
                    null
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
        val imageUri = selection.image_path.toUri()
        var modifiedImageUri = Uri.EMPTY
        var displayName = String.Empty
        var containsMetadata = false
        var imageSaved = false
        runCatching {
            val exifInterfaceExtended: ExifInterfaceExtended
            openInputStreamOrThrow(imageUri).use { source ->
                exifInterfaceExtended = ExifInterfaceExtended(source)
                containsMetadata = exifInterfaceExtended.hasAttributes(true) ||
                        exifInterfaceExtended.hasExtendedXmp() ||
                        exifInterfaceExtended.hasIccProfile() ||
                        exifInterfaceExtended.hasPhotoshopImageResources()
            }
            displayName = getDisplayNameOrNull(imageUri).orEmpty()
            val endIndex = displayName.indexOfFirst { c -> c == '.' }
            displayName = displayName.substring(
                startIndex = 0,
                endIndex = if (endIndex > 0) endIndex else displayName.length
            )
            if (displayNameSuffix.isNotEmpty()) {
                displayName = displayName.plus('_').plus(displayNameSuffix)
            }
            if (!containsMetadata) {
                return@runCatching
            }
            val mimeType = getMimeTypeOrNull(imageUri)
            modifiedImageUri = getChildDocumentPathOrNull(
                documentPath = imageUri,
                treeUri = treeUri,
                displayName = displayName,
                mimeType = mimeType
            )
            if (modifiedImageUri != null) {
                openInputStreamOrThrow(imageUri).use { source ->
                    openOutputStreamOrThrow(modifiedImageUri).use { sink ->
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
                imageModified = containsMetadata,
                imageSaved = imageSaved,
                imageUri = if (imageSaved) modifiedImageUri else imageUri
            )
        )
    }

    @WorkerThread
    private fun getChildDocumentPathOrNull(
        documentPath: Uri,
        treeUri: Uri = Uri.EMPTY,
        displayName: String?,
        mimeType: String?
    ): Uri? {
        return runCatching {
            val documentTreeUri = DocumentFile.fromTreeUri(
                context,
                if (treeUri.isNotEmpty()) {
                    treeUri
                } else {
                    documentPath
                }
            )?.uri
            if (documentTreeUri != null &&
                !displayName.isNullOrEmpty() &&
                !mimeType.isNullOrEmpty()
            ) {
                DocumentsContract.createDocument(
                    contentResolver,
                    documentTreeUri,
                    mimeType,
                    displayName
                )
            } else {
                null
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            null
        }
    }

    @WorkerThread
    private fun getDisplayNameOrNull(documentPath: Uri): String? {
        return runCatching {
            queryOrThrow(documentPath).use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnName = MediaStore.Images.ImageColumns.DISPLAY_NAME
                    cursor.getString(cursor.getColumnIndexOrThrow(columnName))
                } else {
                    null
                }
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            null
        }
    }

    @WorkerThread
    private fun getMimeTypeOrNull(documentPath: Uri): String? {
        return runCatching {
            contentResolver.getType(documentPath)
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
                    displayName.plus(extension)
                )
            )
        }.getOrElse { exception ->
            Timber.e(exception)
            null
        }
    }

    @WorkerThread
    private fun openInputStreamOrThrow(documentPath: Uri): InputStream {
        return contentResolver.openInputStream(documentPath) ?: throw IllegalStateException()
    }

    @WorkerThread
    private fun openOutputStreamOrThrow(documentPath: Uri): OutputStream {
        return contentResolver.openOutputStream(documentPath) ?: throw IllegalStateException()
    }

    @SuppressLint("Recycle")
    @WorkerThread
    private fun queryOrThrow(documentPath: Uri): Cursor {
        return contentResolver.query(documentPath, null, null, null, null)
            ?: throw IllegalStateException()
    }
}
