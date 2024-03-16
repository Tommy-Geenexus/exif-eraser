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

package com.none.tom.exiferaser.main.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.main.data.ImageSourcesRepository
import com.none.tom.exiferaser.main.data.MainRepository
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.main.data.supportedImageFormats
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.squareup.wire.AnyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Collections
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val mainRepository: MainRepository,
    private val imageSourcesRepository: ImageSourcesRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository
) : ContainerHost<MainState, MainSideEffect>,
    ViewModel() {

    private companion object {

        const val KEY_NAV_DESTINATION_ID = TOP_LEVEL_PACKAGE_NAME + "NAV_DESTINATION_ID"
    }

    override val container = container<MainState, MainSideEffect>(
        initialState = MainState(),
        savedStateHandle = savedStateHandle,
        onCreate = {
            reduce {
                state.copy(
                    isImageSourceReorderingEnabled = false,
                    loadingTasks = 0
                )
            }
        }
    )

    var navDestinationId: Int?
        get() = savedStateHandle[KEY_NAV_DESTINATION_ID]
        set(value) = savedStateHandle.set(KEY_NAV_DESTINATION_ID, value)

    fun chooseImage() = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val isLegacyImageSelectionEnabled = settingsRepository.isLegacyImageSelectionEnabled()
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            MainSideEffect.ImageSources.Image(
                supportedMimeTypes = supportedImageFormats.map { f -> f.mimeType }.toTypedArray(),
                isLegacyImageSelectionEnabled = isLegacyImageSelectionEnabled
            )
        )
    }

    fun chooseImages() = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val isLegacyImageSelectionEnabled = settingsRepository.isLegacyImageSelectionEnabled()
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            MainSideEffect.ImageSources.Images(
                supportedMimeTypes = supportedImageFormats.map { f -> f.mimeType }.toTypedArray(),
                isLegacyImageSelectionEnabled = isLegacyImageSelectionEnabled
            )
        )
    }

    fun chooseImageDirectory() = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val result = settingsRepository.getPrivilegedDefaultOpenPath()
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            if (result.isSuccess) {
                MainSideEffect.ImageSources.ImageDirectory.Success(uri = result.getOrThrow())
            } else {
                MainSideEffect.ImageSources.ImageDirectory.Failure
            }
        )
    }

    fun chooseSelectionNavigationRoute(isFromCamera: Boolean = false) = intent {
        if (isFromCamera) {
            postSideEffect(MainSideEffect.Navigate.ToSelection(savePath = Uri.EMPTY))
            return@intent
        }
        if (settingsRepository.isSkipSavePathSelectionEnabled()) {
            val result = settingsRepository.getPrivilegedDefaultSavePath()
            if (result.isSuccess) {
                postSideEffect(MainSideEffect.Navigate.ToSelection(savePath = result.getOrThrow()))
                return@intent
            }
        }
        postSideEffect(MainSideEffect.Navigate.ToSelectionSavePath)
    }

    fun deleteCameraImages() = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val result = mainRepository.deleteCameraImages()
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            if (result.isSuccess) {
                MainSideEffect.Images.Delete.Success
            } else {
                MainSideEffect.Images.Delete.Failure
            }
        )
    }

    fun handleDeleteCameraImages() = intent {
        postSideEffect(MainSideEffect.Navigate.ToDeleteCameraImages)
    }

    fun handleHelp() = intent {
        postSideEffect(MainSideEffect.Navigate.ToHelp)
    }

    fun handlePasteImages() = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val result = mainRepository.getPrimaryClipImageUris()
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            if (result.isSuccess) {
                MainSideEffect.Images.Paste.Success(uris = result.getOrThrow())
            } else {
                MainSideEffect.Images.Paste.Failure
            }
        )
    }

    fun handleReceivedImages(uris: List<Uri>) = intent {
        postSideEffect(
            if (uris.isEmpty()) {
                MainSideEffect.Images.Received.None
            } else if (uris.size < 2) {
                MainSideEffect.Images.Received.Single(uri = uris.first())
            } else {
                MainSideEffect.Images.Received.Multiple(uris)
            }
        )
    }

    fun handleSettings() = intent {
        postSideEffect(MainSideEffect.Navigate.ToSettings)
    }

    fun handleShortcut(shortcutAction: String) = intent {
        postSideEffect(MainSideEffect.Shortcut.Handle(shortcutAction))
    }

    fun reportShortcutUsed(shortcutAction: String) = intent {
        postSideEffect(MainSideEffect.Shortcut.ReportUsage(shortcutAction))
    }

    fun launchCamera() = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val result = mainRepository.getFileProviderUri()
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            if (result.isSuccess) {
                MainSideEffect.ImageSources.Camera.Success(uri = result.getOrThrow())
            } else {
                MainSideEffect.ImageSources.Camera.Failure
            }
        )
    }

    fun putImageSources(imageSources: List<AnyMessage>) = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val result = imageSourcesRepository.putImageSources(imageSources)
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            if (result.isSuccess) {
                MainSideEffect.ImageSources.Put.Success
            } else {
                MainSideEffect.ImageSources.Put.Failure
            }
        )
    }

    fun putImageSelection(uri: Uri?, isFromCamera: Boolean = false) = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val result = selectionRepository.putSelection(uri, isFromCamera)
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            if (result.isSuccess) {
                MainSideEffect.Selection.Image.Success(isFromCamera)
            } else {
                MainSideEffect.Selection.Image.Failure
            }
        )
    }

    fun putImagesSelection(uris: List<Uri>) = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val result = selectionRepository.putSelection(uris)
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            if (result.isSuccess) {
                MainSideEffect.Selection.Images.Success
            } else {
                MainSideEffect.Selection.Images.Failure
            }
        )
    }

    fun putImageDirectorySelection(uri: Uri?) = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val result = mainRepository.getChildDocuments(uri)
        if (result.isFailure) {
            reduce {
                state.copy(loadingTasks = state.loadingTasks.dec())
            }
            postSideEffect(MainSideEffect.Selection.ImageDirectory.Failure)
            return@intent
        }
        val putResult = selectionRepository.putSelection(result.getOrThrow())
        reduce {
            state.copy(loadingTasks = state.loadingTasks.dec())
        }
        postSideEffect(
            if (putResult.isSuccess) {
                MainSideEffect.Selection.ImageDirectory.Success
            } else {
                MainSideEffect.Selection.ImageDirectory.Failure
            }
        )
    }

    fun readDefaultValues() = intent {
        reduce {
            state.copy(loadingTasks = state.loadingTasks.inc())
        }
        val defaultNightMode = settingsRepository.getDefaultNightMode()
        val imageSources = imageSourcesRepository.getImageSources()
        reduce {
            state.copy(
                imageSources = imageSources,
                loadingTasks = state.loadingTasks.dec()
            )
        }
        postSideEffect(MainSideEffect.ImageSources.Initialized)
        postSideEffect(MainSideEffect.DefaultNightMode(defaultNightMode))
    }

    fun reorderImageSources(imageSources: List<AnyMessage>, oldIndex: Int, newIndex: Int) = intent {
        reduce {
            state.copy(
                imageSources = imageSources
                    .toMutableList()
                    .apply { Collections.swap(this, newIndex, oldIndex) }
                    .toList()
            )
        }
    }

    fun toggleImageSourceReorderingEnabled() = intent {
        reduce {
            state.copy(isImageSourceReorderingEnabled = !state.isImageSourceReorderingEnabled)
        }
        postSideEffect(
            MainSideEffect.ImageSourceReordering(
                imageSources = state.imageSources,
                isEnabled = state.isImageSourceReorderingEnabled
            )
        )
    }
}
