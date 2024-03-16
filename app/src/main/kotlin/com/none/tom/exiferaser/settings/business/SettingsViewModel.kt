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

package com.none.tom.exiferaser.settings.business

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.none.tom.exiferaser.settings.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.viewmodel.container

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

    fun readDefaultValues() = intent {
        val isRandomizeFileNamesEnabled = settingsRepository.isRandomizeFileNamesEnabled()
        val defaultPathOpenName = settingsRepository.getDefaultOpenPathName()
        val defaultPathSaveName: String = settingsRepository.getDefaultSavePathName()
        val isAutoDeleteEnabled = settingsRepository.isAutoDeleteEnabled()
        val isPreserveOrientationEnabled = settingsRepository.isPreserveOrientationEnabled()
        val isShareByDefaultEnabled = settingsRepository.isShareByDefaultEnabled()
        val defaultDisplayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix()
        val isLegacyImageSelectionEnabled = settingsRepository.isLegacyImageSelectionEnabled()
        val isSkipSavePathSelectionEnabled = settingsRepository.isSkipSavePathSelectionEnabled()
        val defaultNightModeName = settingsRepository.getDefaultNightModeName()
        reduce {
            state.copy(
                isRandomizeFileNamesEnabled = isRandomizeFileNamesEnabled,
                defaultOpenPathName = defaultPathOpenName,
                defaultSavePathName = defaultPathSaveName,
                isAutoDeleteEnabled = isAutoDeleteEnabled,
                isPreserveOrientationEnabled = isPreserveOrientationEnabled,
                isShareByDefaultEnabled = isShareByDefaultEnabled,
                defaultDisplayNameSuffix = defaultDisplayNameSuffix,
                isLegacyImageSelectionEnabled = isLegacyImageSelectionEnabled,
                isSkipSavePathSelectionEnabled = isSkipSavePathSelectionEnabled,
                defaultNightModeName = defaultNightModeName
            )
        }
    }

    fun storeRandomizeFileNames(isEnabled: Boolean) = intent {
        val result = settingsRepository.putRandomizeFileNames(isEnabled)
        if (result.isSuccess) {
            reduce {
                state.copy(isRandomizeFileNamesEnabled = isEnabled)
            }
            postSideEffect(SettingsSideEffect.RandomizeFileNames.Success)
        } else {
            postSideEffect(SettingsSideEffect.RandomizeFileNames.Failure)
        }
    }

    fun handleDefaultPathOpen() = intent {
        val result = settingsRepository.getPrivilegedDefaultOpenPath()
        if (result.isSuccess) {
            postSideEffect(
                SettingsSideEffect.DefaultOpenPath.Select.Success(uri = result.getOrThrow())
            )
        } else {
            postSideEffect(SettingsSideEffect.DefaultOpenPath.Select.Failure)
        }
    }

    fun clearDefaultPathOpen() = intent {
        val result = settingsRepository.putDefaultOpenPath(
            newDefaultOpenPath = Uri.EMPTY,
            currentDefaultOpenPath = settingsRepository
                .getPrivilegedDefaultOpenPath()
                .getOrDefault(Uri.EMPTY)
        )
        if (result.isSuccess) {
            reduce {
                state.copy(defaultOpenPathName = "")
            }
            postSideEffect(SettingsSideEffect.DefaultOpenPath.Clear.Success)
        } else {
            postSideEffect(SettingsSideEffect.DefaultOpenPath.Clear.Failure)
        }
    }

    fun storeDefaultPathOpen(uri: Uri) = intent {
        val result = settingsRepository.putDefaultOpenPath(
            newDefaultOpenPath = uri,
            currentDefaultOpenPath = settingsRepository
                .getPrivilegedDefaultOpenPath()
                .getOrDefault(Uri.EMPTY)
        )
        if (result.isSuccess) {
            val name = settingsRepository.getDefaultOpenPathName()
            reduce {
                state.copy(defaultOpenPathName = name)
            }
            postSideEffect(SettingsSideEffect.DefaultOpenPath.Store.Success)
        } else {
            postSideEffect(SettingsSideEffect.DefaultOpenPath.Store.Failure)
        }
    }

    fun handleDefaultPathSave() = intent {
        val result = settingsRepository.getPrivilegedDefaultSavePath()
        if (result.isSuccess) {
            postSideEffect(
                SettingsSideEffect.DefaultSavePath.Select.Success(uri = result.getOrThrow())
            )
        } else {
            postSideEffect(SettingsSideEffect.DefaultSavePath.Select.Failure)
        }
    }

    fun clearDefaultPathSave() = intent {
        val result = settingsRepository.putDefaultSavePath(
            newDefaultSavePath = Uri.EMPTY,
            currentDefaultSavePath = settingsRepository
                .getPrivilegedDefaultSavePath()
                .getOrDefault(Uri.EMPTY)
        )
        if (result.isSuccess) {
            reduce {
                state.copy(defaultSavePathName = "")
            }
            postSideEffect(SettingsSideEffect.DefaultSavePath.Clear.Success)
        } else {
            postSideEffect(SettingsSideEffect.DefaultSavePath.Clear.Failure)
        }
    }

    fun storeDefaultPathSave(uri: Uri) = intent {
        val result = settingsRepository.putDefaultSavePath(
            newDefaultSavePath = uri,
            currentDefaultSavePath = settingsRepository
                .getPrivilegedDefaultSavePath()
                .getOrDefault(Uri.EMPTY)
        )
        if (result.isSuccess) {
            val name = settingsRepository.getDefaultSavePathName()
            reduce {
                state.copy(defaultSavePathName = name)
            }
            postSideEffect(SettingsSideEffect.DefaultSavePath.Store.Success)
        } else {
            postSideEffect(SettingsSideEffect.DefaultSavePath.Store.Failure)
        }
    }

    fun storeAutoDelete(isEnabled: Boolean) = intent {
        val result = settingsRepository.putAutoDelete(isEnabled)
        if (result.isSuccess) {
            reduce {
                state.copy(isAutoDeleteEnabled = isEnabled)
            }
            postSideEffect(SettingsSideEffect.AutoDelete.Success)
        } else {
            postSideEffect(SettingsSideEffect.AutoDelete.Failure)
        }
    }

    fun storePreserveOrientation(isEnabled: Boolean) = intent {
        val result = settingsRepository.putPreserveOrientation(isEnabled)
        if (result.isSuccess) {
            reduce {
                state.copy(isPreserveOrientationEnabled = isEnabled)
            }
            postSideEffect(SettingsSideEffect.PreserveOrientation.Success)
        } else {
            postSideEffect(SettingsSideEffect.PreserveOrientation.Failure)
        }
    }

    fun storeShareByDefault(isEnabled: Boolean) = intent {
        val result = settingsRepository.putShareByDefault(isEnabled)
        if (result.isSuccess) {
            reduce {
                state.copy(isShareByDefaultEnabled = isEnabled)
            }
            postSideEffect(SettingsSideEffect.ShareByDefault.Success)
        } else {
            postSideEffect(SettingsSideEffect.ShareByDefault.Failure)
        }
    }

    fun handleDefaultDisplayNameSuffix() = intent {
        postSideEffect(
            SettingsSideEffect.Navigate.ToDefaultDisplayNameSuffix(
                defaultDisplayNameSuffix = settingsRepository.getDefaultDisplayNameSuffix()
            )
        )
    }

    fun storeDefaultDisplayNameSuffix(value: String) = intent {
        val result = settingsRepository.putDefaultDisplayNameSuffix(value)
        if (result.isSuccess) {
            reduce {
                state.copy(defaultDisplayNameSuffix = value)
            }
            postSideEffect(SettingsSideEffect.DefaultDisplayNameSuffix.Success)
        } else {
            postSideEffect(SettingsSideEffect.DefaultDisplayNameSuffix.Failure)
        }
    }

    fun storeLegacyImageSelection(isEnabled: Boolean) = intent {
        val result = settingsRepository.putSelectImagesLegacy(isEnabled)
        if (result.isSuccess) {
            reduce {
                state.copy(isLegacyImageSelectionEnabled = isEnabled)
            }
            postSideEffect(SettingsSideEffect.LegacyImageSelection.Success)
        } else {
            postSideEffect(SettingsSideEffect.LegacyImageSelection.Failure)
        }
    }

    fun storeSavePathSelectionSkip(isEnabled: Boolean) = intent {
        val result = settingsRepository.putSavePathSelectionSkip(isEnabled)
        if (result.isSuccess) {
            reduce {
                state.copy(isSkipSavePathSelectionEnabled = isEnabled)
            }
            postSideEffect(SettingsSideEffect.DefaultSavePath.SelectionSkip.Success)
        } else {
            postSideEffect(SettingsSideEffect.DefaultSavePath.SelectionSkip.Failure)
        }
    }

    fun handleDefaultNightMode() = intent {
        postSideEffect(
            SettingsSideEffect.Navigate.ToDefaultNightMode(
                defaultNightMode = settingsRepository.getDefaultNightMode()
            )
        )
    }

    fun storeDefaultNightMode(value: Int) = intent {
        val result = settingsRepository.putDefaultNightMode(value)
        if (result.isSuccess) {
            val name = settingsRepository.getDefaultNightModeName()
            reduce {
                state.copy(defaultNightModeName = name)
            }
            postSideEffect(SettingsSideEffect.DefaultNightMode.Success)
        } else {
            postSideEffect(SettingsSideEffect.DefaultNightMode.Failure)
        }
    }
}
