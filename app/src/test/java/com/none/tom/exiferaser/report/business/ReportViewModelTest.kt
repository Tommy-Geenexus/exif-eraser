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

package com.none.tom.exiferaser.report.business

import android.content.ContentResolver
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.EXTENSION_JPEG
import com.none.tom.exiferaser.MIME_TYPE_JPEG
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.selection.data.Summary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@RunWith(RobolectricTestRunner::class)
class ReportViewModelTest {

    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()
    private val imageRepository = mockk<ImageRepository>()

    private val summary = Summary(
        displayName = "test.jpg",
        extension = EXTENSION_JPEG,
        mimeType = MIME_TYPE_JPEG,
        imageUri = testUri,
        imageModified = true,
        imageSaved = true,
        containsIccProfile = false,
        containsExif = true,
        containsPhotoshopImageResources = false,
        containsXmp = true,
        containsExtendedXmp = false
    )

    @Test
    fun test_handleImageSummaries() = runTest {
        ReportViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository
        ).test(
            testScope = this,
            initialState = ReportState()
        ) {
            expectInitialState()
            val summaries = listOf(summary)
            invokeIntent {
                handleImageSummaries(summaries)
            }.join()
            expectState {
                copy(imageSummaries = summaries)
            }
        }
    }

    @Test
    fun test_handleViewImage() = runTest {
        ReportViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository
        ).test(
            testScope = this,
            initialState = ReportState(imageSummaries = listOf(summary))
        ) {
            expectInitialState()
            invokeIntent {
                handleViewImage(position = 0)
            }.join()
            expectSideEffect(ReportSideEffect.ViewImage(imageUri = testUri))
        }
    }

    @Test
    fun test_handleImageModifiedDetails() = runTest {
        ReportViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository
        ).test(
            testScope = this,
            initialState = ReportState(imageSummaries = listOf(summary))
        ) {
            expectInitialState()
            invokeIntent {
                handleImageModifiedDetails(position = 0)
            }.join()
            expectSideEffect(
                ReportSideEffect.NavigateToImageModifiedDetails(
                    displayName = summary.displayName,
                    extension = summary.extension,
                    mimeType = summary.mimeType,
                    containsIccProfile = summary.containsIccProfile,
                    containsExif = summary.containsExif,
                    containsPhotoshopImageResources = summary.containsPhotoshopImageResources,
                    containsXmp = summary.containsXmp,
                    containsExtendedXmp = summary.containsExtendedXmp
                )
            )
        }
    }

    @Test
    fun test_handleImageSavedDetails() = runTest {
        ReportViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository
        ).test(
            testScope = this,
            initialState = ReportState(imageSummaries = listOf(summary))
        ) {
            expectInitialState()
            val imagePath = ContentResolver.SCHEME_CONTENT
            invokeIntent {
                coEvery {
                    imageRepository.getDocumentPathOrNull(summary.imageUri)
                } returns imagePath
                handleImageSavedDetails(position = 0)
            }.join()
            coVerify(exactly = 1) {
                imageRepository.getDocumentPathOrNull(summary.imageUri)
            }
            expectSideEffect(ReportSideEffect.NavigateToImageSavedDetails(imagePath))
        }
    }
}
