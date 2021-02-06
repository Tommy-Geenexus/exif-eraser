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

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.main.data.ImageSourceRepository
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.squareup.wire.AnyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Collections
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.coroutines.transformFlow
import org.orbitmvi.orbit.coroutines.transformSuspend
import org.orbitmvi.orbit.syntax.strict.orbit
import org.orbitmvi.orbit.syntax.strict.reduce
import org.orbitmvi.orbit.syntax.strict.sideEffect
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val imageRepository: ImageRepository,
    private val imageSourceRepository: ImageSourceRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository
) : ContainerHost<MainState, MainSideEffect>,
    ViewModel() {

    override val container = container<MainState, MainSideEffect>(
        initialState = MainState(),
        savedStateHandle = savedStateHandle,
        onCreate = {
            prepareReadImageSources()
            readImageSources()
        }
    )

    private fun prepareReadImageSources() = orbit {
        reduce {
            state.copy(imageSourcesFetching = true)
        }
    }

    private fun readImageSources() = orbit {
        transformFlow {
            imageSourceRepository.getImageSources()
        }.reduce {
            state.copy(
                imageSources = event,
                imageSourcesFetching = false
            )
        }
    }

    fun prepareReorderImageSources() = orbit {
        reduce {
            state.copy(
                imageSourcesPersisting = false,
                imageSourcesPersisted = false,
                imageSourcesReordering = false,
                imageSourcesReorder = true
            )
        }
    }

    fun reorderImageSources(
        imageSources: MutableList<AnyMessage>,
        oldIndex: Int,
        newIndex: Int
    ) = orbit {
        transformSuspend {
            Collections.swap(imageSources, newIndex, oldIndex)
            imageSources
        }.reduce {
            state.copy(
                imageSources = event,
                imageSourcesPersisting = false,
                imageSourcesPersisted = false,
                imageSourcesReordering = true,
                imageSourcesReorder = false
            )
        }
    }

    fun preparePutImageSources() = orbit {
        reduce {
            state.copy(
                imageSourcesPersisting = true,
                imageSourcesPersisted = false,
                imageSourcesReordering = false,
                imageSourcesReorder = false
            )
        }
    }

    fun putImageSources(imageSources: MutableList<AnyMessage>) = orbit {
        transformSuspend {
            imageSourceRepository.putImageSources(imageSources)
        }.reduce {
            state.copy(
                imageSourcesPersisting = false,
                imageSourcesPersisted = true,
                imageSourcesReordering = false,
                imageSourcesReorder = false
            )
        }
    }

    fun <T> preparePutSelection(result: T?) = orbit {
        reduce {
            state.copy(selectionPersisting = result != null)
        }
    }

    fun putImageSelection(
        imageUri: Uri?,
        fromCamera: Boolean = false
    ) = orbit {
        transformSuspend {
            selectionRepository.putSelection(
                imageUri = imageUri,
                fromCamera = fromCamera
            )
        }.reduce {
            state.copy(selectionPersisting = false)
        }.sideEffect {
            if (event != null) {
                post(MainSideEffect.NavigateToSelectionSavePath)
            }
        }
    }

    fun putImagesSelection(
        imageUris: List<Uri>? = null,
        intentImageUris: Array<Uri>? = null
    ) = orbit {
        transformSuspend {
            selectionRepository.putSelection(
                imageUris = imageUris,
                intentImageUris = intentImageUris
            )
        }.reduce {
            state.copy(selectionPersisting = false)
        }.sideEffect {
            if (!event.isNullOrEmpty()) {
                post(MainSideEffect.NavigateToSelectionSavePath)
            }
        }
    }

    fun putImageDirectorySelection(treeUri: Uri?) = orbit {
        transformSuspend {
            if (treeUri != null) {
                val message = imageRepository.getDocumentTreeAsMessageOrNull(treeUri)
                selectionRepository.putSelection(message)
            } else {
                treeUri
            }
        }.reduce {
            state.copy(selectionPersisting = false)
        }.sideEffect {
            if (event != null) {
                post(MainSideEffect.NavigateToSelectionSavePath)
            }
        }
    }

    fun handleSettings() = orbit {
        sideEffect {
            post(MainSideEffect.NavigateToSettings)
        }
    }

    fun prepareChooseImagesOrLaunchCamera() = orbit {
        reduce {
            state.copy(accessingPreferences = true)
        }
    }

    fun chooseImage() = orbit {
        transformSuspend {
            settingsRepository.getDefaultOpenPath()
        }.reduce {
            state.copy(accessingPreferences = false)
        }.sideEffect {
            post(MainSideEffect.ChooseImage(event))
        }
    }

    fun chooseImages() = orbit {
        transformSuspend {
            settingsRepository.getDefaultOpenPath()
        }.reduce {
            state.copy(accessingPreferences = false)
        }.sideEffect {
            post(MainSideEffect.ChooseImages(event))
        }
    }

    fun chooseImageDirectory() = orbit {
        transformSuspend {
            settingsRepository.getDefaultOpenPath()
        }.reduce {
            state.copy(accessingPreferences = false)
        }.sideEffect {
            post(MainSideEffect.ChooseImageDirectory(event))
        }
    }

    fun launchCamera(displayName: String) = orbit {
        transformSuspend {
            imageRepository.getExternalPicturesFileProviderUriOrNull(displayName = displayName)
        }.reduce {
            state.copy(accessingPreferences = false)
        }.sideEffect {
            val imageUri = event
            if (imageUri != null) {
                post(MainSideEffect.LaunchCamera(imageUri))
            }
        }
    }

    fun handleShortcut(shortcutAction: String) = orbit {
        sideEffect {
            post(MainSideEffect.ShortcutHandle(shortcutAction))
        }
    }

    fun reportShortcutUsed(shortcutAction: String) = orbit {
        sideEffect {
            post(MainSideEffect.ShortcutReportUsed(shortcutAction))
        }
    }

    fun handleMultiWindowMode(isInMultiWindowMode: Boolean) = orbit {
        reduce {
            state.copy(isInMultiWindowMode = isInMultiWindowMode)
        }
    }
}
