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

package com.none.tom.exiferaser.selection.business

import android.net.Uri
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import app.cash.exhaustive.Exhaustive
import com.babylon.orbit2.ContainerHost
import com.babylon.orbit2.coroutines.transformFlow
import com.babylon.orbit2.coroutines.transformSuspend
import com.babylon.orbit2.syntax.strict.orbit
import com.babylon.orbit2.syntax.strict.reduce
import com.babylon.orbit2.syntax.strict.sideEffect
import com.babylon.orbit2.viewmodel.container
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.selection.data.Result
import com.none.tom.exiferaser.selection.setOrSkip
import com.none.tom.exiferaser.selection.toInt
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.squareup.wire.AnyMessage

class SelectionViewModel @ViewModelInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    private val imageRepository: ImageRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository
) : ContainerHost<SelectionState, SelectionSideEffect>,
    ViewModel() {

    override val container = container<SelectionState, SelectionSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = SelectionState(),
        onCreate = { readSelection() }
    )

    private fun readSelection() = orbit {
        transformFlow {
            selectionRepository.getSelection()
        }.sideEffect {
            post(SelectionSideEffect.ReadComplete(event))
        }
    }

    fun prepareReport() = orbit {
        sideEffect {
            post(
                SelectionSideEffect.PrepareReport(
                    imageSummaries = container.currentState.imageSummaries.filterNotNull()
                )
            )
        }
    }

    fun shareImages() = orbit {
        sideEffect {
            post(
                SelectionSideEffect.ShareImages(
                    imagePaths = ArrayList(container.currentState.imagePaths.filterNotNull())
                )
            )
        }
    }

    fun handleSelection(
        message: AnyMessage?,
        parentDirectoryPath: Uri
    ) = orbit {
        transformSuspend {
            if (message == null) {
                handleUnsupportedSelection()
                return@transformSuspend
            }
            when (message.typeUrl) {
                UserImageSelectionProto.ADAPTER.typeUrl -> {
                    handleUserImageSelection(
                        proto = listOf(message.unpack(UserImageSelectionProto.ADAPTER))
                            .drop(state.imagesTotal)
                            .firstOrNull()
                            ?: UserImageSelectionProto(),
                        parentDirectoryPath = parentDirectoryPath
                    )
                }
                UserImagesSelectionProto.ADAPTER.typeUrl -> {
                    handleUserImagesSelection(
                        protos = (message.unpack(UserImagesSelectionProto.ADAPTER))
                            .user_images_selection
                            .drop(state.imagesTotal),
                        parentDirectoryPath = parentDirectoryPath
                    )
                }
                else -> {
                    handleUnsupportedSelection()
                }
            }
        }
    }

    private fun handleUserImageSelection(
        proto: UserImageSelectionProto,
        parentDirectoryPath: Uri
    ) = orbit {
        transformFlow {
            imageRepository.removeMetaDataSingle(
                proto = proto,
                parentDirectoryPath = parentDirectoryPath,
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
                    val summaries = state.imageSummaries.apply {
                        setOrSkip(state.imagesTotal, result.summary)
                    }
                    val modified = state.imagesModified + result.summary.imageModified.toInt()
                    val saved = state.imagesSaved + result.summary.imageSaved.toInt()
                    state.copy(
                        imageResult = result,
                        imageSummaries = summaries,
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
            }
        }
    }

    private fun handleUserImagesSelection(
        protos: List<UserImageSelectionProto>,
        parentDirectoryPath: Uri
    ) = orbit {
        transformFlow {
            imageRepository.removeMetaDataBulk(
                protos = protos,
                parentDirectoryPath = parentDirectoryPath,
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
                    val summaries = state.imageSummaries.apply {
                        setOrSkip(state.imagesTotal, result.summary)
                    }
                    val modified = state.imagesModified + result.summary.imageModified.toInt()
                    val saved = state.imagesSaved + result.summary.imageSaved.toInt()
                    state.copy(
                        imageResult = result,
                        imageSummaries = summaries,
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
            }
        }
    }

    private fun handleUnsupportedSelection() = orbit {
        reduce {
            state.copy(progress = PROGRESS_MAX)
        }
    }
}
