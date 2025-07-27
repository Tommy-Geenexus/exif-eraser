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

package com.none.tom.exiferaser.imageProcessing.business

import android.net.Uri
import android.os.Parcelable
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.core.image.ImageProcessingSummary
import kotlinx.parcelize.Parcelize

sealed class ImageProcessingSideEffect : Parcelable {

    sealed class Handle : ImageProcessingSideEffect() {

        @Parcelize
        data class UserImagesSelection(
            val protos: List<UserImageSelectionProto>,
            val treeUri: Uri
        ) : Handle()

        @Parcelize
        data object UnsupportedSelection : Handle()
    }

    sealed class Navigate : ImageProcessingSideEffect() {

        @Parcelize
        data class ToImageProcessingDetails(
            val imageProcessingSummaries: Array<ImageProcessingSummary>
        ) : Navigate() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as ToImageProcessingDetails

                return imageProcessingSummaries.contentEquals(other.imageProcessingSummaries)
            }

            override fun hashCode(): Int = imageProcessingSummaries.contentHashCode()
        }
    }

    @Parcelize
    data class ShareImages(val imageUris: ArrayList<Uri>) : ImageProcessingSideEffect()
}
