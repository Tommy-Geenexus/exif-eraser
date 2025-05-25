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

package com.none.tom.exiferaser.imageProcessingDetails.business

import android.content.ContentResolver
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.core.roboelectric.ROBOELECTRIC_BUILD_VERSION_CODE
import com.none.tom.exiferaser.core.util.testImageMetadataSnapshot
import com.none.tom.exiferaser.core.util.testImageProcessingSummary
import com.none.tom.exiferaser.core.util.testUri
import com.none.tom.exiferaser.imageProcessing.data.ImageProcessingRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [ROBOELECTRIC_BUILD_VERSION_CODE])
@RunWith(RobolectricTestRunner::class)
class ImageProcessingDetailsViewModelTest {

    private val imageProcessingRepository = mockk<ImageProcessingRepository>()

    @Test
    fun test_handleImageSummaries() = runTest {
        ImageProcessingDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            imageProcessingRepository = imageProcessingRepository
        ).test(
            testScope = this,
            initialState = ImageProcessingDetailsState()
        ) {
            val summaries = listOf(testImageProcessingSummary)
            containerHost.handleImageProcessingSummaries(summaries).join()
            expectState {
                copy(imageProcessingSummaries = summaries)
            }
        }
    }

    @Test
    fun test_handleViewImage() = runTest {
        ImageProcessingDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            imageProcessingRepository = imageProcessingRepository
        ).test(
            testScope = this,
            initialState = ImageProcessingDetailsState(
                imageProcessingSummaries = listOf(testImageProcessingSummary)
            )
        ) {
            containerHost.handleViewImage(position = 0).join()
            expectSideEffect(ImageProcessingDetailsSideEffect.ViewImage(imageUri = testUri))
        }
    }

    @Test
    fun test_handleImageModifiedDetails() = runTest {
        ImageProcessingDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            imageProcessingRepository = imageProcessingRepository
        ).test(
            testScope = this,
            initialState = ImageProcessingDetailsState(
                imageProcessingSummaries = listOf(testImageProcessingSummary)
            )
        ) {
            containerHost.handleImageModifiedDetails(position = 0).join()
            expectSideEffect(
                ImageProcessingDetailsSideEffect.Navigate.ToImageModifiedDetails(
                    displayName = testImageProcessingSummary.displayName,
                    extension = testImageProcessingSummary.extension,
                    mimeType = testImageProcessingSummary.mimeType,
                    imageMetadataSnapshot = testImageMetadataSnapshot
                )
            )
        }
    }

    @Test
    fun test_handleImageSavedDetails() = runTest {
        ImageProcessingDetailsViewModel(
            savedStateHandle = SavedStateHandle(),
            imageProcessingRepository = imageProcessingRepository
        ).test(
            testScope = this,
            initialState = ImageProcessingDetailsState(
                imageProcessingSummaries = listOf(testImageProcessingSummary)
            )
        ) {
            val imagePath = ContentResolver.SCHEME_CONTENT
            coEvery {
                imageProcessingRepository.getLastDocumentPathSegment(testImageProcessingSummary.uri)
            } returns Result.success(imagePath)
            containerHost.handleImageSavedDetails(position = 0).join()
            coVerify(exactly = 1) {
                imageProcessingRepository.getLastDocumentPathSegment(testImageProcessingSummary.uri)
            }
            expectSideEffect(
                ImageProcessingDetailsSideEffect.Navigate.ToImageSavedDetails(imagePath)
            )
        }
    }
}
