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

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.addOrShift
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.selection.data.Result
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.none.tom.exiferaser.toInt
import com.squareup.wire.AnyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class SelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val imageRepository: ImageRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository
) : ContainerHost<SelectionState, SelectionSideEffect>,
    ViewModel() {

    override val container = container<SelectionState, SelectionSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = SelectionState(),
        onCreate = {
            if (!state.handledAll) {
                readSelection(state.imagesTotal)
            }
        }
    )

    fun handleSelection(
        selection: AnyMessage?,
        treeUri: Uri
    ) = intent {
        if (selection == null) {
            handleUnsupportedSelection()
            return@intent
        }
        when (selection.typeUrl) {
            UserImageSelectionProto.ADAPTER.typeUrl -> {
                handleUserImageSelection(selection, treeUri)
            }
            UserImagesSelectionProto.ADAPTER.typeUrl -> {
                handleUserImagesSelection(selection, treeUri)
            }
            else -> {
                handleUnsupportedSelection()
            }
        }
    }

    fun handleUserImageSelection(
        selection: AnyMessage,
        treeUri: Uri
    ) = intent {
        val displayNameSuffix =
            settingsRepository.getDefaultDisplayNameSuffix().firstOrNull().orEmpty()
        val autoDelete = settingsRepository.shouldAutoDelete().firstOrNull() == true
        val preserveOrientation =
            settingsRepository.shouldPreserveOrientation().firstOrNull() == true
        val randomizeFileNames = settingsRepository.shouldRandomizeFileNames().firstOrNull() == true
        imageRepository.removeMetadataSingle(
            selection = selection,
            treeUri = treeUri,
            displayNameSuffix = displayNameSuffix,
            autoDelete = autoDelete,
            preserveOrientation = preserveOrientation,
            randomizeFileNames = randomizeFileNames
        ).collect { result ->
            reduce {
                when (result) {
                    is Result.Empty -> {
                        state.copy(imageResult = result)
                    }
                    is Result.Report -> {
                        val imageSummaries =
                            state.imageSummaries.toMutableList().addOrShift(result.summary)
                        val imageUris =
                            state.imageUris.toMutableList().addOrShift(result.summary.imageUri)
                        val modified = state.imagesModified + result.summary.imageModified.toInt()
                        val saved = state.imagesSaved + result.summary.imageSaved.toInt()
                        state.copy(
                            imageResult = result,
                            imageSummaries = imageSummaries,
                            imageUris = imageUris,
                            imagesModified = modified,
                            imagesSaved = saved
                        )
                    }
                    is Result.Handled -> {
                        state.copy(
                            imageResult = result,
                            imagesTotal = state.imagesTotal.inc(),
                            progress = result.progress
                        )
                    }
                    is Result.HandledAll -> {
                        state.copy(handledAll = true)
                    }
                }
            }
            if (result is Result.HandledAll) {
                postSideEffect(SelectionSideEffect.SelectionHandled)
            }
        }
    }

    fun handleUserImagesSelection(
        selection: AnyMessage,
        treeUri: Uri
    ) = intent {
        val displayNameSuffix =
            settingsRepository.getDefaultDisplayNameSuffix().firstOrNull().orEmpty()
        val autoDelete = settingsRepository.shouldAutoDelete().firstOrNull() == true
        val preserveOrientation =
            settingsRepository.shouldPreserveOrientation().firstOrNull() == true
        val randomizeFileNames = settingsRepository.shouldRandomizeFileNames().firstOrNull() == true
        imageRepository.removeMetadataBulk(
            selection = selection,
            treeUri = treeUri,
            displayNameSuffix = displayNameSuffix,
            autoDelete = autoDelete,
            preserveOrientation = preserveOrientation,
            randomizeFileNames = randomizeFileNames
        ).collect { result ->
            reduce {
                when (result) {
                    is Result.Empty -> {
                        state.copy(imageResult = result)
                    }
                    is Result.Report -> {
                        val imageSummaries =
                            state.imageSummaries.toMutableList().addOrShift(result.summary)
                        val imageUris =
                            state.imageUris.toMutableList().addOrShift(result.summary.imageUri)
                        val modified = state.imagesModified + result.summary.imageModified.toInt()
                        val saved = state.imagesSaved + result.summary.imageSaved.toInt()
                        state.copy(
                            imageResult = result,
                            imageSummaries = imageSummaries,
                            imageUris = imageUris,
                            imagesModified = modified,
                            imagesSaved = saved
                        )
                    }
                    is Result.Handled -> {
                        state.copy(
                            imageResult = result,
                            imagesTotal = state.imagesTotal.inc(),
                            progress = result.progress
                        )
                    }
                    is Result.HandledAll -> {
                        state.copy(handledAll = true)
                    }
                }
            }
            if (result is Result.HandledAll) {
                postSideEffect(SelectionSideEffect.SelectionHandled)
            }
        }
    }

    fun handleUnsupportedSelection() = intent {
        reduce {
            state.copy(handledAll = true)
        }
    }

    fun readSelection(dropFirstN: Int) = intent {
        val selection = selectionRepository.getSelection(dropFirstN).firstOrNull()
        if (selection != null) {
            postSideEffect(SelectionSideEffect.ReadComplete(selection))
        }
    }

    fun shareImages() = intent {
        val result = state
            .imageSummaries
            .filter { summary -> summary.imageModified && summary.imageSaved }
            .map { summary -> summary.imageUri }
        if (result.isNotEmpty()) {
            postSideEffect(SelectionSideEffect.ShareImages(ArrayList(result)))
        }
    }

    fun shareImagesByDefault() = intent {
        if (settingsRepository.shouldShareByDefault().firstOrNull() == true) {
            shareImages()
        }
    }
}
