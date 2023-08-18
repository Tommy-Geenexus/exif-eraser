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

package com.none.tom.exiferaser.selection.business

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.EXTENSION_JPEG
import com.none.tom.exiferaser.MIME_TYPE_JPEG
import com.none.tom.exiferaser.PROGRESS_MAX
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.selection.data.Result
import com.none.tom.exiferaser.selection.data.Summary
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.squareup.wire.AnyMessage
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@RunWith(RobolectricTestRunner::class)
class SelectionViewModelTest {

    private val imageRepository = mockk<ImageRepository>()
    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()

    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()
    private val testUris = listOf(testUri)

    private val testImageSelection =
        AnyMessage.pack(UserImageSelectionProto(image_path = ""))
    private val testImagesSelection =
        AnyMessage.pack(
            UserImagesSelectionProto(
                user_images_selection = listOf(
                    UserImageSelectionProto(image_path = ""),
                    UserImageSelectionProto(image_path = "")
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
        SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SelectionState()
        ) {
            expectInitialState()
            invokeIntent {
                readSelection(dropFirstN = 0)
            }.join()
            coVerify(exactly = 1) {
                selectionRepository.getSelection(dropFirstN = 0)
            }
            expectSideEffect(SelectionSideEffect.ReadComplete(testImageSelection))
        }
    }

    @Test
    fun test_shareImages() = runTest {
        SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SelectionState(imageSummaries = listOf(testSummary))
        ) {
            expectInitialState()
            invokeIntent {
                shareImages()
            }
            expectSideEffect(SelectionSideEffect.ShareImages(ArrayList(testUris)))
        }
    }

