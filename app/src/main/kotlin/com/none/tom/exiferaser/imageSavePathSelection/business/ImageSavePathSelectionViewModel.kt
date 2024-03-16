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

package com.none.tom.exiferaser.imageSavePathSelection.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.isNotEmpty
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class ImageSavePathSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : ContainerHost<ImageSavePathSelectionState, ImageSavePathSelectionSideEffect>,
    ViewModel() {

    override val container =
        container<ImageSavePathSelectionState, ImageSavePathSelectionSideEffect>(
            initialState = ImageSavePathSelectionState(),
            savedStateHandle = savedStateHandle,
            onCreate = {
                verifyHasPrivilegedDefaultSavePath()
            }
        )

    fun chooseSelectionSavePath(openPath: Uri = Uri.EMPTY) = intent {
        val realOpenPath = if (openPath.isNotEmpty()) {
            openPath
        } else {
            settingsRepository.getPrivilegedDefaultOpenPath().getOrDefault(Uri.EMPTY)
        }
        postSideEffect(ImageSavePathSelectionSideEffect.ChooseSavePath(realOpenPath))
    }

    fun handleSelection(savePath: Uri? = null) = intent {
        val realSavePath = if (savePath.isNotNullOrEmpty()) {
            savePath
        } else {
            settingsRepository.getPrivilegedDefaultSavePath().getOrDefault(Uri.EMPTY)
        }
        if (realSavePath.isNotEmpty()) {
            postSideEffect(ImageSavePathSelectionSideEffect.NavigateToSelection(realSavePath))
        }
    }

    fun verifyHasPrivilegedDefaultSavePath() = intent {
        val hasPrivilegedDefaultSavePath = settingsRepository
            .getPrivilegedDefaultSavePath()
            .getOrDefault(Uri.EMPTY)
            .isNotEmpty()
        reduce {
            state.copy(hasPrivilegedDefaultSavePath = hasPrivilegedDefaultSavePath)
        }
    }
}
