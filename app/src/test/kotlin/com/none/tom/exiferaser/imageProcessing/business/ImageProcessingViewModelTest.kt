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

package com.none.tom.exiferaser.imageProcessing.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.core.image.ImageProcessingProgress
import com.none.tom.exiferaser.core.image.ImageProcessingStep
import com.none.tom.exiferaser.core.roboelectric.ROBOELECTRIC_BUILD_VERSION_CODE
import com.none.tom.exiferaser.core.util.testImageProcessingSummary
import com.none.tom.exiferaser.core.util.testImagesSelection
import com.none.tom.exiferaser.core.util.testUri
import com.none.tom.exiferaser.core.util.testUris
import com.none.tom.exiferaser.imageProcessing.data.ImageProcessingRepository
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.settings.data.SettingsRepository
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [ROBOELECTRIC_BUILD_VERSION_CODE])
@RunWith(RobolectricTestRunner::class)
class ImageProcessingViewModelTest {

    private val imageProcessingRepository = mockk<ImageProcessingRepository>()
    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()

    @Test
    fun test_handleUserImagesSelection() = runTest {
        ImageProcessingViewModel(
            savedStateHandle = SavedStateHandle(),
            imageProcessingRepository = imageProcessingRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = ImageProcessingState()
        ) {
            expectInitialState()
            coEvery {
                settingsRepository.getDefaultDisplayNameSuffix()
            } returns ""
            coEvery {
                settingsRepository.isAutoDeleteEnabled()
            } returns false
            coEvery {
                settingsRepository.isPreserveOrientationEnabled()
            } returns false
            coEvery {
                settingsRepository.isRandomizeFileNamesEnabled()
            } returns false
            coEvery {
                settingsRepository.isShareByDefaultEnabled()
            } returns true
            val displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix()
            val isAutoDeleteEnabled = settingsRepository.isAutoDeleteEnabled()
            val isPreserveOrientationEnabled = settingsRepository.isPreserveOrientationEnabled()
            val isRandomizeFileNamesEnabled = settingsRepository.isRandomizeFileNamesEnabled()
            coEvery {
                imageProcessingRepository.removeMetadata(
                    protos = testImagesSelection,
                    treeUri = Uri.EMPTY,
                    displayNameSuffix = displayNameSuffix,
                    isAutoDeleteEnabled = isAutoDeleteEnabled,
                    isPreserveOrientationEnabled = isPreserveOrientationEnabled,
                    isRandomizeFileNamesEnabled = isRandomizeFileNamesEnabled
                )
            } returns flow {
                emit(
                    ImageProcessingStep.FinishedSingle(
                        imageProcessingSummary = testImageProcessingSummary,
                        ImageProcessingProgress(displayValue = 50)
                    )
                )
                emit(
                    ImageProcessingStep.FinishedSingle(
                        imageProcessingSummary = testImageProcessingSummary,
                        ImageProcessingProgress(displayValue = 100)
                    )
                )
                emit(ImageProcessingStep.FinishedBulk)
            }
            containerHost
                .handleUserImagesSelection(protos = testImagesSelection, treeUri = Uri.EMPTY)
                .join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.isAutoDeleteEnabled()
                settingsRepository.isPreserveOrientationEnabled()
                settingsRepository.isRandomizeFileNamesEnabled()
                settingsRepository.getDefaultDisplayNameSuffix()
                imageProcessingRepository.removeMetadata(
                    protos = testImagesSelection,
                    treeUri = Uri.EMPTY,
                    displayNameSuffix = displayNameSuffix,
                    isAutoDeleteEnabled = isAutoDeleteEnabled,
                    isPreserveOrientationEnabled = isPreserveOrientationEnabled,
                    isRandomizeFileNamesEnabled = isRandomizeFileNamesEnabled
                )
                settingsRepository.isShareByDefaultEnabled()
            }
            expectState {
                copy(
                    imageImageProcessingStep = ImageProcessingStep.FinishedSingle(
                        imageProcessingSummary = testImageProcessingSummary,
                        ImageProcessingProgress(displayValue = 50)
                    ),
                    imageProcessingSummaries = listOf(testImageProcessingSummary),
                    uris = listOf(testImageProcessingSummary.uri),
                    imagesWithMetadataCount = 1,
                    imagesSavedCount = 1,
                    imagesProcessedCount = 1,
                    progress = ImageProcessingProgress(displayValue = 50)
                )
            }
            expectState {
                copy(
                    imageImageProcessingStep = ImageProcessingStep.FinishedSingle(
                        imageProcessingSummary = testImageProcessingSummary,
                        ImageProcessingProgress(displayValue = 100)
                    ),
                    imageProcessingSummaries = listOf(
                        testImageProcessingSummary,
                        testImageProcessingSummary
                    ),
                    uris = listOf(testImageProcessingSummary.uri, testImageProcessingSummary.uri),
                    imagesWithMetadataCount = 2,
                    imagesSavedCount = 2,
                    imagesProcessedCount = 2,
                    progress = ImageProcessingProgress(displayValue = 100)
                )
            }
            expectState {
                copy(imageImageProcessingStep = ImageProcessingStep.FinishedBulk)
            }
            expectSideEffect(
                ImageProcessingSideEffect.ShareImages(
                    imageUris = arrayListOf(
                        testImageProcessingSummary.uri,
                        testImageProcessingSummary.uri
                    )
                )
            )
        }
    }

    @Test
    fun test_handleUnsupportedSelection() = runTest {
        ImageProcessingViewModel(
            savedStateHandle = SavedStateHandle(),
            imageProcessingRepository = imageProcessingRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = ImageProcessingState()
        ) {
            expectInitialState()
            containerHost.handleUnsupportedSelection().join()
            expectState {
                copy(imageImageProcessingStep = ImageProcessingStep.FinishedBulk)
            }
        }
    }

    @Test
    fun test_readSelection() = runTest {
        ImageProcessingViewModel(
            savedStateHandle = SavedStateHandle(),
            imageProcessingRepository = imageProcessingRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = ImageProcessingState()
        ) {
            expectInitialState()
            coEvery {
                selectionRepository.getSelection(any())
            } returns emptyList()
            containerHost.readSelection(fromIndex = 0, treeUri = testUri).join()
            coEvery {
                selectionRepository.getSelection(any())
            } returns testImagesSelection
            containerHost.readSelection(fromIndex = 0, treeUri = testUri).join()
            coVerify(exactly = 2) {
                selectionRepository.getSelection(any())
            }
            expectSideEffect(ImageProcessingSideEffect.Handle.UnsupportedSelection)
            expectSideEffect(
                ImageProcessingSideEffect.Handle.UserImagesSelection(
                    protos = testImagesSelection,
                    treeUri = testUri
                )
            )
        }
    }

    @Test
    fun test_shareImages() = runTest {
        ImageProcessingViewModel(
            savedStateHandle = SavedStateHandle(),
            imageProcessingRepository = imageProcessingRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = ImageProcessingState(
                imageProcessingSummaries = listOf(testImageProcessingSummary)
            )
        ) {
            expectInitialState()
            containerHost.shareImages()
            expectSideEffect(ImageProcessingSideEffect.ShareImages(ArrayList(testUris)))
        }
    }
}
