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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
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

    @ExperimentalContracts
    @Test
    fun test_handleImageSummaries() = runTest {
        val initialState = ReportState()
        val viewModel = ReportViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository
        ).test(initialState)
        val summaries = listOf(summary)
        viewModel.testIntent {
            handleImageSummaries(summaries)
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(imageSummaries = summaries)
                }
            )
        }
    }

    @ExperimentalContracts
    @Test
    fun test_handleViewImage() = runTest {
        val initialState = ReportState(imageSummaries = listOf(summary))
        val viewModel = ReportViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository
        ).test(initialState)
        viewModel.testIntent {
            handleViewImage(position = 0)
        }
        viewModel.assert(initialState) {
            postedSideEffects(ReportSideEffect.ViewImage(imageUri = testUri))
        }
    }

    @ExperimentalContracts
    @Test
    fun test_handleImageModifiedDetails() = runTest {
        val initialState = ReportState(imageSummaries = listOf(summary))
        val viewModel = ReportViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository
        ).test(initialState)
        viewModel.testIntent {
            handleImageModifiedDetails(position = 0)
        }
        viewModel.assert(initialState) {
            postedSideEffects(
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

    @ExperimentalContracts
    @Test
    fun test_handleImageSavedDetails() = runTest {
        val initialState = ReportState(imageSummaries = listOf(summary))
        val viewModel = ReportViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository
        ).test(initialState)
        val imagePath = ContentResolver.SCHEME_CONTENT
        coEvery {
            imageRepository.getDocumentPathOrNull(summary.imageUri)
        } returns imagePath
        viewModel.testIntent {
            handleImageSavedDetails(position = 0)
        }
        coVerify(exactly = 1) {
            imageRepository.getDocumentPathOrNull(summary.imageUri)
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                ReportSideEffect.NavigateToImageSavedDetails(imagePath)
            )
        }
    }
}
