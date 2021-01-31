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

package com.none.tom.exiferaser.report.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.selection.data.Summary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.strict.orbit
import org.orbitmvi.orbit.syntax.strict.reduce
import org.orbitmvi.orbit.syntax.strict.sideEffect
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ContainerHost<ReportState, ReportSideEffect>,
    ViewModel() {

    override val container = container<ReportState, ReportSideEffect>(
        savedStateHandle = savedStateHandle,
        initialState = ReportState()
    )

    fun handleImageSummaries(imageSummaries: List<Summary>) = orbit {
        reduce {
            state.copy(imageSummaries = imageSummaries)
        }
    }

    fun handleViewImage(position: Int) = orbit {
        sideEffect {
            val imageUri = state.imageSummaries.getOrNull(position)?.imageUri
            if (imageUri != null) {
                post(ReportSideEffect.ViewImage(imageUri))
            }
        }
    }

    fun handleImageDetails(position: Int) = orbit {
        sideEffect {
            val summary = state.imageSummaries.getOrNull(position)
            if (summary != null) {
                post(
                    ReportSideEffect.NavigateToDetails(
                        displayName = summary.displayName,
                        extension = summary.extension,
                        mimeType = summary.mimeType,
                        containsIccProfile = summary.containsIccProfile,
                        containsExif = summary.containsExif,
                        containsPhotoshopImageResources = summary.containsPhotoshopImageResources,
                        containsXmp = summary.containsXmp,
                        containsExtendedXmp = summary.containsExtendedXmp,
                    )
                )
            }
        }
    }
}
