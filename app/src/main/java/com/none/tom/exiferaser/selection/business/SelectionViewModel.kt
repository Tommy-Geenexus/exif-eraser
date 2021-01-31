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

package com.none.tom.exiferaser.selection.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import app.cash.exhaustive.Exhaustive
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.selection.data.Result
import com.none.tom.exiferaser.selection.setOrSkip
import com.none.tom.exiferaser.selection.toInt
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.squareup.wire.AnyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.coroutines.transformFlow
import org.orbitmvi.orbit.coroutines.transformSuspend
import org.orbitmvi.orbit.syntax.strict.orbit
import org.orbitmvi.orbit.syntax.strict.reduce
import org.orbitmvi.orbit.syntax.strict.sideEffect
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
        onCreate = { state ->
            if (!state.handledAll) {
                readSelection(state.imagesTotal)
            }
        }
    )

    private fun readSelection(dropFirstN: Int) = orbit {
        transformFlow {
            selectionRepository.getSelection(dropFirstN)
        }.sideEffect {
            post(SelectionSideEffect.ReadComplete(event))
        }
    }

    fun prepareReport() = orbit {
        transformSuspend {
            state.imageSummaries.filterNotNull()
        }.sideEffect {
            val result = event
            if (result.isNotEmpty()) {
                post(SelectionSideEffect.PrepareReport(result))
            }
        }
    }

    fun shareImages() = orbit {
        transformSuspend {
            state
                .imageSummaries
                .filterNotNull()
                .filter { summary ->
                    summary.imageModified && summary.imageSaved
                }
                .map { summary ->
                    summary.imageUri
                }
        }.sideEffect {
            val result = event
            if (!result.isNullOrEmpty()) {
                post(SelectionSideEffect.ShareImages(ArrayList(result)))
            }
        }
    }

    fun shareImagesByDefault() = orbit {
        transformSuspend {
            settingsRepository.shouldShareImagesByDefault()
        }.sideEffect {
            if (event) {
                shareImages()
            }
        }
    }

    fun hasSavedImages() = container.currentState.imagesSaved > 0

    fun handleSelection(
        selection: AnyMessage?,
        treeUri: Uri
    ) = orbit {
        transformSuspend {
            if (selection == null) {
                handleUnsupportedSelection()
                return@transformSuspend
            }
            when (selection.typeUrl) {
                UserImageSelectionProto.ADAPTER.typeUrl -> {
                    handleUserImageSelection(
                        selection = selection.unpack(UserImageSelectionProto.ADAPTER),
                        treeUri = treeUri
                    )
                }
                UserImagesSelectionProto.ADAPTER.typeUrl -> {
                    handleUserImagesSelection(
                        selection = selection
                            .unpack(UserImagesSelectionProto.ADAPTER)
                            .user_images_selection,
                        treeUri = treeUri
                    )
                }
                else -> {
                    handleUnsupportedSelection()
                }
            }
        }
    }

    private fun handleUserImageSelection(
        selection: UserImageSelectionProto,
        treeUri: Uri
    ) = orbit {
        transformFlow {
            imageRepository.removeMetadataSingle(
                selection = selection,
                treeUri = treeUri,
                displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix(),
                preserveOrientation = settingsRepository.shouldPreserveImageOrientation()
            )
        }.reduce {
            @Exhaustive
            when (val result = event) {
                is Result.Empty -> {
                    state.copy(imageResult = result)
                }
                is Result.Report -> {
                    val imageSummaries = state.imageSummaries.apply {
                        setOrSkip(state.imagesTotal, result.summary)
                    }
                    val imageUris = state.imageUris.apply {
                        setOrSkip(state.imagesTotal, result.summary.imageUri)
                    }
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
                        imagesTotal = state.imagesTotal + 1,
                        progress = result.progress
                    )
                }
                is Result.HandledAll -> {
                    state.copy(handledAll = true)
                }
            }
        }.sideEffect {
            if (event is Result.HandledAll) {
                post(SelectionSideEffect.SelectionHandled)
            }
        }
    }

    private fun handleUserImagesSelection(
        selection: List<UserImageSelectionProto>,
        treeUri: Uri
    ) = orbit {
        transformFlow {
            imageRepository.removeMetadataBulk(
                selection = selection,
                treeUri = treeUri,
                displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix(),
                preserveOrientation = settingsRepository.shouldPreserveImageOrientation(),
            )
        }.reduce {
            @Exhaustive
            when (val result = event) {
                is Result.Empty -> {
                    state.copy(imageResult = result)
                }
                is Result.Report -> {
                    val imageSummaries = state.imageSummaries.apply {
                        setOrSkip(state.imagesTotal, result.summary)
                    }
                    val imageUris = state.imageUris.apply {
                        setOrSkip(state.imagesTotal, result.summary.imageUri)
                    }
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
                        imagesTotal = state.imagesTotal + 1,
                        progress = result.progress
                    )
                }
                is Result.HandledAll -> {
                    state.copy(handledAll = true)
                }
            }
        }.sideEffect {
            if (event is Result.HandledAll) {
                if (event is Result.HandledAll) {
                    post(SelectionSideEffect.SelectionHandled)
                }
            }
        }
    }

    private fun handleUnsupportedSelection() = orbit {
        reduce {
            state.copy(handledAll = true)
        }
    }
}
