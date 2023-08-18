/*
 * Copyright (c) 2018-2023, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.none.tom.exiferaser.EXTENSION_JPEG
import com.none.tom.exiferaser.PROGRESS_MAX
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.di.DispatcherIo
import com.none.tom.exiferaser.getExtensionFromMimeTypeOrEmpty
import com.none.tom.exiferaser.isFileProviderUri
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.isNullOrEmpty
import com.none.tom.exiferaser.openInputStreamOrThrow
import com.none.tom.exiferaser.openOutputStreamOrThrow
import com.none.tom.exiferaser.queryOrThrow
import com.none.tom.exiferaser.supportedMimeTypes
import com.none.tom.exiferaser.suspendRunCatching
import com.none.tom.exiferaser.toProgress
import com.squareup.wire.AnyMessage
import com.tomg.exifinterfaceextended.ExifInterfaceExtended
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class ImageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher
) {

    suspend fun removeMetadataBulk(
        selection: AnyMessage,
        treeUri: Uri = Uri.EMPTY,
        displayNameSuffix: String = "",
        autoDelete: Boolean = false,
        preserveOrientation: Boolean = false,
        randomizeFileNames: Boolean = false
    ): Flow<Result> {
        return flow {
            val imagesSelection = selection
                .unpack(UserImagesSelectionProto.ADAPTER)
                .user_images_selection
            imagesSelection.forEachIndexed { index, imageSelection ->
                emit(
                    removeMetaData(
                        selection = imageSelection,
                        treeUri = treeUri,
                        displayNameSuffix = displayNameSuffix,
                        autoDelete = autoDelete,
                        preserveOrientation = preserveOrientation,
                        randomizeFileNames = randomizeFileNames
                    )
                )
                emit(Result.Handled(progress = (index + 1).toProgress(imagesSelection.size)))
            }
            emit(Result.HandledAll)
        }
            .buffer()
            .flowOn(dispatcherIo)
    }

    suspend fun removeMetadataSingle(
        selection: AnyMessage,
        treeUri: Uri = Uri.EMPTY,
        displayNameSuffix: String = "",
        autoDelete: Boolean = false,
        preserveOrientation: Boolean = false,
        randomizeFileNames: Boolean = false
    ): Flow<Result> {
        return flow {
            emit(
                removeMetaData(
                    selection = selection.unpack(UserImageSelectionProto.ADAPTER),
                    treeUri = treeUri,
                    displayNameSuffix = displayNameSuffix,
                    autoDelete = autoDelete,
                    preserveOrientation = preserveOrientation,
                    randomizeFileNames = randomizeFileNames
                )
            )
            emit(Result.Handled(progress = PROGRESS_MAX))
            emit(Result.HandledAll)
        }
            .buffer()
            .flowOn(dispatcherIo)
    }

    suspend fun packDocumentTreeToAnyMessageOrNull(treeUri: Uri): AnyMessage? {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val childDocumentsUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                context
                    .contentResolver
                    .queryOrThrow(dispatcherIo, childDocumentsUri)
                    .use { cursor ->
                        if (cursor.moveToFirst()) {
                            packChildDocumentsToAnyMessageOrNull(cursor, treeUri)
                        } else {
                            error("Empty cursor")
                        }
                    }
            }.getOrElse { exception ->
                Timber.e(exception)
                null
            }
        }
    }

    private suspend fun packChildDocumentsToAnyMessageOrNull(
        cursor: Cursor,
        treeUri: Uri
    ): AnyMessage? {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
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
                        error("Failed to move cursor to next row")
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
                        error("No child documents found")
                    }
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                null
            }
        }
    }

    private suspend fun removeMetaData(
        selection: UserImageSelectionProto,
        treeUri: Uri = Uri.EMPTY,
        displayNameSuffix: String = "",
        autoDelete: Boolean = false,
        preserveOrientation: Boolean = false,
        randomizeFileNames: Boolean = false
    ): Result.Report {
        var uri = Uri.EMPTY
        var modifiedUri = Uri.EMPTY
        var displayName = ""
        var mimeType = ""
        var extension = ""
        var containsMetadata = false
        var imageSaved = false
        var containsIccProfile = false
        var containsExif = false
        var containsPhotoshopImageResources = false
        var containsXmp = false
        var containsExtendedXmp = false
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                uri = selection.image_path.toUri()
                mimeType = getMimeTypeOrNull(uri).orEmpty()
                extension = mimeType.getExtensionFromMimeTypeOrEmpty()
                val exifInterfaceExtended: ExifInterfaceExtended
                context
                    .contentResolver
                    .openInputStreamOrThrow(dispatcherIo, uri)
                    .use { source ->
                        exifInterfaceExtended = ExifInterfaceExtended(source)
                        containsIccProfile = exifInterfaceExtended.hasIccProfile()
                        containsExif = exifInterfaceExtended.hasAttributes(true)
                        containsPhotoshopImageResources =
                            exifInterfaceExtended.hasPhotoshopImageResources()
                        containsXmp = exifInterfaceExtended.hasXmp()
                        containsExtendedXmp = exifInterfaceExtended.hasExtendedXmp()
                        containsMetadata = containsIccProfile ||
                            containsExif ||
                            containsPhotoshopImageResources ||
                            containsXmp ||
                            containsExtendedXmp
                    }
                displayName = if (randomizeFileNames) {
                    UUID.randomUUID().toString()
                } else {
                    getDisplayNameOrNull(uri)
                        .orEmpty()
                        .replaceAfter(delimiter = '.', replacement = "")
                        .trimEnd { c -> c == '.' }
                }
                if (displayNameSuffix.isNotEmpty()) {
                    val displayNameWithSuffix = displayName + displayNameSuffix
                    if (FileUtilsKt.isValidExtFilename(displayNameWithSuffix)) {
                        displayName = displayNameWithSuffix
                    }
                }
                if (!containsMetadata) {
                    return@suspendRunCatching
                }
                modifiedUri = createDocument(
                    uri = uri,
                    defaultTreeUri = treeUri,
                    displayName = displayName,
                    mimeType = mimeType,
                    extension = extension
                )
                if (modifiedUri.isNotNullOrEmpty()) {
                    context
                        .contentResolver
                        .openInputStreamOrThrow(dispatcherIo, uri)
                        .use { source ->
                            context
                                .contentResolver
                                .openOutputStreamOrThrow(dispatcherIo, modifiedUri)
                                .use { sink ->
                                    exifInterfaceExtended.saveExclusive(
                                        source,
                                        sink,
                                        preserveOrientation
                                    )
                                    imageSaved = true
                                }
                        }
                    if (autoDelete && imageSaved && DocumentsContract.isDocumentUri(context, uri)) {
                        if (DocumentFile.fromSingleUri(context, uri)?.delete() != true) {
                            Timber.e("Failed to delete %s", uri)
                        }
                    }
                    displayName = DocumentFile
                        .fromSingleUri(context, modifiedUri)
                        ?.name
                        ?.replaceAfter(delimiter = '.', replacement = "")
                        ?.trimEnd { c -> c == '.' }
                        ?: displayName
                }
            }.getOrElse { exception ->
                Timber.e(exception)
            }
            Result.Report(
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
    }

    private suspend fun createDocument(
        uri: Uri,
        defaultTreeUri: Uri = Uri.EMPTY,
        displayName: String = "",
        mimeType: String = "",
        extension: String = ""
    ): Uri? {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                require(uri.scheme == ContentResolver.SCHEME_CONTENT)
                if (DocumentFile.isDocumentUri(context, uri) || !uri.isFileProviderUri()) {
                    require(displayName.isNotEmpty() && mimeType.isNotEmpty())
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
                        error("Failed to resolve tree uri")
                    }
                } else {
                    require(displayName.isNotEmpty() && extension.isNotEmpty())
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
    }

    suspend fun getExternalPicturesFileProviderUriOrNull(
        fileProviderPackage: String = context.getString(R.string.file_provider_package),
        displayName: String,
        extension: String = EXTENSION_JPEG
    ): Uri? {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
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

    suspend fun deleteExternalPictures(): Boolean {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val pictures = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                if (pictures != null) {
                    val files = pictures.listFiles()?.filterNotNull()
                    if (!files.isNullOrEmpty()) {
                        files.forEach { file ->
                            if (!file.delete()) {
                                return@suspendRunCatching false
                            }
                        }
                    }
                    true
                } else {
                    false
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    private suspend fun getDisplayNameOrNull(uri: Uri): String? {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                context
                    .contentResolver
                    .queryOrThrow(dispatcherIo, uri)
                    .use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnName = MediaStore.Images.ImageColumns.DISPLAY_NAME
                            cursor.getString(cursor.getColumnIndexOrThrow(columnName))
                        } else {
                            error("Empty cursor")
                        }
                    }
            }.getOrElse { exception ->
                Timber.e(exception)
                null
            }
        }
    }

    private suspend fun getMimeTypeOrNull(uri: Uri): String? {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                context.contentResolver.getType(uri)
            }.getOrElse { exception ->
                Timber.e(exception)
                null
            }
        }
    }

    suspend fun getDocumentPathOrNull(uri: Uri): String? {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                DocumentsContract
                    .findDocumentPath(context.contentResolver, uri)
                    ?.path
                    ?.lastOrNull()
            }.getOrElse { exception ->
                Timber.e(exception)
                null
            }
        }
    }
}
