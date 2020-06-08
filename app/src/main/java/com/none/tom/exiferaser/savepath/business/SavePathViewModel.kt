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

package com.none.tom.exiferaser.savepath.business

import android.net.Uri
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.babylon.orbit2.Container
import com.babylon.orbit2.ContainerHost
import com.babylon.orbit2.coroutines.transformSuspend
import com.babylon.orbit2.syntax.strict.orbit
import com.babylon.orbit2.syntax.strict.reduce
import com.babylon.orbit2.syntax.strict.sideEffect
import com.babylon.orbit2.viewmodel.container
import com.none.tom.exiferaser.settings.data.SettingsRepository

class SavePathViewModel @ViewModelInject constructor(
    @Assisted savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : ContainerHost<SavePathState, SavePathSideEffect>,
    ViewModel() {

    override val container: Container<SavePathState, SavePathSideEffect> =
        container(
            initialState = SavePathState(),
            savedStateHandle = savedStateHandle,
            onCreate = { verifyHasPrivilegedDefaultSavePath() }
        )

    private fun verifyHasPrivilegedDefaultSavePath() = orbit {
        transformSuspend {
            settingsRepository.hasPrivilegedDefaultSavePath()
        }.reduce {
            state.copy(hasPrivilegedDefaultSavePath = event)
        }
    }

    fun chooseSelectionSavePath(openPath: Uri = settingsRepository.getDefaultOpenPath()) = orbit {
        sideEffect {
            post(SavePathSideEffect.ChooseSavePath(openPath))
        }
    }

    fun navigateToSelection(savePath: Uri = settingsRepository.getDefaultSavePath()) = orbit {
        sideEffect {
            post(SavePathSideEffect.NavigateTo(savePath))
        }
    }
}
