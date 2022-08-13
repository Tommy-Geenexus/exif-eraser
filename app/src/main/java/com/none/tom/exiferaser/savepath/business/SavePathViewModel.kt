/*
 * Copyright (c) 2018-2022, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import com.none.tom.exiferaser.isNotEmpty
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@HiltViewModel
class SavePathViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : ContainerHost<SavePathState, SavePathSideEffect>,
    ViewModel() {

    override val container = container<SavePathState, SavePathSideEffect>(
        initialState = SavePathState(),
        savedStateHandle = savedStateHandle,
        onCreate = {
            verifyHasPrivilegedDefaultSavePath()
        }
    )

    fun chooseSelectionSavePath(openPath: Uri = Uri.EMPTY) = intent {
        val realOpenPath = if (openPath.isNotEmpty()) {
            openPath
        } else {
            settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        }
        postSideEffect(SavePathSideEffect.ChooseSavePath(realOpenPath))
    }

    fun handleSelection(savePath: Uri? = null) = intent {
        val realSavePath = if (savePath.isNotNullOrEmpty()) {
            savePath
        } else {
            settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        }
        if (realSavePath.isNotEmpty()) {
            postSideEffect(SavePathSideEffect.NavigateToSelection(realSavePath))
        }
    }

    fun verifyHasPrivilegedDefaultSavePath() = intent {
        val savePath = settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        reduce {
            state.copy(hasPrivilegedDefaultSavePath = savePath.isNotEmpty())
        }
    }
}
