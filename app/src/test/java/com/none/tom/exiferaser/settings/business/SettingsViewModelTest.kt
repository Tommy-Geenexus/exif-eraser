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
import kotlinx.coroutines.test.runBlockingTest
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
    fun test_readDefaultValues() = runBlockingTest {
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
            settingsRepository.getDefaultNightModeName()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(
                        defaultPathOpenName = testDefaultPathOpenName,
                        defaultPathSaveName = testDefaultPathSaveName,
                        initialPreserveOrientation = true,
                        initialShareByDefault = true,
                        defaultDisplayNameSuffix = testDefaultDisplayNameSuffix,
                        defaultNightModeName = testDefaultNightModeName
                    )
                }
            )
        }
    }

    @Test
    fun test_handleDefaultPathOpen() = runBlockingTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultPathOpen()
        } returns flowOf(testUri)
        viewModel.testIntent {
            handleDefaultPathOpen()
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultPathOpen()
        }
        viewModel.assert(initialState) {
            postedSideEffects(SettingsSideEffect.DefaultPathOpenSelect(testUri))
        }
    }

    @Test
    fun test_clearDefaultPathOpen() = runBlockingTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultPathOpen()
        } returns flowOf(testUri)
        coEvery {
            settingsRepository.putDefaultPathOpen(any(), any(), any())
        } returns true
        viewModel.testIntent {
            clearDefaultPathOpen()
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getDefaultPathOpen()
            settingsRepository.putDefaultPathOpen(any(), any(), any())
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
    fun test_storeDefaultPathOpen() = runBlockingTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultPathOpen()
        } returns flowOf(testUri)
        coEvery {
            settingsRepository.putDefaultPathOpen(any(), any(), any())
        } returns true
        coEvery {
            settingsRepository.getDefaultPathOpenName()
        } returns testDefaultPathOpenName
        viewModel.testIntent {
            storeDefaultPathOpen(testUri)
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getDefaultPathOpen()
            settingsRepository.putDefaultPathOpen(any(), any(), any())
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
    fun test_handleDefaultPathSave() = runBlockingTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultPathSave()
        } returns flowOf(testUri)
        viewModel.testIntent {
            handleDefaultPathSave()
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultPathSave()
        }
        viewModel.assert(initialState) {
            postedSideEffects(SettingsSideEffect.DefaultPathSaveSelect(testUri))
        }
    }

    @Test
    fun test_clearDefaultPathSave() = runBlockingTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultPathSave()
        } returns flowOf(testUri)
        coEvery {
            settingsRepository.putDefaultPathSave(any(), any(), any())
        } returns true
        viewModel.testIntent {
            clearDefaultPathSave()
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getDefaultPathSave()
            settingsRepository.putDefaultPathSave(any(), any(), any())
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
    fun test_storeDefaultPathSave() = runBlockingTest {
        val initialState = SettingsState()
        val viewModel = SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(initialState)
        coEvery {
            settingsRepository.getDefaultPathSave()
        } returns flowOf(testUri)
        coEvery {
            settingsRepository.putDefaultPathSave(any(), any(), any())
        } returns true
        coEvery {
            settingsRepository.getDefaultPathSaveName()
        } returns testDefaultPathSaveName
        viewModel.testIntent {
            storeDefaultPathSave(testUri)
        }
        coVerify(ordering = Ordering.ALL) {
            settingsRepository.getDefaultPathSave()
            settingsRepository.putDefaultPathSave(any(), any(), any())
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
    fun test_storePreserveOrientation() = runBlockingTest {
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
            postedSideEffects(SettingsSideEffect.PreserveOrientation(success = true))
        }
    }

    @Test
    fun test_storeShareByDefault() = runBlockingTest {
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
            postedSideEffects(SettingsSideEffect.ShareByDefault(success = true))
        }
    }

    @Test
    fun test_handleDefaultDisplayNameSuffix() = runBlockingTest {
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
    fun test_storeDefaultDisplayNameSuffix() = runBlockingTest {
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
    fun test_handleDefaultNightMode() = runBlockingTest {
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
    fun test_storeDefaultNightMode() = runBlockingTest {
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
