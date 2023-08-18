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

package com.none.tom.exiferaser.main.business

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.CameraProto
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
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
    fun test_readDefaultValues() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getDefaultNightMode()
                } returns flowOf(0)
                coEvery {
                    imageSourceRepository.getImageSources()
                } returns flowOf(testImageSources)
                coEvery {
                    settingsRepository.shouldSelectImagesLegacy()
                } returns flowOf(true)
                readDefaultValues()
            }.join()
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(
                    imageSources = testImageSources,
                    legacyImageSelection = true,
                    loading = false
                )
            }
            expectSideEffect(MainSideEffect.ImageSourcesReadComplete)
            expectSideEffect(MainSideEffect.DefaultNightMode(0))
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultNightMode()
            imageSourceRepository.getImageSources()
            settingsRepository.shouldSelectImagesLegacy()
        }
    }

    @Test
    fun test_reorderImageSources() = runTest {
        val reorderedImageSources = testImageSources.toMutableList()
        Collections.swap(reorderedImageSources, 1, 0)
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                reorderImageSources(
                    imageSources = testImageSources,
                    oldIndex = 0,
                    newIndex = 1
                )
            }.join()
            expectState {
                copy(imageSources = reorderedImageSources)
            }
        }
    }

    @Test
    fun test_putImageSources() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    imageSourceRepository.putImageSources(testImageSources)
                } returns true
                putImageSources(testImageSources)
            }.join()
            coVerify(exactly = 1) {
                imageSourceRepository.putImageSources(testImageSources)
            }
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
        }
    }

    @Test
    fun test_putImageSelection() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    selectionRepository.putSelection(
                        uri = testUri,
                        fromCamera = false
                    )
                } returns true
                coEvery {
                    settingsRepository.shouldSkipSavePathSelection()
                } returns flowOf(false)
                putImageSelection(
                    uri = testUri,
                    fromCamera = false
                )
            }.join()
            coVerify(exactly = 1) {
                selectionRepository.putSelection(
                    uri = testUri,
                    fromCamera = false
                )
            }
            invokeIntent {
                coEvery {
                    selectionRepository.putSelection(
                        uri = testUri,
                        fromCamera = true
                    )
                } returns true
                putImageSelection(
                    uri = testUri,
                    fromCamera = true
                )
            }.join()
            coVerify(exactly = 1) {
                selectionRepository.putSelection(
                    uri = testUri,
                    fromCamera = true
                )
            }
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
            expectSideEffect(MainSideEffect.NavigateToSelectionSavePath)
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
            expectSideEffect(MainSideEffect.NavigateToSelection(savePath = Uri.EMPTY))
        }
    }

    @Test
    fun test_putImagesSelection() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    selectionRepository.putSelection(
                        uris = testUris,
                        urisFromIntent = testUris.toTypedArray()
                    )
                } returns true
                coEvery {
                    settingsRepository.shouldSkipSavePathSelection()
                } returns flowOf(false)
                putImagesSelection(
                    uris = testUris,
                    urisFromIntent = testUris.toTypedArray()
                )
            }.join()
            coVerify(exactly = 1) {
                selectionRepository.putSelection(
                    uris = testUris,
                    urisFromIntent = testUris.toTypedArray()
                )
            }
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
            expectSideEffect(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    @Test
    fun test_putImageDirectorySelection() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            val result = AnyMessage.pack(UserImageSelectionProto(image_path = testUri.toString()))
            invokeIntent {
                coEvery {
                    imageRepository.packDocumentTreeToAnyMessageOrNull(testUri)
                } returns result
                coEvery {
                    selectionRepository.putSelection(result)
                } returns true
                coEvery {
                    settingsRepository.shouldSkipSavePathSelection()
                } returns flowOf(false)
                putImageDirectorySelection(uri = testUri)
            }.join()
            coVerify(ordering = Ordering.SEQUENCE) {
                imageRepository.packDocumentTreeToAnyMessageOrNull(testUri)
                selectionRepository.putSelection(result)
            }
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
            expectSideEffect(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    @Test
    fun test_handleSettings() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                handleSettings()
            }.join()
            expectSideEffect(MainSideEffect.NavigateToSettings)
        }
    }

    @Test
    fun test_chooseImage() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                } returns testUri
                chooseImage(canReorderImageSources = false)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
            }
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
            expectSideEffect(MainSideEffect.ChooseImage(openPath = testUri))
        }
    }

    @Test
    fun test_chooseImages() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                } returns testUri
                chooseImages(canReorderImageSources = false)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
            }
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
            expectSideEffect(MainSideEffect.ChooseImages(openPath = testUri))
        }
    }

    @Test
    fun test_chooseImageDirectory() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                } returns testUri
                chooseImageDirectory(canReorderImageSources = false)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
            }
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
            expectSideEffect(MainSideEffect.ChooseImageDirectory(openPath = testUri))
        }
    }

    @Test
    fun test_chooseSelectionNavigationRoute() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                } returns testUri
                chooseSelectionNavigationRoute(fromCamera = true)
            }.join()
            invokeIntent {
                coEvery {
                    settingsRepository.shouldSkipSavePathSelection()
                } returns flowOf(true)
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                } returns testUri
                chooseSelectionNavigationRoute()
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.shouldSkipSavePathSelection()
                settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
            }
            invokeIntent {
                coEvery {
                    settingsRepository.shouldSkipSavePathSelection()
                } returns flowOf(false)
                chooseSelectionNavigationRoute()
            }.join()
            expectSideEffect(MainSideEffect.NavigateToSelection(savePath = Uri.EMPTY))
            expectSideEffect(MainSideEffect.NavigateToSelection(savePath = testUri))
            expectSideEffect(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    @Test
    fun test_launchCamera() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    imageRepository.getExternalPicturesFileProviderUriOrNull(
                        fileProviderPackage = "",
                        displayName = ""
                    )
                } returns testUri
                launchCamera(
                    fileProviderPackage = "",
                    displayName = "",
                    canReorderImageSources = false
                )
            }.join()
            coVerify(exactly = 1) {
                imageRepository.getExternalPicturesFileProviderUriOrNull(
                    fileProviderPackage = "",
                    displayName = ""
                )
            }
            expectState {
                copy(loading = true)
            }
            expectState {
                copy(loading = false)
            }
            expectSideEffect(MainSideEffect.LaunchCamera(fileProviderImagePath = testUri))
        }
    }

    @Test
    fun test_handleShortcut() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                handleShortcut("")
            }.join()
            expectSideEffect(MainSideEffect.Shortcut.Handle(""))
        }
    }

    @Test
    fun test_reportShortcutUsed() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                reportShortcutUsed("")
            }.join()
            expectSideEffect(MainSideEffect.Shortcut.ReportUsage(""))
        }
    }

    @Test
    fun test_handleReceivedImages() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                handleReceivedImages(emptyList())
            }.join()
            invokeIntent {
                handleReceivedImages(listOf(testUri))
            }.join()
            invokeIntent {
                handleReceivedImages(listOf(testUri, testUri))
            }.join()
            expectSideEffect(MainSideEffect.ReceivedImage(uri = testUri))
            expectSideEffect(MainSideEffect.ReceivedImages(uris = listOf(testUri, testUri)))
        }
    }

    @Test
    fun test_handlePasteImages() = runTest {
        MainViewModel(
            savedStateHandle = SavedStateHandle(),
            imageRepository = imageRepository,
            imageSourceRepository = imageSourceRepository,
            selectionRepository = selectionRepository,
            settingsRepository = settingsRepository,
            updateRepository = updateRepository
        ).test(
            testScope = this,
            initialState = MainState()
        ) {
            expectInitialState()
            invokeIntent {
                handlePasteImages(emptyList())
            }.join()
            invokeIntent {
                handlePasteImages(listOf(testUri))
            }.join()
            invokeIntent {
                handlePasteImages(listOf(testUri, testUri))
            }.join()
            expectSideEffect(MainSideEffect.PasteImagesNone)
            expectSideEffect(MainSideEffect.PasteImages(uris = listOf(testUri)))
            expectSideEffect(MainSideEffect.PasteImages(uris = listOf(testUri, testUri)))
        }
    }
}
