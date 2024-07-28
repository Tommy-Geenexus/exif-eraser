/*
 * Copyright (c) 2018-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.imageProcessingDetails.business

import android.net.Uri
import android.os.Parcelable
import com.none.tom.exiferaser.core.image.ImageMetadataSnapshot
import kotlinx.parcelize.Parcelize

sealed class ImageProcessingDetailsSideEffect : Parcelable {

    sealed class ImageSaved : ImageProcessingDetailsSideEffect() {

        @Parcelize
        data object Unsupported : ImageSaved()
    }

    sealed class Navigate : ImageProcessingDetailsSideEffect() {

        @Parcelize
        data class ToImageModifiedDetails(
            val displayName: String,
            val extension: String,
            val mimeType: String,
            val imageMetadataSnapshot: ImageMetadataSnapshot
        ) : Navigate()

        @Parcelize
        data class ToImageSavedDetails(
            val name: String
        ) : Navigate()
    }

    @Parcelize
    data class ViewImage(
        val imageUri: Uri
    ) : ImageProcessingDetailsSideEffect()
}
