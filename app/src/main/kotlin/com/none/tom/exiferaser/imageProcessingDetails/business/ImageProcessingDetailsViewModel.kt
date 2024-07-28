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

package com.none.tom.exiferaser.imageProcessingDetails.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.core.extension.isNotNullOrEmpty
import com.none.tom.exiferaser.core.image.ImageProcessingSummary
import com.none.tom.exiferaser.imageProcessing.data.ImageProcessingRepository
import com.none.tom.exiferaser.imageProcessingDetails.ui.ImageProcessingDetailsFragmentArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class ImageProcessingDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val imageProcessingRepository: ImageProcessingRepository
) : ContainerHost<ImageProcessingDetailsState, ImageProcessingDetailsSideEffect>,
    ViewModel() {

    override val container =
        container<ImageProcessingDetailsState, ImageProcessingDetailsSideEffect>(
            savedStateHandle = savedStateHandle,
            initialState = ImageProcessingDetailsState(),
            onCreate = {
                handleImageProcessingSummaries()
            }
        )

    fun handleImageProcessingSummaries(
        summaries: List<ImageProcessingSummary> = ImageProcessingDetailsFragmentArgs
            .fromSavedStateHandle(savedStateHandle)
            .navArgImageProcessingSummaries
            .toList()
    ) = intent {
        reduce {
            state.copy(imageProcessingSummaries = summaries)
        }
    }

    fun handleImageModifiedDetails(position: Int) = intent {
        val summary = state.imageProcessingSummaries.getOrNull(position)
        if (summary != null) {
            postSideEffect(
                ImageProcessingDetailsSideEffect.Navigate.ToImageModifiedDetails(
                    displayName = summary.displayName,
                    extension = summary.extension,
                    mimeType = summary.mimeType,
                    imageMetadataSnapshot = summary.imageMetadataSnapshot
                )
            )
        }
    }

    fun handleImageSavedDetails(position: Int) = intent {
        val summary = state.imageProcessingSummaries.getOrNull(position)
        if (summary != null) {
            val result = imageProcessingRepository.getLastDocumentPathSegment(summary.uri)
            postSideEffect(
                if (result.isSuccess) {
                    ImageProcessingDetailsSideEffect.Navigate.ToImageSavedDetails(
                        name = result.getOrThrow()
                    )
                } else {
                    ImageProcessingDetailsSideEffect.ImageSaved.Unsupported
                }
            )
        }
    }

    fun handleViewImage(position: Int) = intent {
        val uri = state.imageProcessingSummaries.getOrNull(position)?.uri
        if (uri.isNotNullOrEmpty()) {
            postSideEffect(ImageProcessingDetailsSideEffect.ViewImage(uri))
        }
    }
}