    @Test
    fun test_shareImagesByDefault() = runTest {
        SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SelectionState(imageSummaries = listOf(testSummary))
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.shouldShareByDefault()
                } returns flowOf(true)
                shareImagesByDefault()
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.shouldShareByDefault()
            }
            expectSideEffect(
                SelectionSideEffect.ShareImages(imageUris = arrayListOf(testSummary.imageUri))
            )
        }
    }

    @Test
    fun test_handleUserImageSelection() = runTest {
        SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SelectionState()
        ) {
            expectInitialState()
            coEvery {
                settingsRepository.getDefaultDisplayNameSuffix()
            } returns flowOf("")
            coEvery {
                settingsRepository.shouldAutoDelete()
            } returns flowOf(false)
            coEvery {
                settingsRepository.shouldPreserveOrientation()
            } returns flowOf(false)
            coEvery {
                settingsRepository.shouldRandomizeFileNames()
            } returns flowOf(false)
            val displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix().first()
            val autoDelete = settingsRepository.shouldAutoDelete().first()
            val preserveOrientation = settingsRepository.shouldPreserveOrientation().first()
            val randomizeFileNames = settingsRepository.shouldRandomizeFileNames().first()
            invokeIntent {
                coEvery {
                    imageRepository.removeMetadataSingle(
                        selection = testImageSelection,
                        treeUri = Uri.EMPTY,
                        displayNameSuffix = displayNameSuffix,
                        autoDelete = autoDelete,
                        preserveOrientation = preserveOrientation,
                        randomizeFileNames = randomizeFileNames
                    )
                } returns flow {
                    emit(Result.Report(summary = testSummary))
                    emit(Result.Handled(progress = PROGRESS_MAX))
                    emit(Result.HandledAll)
                }
                handleUserImageSelection(
                    selection = testImageSelection,
                    treeUri = Uri.EMPTY
                )
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.shouldAutoDelete()
                settingsRepository.shouldPreserveOrientation()
                settingsRepository.shouldRandomizeFileNames()
                settingsRepository.getDefaultDisplayNameSuffix()
                imageRepository.removeMetadataSingle(
                    selection = testImageSelection,
                    treeUri = Uri.EMPTY,
                    displayNameSuffix = displayNameSuffix,
                    autoDelete = autoDelete,
                    preserveOrientation = preserveOrientation,
                    randomizeFileNames = randomizeFileNames
                )
            }
            expectState {
                copy(
                    imageResult = Result.Report(summary = testSummary),
                    imageSummaries = listOf(testSummary),
                    imageUris = listOf(testSummary.imageUri),
                    imagesModified = 1,
                    imagesSaved = 1
                )
            }
            expectState {
                copy(
                    imageResult = Result.Handled(progress = PROGRESS_MAX),
                    imagesTotal = 1,
                    progress = PROGRESS_MAX
                )
            }
            expectState {
                copy(handledAll = true)
            }
            expectSideEffect(SelectionSideEffect.SelectionHandled)
        }
    }

    @Test
    fun test_handleUserImagesSelection() = runTest {
        SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SelectionState()
        ) {
            expectInitialState()
            coEvery {
                settingsRepository.getDefaultDisplayNameSuffix()
            } returns flowOf("")
            coEvery {
                settingsRepository.shouldAutoDelete()
            } returns flowOf(false)
            coEvery {
                settingsRepository.shouldPreserveOrientation()
            } returns flowOf(false)
            coEvery {
                settingsRepository.shouldRandomizeFileNames()
            } returns flowOf(false)
            val displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix().first()
            val autoDelete = settingsRepository.shouldAutoDelete().first()
            val preserveOrientation = settingsRepository.shouldPreserveOrientation().first()
            val randomizeFileNames = settingsRepository.shouldRandomizeFileNames().first()
            invokeIntent {
                coEvery {
                    imageRepository.removeMetadataBulk(
                        selection = testImagesSelection,
                        treeUri = Uri.EMPTY,
                        displayNameSuffix = displayNameSuffix,
                        autoDelete = autoDelete,
                        preserveOrientation = preserveOrientation,
                        randomizeFileNames = randomizeFileNames
                    )
                } returns flow {
                    emit(Result.Report(summary = testSummary))
                    emit(Result.Handled(progress = PROGRESS_MAX / 2))
                    emit(Result.Report(summary = testSummary))
                    emit(Result.Handled(progress = PROGRESS_MAX))
                    emit(Result.HandledAll)
                }
                handleUserImagesSelection(
                    selection = testImagesSelection,
                    treeUri = Uri.EMPTY
                )
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.shouldAutoDelete()
                settingsRepository.shouldPreserveOrientation()
                settingsRepository.shouldRandomizeFileNames()
                settingsRepository.getDefaultDisplayNameSuffix()
                imageRepository.removeMetadataBulk(
                    selection = testImagesSelection,
                    treeUri = Uri.EMPTY,
                    displayNameSuffix = displayNameSuffix,
                    autoDelete = autoDelete,
                    preserveOrientation = preserveOrientation,
                    randomizeFileNames = randomizeFileNames
                )
            }
            expectState {
                copy(
                    imageResult = Result.Report(summary = testSummary),
                    imageSummaries = listOf(testSummary),
                    imageUris = listOf(testSummary.imageUri),
                    imagesModified = 1,
                    imagesSaved = 1
                )
            }
            expectState {
                copy(
                    imageResult = Result.Handled(progress = PROGRESS_MAX / 2),
                    imagesTotal = 1,
                    progress = PROGRESS_MAX / 2
                )
            }
            expectState {
                copy(
                    imageResult = Result.Report(summary = testSummary),
                    imageSummaries = listOf(testSummary, testSummary),
                    imageUris = listOf(testSummary.imageUri, testSummary.imageUri),
                    imagesModified = 2,
                    imagesSaved = 2
                )
            }
            expectState {
                copy(
                    imageResult = Result.Handled(progress = PROGRESS_MAX),
                    imagesTotal = 2,
                    progress = PROGRESS_MAX
                )
            }
            expectState {
                copy(handledAll = true)
            }
            expectSideEffect(SelectionSideEffect.SelectionHandled)
        }
    }

    @Test
    fun test_handleUnsupportedSelection() = runTest {
        SelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SelectionState()
        ) {
            expectInitialState()
            invokeIntent {
                handleUnsupportedSelection()
            }.join()
            expectState {
                copy(handledAll = true)
            }
        }
    }
}
