/*
 * Copyright (c) 2018-2023, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.settings.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.none.tom.exiferaser.settings.defaultNightModeValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.syntax.simple.reduce
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository
) : ViewModel(),
    ContainerHost<SettingsState, SettingsSideEffect> {

    override val container = container<SettingsState, SettingsSideEffect>(
        initialState = SettingsState(),
        savedStateHandle = savedStateHandle,
        onCreate = {
            readDefaultValues()
        }
    )

    private fun readDefaultValues() = intent {
        val defaultPathOpenName = settingsRepository.getDefaultPathOpenName()
        val defaultPathSaveName: String = settingsRepository.getDefaultPathSaveName()
        val preserveOrientation = settingsRepository.shouldPreserveOrientation().firstOrNull()
        val shareByDefault = settingsRepository.shouldShareByDefault().firstOrNull()
        val defaultDisplayNameSuffix =
            settingsRepository.getDefaultDisplayNameSuffix().firstOrNull()
        val skipSavePathSelection = settingsRepository.shouldSkipSavePathSelection().firstOrNull()
        val defaultNightModeName = settingsRepository.getDefaultNightModeName()
        reduce {
            state.copy(
                defaultPathOpenName = defaultPathOpenName,
                defaultPathSaveName = defaultPathSaveName,
                preserveOrientation = preserveOrientation == true,
                shareByDefault = shareByDefault == true,
                defaultDisplayNameSuffix = defaultDisplayNameSuffix.orEmpty(),
                skipSavePathSelection = skipSavePathSelection == true,
                defaultNightModeName = defaultNightModeName
            )
        }
    }

    fun handleDefaultPathOpen() = intent {
        val pathOpen = settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        postSideEffect(SettingsSideEffect.DefaultPathOpenSelect(pathOpen))
    }

    fun clearDefaultPathOpen() = intent {
        val pathOpen = settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        val success = settingsRepository.putDefaultPathOpen(
            defaultPathOpenNew = Uri.EMPTY,
            defaultPathOpenCurrent = pathOpen
        )
        if (success) {
            reduce {
                state.copy(defaultPathOpenName = String.Empty)
            }
        }
        postSideEffect(SettingsSideEffect.DefaultPathOpenClear(success))
    }

    fun storeDefaultPathOpen(uriNew: Uri) = intent {
        val pathOpen = settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        val success = settingsRepository.putDefaultPathOpen(
            defaultPathOpenNew = uriNew,
            defaultPathOpenCurrent = pathOpen
        )
        if (success) {
            val name = settingsRepository.getDefaultPathOpenName()
            reduce {
                state.copy(defaultPathOpenName = name)
            }
        }
        postSideEffect(SettingsSideEffect.DefaultPathOpenStore(success))
    }

    fun handleDefaultPathSave() = intent {
        val pathSave = settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        postSideEffect(SettingsSideEffect.DefaultPathSaveSelect(pathSave))
    }

    fun clearDefaultPathSave() = intent {
        val pathSave = settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        val success = settingsRepository.putDefaultPathSave(
            defaultPathSaveNew = Uri.EMPTY,
            defaultPathSaveCurrent = pathSave
        )
        if (success) {
            reduce {
                state.copy(defaultPathSaveName = String.Empty)
            }
        }
        postSideEffect(SettingsSideEffect.DefaultPathSaveClear(success))
    }

    fun storeDefaultPathSave(uriNew: Uri) = intent {
        val pathSave = settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        val success = settingsRepository.putDefaultPathSave(
            defaultPathSaveNew = uriNew,
            defaultPathSaveCurrent = pathSave
        )
        if (success) {
            val name = settingsRepository.getDefaultPathSaveName()
            reduce {
                state.copy(defaultPathSaveName = name)
            }
        }
        postSideEffect(SettingsSideEffect.DefaultPathSaveStore(success))
    }

    fun storeAutoDelete(value: Boolean) = intent {
        val success = settingsRepository.putAutoDelete(value)
        if (success) {
            reduce {
                state.copy(autoDelete = value)
            }
        }
        postSideEffect(SettingsSideEffect.AutoDelete(success))
    }

    fun storePreserveOrientation(value: Boolean) = intent {
        val success = settingsRepository.putPreserveOrientation(value)
        if (success) {
            reduce {
                state.copy(preserveOrientation = value)
            }
        }
        postSideEffect(SettingsSideEffect.PreserveOrientation(success))
    }

    fun storeShareByDefault(value: Boolean) = intent {
        val success = settingsRepository.putShareByDefault(value)
        if (success) {
            reduce {
                state.copy(shareByDefault = value)
            }
        }
        postSideEffect(SettingsSideEffect.ShareByDefault(success))
    }

    fun handleDefaultDisplayNameSuffix() = intent {
        val value = settingsRepository.getDefaultDisplayNameSuffix().firstOrNull().orEmpty()
        postSideEffect(SettingsSideEffect.NavigateToDefaultDisplayNameSuffix(value))
    }

    fun storeDefaultDisplayNameSuffix(value: String) = intent {
        val success = settingsRepository.putDefaultDisplayNameSuffix(value)
        if (success) {
            reduce {
                state.copy(defaultDisplayNameSuffix = value)
            }
        }
    }

    fun storeSavePathSelectionSkip(value: Boolean) = intent {
        val success = settingsRepository.putSavePathSelectionSkip(value)
        if (success) {
            reduce {
                state.copy(skipSavePathSelection = value)
            }
        }
        postSideEffect(SettingsSideEffect.SavePathSelectionSkip(success))
    }

    fun handleDefaultNightMode() = intent {
        val value = settingsRepository.getDefaultNightMode().firstOrNull() ?: defaultNightModeValue
        postSideEffect(SettingsSideEffect.NavigateToDefaultNightMode(value))
    }

    fun storeDefaultNightMode(value: Int) = intent {
        val success = settingsRepository.putDefaultNightMode(value)
        if (success) {
            val name = settingsRepository.getDefaultNightModeName()
            reduce {
                state.copy(defaultNightModeName = name)
            }
        }
    }
}
