/*
 * Copyright (c) 2018-2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import com.none.tom.exiferaser.selection.data.Summary
import kotlin.contracts.ExperimentalContracts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class ReportViewModelTest {

    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()

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
    fun test_handleImageSummaries() = runBlockingTest {
        val initialState = ReportState()
        val viewModel = ReportViewModel(savedStateHandle = SavedStateHandle()).test(initialState)
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
    fun test_handleViewImage() = runBlockingTest {
        val initialState = ReportState(imageSummaries = listOf(summary))
        val viewModel = ReportViewModel(savedStateHandle = SavedStateHandle()).test(initialState)
        viewModel.testIntent {
            handleViewImage(position = 0)
        }
        viewModel.assert(initialState) {
            postedSideEffects(ReportSideEffect.ViewImage(imageUri = testUri))
        }
    }

    @ExperimentalContracts
    @Test
    fun test_handleImageDetails() = runBlockingTest {
        val initialState = ReportState(imageSummaries = listOf(summary))
        val viewModel = ReportViewModel(savedStateHandle = SavedStateHandle()).test(initialState)
        viewModel.testIntent {
            handleImageDetails(position = 0)
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                ReportSideEffect.NavigateToDetails(
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
}
