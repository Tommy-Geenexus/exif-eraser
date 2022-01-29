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

package com.none.tom.exiferaser.selection.business

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.EXTENSION_JPEG
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.MIME_TYPE_JPEG
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.HISTORY_SIZE
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.selection.data.Result
import com.none.tom.exiferaser.selection.data.Summary
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.squareup.wire.AnyMessage
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isTrue
import kotlin.contracts.ExperimentalContracts

@ExperimentalCoroutinesApi
@ExperimentalContracts
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class SelectionViewModelTest {

    private val imageRepository = mockk<ImageRepository>()
    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()

    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()
    private val testUris = listOf(testUri)

    private val testImageSelection =
        AnyMessage.pack(UserImageSelectionProto(image_path = String.Empty))
    private val testImagesSelection =
        AnyMessage.pack(
            UserImagesSelectionProto(
                user_images_selection = listOf(
                    UserImageSelectionProto(image_path = String.Empty),
                    UserImageSelectionProto(image_path = String.Empty)
                )
            )
        )

    private val testSummary = Summary(
        displayName = "test.jpg",
        extension = EXTENSION_JPEG,
        mimeType = MIME_TYPE_JPEG,
        imageUri = testUri,
        imageModified = true,
        imageSaved = true,
        containsIccProfile = true,
        containsExif = true,
        containsPhotoshopImageResources = true,
        containsXmp = true,
        containsExtendedXmp = false
    )

    @Test
    fun test_readSelection() = runTest {
        coEvery {
            selectionRepository.getSelection(dropFirstN = 0)
        } returns flowOf(testImageSelection)
        val initialState = SelectionState()
        val viewModel = SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.runOnCreate()
        coVerify(exactly = 1) {
            selectionRepository.getSelection(dropFirstN = 0)
        }
        viewModel.assert(initialState) {
            postedSideEffects(SelectionSideEffect.ReadComplete(testImageSelection))
        }
    }

    @Test
    fun test_prepareReport() = runTest {
        val initialState = SelectionState(
            imageSummaries = arrayOf(testSummary)
        )
        val viewModel = SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(initialState)
        viewModel.testIntent {
            prepareReport()
        }
        viewModel.assert(initialState) {
            postedSideEffects(SelectionSideEffect.PrepareReport(listOf(testSummary)))
        }
    }

    @Test
    fun test_shareImages() = runTest {
        val initialState = SelectionState(
            imageSummaries = arrayOf(testSummary)
        )
        val viewModel = SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(initialState)
        viewModel.testIntent {
            shareImages()
        }
        viewModel.assert(initialState) {
            postedSideEffects(SelectionSideEffect.ShareImages(ArrayList(testUris)))
        }
    }

    @Test
    fun test_shareImagesByDefault() = runTest {
        val initialState = SelectionState(
            imageSummaries = arrayOf(testSummary)
        )
        val viewModel = SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.shouldShareByDefault()
        } returns flowOf(true)
        viewModel.testIntent {
            shareImagesByDefault()
        }
        coVerify(exactly = 1) {
            settingsRepository.shouldShareByDefault()
        }
    }

    @Test
    fun test_hasSavedImages() = runTest {
        val initialState = SelectionState(
            imagesSaved = 1
        )
        val viewModel = SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(initialState)
        viewModel.testIntent {
            expectThat(hasSavedImages()).isTrue()
        }
    }

    @Test
    fun test_handleUserImageSelection() = runTest {
        val initialState = SelectionState()
        val viewModel = SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultDisplayNameSuffix()
        } returns flowOf(String.Empty)
        coEvery {
            settingsRepository.shouldPreserveOrientation()
        } returns flowOf(false)
        val displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix().first()
        val preserveOrientation = settingsRepository.shouldPreserveOrientation().first()
        coEvery {
            imageRepository.removeMetadataSingle(
                selection = testImageSelection,
                treeUri = Uri.EMPTY,
                displayNameSuffix = displayNameSuffix,
                preserveOrientation = preserveOrientation
            )
        } returns flow {
            emit(Result.Report(summary = testSummary))
            emit(Result.Handled(progress = PROGRESS_MAX))
            emit(Result.HandledAll)
        }
        viewModel.testIntent {
            handleUserImageSelection(
                selection = testImageSelection,
                treeUri = Uri.EMPTY
            )
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.shouldPreserveOrientation()
            settingsRepository.getDefaultDisplayNameSuffix()
            imageRepository.removeMetadataSingle(
                selection = testImageSelection,
                treeUri = Uri.EMPTY,
                displayNameSuffix = displayNameSuffix,
                preserveOrientation = preserveOrientation
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(
                        imageResult = Result.Report(summary = testSummary),
                        imageSummaries = arrayOfNulls<Summary>(size = HISTORY_SIZE).apply {
                            set(0, testSummary)
                        },
                        imageUris = arrayOfNulls<Uri>(size = HISTORY_SIZE).apply {
                            set(0, testSummary.imageUri)
                        },
                        imagesModified = 1,
                        imagesSaved = 1
                    )
                },
                {
                    copy(
                        imageResult = Result.Handled(progress = PROGRESS_MAX),
                        imagesTotal = 1,
                        progress = PROGRESS_MAX
                    )
                },
                {
                    copy(handledAll = true)
                }
            )
            postedSideEffects(SelectionSideEffect.SelectionHandled)
        }
    }

    @Test
    fun test_handleUserImagesSelection() = runTest {
        val initialState = SelectionState()
        val viewModel = SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultDisplayNameSuffix()
        } returns flowOf(String.Empty)
        coEvery {
            settingsRepository.shouldPreserveOrientation()
        } returns flowOf(false)
        val displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix().first()
        val preserveOrientation = settingsRepository.shouldPreserveOrientation().first()
        coEvery {
            imageRepository.removeMetadataBulk(
                selection = testImagesSelection,
                treeUri = Uri.EMPTY,
                displayNameSuffix = displayNameSuffix,
                preserveOrientation = preserveOrientation
            )
        } returns flow {
            emit(Result.Report(summary = testSummary))
            emit(Result.Handled(progress = PROGRESS_MAX / 2))
            emit(Result.Report(summary = testSummary))
            emit(Result.Handled(progress = PROGRESS_MAX))
            emit(Result.HandledAll)
        }
        viewModel.testIntent {
            handleUserImagesSelection(
                selection = testImagesSelection,
                treeUri = Uri.EMPTY
            )
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.shouldPreserveOrientation()
            settingsRepository.getDefaultDisplayNameSuffix()
            imageRepository.removeMetadataBulk(
                selection = testImagesSelection,
                treeUri = Uri.EMPTY,
                displayNameSuffix = displayNameSuffix,
                preserveOrientation = preserveOrientation
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(
                        imageResult = Result.Report(summary = testSummary),
                        imageSummaries = arrayOfNulls<Summary>(size = HISTORY_SIZE).apply {
                            set(0, testSummary)
                            set(1, testSummary)
                        },
                        imageUris = arrayOfNulls<Uri>(size = HISTORY_SIZE).apply {
                            set(0, testSummary.imageUri)
                            set(1, testSummary.imageUri)
                        },
                        imagesModified = 1,
                        imagesSaved = 1
                    )
                },
                {
                    copy(
                        imageResult = Result.Handled(progress = PROGRESS_MAX / 2),
                        imagesTotal = 1,
                        progress = PROGRESS_MAX / 2
                    )
                },
                {
                    copy(
                        imageResult = Result.Report(summary = testSummary),
                        imageSummaries = arrayOfNulls<Summary>(size = HISTORY_SIZE).apply {
                            set(0, testSummary)
                            set(1, testSummary)
                        },
                        imageUris = arrayOfNulls<Uri>(size = HISTORY_SIZE).apply {
                            set(0, testSummary.imageUri)
                            set(1, testSummary.imageUri)
                        },
                        imagesModified = 2,
                        imagesSaved = 2
                    )
                },
                {
                    copy(
                        imageResult = Result.Handled(progress = PROGRESS_MAX),
                        imagesTotal = 2,
                        progress = PROGRESS_MAX
                    )
                },
                {
                    copy(handledAll = true)
                }
            )
            postedSideEffects(SelectionSideEffect.SelectionHandled)
        }
    }

    @Test
    fun test_handleUnsupportedSelection() = runTest {
        val initialState = SelectionState()
        val viewModel = SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(initialState)
        viewModel.testIntent {
            handleUnsupportedSelection()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(handledAll = true)
                }
            )
        }
    }
}
