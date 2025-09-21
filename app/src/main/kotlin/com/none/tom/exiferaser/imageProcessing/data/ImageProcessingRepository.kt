/*
 * Copyright (c) 2018-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.imageProcessing.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.core.di.DispatcherIo
import com.none.tom.exiferaser.core.extension.isNotNullOrEmpty
import com.none.tom.exiferaser.core.extension.suspendRunCatching
import com.none.tom.exiferaser.core.image.ImageMetadataSnapshot
import com.none.tom.exiferaser.core.image.ImageProcessingProgress
import com.none.tom.exiferaser.core.image.ImageProcessingStep
import com.none.tom.exiferaser.core.image.ImageProcessingSummary
import com.none.tom.exiferaser.core.image.supportedImageFormats
import com.none.tom.exiferaser.core.provider.CameraFileProvider
import com.none.tom.exiferaser.core.util.FileUtilsKt
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.tommygeenexus.exifinterfaceextended.ExifInterfaceExtended
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import timber.log.Timber

@Singleton
class ImageProcessingRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:DispatcherIo private val dispatcherIo: CoroutineDispatcher
) {

    fun removeMetadata(
        protos: List<UserImageSelectionProto>,
        treeUri: Uri,
        displayNameSuffix: String,
        isAutoDeleteEnabled: Boolean,
        isPreserveOrientationEnabled: Boolean,
        isRandomizeFileNamesEnabled: Boolean
    ): Flow<ImageProcessingStep> = flow {
        protos.forEachIndexed { index, proto ->
            emit(
                removeMetaData(
                    proto = proto,
                    treeUri = treeUri,
                    displayNameSuffix = displayNameSuffix,
                    isAutoDeleteEnabled = isAutoDeleteEnabled,
                    isPreserveOrientationEnabled = isPreserveOrientationEnabled,
                    isRandomizeFileNamesEnabled = isRandomizeFileNamesEnabled,
                    progress = ImageProcessingProgress.calculate(
                        index = index,
                        count = protos.size
                    )
                )
            )
        }
        emit(ImageProcessingStep.FinishedBulk)
    }
        .buffer()
        .flowOn(dispatcherIo)

    private suspend fun removeMetaData(
        proto: UserImageSelectionProto,
        treeUri: Uri,
        displayNameSuffix: String,
        isAutoDeleteEnabled: Boolean,
        isPreserveOrientationEnabled: Boolean,
        isRandomizeFileNamesEnabled: Boolean,
        progress: ImageProcessingProgress
    ): ImageProcessingStep.FinishedSingle {
        var sourceUri = Uri.EMPTY
        var sinkUri = Uri.EMPTY
        var displayName = ""
        var formattedDisplayName = ""
        var extension = ""
        var mimeType = ""
        var imageMetadataSnapshot = ImageMetadataSnapshot()
        var isImageSaved = false
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                sourceUri = proto.image_path.toUri()
                displayName = getDisplayName(sourceUri).getOrThrow()
                mimeType = checkNotNull(context.contentResolver.getType(sourceUri)).also { t ->
                    check(supportedImageFormats.any { it.mimeType == t })
                }
                extension = checkNotNull(
                    MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                )
                val (exifInterfaceExtended, scanResult) = scanImage(sourceUri).getOrThrow()
                imageMetadataSnapshot = scanResult
                if (scanResult.isMetadataContained() || isRandomizeFileNamesEnabled) {
                    formattedDisplayName = if (isRandomizeFileNamesEnabled) {
                        UUID.randomUUID().toString()
                    } else {
                        displayName
                            .plus(
                                if (sourceUri.authority == CameraFileProvider.AUTHORITY) {
                                    CameraFileProvider.SUFFIX
                                } else {
                                    ""
                                }
                            )
                            .plus(
                                if (FileUtilsKt.isValidExtFilename(displayNameSuffix)) {
                                    displayNameSuffix
                                } else {
                                    ""
                                }
                            )
                    }
                    sinkUri = createImage(
                        uri = sourceUri,
                        defaultTreeUri = treeUri,
                        displayName = formattedDisplayName,
                        mimeType = mimeType
                    ).getOrThrow()
                    saveImage(
                        exifInterfaceExtended = exifInterfaceExtended,
                        sourceUri = sourceUri,
                        sinkUri = sinkUri,
                        isAutoDeleteEnabled = isAutoDeleteEnabled,
                        isPreserveOrientationEnabled = isPreserveOrientationEnabled
                    ).getOrThrow()
                    isImageSaved = true
                }
            }.getOrElse { exception ->
                Timber.e(exception)
            }
            ImageProcessingStep.FinishedSingle(
                imageProcessingSummary = ImageProcessingSummary(
                    displayName = if (isImageSaved) formattedDisplayName else displayName,
                    extension = extension,
                    mimeType = mimeType,
                    isImageSaved = isImageSaved,
                    uri = if (isImageSaved) sinkUri else sourceUri,
                    imageMetadataSnapshot = imageMetadataSnapshot
                ),
                progress = progress
            )
        }
    }

    private suspend fun scanImage(
        sourceUri: Uri
    ): Result<Pair<ExifInterfaceExtended, ImageMetadataSnapshot>> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            val exifInterfaceExtended: ExifInterfaceExtended
            checkNotNull(context.contentResolver.openInputStream(sourceUri)).use { source ->
                exifInterfaceExtended = ExifInterfaceExtended(source)
            }
            Result.success(
                value = exifInterfaceExtended to ImageMetadataSnapshot(
                    isIccProfileContained = exifInterfaceExtended.hasIccProfile(),
                    isExifContained = exifInterfaceExtended.hasAttributes(true),
                    isPhotoshopImageResourcesContained =
                    exifInterfaceExtended.hasPhotoshopImageResources(),
                    isXmpContained = exifInterfaceExtended.hasXmp(),
                    isExtendedXmpContained = exifInterfaceExtended.hasExtendedXmp()
                )
            )
        }.getOrElse { exception ->
            Timber.e(exception)
            Result.failure(exception)
        }
    }

    private suspend fun createImage(
        uri: Uri,
        defaultTreeUri: Uri = Uri.EMPTY,
        displayName: String,
        mimeType: String
    ): Result<Uri> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                if (uri.authority == CameraFileProvider.AUTHORITY) {
                    return@suspendRunCatching Result.success(
                        value = getFileProviderUri(
                            displayName = displayName,
                            extension = checkNotNull(
                                MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                            )
                        ).getOrThrow()
                    )
                }
                require(uri.scheme == ContentResolver.SCHEME_CONTENT)
                require(displayName.isNotEmpty())
                require(mimeType.isNotEmpty())
                var treeUri: Uri? = null
                if (defaultTreeUri.isNotNullOrEmpty()) {
                    val file = DocumentFile.fromTreeUri(context, defaultTreeUri)
                    if (file != null && file.isDirectory && file.canWrite()) {
                        treeUri = file.uri
                    }
                }
                if (!treeUri.isNotNullOrEmpty()) {
                    val file = DocumentFile.fromSingleUri(context, uri)
                    if (file != null && file.isFile && file.exists()) {
                        treeUri = file.uri
                    }
                }
                checkNotNull(treeUri)
                Result.success(
                    value = checkNotNull(
                        DocumentsContract.createDocument(
                            context.contentResolver,
                            treeUri,
                            mimeType,
                            displayName
                        )
                    )
                )
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    private suspend fun saveImage(
        exifInterfaceExtended: ExifInterfaceExtended,
        sourceUri: Uri,
        sinkUri: Uri,
        isAutoDeleteEnabled: Boolean,
        isPreserveOrientationEnabled: Boolean
    ): Result<Unit> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            checkNotNull(context.contentResolver.openInputStream(sourceUri)).use { source ->
                checkNotNull(context.contentResolver.openOutputStream(sinkUri)).use { sink ->
                    exifInterfaceExtended.saveExclusive(
                        source,
                        sink,
                        isPreserveOrientationEnabled
                    )
                }
            }
            if (isAutoDeleteEnabled && DocumentsContract.isDocumentUri(context, sourceUri)) {
                if (!checkNotNull(DocumentFile.fromSingleUri(context, sourceUri)).delete()) {
                    Timber.e("Failed to delete %s", sourceUri)
                }
            }
            Result.success(Unit)
        }.getOrElse { exception ->
            Timber.e(exception)
            Result.failure(exception)
        }
    }

    private suspend fun getDisplayName(uri: Uri): Result<String> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            checkNotNull(
                context
                    .contentResolver
                    .query(uri, null, null, null, null)
            ).use { cursor ->
                check(cursor.moveToFirst())
                val baseName = FilenameUtils.getBaseName(
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            MediaStore.Images.ImageColumns.DISPLAY_NAME
                        )
                    )
                )
                check(baseName.isNotEmpty())
                Result.success(baseName)
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            Result.failure(exception)
        }
    }

    suspend fun getLastDocumentPathSegment(uri: Uri): Result<String> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            Result.success(
                value = checkNotNull(
                    DocumentsContract
                        .findDocumentPath(context.contentResolver, uri)
                        ?.path
                        ?.lastOrNull()
                )
            )
        }.getOrElse { exception ->
            Timber.e(exception)
            Result.failure(exception)
        }
    }

    suspend fun getFileProviderUri(displayName: String, extension: String): Result<Uri> =
        withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                Result.success(
                    value = CameraFileProvider.getUriForFile(
                        context = context,
                        displayName = displayName.ifEmpty { UUID.randomUUID().toString() },
                        extension = extension.ifEmpty {
                            checkNotNull(
                                MimeTypeMap
                                    .getSingleton()
                                    .getExtensionFromMimeType(
                                        supportedImageFormats.first().mimeType
                                    )
                            )
                        }
                    )
                )
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
}
