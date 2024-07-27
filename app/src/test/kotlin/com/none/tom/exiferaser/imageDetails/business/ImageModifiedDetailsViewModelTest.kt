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

package com.none.tom.exiferaser.imageDetails.business

import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.core.image.ImageMetadataSnapshot
import com.none.tom.exiferaser.core.util.EXTENSION_JPEG
import com.none.tom.exiferaser.core.util.MIME_TYPE_JPEG
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test

class ImageModifiedDetailsViewModelTest {

    @Test
    fun test_handleImageDetails() = runTest {
        ImageMetadataDetailsViewModel(SavedStateHandle()).test(
            testScope = this,
            initialState = ImageMetadataDetailsState()
        ) {
            expectInitialState()
            val mimeType = MIME_TYPE_JPEG
            val extension = EXTENSION_JPEG
            val imageMetadataSnapshot = ImageMetadataSnapshot(
                isIccProfileContained = true,
                isExifContained = true,
                isPhotoshopImageResourcesContained = true,
                isXmpContained = true,
                isExtendedXmpContained = true
            )
            containerHost.handleImageDetails(
                extension = extension,
                mimeType = mimeType,
                imageMetadataSnapshot = imageMetadataSnapshot
            ).join()
            expectState {
                copy(
                    extension = extension,
                    mimeType = mimeType,
                    imageMetadataSnapshot = imageMetadataSnapshot
                )
            }
        }
    }
}
