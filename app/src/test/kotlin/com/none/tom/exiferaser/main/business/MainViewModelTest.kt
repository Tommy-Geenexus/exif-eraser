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

package com.none.tom.exiferaser.main.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.core.image.supportedImageFormats
import com.none.tom.exiferaser.core.roboelectric.ROBOELECTRIC_BUILD_VERSION_CODE
import com.none.tom.exiferaser.core.util.DEFAULT_NIGHT_MODE
import com.none.tom.exiferaser.core.util.testImageSources
import com.none.tom.exiferaser.core.util.testUri
import com.none.tom.exiferaser.core.util.testUris
import com.none.tom.exiferaser.core.util.testUris2
import com.none.tom.exiferaser.main.data.ImageSourcesRepository
import com.none.tom.exiferaser.main.data.MainRepository
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.settings.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Collections

@Config(sdk = [ROBOELECTRIC_BUILD_VERSION_CODE])
@RunWith(RobolectricTestRunner::class)
class MainViewModelTest {

    private val imageSourcesRepository = mockk<ImageSourcesRepository>()
    private val mainRepository = mockk<MainRepository>()
    private val selectionRepository = mockk<SelectionRepository>()
    private val settingsRepository = mockk<SettingsRepository>()

    @Test
    fun test_chooseImage() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                settingsRepository.isLegacyImageSelectionEnabled()
            } returns false
            containerHost.chooseImage().join()
            coVerify(exactly = 1) {
                settingsRepository.isLegacyImageSelectionEnabled()
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(
                MainSideEffect.ImageSources.Image(
                    supportedMimeTypes = supportedImageFormats
                        .map { f -> f.mimeType }
                        .toTypedArray(),
                    isLegacyImageSelectionEnabled = false
                )
            )
        }
    }

    @Test
    fun test_chooseImages() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                settingsRepository.isLegacyImageSelectionEnabled()
            } returns false
            containerHost.chooseImages().join()
            coVerify(exactly = 1) {
                settingsRepository.isLegacyImageSelectionEnabled()
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(
                MainSideEffect.ImageSources.Images(
                    supportedMimeTypes = supportedImageFormats
                        .map { f -> f.mimeType }
                        .toTypedArray(),
                    isLegacyImageSelectionEnabled = false
                )
            )
        }
    }

    @Test
    fun test_chooseImageDirectory() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.success(testUri)
            containerHost.chooseImageDirectory().join()
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.failure(Exception(""))
            containerHost.chooseImageDirectory().join()
            coVerify(exactly = 2) {
                settingsRepository.getPrivilegedDefaultOpenPath()
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.ImageSources.ImageDirectory.Success(testUri))
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.ImageSources.ImageDirectory.Failure)
        }
    }

    @Test
    fun test_chooseSelectionNavigationRoute() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            containerHost.chooseSelectionNavigationRoute(isFromCamera = true).join()
            coEvery {
                settingsRepository.isSkipSavePathSelectionEnabled()
            } returns true
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.success(testUri)
            containerHost.chooseSelectionNavigationRoute(isFromCamera = false).join()
            coEvery {
                settingsRepository.isSkipSavePathSelectionEnabled()
            } returns false
            containerHost.chooseSelectionNavigationRoute(isFromCamera = false).join()
            expectSideEffect(MainSideEffect.Navigate.ToSelection(savePath = Uri.EMPTY))
            expectSideEffect(MainSideEffect.Navigate.ToSelection(savePath = testUri))
            expectSideEffect(MainSideEffect.Navigate.ToSelectionSavePath)
        }
    }

    @Test
    fun test_deleteCameraImages() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                mainRepository.deleteCameraImages()
            } returns Result.success(Unit)
            containerHost.deleteCameraImages().join()
            coEvery {
                mainRepository.deleteCameraImages()
            } returns Result.failure(Exception(""))
            containerHost.deleteCameraImages().join()
            coVerify(exactly = 2) {
                mainRepository.deleteCameraImages()
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Images.Delete.Success)
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Images.Delete.Failure)
        }
    }

    @Test
    fun test_handleDeleteCameraImages() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            containerHost.handleDeleteCameraImages()
            expectSideEffect(MainSideEffect.Navigate.ToDeleteCameraImages)
        }
    }

    @Test
    fun test_handleHelp() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            containerHost.handleHelp()
            expectSideEffect(MainSideEffect.Navigate.ToHelp)
        }
    }

    @Test
    fun test_handlePasteImages() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                mainRepository.getPrimaryClipImageUris()
            } returns Result.success(testUris)
            containerHost.handlePasteImages().join()
            coEvery {
                mainRepository.getPrimaryClipImageUris()
            } returns Result.failure(Exception(""))
            containerHost.handlePasteImages().join()
            coVerify(exactly = 2) {
                mainRepository.getPrimaryClipImageUris()
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Images.Paste.Success(uris = testUris))
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Images.Paste.Failure)
        }
    }

    @Test
    fun test_handleReceivedImages() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            containerHost.handleReceivedImages(emptyList()).join()
            containerHost.handleReceivedImages(testUris).join()
            containerHost.handleReceivedImages(testUris2).join()
            expectSideEffect(MainSideEffect.Images.Received.None)
            expectSideEffect(MainSideEffect.Images.Received.Single(uri = testUri))
            expectSideEffect(MainSideEffect.Images.Received.Multiple(uris = testUris2))
        }
    }

    @Test
    fun test_handleSettings() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            containerHost.handleSettings().join()
            expectSideEffect(MainSideEffect.Navigate.ToSettings)
        }
    }

    @Test
    fun test_handleShortcut() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            containerHost.handleShortcut("").join()
            expectSideEffect(MainSideEffect.Shortcut.Handle(""))
        }
    }

    @Test
    fun test_reportShortcutUsed() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            containerHost.reportShortcutUsed("").join()
            expectSideEffect(MainSideEffect.Shortcut.ReportUsage(""))
        }
    }

    @Test
    fun test_launchCamera() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                mainRepository.getFileProviderUri()
            } returns Result.success(testUri)
            containerHost.launchCamera().join()
            coEvery {
                mainRepository.getFileProviderUri()
            } returns Result.failure(Exception(""))
            containerHost.launchCamera().join()
            coVerify(exactly = 2) {
                mainRepository.getFileProviderUri()
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.ImageSources.Camera.Success(uri = testUri))
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.ImageSources.Camera.Failure)
        }
    }

    @Test
    fun test_putImageSources() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                imageSourcesRepository.putImageSources(testImageSources)
            } returns Result.success(Unit)
            containerHost.putImageSources(testImageSources).join()
            coEvery {
                imageSourcesRepository.putImageSources(testImageSources)
            } returns Result.failure(Exception(""))
            containerHost.putImageSources(testImageSources).join()
            coVerify(exactly = 2) {
                imageSourcesRepository.putImageSources(testImageSources)
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.ImageSources.Put.Success)
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.ImageSources.Put.Failure)
        }
    }

    @Test
    fun test_putImageSelection() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                selectionRepository.putSelection(uri = testUri, isFromCamera = false)
            } returns Result.success(Unit)
            containerHost.putImageSelection(uri = testUri, isFromCamera = false).join()
            coEvery {
                selectionRepository.putSelection(uri = testUri, isFromCamera = false)
            } returns Result.failure(Exception(""))
            containerHost.putImageSelection(uri = testUri, isFromCamera = false).join()
            coVerify(exactly = 2) {
                selectionRepository.putSelection(uri = testUri, isFromCamera = false)
            }
            coEvery {
                selectionRepository.putSelection(uri = testUri, isFromCamera = true)
            } returns Result.success(Unit)
            containerHost.putImageSelection(uri = testUri, isFromCamera = true).join()
            coEvery {
                selectionRepository.putSelection(uri = testUri, isFromCamera = true)
            } returns Result.failure(Exception(""))
            containerHost.putImageSelection(uri = testUri, isFromCamera = true).join()
            coVerify(exactly = 2) {
                selectionRepository.putSelection(uri = testUri, isFromCamera = true)
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.Image.Success(isFromCamera = false))
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.Image.Failure)
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.Image.Success(isFromCamera = true))
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.Image.Failure)
        }
    }

    @Test
    fun test_putImagesSelection() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                selectionRepository.putSelection(testUris)
            } returns Result.success(Unit)
            containerHost.putImagesSelection(testUris).join()
            coEvery {
                selectionRepository.putSelection(testUris)
            } returns Result.failure(Exception(""))
            containerHost.putImagesSelection(testUris).join()
            coVerify(exactly = 2) {
                selectionRepository.putSelection(testUris)
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.Images.Success)
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.Images.Failure)
        }
    }

    @Test
    fun test_putImageDirectorySelection() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                mainRepository.getChildDocuments(testUri)
            } returns Result.success(testUris)
            coEvery {
                selectionRepository.putSelection(testUris)
            } returns Result.success(Unit)
            containerHost.putImageDirectorySelection(testUri).join()
            coEvery {
                selectionRepository.putSelection(testUris)
            } returns Result.failure(Exception(""))
            containerHost.putImageDirectorySelection(testUri).join()
            coEvery {
                mainRepository.getChildDocuments(testUri)
            } returns Result.failure(Exception(""))
            containerHost.putImageDirectorySelection(testUri).join()
            coVerify(exactly = 3) {
                mainRepository.getChildDocuments(testUri)
            }
            coVerify(exactly = 2) {
                selectionRepository.putSelection(testUris)
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.ImageDirectory.Success)
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.ImageDirectory.Failure)
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(loadingTasks = 0)
            }
            expectSideEffect(MainSideEffect.Selection.ImageDirectory.Failure)
        }
    }

    @Test
    fun test_readDefaultValues() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            coEvery {
                settingsRepository.getDefaultNightMode()
            } returns DEFAULT_NIGHT_MODE
            coEvery {
                imageSourcesRepository.getImageSources()
            } returns testImageSources
            containerHost.readDefaultValues().join()
            coVerify(exactly = 1) {
                settingsRepository.getDefaultNightMode()
                imageSourcesRepository.getImageSources()
            }
            expectState {
                copy(loadingTasks = 1)
            }
            expectState {
                copy(
                    imageSources = testImageSources,
                    isImageSourceReorderingEnabled = false,
                    loadingTasks = 0
                )
            }
            expectSideEffect(MainSideEffect.ImageSources.Initialized)
            expectSideEffect(MainSideEffect.DefaultNightMode(DEFAULT_NIGHT_MODE))
        }
    }

    @Test
    fun test_reorderImageSources() = runTest {
        val reorderedImageSources = testImageSources.toMutableList()
        Collections.swap(reorderedImageSources, 1, 0)
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            containerHost
                .reorderImageSources(
                    imageSources = testImageSources,
                    oldIndex = 0,
                    newIndex = 1
                )
                .join()
            expectState {
                copy(imageSources = reorderedImageSources)
            }
        }
    }

    @Test
    fun test_toggleImageSourceReorderingEnabled() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            mainRepository = mainRepository,
            imageSourcesRepository = imageSourcesRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = MainState(imageSources = testImageSources)
        ) {
            containerHost.toggleImageSourceReorderingEnabled().join()
            containerHost.toggleImageSourceReorderingEnabled().join()
            expectState {
                copy(isImageSourceReorderingEnabled = true)
            }
            expectSideEffect(
                MainSideEffect.ImageSourceReordering(
                    imageSources = testImageSources,
                    isEnabled = true
                )
            )
            expectState {
                copy(isImageSourceReorderingEnabled = false)
            }
            expectSideEffect(
                MainSideEffect.ImageSourceReordering(
                    imageSources = testImageSources,
                    isEnabled = false
                )
            )
        }
    }
}
