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

package com.none.tom.exiferaser.details.business

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.strict.orbit
import org.orbitmvi.orbit.syntax.strict.reduce
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ContainerHost<DetailsState, Nothing>,
    ViewModel() {

    override val container = container<DetailsState, Nothing>(
        savedStateHandle = savedStateHandle,
        initialState = DetailsState()
    )

    fun handleImageDetails(
        extension: String,
        mimeType: String,
        containsIccProfile: Boolean,
        containsExif: Boolean,
        containsPhotoshopImageResources: Boolean,
        containsXmp: Boolean,
        containsExtendedXmp: Boolean
    ) = orbit {
        reduce {
            state.copy(
                extension = extension,
                mimeType = mimeType,
                containsIccProfile = containsIccProfile,
                containsExif = containsExif,
                containsPhotoshopImageResources = containsPhotoshopImageResources,
                containsXmp = containsXmp,
                containsExtendedXmp = containsExtendedXmp
            )
        }
    }
}
