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

package com.none.tom.exiferaser.details.business

import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.EXTENSION_JPEG
import com.none.tom.exiferaser.MIME_TYPE_JPEG
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test

class ImageModifiedDetailsViewModelTest {

    @Test
    fun test_handleImageDetails() = runTest {
        ImageModifiedDetailsViewModel(SavedStateHandle()).test(
            testScope = this,
            initialState = ImageModifiedDetailsState()
        ) {
            expectInitialState()
            invokeIntent {
                handleImageDetails(
                    extension = EXTENSION_JPEG,
                    mimeType = MIME_TYPE_JPEG,
                    containsIccProfile = true,
                    containsExif = true,
                    containsPhotoshopImageResources = true,
                    containsXmp = true,
                    containsExtendedXmp = true
                )
            }.join()
            expectState {
                copy(
                    extension = EXTENSION_JPEG,
                    mimeType = MIME_TYPE_JPEG,
                    containsIccProfile = true,
                    containsExif = true,
                    containsPhotoshopImageResources = true,
                    containsXmp = true,
                    containsExtendedXmp = true
                )
            }
        }
    }
}
