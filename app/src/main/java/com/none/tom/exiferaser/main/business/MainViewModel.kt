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
import androidx.annotation.IntRange
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.main.data.ImageSourceRepository
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.PROGRESS_MIN
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.none.tom.exiferaser.update.data.UpdateRepository
import com.squareup.wire.AnyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import java.util.Collections
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@HiltViewModel
class MainViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageRepository: ImageRepository,
    private val imageSourceRepository: ImageSourceRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository,
    private val updateRepository: UpdateRepository
) : ContainerHost<MainState, MainSideEffect>,
    ViewModel() {

    private companion object {
        const val KEY_TRANSITION = TOP_LEVEL_PACKAGE_NAME + "TRANSITION"
    }

    override val container = container<MainState, MainSideEffect>(
        initialState = MainState(),
        savedStateHandle = savedStateHandle,
        onCreate = {
            prepareReadImageSources()
            readImageSources()
        }
    )

    var sharedTransitionAxis: Int
        get() {
            val axis = savedStateHandle.get<Int>(KEY_TRANSITION)
            return if (axis != null) {
                savedStateHandle.remove<Int>(KEY_TRANSITION)
                axis
            } else {
                -1
            }
        }
        set(value) = savedStateHandle.set(KEY_TRANSITION, value)

    private fun prepareReadImageSources() = intent {
        reduce {
            state.copy(imageSourcesFetching = true)
        }
    }

    private fun readImageSources() = intent {
        imageSourceRepository.getImageSources().collect { imageSources ->
            reduce {
                state.copy(
                    imageSources = imageSources,
                    imageSourcesFetching = false
                )
            }
        }
    }

    fun prepareReorderImageSources() = intent {
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
    ) = intent {
        Collections.swap(imageSources, newIndex, oldIndex)
        reduce {
            state.copy(
                imageSources = imageSources,
                imageSourcesPersisting = false,
                imageSourcesPersisted = false,
                imageSourcesReordering = true,
                imageSourcesReorder = false
            )
        }
    }

    fun preparePutImageSources() = intent {
        reduce {
            state.copy(
                imageSourcesPersisting = true,
                imageSourcesPersisted = false,
                imageSourcesReordering = false,
                imageSourcesReorder = false
            )
        }
    }

    fun putImageSources(imageSources: MutableList<AnyMessage>) = intent {
        val success = imageSourceRepository.putImageSources(imageSources)
        reduce {
            state.copy(
                imageSourcesPersisting = false,
                imageSourcesPersisted = success,
                imageSourcesReordering = false,
                imageSourcesReorder = false
            )
        }
    }

    fun <T> preparePutSelection(result: T?) = intent {
        reduce {
            state.copy(selectionPersisting = result != null)
        }
    }

    fun putImageSelection(
        imageUri: Uri?,
        fromCamera: Boolean = false
    ) = intent {
        val success = selectionRepository.putSelection(
            imageUri = imageUri,
            fromCamera = fromCamera
        )
        reduce {
            state.copy(selectionPersisting = false)
        }
        if (success) {
            postSideEffect(
                if (fromCamera) {
                    MainSideEffect.NavigateToSelection
                } else {
                    MainSideEffect.NavigateToSelectionSavePath
                }
            )
        }
    }

    fun putImagesSelection(
        imageUris: List<Uri>? = null,
        intentImageUris: Array<Uri>? = null
    ) = intent {
        val success = selectionRepository.putSelection(
            imageUris = imageUris,
            intentImageUris = intentImageUris
        )
        reduce {
            state.copy(selectionPersisting = false)
        }
        if (success) {
            postSideEffect(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    fun putImageDirectorySelection(treeUri: Uri?) = intent {
        val success = if (treeUri.isNotNullOrEmpty()) {
            val message = imageRepository.packDocumentTreeToAnyMessageOrNull(treeUri)
            selectionRepository.putSelection(message)
        } else {
            false
        }
        reduce {
            state.copy(selectionPersisting = false)
        }
        if (success) {
            postSideEffect(MainSideEffect.NavigateToSelectionSavePath)
        }
    }

    fun handleSettings() = intent {
        postSideEffect(MainSideEffect.NavigateToSettings)
    }

    fun prepareChooseImagesOrLaunchCamera() = intent {
        reduce {
            state.copy(accessingPreferences = true)
        }
    }

    fun chooseImage() = intent {
        val path = settingsRepository.getDefaultOpenPathSuspending()
        reduce {
            state.copy(accessingPreferences = false)
        }
        postSideEffect(MainSideEffect.ChooseImage(path))
    }

    fun chooseImages() = intent {
        val path = settingsRepository.getDefaultOpenPathSuspending()
        reduce {
            state.copy(accessingPreferences = false)
        }
        postSideEffect(MainSideEffect.ChooseImages(path))
    }

    fun chooseImageDirectory() = intent {
        val path = settingsRepository.getDefaultOpenPathSuspending()
        reduce {
            state.copy(accessingPreferences = false)
        }
        postSideEffect(MainSideEffect.ChooseImageDirectory(path))
    }

    fun launchCamera(
        fileProviderPackage: String,
        displayName: String
    ) = intent {
        val uri =
            imageRepository.getExternalPicturesFileProviderUriOrNull(
                fileProviderPackage = fileProviderPackage,
                displayName = displayName
            )
        reduce {
            state.copy(accessingPreferences = false)
        }
        if (uri.isNotNullOrEmpty()) {
            postSideEffect(MainSideEffect.LaunchCamera(uri))
        }
    }

    fun handleShortcut(shortcutAction: String) = intent {
        postSideEffect(MainSideEffect.ShortcutHandle(shortcutAction))
    }

    fun reportShortcutUsed(shortcutAction: String) = intent {
        postSideEffect(MainSideEffect.ShortcutReportUsed(shortcutAction))
    }

    fun handleMultiWindowMode(isInMultiWindowMode: Boolean) = intent {
        reduce {
            state.copy(isInMultiWindowMode = isInMultiWindowMode)
        }
    }

    fun handleReceivedImages(uris: List<Uri>) = intent {
        if (uris.isNotEmpty()) {
            postSideEffect(
                if (uris.size > 1) {
                    MainSideEffect.ReceivedImages(uris)
                } else {
                    MainSideEffect.ReceivedImage(uris.first())
                }
            )
        }
    }

    fun handlePasteImages(uris: List<Uri>) = intent {
        val accessingStorage = state.imageSourcesFetching ||
            state.imageSourcesPersisting ||
            state.selectionPersisting ||
            state.accessingPreferences
        if (!accessingStorage) {
            postSideEffect(
                if (uris.isNotEmpty()) {
                    MainSideEffect.PasteImages(uris)
                } else {
                    MainSideEffect.PasteImagesNone
                }
            )
        }
    }

    fun handleFlexibleUpdateFailure() = intent {
        updateRepository.showAppUpdateProgressNotification(failed = true)
        postSideEffect(MainSideEffect.FlexibleUpdateFailed)
    }

    fun handleFlexibleUpdateInProgress(
        @IntRange(from = PROGRESS_MIN.toLong(), to = PROGRESS_MAX.toLong()) progress: Int,
        notify: Boolean
    ) = intent {
        updateRepository.showAppUpdateProgressNotification(progress)
        if (notify) {
            postSideEffect(MainSideEffect.FlexibleUpdateInProgress(progress))
        }
    }

    fun handleFlexibleUpdateReadyToInstall() = intent {
        updateRepository.showAppUpdateProgressNotification(PROGRESS_MAX)
        postSideEffect(MainSideEffect.FlexibleUpdateReadyToInstall)
    }

    fun completeFlexibleUpdate() = intent {
        val success = updateRepository.completeFlexibleAppUpdate()
        if (!success) {
            postSideEffect(MainSideEffect.FlexibleUpdateFailed)
        }
    }
}
