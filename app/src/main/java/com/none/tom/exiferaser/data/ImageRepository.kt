// Copyright (c) 2018-2020, Tom Geiselmann
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software
// and associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.none.tom.exiferaser.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import com.none.tom.exiferaser.EMPTY_STRING
import com.none.tom.exiferaser.MIME_TYPE_JPEG
import com.none.tom.exiferaser.reactive.images.EmptySelection
import com.none.tom.exiferaser.reactive.images.ImageSelection
import com.none.tom.exiferaser.reactive.images.ImagesSelection
import com.none.tom.exiferaser.reactive.images.Selection
import com.none.tom.exiferaser.reactive.images.ImageModifyResult
import com.none.tom.exiferaser.reactive.images.ImageSaveResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.commons.imaging.Imaging
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet
import timber.log.Timber
import java.io.ByteArrayOutputStream

class ImageRepository(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher
) {

    /**
     * Modifies the JPEG image located at [imageSelection], rewriting or removing it's EXIF segment if present.
     *
     * @param imageSelection The [ImageSelection]
     * @param preserveOrientation Whether to preserve the original image orientation
     * @return The [ImageModifyResult]
     */
    suspend fun modifyImage(
        imageSelection: ImageSelection,
        preserveOrientation: Boolean
    ): ImageModifyResult {
        return withContext(dispatcher) {
            rewriteOrRemoveImageMetaData(imageSelection, preserveOrientation).also { result ->
                imageSelection.modified = result.containsMetadata && result.success
                imageSelection.handled = true
            }
        }
    }

    /**
     * Returns the display name for the JPEG image located at [imageSelection].
     *
     * @param imageSelection The image uri
     * @return The display name, or [EMPTY_STRING] if not present
     */
    @WorkerThread
    private fun getImageDisplayName(imageSelection: ImageSelection): String {
        val result = context.contentResolver.runCatching {
            query(imageSelection.uri, null, null, null, null).use { cursor ->
                if (cursor == null || !cursor.moveToFirst()) {
                    throw IllegalStateException("Empty cursor")
                }
                cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME).let { index ->
                    cursor.getString(index)
                        .removeSuffix(".JPG")
                        .removeSuffix(".jpg")
                }
            }
        }
        return if (result.isSuccess) {
            result.getOrNull()!!.let { displayName ->
                imageSelection.displayName = displayName
                displayName
            }
        } else {
            Timber.e(result.exceptionOrNull())
            EMPTY_STRING
        }
    }

    /**
     * Modifies the JPEG image located at [imageSelection], rewriting or removing it's EXIF segment if present.
     *
     * @param imageSelection The [ImageSelection]
     * @param preserveOrientation Whether to preserve the original image orientation
     * @return The [ImageModifyResult]
     */
    @WorkerThread
    private fun rewriteOrRemoveImageMetaData(
        imageSelection: ImageSelection,
        preserveOrientation: Boolean
    ): ImageModifyResult {
        val displayName = getImageDisplayName(imageSelection)
        var modifiedImage: ByteArray? = null
        var containsMetaData = false
        var success = false
        val result = runCatching {
            ByteArrayOutputStream().use { out ->
                context.contentResolver.openInputStream(imageSelection.uri).use { `in` ->
                    `in`?.copyTo(out) ?: throw IllegalStateException("Failed to copy image data")
                }
                val image = out.toByteArray()
                val metaData = Imaging.getMetadata(image)
                if (metaData is JpegImageMetadata && metaData.exif is TiffImageMetadata) {
                    containsMetaData = !containsMetaData
                    ExifRewriter().run {
                        out.reset()
                        removeExifMetadata(image, out)
                        modifiedImage = out.toByteArray()
                        if (preserveOrientation) {
                            val tiffImageMetadata = metaData.exif as TiffImageMetadata
                            val oldOutputSet = tiffImageMetadata.outputSet
                            oldOutputSet.findField(TiffTagConstants.TIFF_TAG_ORIENTATION)
                                ?.let { orientationField ->
                                    val newOutputSet = TiffOutputSet(oldOutputSet.byteOrder).apply {
                                        addRootDirectory().add(orientationField)
                                        addExifDirectory().add(orientationField)
                                    }
                                    out.reset()
                                    updateExifMetadataLossy(modifiedImage, out, newOutputSet)
                                    modifiedImage = out.toByteArray()
                                }
                        }
                    }
                }
            }
        }
        if (result.isSuccess) {
            success = !success
        } else {
            Timber.e(result.exceptionOrNull())
        }
        return ImageModifyResult(displayName, modifiedImage, containsMetaData, success)
    }

    /**
     * Saves the image with [imageData] to [destination].
     *
     * @param destination The destination uri
     * @param imageData The image data
     * @return The [ImageSaveResult]
     */
    suspend fun saveImage(
        destination: Uri,
        imageData: ByteArray
    ): ImageSaveResult {
        return withContext(dispatcher) {
            val result = context.contentResolver.runCatching {
                openOutputStream(destination).use { out ->
                    if (out != null) {
                        out.write(imageData)
                        true
                    } else {
                        throw IllegalStateException("Failed to open file descriptor")
                    }
                }
            }
            if (result.isSuccess) {
                ImageSaveResult(result.getOrNull()!!)
            } else {
                Timber.e(result.exceptionOrNull())
                ImageSaveResult()
            }
        }
    }

    /**
     * Resolves all supported child images in the tree [treeUri].
     *
     * @param treeUri The tree [Uri]
     * @return The [ImageSelection] or [ImagesSelection] if successful, [EmptySelection] otherwise
     */
    suspend fun resolveImageDirectory(treeUri: Uri): Selection {
        return withContext(dispatcher) {
            val result = context.contentResolver.runCatching {
                val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
                val childDocumentsUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId)
                query(childDocumentsUri, null, null, null, null)
                    .use { cursor ->
                        if (cursor == null || !cursor.moveToFirst()) {
                            throw IllegalStateException("Empty cursor")
                        }
                        mutableListOf<ImageSelection>().let { selectedImages ->
                            with(cursor) {
                                while (!isAfterLast) {
                                    getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                                        .let { index ->
                                            DocumentsContract.buildDocumentUriUsingTree(
                                                treeUri,
                                                cursor.getString(index)
                                            ).let { uri ->
                                                if (getType(uri) == MIME_TYPE_JPEG) {
                                                    selectedImages.add(
                                                        ImageSelection(
                                                            uri
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    if (!moveToNext() && !isAfterLast) {
                                        throw IllegalStateException("Failed to move cursor to next row")
                                    }
                                }
                            }
                            selectedImages
                        }
                    }
            }
            if (result.isSuccess) {
                result.getOrNull()!!.let { selections ->
                    when {
                        selections.isEmpty() -> EmptySelection
                        selections.size < 2 -> selections[0]
                        else -> ImagesSelection(selections)
                    }
                }
            } else {
                Timber.e(result.exceptionOrNull())
                EmptySelection
            }
        }
    }
}
