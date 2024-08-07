/*
 * Copyright (c) 2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.core.util

import android.content.ContentResolver
import androidx.core.net.toUri
import com.none.tom.exiferaser.CameraProto
import com.none.tom.exiferaser.ImageDirectoryProto
import com.none.tom.exiferaser.ImageFileProto
import com.none.tom.exiferaser.ImageFilesProto
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.core.image.ImageMetadataSnapshot
import com.none.tom.exiferaser.core.image.ImageProcessingSummary
import com.squareup.wire.AnyMessage

val testUri = ContentResolver.SCHEME_CONTENT.toUri()
val testUris = listOf(testUri)
val testUris2 = listOf(testUri, testUri)
val testImageSources = mutableListOf(
    AnyMessage.pack(ImageFileProto(index = 0)),
    AnyMessage.pack(ImageFilesProto(index = 1)),
    AnyMessage.pack(ImageDirectoryProto(index = 2)),
    AnyMessage.pack(CameraProto(index = 3))
)
val testImagesSelection = testUris.map { uri ->
    UserImageSelectionProto(image_path = uri.toString())
}
val testImageMetadataSnapshot = ImageMetadataSnapshot(
    isIccProfileContained = true,
    isExifContained = true,
    isPhotoshopImageResourcesContained = true,
    isXmpContained = true,
    isExtendedXmpContained = true
)
val testImageProcessingSummary = ImageProcessingSummary(
    displayName = "test.jpg",
    extension = EXTENSION_JPEG,
    mimeType = MIME_TYPE_JPEG,
    uri = testUri,
    isImageSaved = true,
    imageMetadataSnapshot = testImageMetadataSnapshot
)
