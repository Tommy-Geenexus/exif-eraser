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

package com.none.tom.exiferaser.imageProcessing.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.core.extension.addOrShift
import com.none.tom.exiferaser.core.image.ImageProcessingStep
import com.none.tom.exiferaser.imageProcessing.data.ImageProcessingRepository
import com.none.tom.exiferaser.imageProcessing.ui.ImageProcessingFragmentArgs
import com.none.tom.exiferaser.main.data.SelectionRepository
import com.none.tom.exiferaser.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class ImageProcessingViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageProcessingRepository: ImageProcessingRepository,
    private val selectionRepository: SelectionRepository,
    private val settingsRepository: SettingsRepository
) : ContainerHost<ImageProcessingState, ImageProcessingSideEffect>,
    ViewModel() {

    private companion object {
        const val IMAGE_PROCESSING_REPORT_IMAGES_MAX = 100
    }

    override val container = container<ImageProcessingState, ImageProcessingSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = ImageProcessingState(),
        onCreate = {
            if (state.imageImageProcessingStep !is ImageProcessingStep.FinishedBulk) {
                readSelection(fromIndex = state.imagesProcessedCount)
            }
        }
    )

    fun handleUserImagesSelection(protos: List<UserImageSelectionProto>, treeUri: Uri) = intent {
        imageProcessingRepository.removeMetadata(
            protos = protos,
            treeUri = treeUri,
            displayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix(),
            isAutoDeleteEnabled = settingsRepository.isAutoDeleteEnabled(),
            isPreserveOrientationEnabled = settingsRepository.isPreserveOrientationEnabled(),
            isRandomizeFileNamesEnabled = settingsRepository.isRandomizeFileNamesEnabled()
        ).collect { result ->
            reduce {
                when (result) {
                    is ImageProcessingStep.Idle,
                    is ImageProcessingStep.FinishedBulk -> {
                        state.copy(imageImageProcessingStep = result)
                    }
                    is ImageProcessingStep.FinishedSingle -> {
                        val imageProcessingSummaries = state
                            .imageProcessingSummaries
                            .toMutableList()
                            .addOrShift(
                                element = result.imageProcessingSummary,
                                shiftAtSize = IMAGE_PROCESSING_REPORT_IMAGES_MAX
                            )
                        val uris = state
                            .uris
                            .toMutableList()
                            .addOrShift(
                                element = result.imageProcessingSummary.uri,
                                shiftAtSize = IMAGE_PROCESSING_REPORT_IMAGES_MAX
                            )
                        val isMetadataContained = result
                            .imageProcessingSummary
                            .imageMetadataSnapshot
                            .isMetadataContained()
                        val imagesWithMetadataCount = state
                            .imagesWithMetadataCount
                            .plus(if (isMetadataContained) 1 else 0)
                        val imagesSavedCount = state
                            .imagesSavedCount
                            .plus(if (result.imageProcessingSummary.isImageSaved) 1 else 0)
                        state.copy(
                            imageImageProcessingStep = result,
                            imageProcessingSummaries = imageProcessingSummaries,
                            uris = uris,
                            imagesWithMetadataCount = imagesWithMetadataCount,
                            imagesSavedCount = imagesSavedCount,
                            imagesProcessedCount = state.imagesProcessedCount.inc(),
                            progress = result.progress
                        )
                    }
                }
            }
            if (result is ImageProcessingStep.FinishedBulk &&
                settingsRepository.isShareByDefaultEnabled()
            ) {
                shareImages()
            }
        }
    }

    fun handleUnsupportedSelection() = intent {
        reduce {
            state.copy(imageImageProcessingStep = ImageProcessingStep.FinishedBulk)
        }
    }

    fun readSelection(
        fromIndex: Int,
        treeUri: Uri = ImageProcessingFragmentArgs.fromSavedStateHandle(savedStateHandle).savePath
    ) = intent {
        val protos = selectionRepository.getSelection(fromIndex)
        postSideEffect(
            if (protos.isEmpty()) {
                ImageProcessingSideEffect.Handle.UnsupportedSelection
            } else {
                ImageProcessingSideEffect.Handle.UserImagesSelection(protos, treeUri)
            }
        )
    }

    fun shareImages() = intent {
        state
            .imageProcessingSummaries
            .filter { summary -> summary.isImageSaved }
            .map { summary -> summary.uri }
            .takeIf { uris -> uris.isNotEmpty() }
            ?.let { uris -> postSideEffect(ImageProcessingSideEffect.ShareImages(ArrayList(uris))) }
    }
}
