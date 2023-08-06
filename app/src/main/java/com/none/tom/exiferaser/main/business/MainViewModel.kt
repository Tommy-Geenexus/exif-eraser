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

import android.net.Uri
import androidx.annotation.IntRange
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.isNotEmpty
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
import java.util.Collections
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

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

        const val KEY_NAV_DESTINATION_ID = TOP_LEVEL_PACKAGE_NAME + "NAV_DESTINATION_ID"
    }

    override val container = container<MainState, MainSideEffect>(
        initialState = MainState(),
        savedStateHandle = savedStateHandle
    )

    var navDestinationId: Int?
        get() = savedStateHandle[KEY_NAV_DESTINATION_ID]
        set(value) = savedStateHandle.set(KEY_NAV_DESTINATION_ID, value)

    fun completeFlexibleUpdate() = intent {
        val success = updateRepository.completeFlexibleAppUpdate()
        if (!success) {
            postSideEffect(MainSideEffect.FlexibleUpdateFailed)
        }
    }

    fun chooseImage(canReorderImageSources: Boolean) = intent {
        if (canReorderImageSources || state.loading) {
            return@intent
        }
        reduce {
            state.copy(loading = true)
        }
        val pathOpen = settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        reduce {
            state.copy(loading = false)
        }
        postSideEffect(MainSideEffect.ChooseImage(pathOpen))
    }

    fun chooseImages(canReorderImageSources: Boolean) = intent {
        if (canReorderImageSources || state.loading) {
            return@intent
        }
        reduce {
            state.copy(loading = true)
        }
        val pathOpen = settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        reduce {
            state.copy(loading = false)
        }
        postSideEffect(MainSideEffect.ChooseImages(pathOpen))
    }

    fun chooseImageDirectory(canReorderImageSources: Boolean) = intent {
        if (canReorderImageSources || state.loading) {
            return@intent
        }
        reduce {
            state.copy(loading = true)
        }
        val pathOpen = settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        reduce {
            state.copy(loading = false)
        }
        postSideEffect(MainSideEffect.ChooseImageDirectory(pathOpen))
    }

    fun chooseSelectionNavigationRoute(fromCamera: Boolean = false) = intent {
        if (fromCamera) {
            postSideEffect(MainSideEffect.NavigateToSelection(savePath = Uri.EMPTY))
            return@intent
        }
        val skipSavePathSelection = settingsRepository.shouldSkipSavePathSelection().firstOrNull()
        if (skipSavePathSelection == true) {
            val savePath = settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
            if (savePath.isNotEmpty()) {
                postSideEffect(MainSideEffect.NavigateToSelection(savePath))
                return@intent
            }
        }
        postSideEffect(MainSideEffect.NavigateToSelectionSavePath)
    }

    fun deleteCameraImages() = intent {
        reduce {
            state.copy(loading = true)
        }
        val result = imageRepository.deleteExternalPictures()
        reduce {
            state.copy(loading = false)
        }
        postSideEffect(MainSideEffect.ExternalPicturesDeleted(success = result))
    }

    fun handleDeleteCameraImages() = intent {
        postSideEffect(MainSideEffect.DeleteCameraImages)
    }

    fun handleHelp() = intent {
        postSideEffect(MainSideEffect.NavigateToHelp)
    }

    fun handlePasteImages(uris: List<Uri>) = intent {
        postSideEffect(
            if (uris.isNotEmpty()) {
                MainSideEffect.PasteImages(uris)
            } else {
                MainSideEffect.PasteImagesNone
            }
        )
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

    fun handleSettings() = intent {
        postSideEffect(MainSideEffect.NavigateToSettings)
    }

    fun handleShortcut(shortcutAction: String) = intent {
        postSideEffect(MainSideEffect.Shortcut.Handle(shortcutAction))
    }

    fun reportShortcutUsed(shortcutAction: String) = intent {
        postSideEffect(MainSideEffect.Shortcut.ReportUsage(shortcutAction))
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

    fun launchCamera(
        fileProviderPackage: String,
        displayName: String,
        canReorderImageSources: Boolean
    ) = intent {
        if (canReorderImageSources || state.loading) {
            return@intent
        }
        reduce {
            state.copy(loading = true)
        }
        val uri = imageRepository.getExternalPicturesFileProviderUriOrNull(
            fileProviderPackage = fileProviderPackage,
            displayName = displayName
        )
        reduce {
            state.copy(loading = false)
        }
        if (uri.isNotNullOrEmpty()) {
            postSideEffect(MainSideEffect.LaunchCamera(uri))
        }
    }

    fun putImageSources(imageSources: MutableList<AnyMessage>) = intent {
        reduce {
            state.copy(loading = true)
        }
        imageSourceRepository.putImageSources(imageSources)
        reduce {
            state.copy(loading = false)
        }
    }

    fun putImageSelection(
        uri: Uri?,
        fromCamera: Boolean = false,
        canReorderImageSources: Boolean = false
    ) = intent {
        if (canReorderImageSources || state.loading) {
            return@intent
        }
        reduce {
            state.copy(loading = true)
        }
        val success = selectionRepository.putSelection(uri, fromCamera)
        reduce {
            state.copy(loading = false)
        }
        if (success) {
            chooseSelectionNavigationRoute(fromCamera)
        }
    }

    fun putImagesSelection(
        uris: List<Uri>? = null,
        urisFromIntent: Array<Uri>? = null,
        canReorderImageSources: Boolean = false
    ) = intent {
        if (canReorderImageSources || state.loading) {
            return@intent
        }
        reduce {
            state.copy(loading = true)
        }
        val success = selectionRepository.putSelection(uris, urisFromIntent)
        reduce {
            state.copy(loading = false)
        }
        if (success) {
            chooseSelectionNavigationRoute()
        }
    }

    fun putImageDirectorySelection(
        uri: Uri?,
        canReorderImageSources: Boolean = false
    ) = intent {
        if (canReorderImageSources || state.loading) {
            return@intent
        }
        val success = if (uri.isNotNullOrEmpty()) {
            reduce {
                state.copy(loading = true)
            }
            val message = imageRepository.packDocumentTreeToAnyMessageOrNull(uri)
            selectionRepository.putSelection(message)
        } else {
            false
        }
        reduce {
            state.copy(loading = false)
        }
        if (success) {
            chooseSelectionNavigationRoute()
        }
    }

    fun readDefaultValues() = intent {
        reduce {
            state.copy(loading = true)
        }
        val defaultNightMode = settingsRepository.getDefaultNightMode().firstOrNull()
        val imageSources = imageSourceRepository.getImageSources().firstOrNull() ?: mutableListOf()
        val legacyImageSelection =
            settingsRepository.shouldSelectImagesLegacy().firstOrNull() == true
        reduce {
            state.copy(
                imageSources = imageSources,
                legacyImageSelection = legacyImageSelection,
                loading = false
            )
        }
        if (imageSources.isNotEmpty()) {
            postSideEffect(MainSideEffect.ImageSourcesReadComplete)
        }
        if (defaultNightMode != null) {
            postSideEffect(MainSideEffect.DefaultNightMode(defaultNightMode))
        }
    }

    fun reorderImageSources(
        imageSources: MutableList<AnyMessage>,
        oldIndex: Int,
        newIndex: Int
    ) = intent {
        Collections.swap(imageSources, newIndex, oldIndex)
        reduce {
            state.copy(imageSources = imageSources)
        }
    }
}
