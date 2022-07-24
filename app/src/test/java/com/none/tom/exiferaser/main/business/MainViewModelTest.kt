/*
 * Copyright (c) 2018-2022, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import android.net.Uri
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections
import kotlin.contracts.ExperimentalContracts

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
    fun test_readImageSources() = runTest {
        coEvery {
            settingsRepository.getDefaultNightMode()
        } returns flowOf(0)
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
            settingsRepository.getDefaultNightMode()
            imageSourceRepository.getImageSources()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(loading = true)
                },
                {
                    copy(
                        imageSources = testImageSources,
                        loading = false
                    )
                }
            )
            postedSideEffects(
                MainSideEffect.DefaultNightMode(0),
                MainSideEffect.ImageSourcesReadComplete
            )
        }
    }

    @Test
    fun test_reorderImageSources() = runTest {
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
            reorderImageSources(
                imageSources = testImageSources,
                oldIndex = 0,
                newIndex = 1
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(imageSources = reorderedImageSources)
                }
            )
        }
    }

    @Test
    fun test_putImageSources() = runTest {
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
            putImageSources(testImageSources)
        }
        coVerify(exactly = 1) {
            imageSourceRepository.putImageSources(testImageSources)
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                }
            )
        }
    }

    @Test
    fun test_putImageSelection() = runTest {
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
                uri = testUri,
                fromCamera = false
            )
        } returns true
        coEvery {
            settingsRepository.shouldSkipSavePathSelection()
        } returns flowOf(false)
        viewModel.testIntent {
            putImageSelection(
                uri = testUri,
                fromCamera = false
            )
        }
        coVerify(exactly = 1) {
            selectionRepository.putSelection(
                uri = testUri,
                fromCamera = false
            )
        }
        coEvery {
            selectionRepository.putSelection(
                uri = testUri,
                fromCamera = true
            )
        } returns true
        viewModel.testIntent {
            putImageSelection(
                uri = testUri,
                fromCamera = true
            )
        }
        coVerify(exactly = 1) {
            selectionRepository.putSelection(
                uri = testUri,
                fromCamera = true
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                },
                {
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                }
            )
            postedSideEffects(
                MainSideEffect.NavigateToSelectionSavePath,
                MainSideEffect.NavigateToSelection(savePath = Uri.EMPTY)
            )
        }
    }

    @Test
    fun test_putImagesSelection() = runTest {
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
                uris = testUris,
                urisFromIntent = testUris.toTypedArray()
            )
        } returns true
        coEvery {
            settingsRepository.shouldSkipSavePathSelection()
        } returns flowOf(false)
        viewModel.testIntent {
            putImagesSelection(
                uris = testUris,
                urisFromIntent = testUris.toTypedArray()
            )
        }
        coVerify(exactly = 1) {
            selectionRepository.putSelection(
                uris = testUris,
                urisFromIntent = testUris.toTypedArray()
            )
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                }
            )
            postedSideEffects(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    @Test
    fun test_putImageDirectorySelection() = runTest {
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
        val result = AnyMessage.pack(UserImageSelectionProto(image_path = testUri.toString()))
        coEvery {
            imageRepository.packDocumentTreeToAnyMessageOrNull(testUri)
        } returns result
        coEvery {
            selectionRepository.putSelection(result)
        } returns true
        coEvery {
            settingsRepository.shouldSkipSavePathSelection()
        } returns flowOf(false)
        viewModel.testIntent {
            putImageDirectorySelection(uri = testUri)
        }
        coVerify(ordering = Ordering.SEQUENCE) {
            imageRepository.packDocumentTreeToAnyMessageOrNull(testUri)
            selectionRepository.putSelection(result)
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                }
            )
            postedSideEffects(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    @Test
    fun test_handleSettings() = runTest {
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
    fun test_chooseImage() = runTest {
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
            settingsRepository.getDefaultPathOpen()
        } returns flowOf(testUri)
        viewModel.testIntent {
            chooseImage(canReorderImageSources = false)
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultPathOpen()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                }
            )
            postedSideEffects(MainSideEffect.ChooseImage(openPath = testUri))
        }
    }

    @Test
    fun test_chooseImages() = runTest {
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
            settingsRepository.getDefaultPathOpen()
        } returns flowOf(testUri)
        viewModel.testIntent {
            chooseImages(canReorderImageSources = false)
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultPathOpen()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                }
            )
            postedSideEffects(MainSideEffect.ChooseImages(openPath = testUri))
        }
    }

    @Test
    fun test_chooseImageDirectory() = runTest {
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
            settingsRepository.getDefaultPathOpen()
        } returns flowOf(testUri)
        viewModel.testIntent {
            chooseImageDirectory(canReorderImageSources = false)
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultPathOpen()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                }
            )
            postedSideEffects(MainSideEffect.ChooseImageDirectory(openPath = testUri))
        }
    }

    @Test
    fun test_chooseSelectionNavigationRoute() = runTest {
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
            settingsRepository.getDefaultPathOpen()
        } returns flowOf(testUri)
        viewModel.testIntent {
            chooseSelectionNavigationRoute(fromCamera = true)
        }
        coEvery {
            settingsRepository.shouldSkipSavePathSelection()
        } returns flowOf(true)
        coEvery {
            settingsRepository.getDefaultPathSave()
        } returns flowOf(testUri)
        coEvery {
            settingsRepository.hasPrivilegedDefaultPathSave(any())
        } returns true
        viewModel.testIntent {
            chooseSelectionNavigationRoute()
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.shouldSkipSavePathSelection()
            settingsRepository.getDefaultPathSave()
            settingsRepository.hasPrivilegedDefaultPathSave(any())
        }
        coEvery {
            settingsRepository.shouldSkipSavePathSelection()
        } returns flowOf(false)
        viewModel.testIntent {
            chooseSelectionNavigationRoute()
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                MainSideEffect.NavigateToSelection(savePath = Uri.EMPTY),
                MainSideEffect.NavigateToSelection(savePath = testUri),
                MainSideEffect.NavigateToSelectionSavePath
            )
        }
    }

    @Test
    fun test_launchCamera() = runTest {
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
                displayName = String.Empty,
                canReorderImageSources = false
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
                    copy(loading = true)
                },
                {
                    copy(loading = false)
                }
            )
            postedSideEffects(MainSideEffect.LaunchCamera(fileProviderImagePath = testUri))
        }
    }

    @Test
    fun test_handleShortcut() = runTest {
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
            postedSideEffects(MainSideEffect.Shortcut.Handle(String.Empty))
        }
    }

    @Test
    fun test_reportShortcutUsed() = runTest {
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
            postedSideEffects(MainSideEffect.Shortcut.ReportUsage(String.Empty))
        }
    }

    @Test
    fun test_handleReceivedImages() = runTest {
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
    fun test_handlePasteImages() = runTest {
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
