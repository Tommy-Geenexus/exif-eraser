/*
 * Copyright (c) 2018-2020, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.babylon.orbit2.Container
import com.babylon.orbit2.ContainerHost
import com.babylon.orbit2.coroutines.transformFlow
import com.babylon.orbit2.coroutines.transformSuspend
import com.babylon.orbit2.syntax.strict.orbit
import com.babylon.orbit2.syntax.strict.reduce
import com.babylon.orbit2.syntax.strict.sideEffect
import com.babylon.orbit2.viewmodel.container
import com.none.tom.exiferaser.main.data.ImageSourceRepository
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.squareup.wire.AnyMessage
import java.util.Collections

class MainViewModel @ViewModelInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    private val imageRepository: ImageRepository,
    private val imageSourceRepository: ImageSourceRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository
) : ContainerHost<MainState, MainSideEffect>,
    ViewModel() {

    override val container: Container<MainState, MainSideEffect> = container(
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
        imagePath: Uri?,
        fromCamera: Boolean = false
    ) = orbit {
        transformSuspend {
            selectionRepository.putSelection(
                imagePath = imagePath,
                fromCamera = fromCamera
            )
            imagePath
        }.reduce {
            state.copy(selectionPersisting = false)
        }.sideEffect {
            if (event != null) {
                post(MainSideEffect.NavigateToSelectionSavePath)
            }
        }
    }

    fun putImagesSelection(imagePaths: List<Uri>?) = orbit {
        transformSuspend {
            selectionRepository.putSelection(imagePaths)
            imagePaths
        }.reduce {
            state.copy(selectionPersisting = false)
        }.sideEffect {
            if (event != null) {
                post(MainSideEffect.NavigateToSelectionSavePath)
            }
        }
    }

    fun putImageDirectorySelection(parentDirectoryPath: Uri?) = orbit {
        transformSuspend {
            if (parentDirectoryPath != null) {
                imageRepository
                    .getParentDirectoryAsMessageOrNull(parentDirectoryPath)
                    .also { message -> selectionRepository.putSelection(message) }
            } else {
                parentDirectoryPath
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

    fun chooseImage(openPath: Uri = settingsRepository.getDefaultOpenPath()) = orbit {
        sideEffect {
            post(MainSideEffect.ChooseImage(openPath))
        }
    }

    fun chooseImages(openPath: Uri = settingsRepository.getDefaultOpenPath()) = orbit {
        sideEffect {
            post(MainSideEffect.ChooseImages(openPath))
        }
    }

    fun chooseImageDirectory(openPath: Uri = settingsRepository.getDefaultOpenPath()) = orbit {
        sideEffect {
            post(MainSideEffect.ChooseImageDirectory(openPath))
        }
    }

    fun launchCamera() = orbit {
        transformSuspend {
            imageRepository.getExternalPicturesFileProviderUriOrNull(
                displayName = System.currentTimeMillis().toString()
            )
        }.sideEffect {
            val imagePath = event
            if (imagePath != null) {
                post(MainSideEffect.LaunchCamera(imagePath))
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
}
