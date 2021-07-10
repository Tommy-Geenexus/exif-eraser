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

package com.none.tom.exiferaser.main.business

import android.content.ContentResolver
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.CameraProto
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.ImageDirectoryProto
import com.none.tom.exiferaser.ImageFileProto
import com.none.tom.exiferaser.ImageFilesProto
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.main.data.ImageSourceRepository
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.none.tom.exiferaser.update.data.UpdateRepository
import com.squareup.wire.AnyMessage
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.util.Collections
import kotlin.contracts.ExperimentalContracts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@ExperimentalContracts
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private val imageRepository = mockk<ImageRepository>()
    private val imageSourceRepository = mockk<ImageSourceRepository>()
    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val updateRepository = mockk<UpdateRepository>()

    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()
    private val testUris = listOf(testUri)

    private val testImageSources = mutableListOf(
        AnyMessage.pack(ImageFileProto(index = 0)),
        AnyMessage.pack(ImageFilesProto(index = 1)),
        AnyMessage.pack(ImageDirectoryProto(index = 2)),
        AnyMessage.pack(CameraProto(index = 3))
    )

    @Test
    fun test_prepareReadImageSources_readImageSources() = runBlockingTest {
        coEvery {
            imageSourceRepository.getImageSources()
        } returns flowOf(testImageSources)
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.runOnCreate()
        coVerify(exactly = 1) {
            imageSourceRepository.getImageSources()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(imageSourcesFetching = true)
                },
                {
                    copy(
                        imageSources = testImageSources,
                        imageSourcesFetching = false
                    )
                }
            )
        }
    }

    @Test
    fun test_prepareReorderImageSources_reorderImageSources() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        val reorderedImageSources = testImageSources.toMutableList()
        Collections.swap(reorderedImageSources, 1, 0)
        viewModel.testIntent {
            prepareReorderImageSources()
        }
        viewModel.testIntent {
            reorderImageSources(
                imageSources = testImageSources,
                oldIndex = 0,
                newIndex = 1
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(imageSourcesReorder = true)
                },
                {
                    copy(
                        imageSources = reorderedImageSources,
                        imageSourcesReordering = true,
                        imageSourcesReorder = false
                    )
                }
            )
        }
    }

    @Test
    fun test_preparePutImageSources_putImageSources() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        coEvery {
            imageSourceRepository.putImageSources(testImageSources)
        } returns true
        viewModel.testIntent {
            preparePutImageSources()
        }
        viewModel.testIntent {
            putImageSources(testImageSources)
        }
        coVerify(exactly = 1) {
            imageSourceRepository.putImageSources(testImageSources)
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(
                        imageSourcesPersisting = true,
                        imageSourcesPersisted = false,
                        imageSourcesReordering = false,
                        imageSourcesReorder = false
                    )
                },
                {
                    copy(
                        imageSourcesPersisting = false,
                        imageSourcesPersisted = true,
                        imageSourcesReordering = false,
                        imageSourcesReorder = false
                    )
                }
            )
        }
    }

    @Test
    fun test_preparePutSelection() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.testIntent {
            preparePutSelection(null)
        }
        viewModel.testIntent {
            preparePutSelection(Unit)
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(selectionPersisting = false)
                },
                {
                    copy(selectionPersisting = true)
                }
            )
        }
    }

    @Test
    fun test_putImageSelection() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        coEvery {
            selectionRepository.putSelection(
                imageUri = testUri,
                fromCamera = false
            )
        } returns true
        viewModel.testIntent {
            putImageSelection(
                imageUri = testUri,
                fromCamera = false
            )
        }
        coVerify(exactly = 1) {
            selectionRepository.putSelection(
                imageUri = testUri,
                fromCamera = false
            )
        }
        coEvery {
            selectionRepository.putSelection(
                imageUri = testUri,
                fromCamera = true
            )
        } returns true
        viewModel.testIntent {
            putImageSelection(
                imageUri = testUri,
                fromCamera = true
            )
        }
        coVerify(exactly = 1) {
            selectionRepository.putSelection(
                imageUri = testUri,
                fromCamera = true
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(
                        selectionPersisting = false
                    )
                },
                {
                    copy(
                        selectionPersisting = false
                    )
                }
            )
            postedSideEffects(
                MainSideEffect.NavigateToSelectionSavePath,
                MainSideEffect.NavigateToSelection
            )
        }
    }

    @Test
    fun test_putImagesSelection() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        coEvery {
            selectionRepository.putSelection(
                imageUris = testUris,
                intentImageUris = testUris.toTypedArray()
            )
        } returns true
        viewModel.testIntent {
            putImagesSelection(
                imageUris = testUris,
                intentImageUris = testUris.toTypedArray()
            )
        }
        coVerify(exactly = 1) {
            selectionRepository.putSelection(
                imageUris = testUris,
                intentImageUris = testUris.toTypedArray()
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(
                        selectionPersisting = false
                    )
                }
            )
            postedSideEffects(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    @Test
    fun test_putImageDirectorySelection() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        val result = AnyMessage.pack(UserImageSelectionProto(image_path = testUri.toString()))
        coEvery {
            imageRepository.packDocumentTreeToAnyMessageOrNull(testUri)
        } returns result
        coEvery {
            selectionRepository.putSelection(result)
        } returns true
        viewModel.testIntent {
            putImageDirectorySelection(treeUri = testUri)
        }
        coVerify(ordering = Ordering.SEQUENCE) {
            imageRepository.packDocumentTreeToAnyMessageOrNull(testUri)
            selectionRepository.putSelection(result)
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(
                        selectionPersisting = false
                    )
                }
            )
            postedSideEffects(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    @Test
    fun test_handleSettings() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        viewModel.testIntent {
            handleSettings()
        }
        viewModel.assert(initialState) {
            postedSideEffects(MainSideEffect.NavigateToSettings)
        }
    }

    @Test
    fun test_prepareChooseImagesOrLaunchCamera() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        viewModel.testIntent {
            prepareChooseImagesOrLaunchCamera()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(accessingPreferences = true)
                }
            )
        }
    }

    @Test
    fun test_chooseImage() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultOpenPathSuspending()
        } returns testUri
        viewModel.testIntent {
            chooseImage()
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultOpenPathSuspending()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(accessingPreferences = false)
                }
            )
            postedSideEffects(MainSideEffect.ChooseImage(openPath = testUri))
        }
    }

    @Test
    fun test_chooseImages() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultOpenPathSuspending()
        } returns testUri
        viewModel.testIntent {
            chooseImages()
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultOpenPathSuspending()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(accessingPreferences = false)
                }
            )
            postedSideEffects(MainSideEffect.ChooseImages(openPath = testUri))
        }
    }

    @Test
    fun test_chooseImageDirectory() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultOpenPathSuspending()
        } returns testUri
        viewModel.testIntent {
            chooseImageDirectory()
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultOpenPathSuspending()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(accessingPreferences = false)
                }
            )
            postedSideEffects(MainSideEffect.ChooseImageDirectory(openPath = testUri))
        }
    }

    @Test
    fun test_launchCamera() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        coEvery {
            imageRepository.getExternalPicturesFileProviderUriOrNull(
                fileProviderPackage = String.Empty,
                displayName = String.Empty
            )
        } returns testUri
        viewModel.testIntent {
            launchCamera(
                fileProviderPackage = String.Empty,
                displayName = String.Empty
            )
        }
        coVerify(exactly = 1) {
            imageRepository.getExternalPicturesFileProviderUriOrNull(
                fileProviderPackage = String.Empty,
                displayName = String.Empty
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(accessingPreferences = false)
                }
            )
            postedSideEffects(MainSideEffect.LaunchCamera(fileProviderImagePath = testUri))
        }
    }

    @Test
    fun test_handleShortcut() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        viewModel.testIntent {
            handleShortcut(String.Empty)
        }
        viewModel.assert(initialState) {
            postedSideEffects(MainSideEffect.ShortcutHandle(String.Empty))
        }
    }

    @Test
    fun test_reportShortcutUsed() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        viewModel.testIntent {
            reportShortcutUsed(String.Empty)
        }
        viewModel.assert(initialState) {
            postedSideEffects(MainSideEffect.ShortcutReportUsed(String.Empty))
        }
    }

    @Test
    fun test_handleMultiWindowMode() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(initialState)
        viewModel.testIntent {
            handleMultiWindowMode(isInMultiWindowMode = true)
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(isInMultiWindowMode = true)
                }
            )
        }
    }

    @Test
    fun test_handleReceivedImages() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.testIntent {
            handleReceivedImages(emptyList())
        }
        viewModel.testIntent {
            handleReceivedImages(listOf(testUri))
        }
        viewModel.testIntent {
            handleReceivedImages(listOf(testUri, testUri))
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                MainSideEffect.ReceivedImage(uri = testUri),
                MainSideEffect.ReceivedImages(uris = listOf(testUri, testUri))
            )
        }
    }

    @Test
    fun test_handlePasteImages() = runBlockingTest {
        val initialState = MainState()
        val viewModel = MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.testIntent {
            handlePasteImages(emptyList())
        }
        viewModel.testIntent {
            handlePasteImages(listOf(testUri))
        }
        viewModel.testIntent {
            handlePasteImages(listOf(testUri, testUri))
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                MainSideEffect.PasteImagesNone,
                MainSideEffect.PasteImages(uris = listOf(testUri)),
                MainSideEffect.PasteImages(uris = listOf(testUri, testUri))
            )
        }
    }
}
