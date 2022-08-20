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

package com.none.tom.exiferaser.settings.business

import android.content.ContentResolver
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.none.tom.exiferaser.settings.defaultNightModeValue
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>()
    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()
    private val testDefaultPathOpenName = "path_open"
    private val testDefaultPathSaveName = "path_save"
    private val testDefaultDisplayNameSuffix = "suffix"
    private val testDefaultNightModeName = "Always"

    @Test
    fun test_readDefaultValues() = runTest {
        coEvery {
            settingsRepository.getDefaultPathOpenName()
        } returns testDefaultPathOpenName
        coEvery {
            settingsRepository.getDefaultPathSaveName()
        } returns testDefaultPathSaveName
        coEvery {
            settingsRepository.shouldPreserveOrientation()
        } returns flowOf(true)
        coEvery {
            settingsRepository.shouldShareByDefault()
        } returns flowOf(true)
        coEvery {
            settingsRepository.getDefaultDisplayNameSuffix()
        } returns flowOf(testDefaultDisplayNameSuffix)
        coEvery {
            settingsRepository.shouldSkipSavePathSelection()
        } returns flowOf(true)
        coEvery {
            settingsRepository.getDefaultNightModeName()
        } returns testDefaultNightModeName
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.runOnCreate()
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getDefaultPathOpenName()
            settingsRepository.getDefaultPathSaveName()
            settingsRepository.shouldPreserveOrientation()
            settingsRepository.shouldShareByDefault()
            settingsRepository.getDefaultDisplayNameSuffix()
            settingsRepository.shouldSkipSavePathSelection()
            settingsRepository.getDefaultNightModeName()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(
                        defaultPathOpenName = testDefaultPathOpenName,
                        defaultPathSaveName = testDefaultPathSaveName,
                        preserveOrientation = true,
                        shareByDefault = true,
                        defaultDisplayNameSuffix = testDefaultDisplayNameSuffix,
                        skipSavePathSelection = true,
                        defaultNightModeName = testDefaultNightModeName
                    )
                }
            )
        }
    }

    @Test
    fun test_handleDefaultPathOpen() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        } returns testUri
        viewModel.testIntent {
            handleDefaultPathOpen()
        }
        coVerify(exactly = 1) {
            settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        }
        viewModel.assert(initialState) {
            postedSideEffects(SettingsSideEffect.DefaultPathOpenSelect(testUri))
        }
    }

    @Test
    fun test_clearDefaultPathOpen() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        } returns testUri
        coEvery {
            settingsRepository.putDefaultPathOpen(any(), any())
        } returns true
        viewModel.testIntent {
            clearDefaultPathOpen()
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
            settingsRepository.putDefaultPathOpen(any(), any())
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(defaultPathOpenName = String.Empty)
                }
            )
            postedSideEffects(SettingsSideEffect.DefaultPathOpenClear(success = true))
        }
    }

    @Test
    fun test_storeDefaultPathOpen() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
        } returns testUri
        coEvery {
            settingsRepository.putDefaultPathOpen(any(), any())
        } returns true
        coEvery {
            settingsRepository.getDefaultPathOpenName()
        } returns testDefaultPathOpenName
        viewModel.testIntent {
            storeDefaultPathOpen(testUri)
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
            settingsRepository.putDefaultPathOpen(any(), any())
            settingsRepository.getDefaultPathOpenName()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(defaultPathOpenName = testDefaultPathOpenName)
                }
            )
            postedSideEffects(SettingsSideEffect.DefaultPathOpenStore(success = true))
        }
    }

    @Test
    fun test_handleDefaultPathSave() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        } returns testUri
        viewModel.testIntent {
            handleDefaultPathSave()
        }
        coVerify(exactly = 1) {
            settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        }
        viewModel.assert(initialState) {
            postedSideEffects(SettingsSideEffect.DefaultPathSaveSelect(testUri))
        }
    }

    @Test
    fun test_clearDefaultPathSave() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        } returns testUri
        coEvery {
            settingsRepository.putDefaultPathSave(any(), any())
        } returns true
        viewModel.testIntent {
            clearDefaultPathSave()
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
            settingsRepository.putDefaultPathSave(any(), any())
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(defaultPathSaveName = String.Empty)
                }
            )
            postedSideEffects(SettingsSideEffect.DefaultPathSaveClear(success = true))
        }
    }

    @Test
    fun test_storeDefaultPathSave() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
        } returns testUri
        coEvery {
            settingsRepository.putDefaultPathSave(any(), any())
        } returns true
        coEvery {
            settingsRepository.getDefaultPathSaveName()
        } returns testDefaultPathSaveName
        viewModel.testIntent {
            storeDefaultPathSave(testUri)
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
            settingsRepository.putDefaultPathSave(any(), any())
            settingsRepository.getDefaultPathSaveName()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(defaultPathSaveName = testDefaultPathSaveName)
                }
            )
            postedSideEffects(SettingsSideEffect.DefaultPathSaveStore(success = true))
        }
    }

    @Test
    fun test_storePreserveOrientation() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.putPreserveOrientation(any())
        } returns true
        viewModel.testIntent {
            storePreserveOrientation(true)
        }
        coVerify(exactly = 1) {
            settingsRepository.putPreserveOrientation(any())
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(preserveOrientation = true)
                }
            )
            postedSideEffects(SettingsSideEffect.PreserveOrientation(success = true))
        }
    }

    @Test
    fun test_storeShareByDefault() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.putShareByDefault(any())
        } returns true
        viewModel.testIntent {
            storeShareByDefault(true)
        }
        coVerify(exactly = 1) {
            settingsRepository.putShareByDefault(any())
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(shareByDefault = true)
                }
            )
            postedSideEffects(SettingsSideEffect.ShareByDefault(success = true))
        }
    }

    @Test
    fun test_handleDefaultDisplayNameSuffix() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultDisplayNameSuffix()
        } returns flowOf(testDefaultDisplayNameSuffix)
        viewModel.testIntent {
            handleDefaultDisplayNameSuffix()
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultDisplayNameSuffix()
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                SettingsSideEffect.NavigateToDefaultDisplayNameSuffix(
                    defaultDisplayNameSuffix = testDefaultDisplayNameSuffix
                )
            )
        }
    }

    @Test
    fun test_storeDefaultDisplayNameSuffix() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.putDefaultDisplayNameSuffix(any())
        } returns true
        viewModel.testIntent {
            storeDefaultDisplayNameSuffix(testDefaultDisplayNameSuffix)
        }
        coVerify(exactly = 1) {
            settingsRepository.putDefaultDisplayNameSuffix(any())
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(defaultDisplayNameSuffix = testDefaultDisplayNameSuffix)
                }
            )
        }
    }

    @Test
    fun test_storeSavePathSelectionSkip() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.putSavePathSelectionSkip(any())
        } returns true
        viewModel.testIntent {
            storeSavePathSelectionSkip(true)
        }
        coVerify(exactly = 1) {
            settingsRepository.putSavePathSelectionSkip(any())
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(skipSavePathSelection = true)
                }
            )
            postedSideEffects(SettingsSideEffect.SavePathSelectionSkip(success = true))
        }
    }

    @Test
    fun test_handleDefaultNightMode() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultNightMode()
        } returns flowOf(defaultNightModeValue)
        viewModel.testIntent {
            handleDefaultNightMode()
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultNightMode()
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                SettingsSideEffect.NavigateToDefaultNightMode(
                    defaultNightMode = defaultNightModeValue
                )
            )
        }
    }

    @Test
    fun test_storeDefaultNightMode() = runTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        val testDefaultNightModeName = "Always"
        coEvery {
            settingsRepository.putDefaultNightMode(any())
        } returns true
        coEvery {
            settingsRepository.getDefaultNightModeName()
        } returns testDefaultNightModeName
        viewModel.testIntent {
            storeDefaultNightMode(defaultNightModeValue)
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.putDefaultNightMode(any())
            settingsRepository.getDefaultNightModeName()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(defaultNightModeName = testDefaultNightModeName)
                }
            )
        }
    }
}
