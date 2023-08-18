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

import android.content.ContentResolver
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
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
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
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
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.shouldRandomizeFileNames()
                } returns flowOf(true)
                coEvery {
                    settingsRepository.getDefaultPathOpenName()
                } returns testDefaultPathOpenName
                coEvery {
                    settingsRepository.getDefaultPathSaveName()
                } returns testDefaultPathSaveName
                coEvery {
                    settingsRepository.shouldAutoDelete()
                } returns flowOf(true)
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
                    settingsRepository.shouldSelectImagesLegacy()
                } returns flowOf(true)
                coEvery {
                    settingsRepository.shouldSkipSavePathSelection()
                } returns flowOf(true)
                coEvery {
                    settingsRepository.getDefaultNightModeName()
                } returns testDefaultNightModeName
                readDefaultValues()
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.shouldRandomizeFileNames()
                settingsRepository.getDefaultPathOpenName()
                settingsRepository.getDefaultPathSaveName()
                settingsRepository.shouldAutoDelete()
                settingsRepository.shouldPreserveOrientation()
                settingsRepository.shouldShareByDefault()
                settingsRepository.getDefaultDisplayNameSuffix()
                settingsRepository.shouldSelectImagesLegacy()
                settingsRepository.shouldSkipSavePathSelection()
                settingsRepository.getDefaultNightModeName()
            }
            expectState {
                copy(
                    randomizeFileNames = true,
                    defaultPathOpenName = testDefaultPathOpenName,
                    defaultPathSaveName = testDefaultPathSaveName,
                    autoDelete = true,
                    preserveOrientation = true,
                    shareByDefault = true,
                    defaultDisplayNameSuffix = testDefaultDisplayNameSuffix,
                    legacyImageSelection = true,
                    skipSavePathSelection = true,
                    defaultNightModeName = testDefaultNightModeName
                )
            }
        }
    }

    @Test
    fun test_storeRandomizeFileNames() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            coEvery {
                settingsRepository.putRandomizeFileNames(any())
            } returns true
            invokeIntent {
                storeRandomizeFileNames(true)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.putRandomizeFileNames(any())
            }
            expectState {
                copy(randomizeFileNames = true)
            }
            expectSideEffect(SettingsSideEffect.RandomizeFileNames(success = true))
        }
    }

    @Test
    fun test_handleDefaultPathOpen() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                } returns testUri
                handleDefaultPathOpen()
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
            }
            expectSideEffect(SettingsSideEffect.DefaultPathOpenSelect(testUri))
        }
    }

    @Test
    fun test_clearDefaultPathOpen() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState(defaultPathOpenName = testDefaultPathOpenName)
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                } returns testUri
                coEvery {
                    settingsRepository.putDefaultPathOpen(any(), any())
                } returns true
                clearDefaultPathOpen()
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                settingsRepository.putDefaultPathOpen(any(), any())
            }
            expectState {
                copy(defaultPathOpenName = "")
            }
            expectSideEffect(SettingsSideEffect.DefaultPathOpenClear(success = true))
        }
    }

    @Test
    fun test_storeDefaultPathOpen() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                } returns testUri
                coEvery {
                    settingsRepository.putDefaultPathOpen(any(), any())
                } returns true
                coEvery {
                    settingsRepository.getDefaultPathOpenName()
                } returns testDefaultPathOpenName
                storeDefaultPathOpen(testUri)
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                settingsRepository.putDefaultPathOpen(any(), any())
                settingsRepository.getDefaultPathOpenName()
            }
            expectState {
                copy(defaultPathOpenName = testDefaultPathOpenName)
            }
            expectSideEffect(SettingsSideEffect.DefaultPathOpenStore(success = true))
        }
    }

    @Test
    fun test_handleDefaultPathSave() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                } returns testUri
                handleDefaultPathSave()
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
            }
            expectSideEffect(SettingsSideEffect.DefaultPathSaveSelect(testUri))
        }
    }

    @Test
    fun test_clearDefaultPathSave() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState(defaultPathSaveName = testDefaultPathSaveName)
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                } returns testUri
                coEvery {
                    settingsRepository.putDefaultPathSave(any(), any())
                } returns true
                clearDefaultPathSave()
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                settingsRepository.putDefaultPathSave(any(), any())
            }
            expectState {
                copy(defaultPathSaveName = "")
            }
            expectSideEffect(SettingsSideEffect.DefaultPathSaveClear(success = true))
        }
    }

    @Test
    fun test_storeDefaultPathSave() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                } returns testUri
                coEvery {
                    settingsRepository.putDefaultPathSave(any(), any())
                } returns true
                coEvery {
                    settingsRepository.getDefaultPathSaveName()
                } returns testDefaultPathSaveName
                storeDefaultPathSave(testUri)
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                settingsRepository.putDefaultPathSave(any(), any())
                settingsRepository.getDefaultPathSaveName()
            }
            expectState {
                copy(defaultPathSaveName = testDefaultPathSaveName)
            }
            expectSideEffect(SettingsSideEffect.DefaultPathSaveStore(success = true))
        }
    }

    @Test
    fun test_storeAutoDelete() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.putAutoDelete(any())
                } returns true
                storeAutoDelete(true)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.putAutoDelete(any())
            }
            expectState {
                copy(autoDelete = true)
            }
            expectSideEffect(SettingsSideEffect.AutoDelete(success = true))
        }
    }

    @Test
    fun test_storePreserveOrientation() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.putPreserveOrientation(any())
                } returns true
                storePreserveOrientation(true)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.putPreserveOrientation(any())
            }
            expectState {
                copy(preserveOrientation = true)
            }
            expectSideEffect(SettingsSideEffect.PreserveOrientation(success = true))
        }
    }

    @Test
    fun test_storeShareByDefault() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.putShareByDefault(any())
                } returns true
                storeShareByDefault(true)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.putShareByDefault(any())
            }
            expectState {
                copy(shareByDefault = true)
            }
            expectSideEffect(SettingsSideEffect.ShareByDefault(success = true))
        }
    }

    @Test
    fun test_handleDefaultDisplayNameSuffix() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getDefaultDisplayNameSuffix()
                } returns flowOf(testDefaultDisplayNameSuffix)
                handleDefaultDisplayNameSuffix()
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getDefaultDisplayNameSuffix()
            }
            expectSideEffect(
                SettingsSideEffect.NavigateToDefaultDisplayNameSuffix(
                    defaultDisplayNameSuffix = testDefaultDisplayNameSuffix
                )
            )
        }
    }

    @Test
    fun test_storeDefaultDisplayNameSuffix() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.putDefaultDisplayNameSuffix(any())
                } returns true
                storeDefaultDisplayNameSuffix(testDefaultDisplayNameSuffix)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.putDefaultDisplayNameSuffix(any())
            }
            expectState {
                copy(defaultDisplayNameSuffix = testDefaultDisplayNameSuffix)
            }
        }
    }

    @Test
    fun test_storeLegacyImageSelection() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.putSelectImagesLegacy(any())
                } returns true
                storeLegacyImageSelection(true)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.putSelectImagesLegacy(any())
            }
            expectState {
                copy(legacyImageSelection = true)
            }
            expectSideEffect(SettingsSideEffect.LegacyImageSelection(success = true))
        }
    }

    @Test
    fun test_storeSavePathSelectionSkip() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.putSavePathSelectionSkip(any())
                } returns true
                storeSavePathSelectionSkip(true)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.putSavePathSelectionSkip(any())
            }
            expectState {
                copy(skipSavePathSelection = true)
            }
            expectSideEffect(SettingsSideEffect.SavePathSelectionSkip(success = true))
        }
    }

    @Test
    fun test_handleDefaultNightMode() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getDefaultNightMode()
                } returns flowOf(defaultNightModeValue)
                handleDefaultNightMode()
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getDefaultNightMode()
            }
            expectSideEffect(
                SettingsSideEffect.NavigateToDefaultNightMode(
                    defaultNightMode = defaultNightModeValue
                )
            )
        }
    }

    @Test
    fun test_storeDefaultNightMode() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState()
        ) {
            expectInitialState()
            invokeIntent {
                val testDefaultNightModeName = "Always"
                coEvery {
                    settingsRepository.putDefaultNightMode(any())
                } returns true
                coEvery {
                    settingsRepository.getDefaultNightModeName()
                } returns testDefaultNightModeName
                storeDefaultNightMode(defaultNightModeValue)
            }.join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.putDefaultNightMode(any())
                settingsRepository.getDefaultNightModeName()
            }
            expectState {
                copy(defaultNightModeName = testDefaultNightModeName)
            }
        }
    }
}
