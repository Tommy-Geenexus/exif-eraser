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

package com.none.tom.exiferaser.savepath.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.coroutines.transformSuspend
import org.orbitmvi.orbit.syntax.strict.orbit
import org.orbitmvi.orbit.syntax.strict.reduce
import org.orbitmvi.orbit.syntax.strict.sideEffect
import org.orbitmvi.orbit.viewmodel.container

@HiltViewModel
class SavePathViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : ContainerHost<SavePathState, SavePathSideEffect>,
    ViewModel() {

    override val container = container<SavePathState, SavePathSideEffect>(
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

    @ExperimentalContracts
    fun navigateToSelection(savePath: Uri? = settingsRepository.getDefaultSavePath()) = orbit {
        sideEffect {
            if (savePath.isNotNullOrEmpty()) {
                post(SavePathSideEffect.NavigateToSelection(savePath))
            }
        }
    }
}
