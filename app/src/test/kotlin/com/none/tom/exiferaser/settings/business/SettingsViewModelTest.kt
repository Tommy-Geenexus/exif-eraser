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

import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.ROBOELECTRIC_BUILD_VERSION_CODE
import com.none.tom.exiferaser.TEST_DEFAULT_DISPLAY_NAME_SUFFIX
import com.none.tom.exiferaser.TEST_DEFAULT_NIGHT_MODE_NAME
import com.none.tom.exiferaser.TEST_DEFAULT_PATH_OPEN_NAME
import com.none.tom.exiferaser.TEST_DEFAULT_PATH_SAVE_NAME
import com.none.tom.exiferaser.settings.data.SettingsRepository
import com.none.tom.exiferaser.settings.ui.defaultNightMode
import com.none.tom.exiferaser.testUri
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [ROBOELECTRIC_BUILD_VERSION_CODE])
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>()

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
            coEvery {
                settingsRepository.isRandomizeFileNamesEnabled()
            } returns true
            coEvery {
                settingsRepository.getDefaultOpenPathName()
            } returns TEST_DEFAULT_PATH_OPEN_NAME
            coEvery {
                settingsRepository.getDefaultSavePathName()
            } returns TEST_DEFAULT_PATH_SAVE_NAME
            coEvery {
                settingsRepository.isAutoDeleteEnabled()
            } returns true
            coEvery {
                settingsRepository.isPreserveOrientationEnabled()
            } returns true
            coEvery {
                settingsRepository.isShareByDefaultEnabled()
            } returns true
            coEvery {
                settingsRepository.getDefaultDisplayNameSuffix()
            } returns TEST_DEFAULT_DISPLAY_NAME_SUFFIX
            coEvery {
                settingsRepository.isLegacyImageSelectionEnabled()
            } returns true
            coEvery {
                settingsRepository.isSkipSavePathSelectionEnabled()
            } returns true
            coEvery {
                settingsRepository.getDefaultNightModeName()
            } returns TEST_DEFAULT_NIGHT_MODE_NAME
            containerHost.readDefaultValues().join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.isRandomizeFileNamesEnabled()
                settingsRepository.getDefaultOpenPathName()
                settingsRepository.getDefaultSavePathName()
                settingsRepository.isAutoDeleteEnabled()
                settingsRepository.isPreserveOrientationEnabled()
                settingsRepository.isShareByDefaultEnabled()
                settingsRepository.getDefaultDisplayNameSuffix()
                settingsRepository.isLegacyImageSelectionEnabled()
                settingsRepository.isSkipSavePathSelectionEnabled()
                settingsRepository.getDefaultNightModeName()
            }
            expectState {
                copy(
                    isRandomizeFileNamesEnabled = true,
                    defaultOpenPathName = TEST_DEFAULT_PATH_OPEN_NAME,
                    defaultSavePathName = TEST_DEFAULT_PATH_SAVE_NAME,
                    isAutoDeleteEnabled = true,
                    isPreserveOrientationEnabled = true,
                    isShareByDefaultEnabled = true,
                    defaultDisplayNameSuffix = TEST_DEFAULT_DISPLAY_NAME_SUFFIX,
                    isLegacyImageSelectionEnabled = true,
                    isSkipSavePathSelectionEnabled = true,
                    defaultNightModeName = TEST_DEFAULT_NIGHT_MODE_NAME
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
            } returns Result.success(Unit)
            containerHost.storeRandomizeFileNames(true).join()
            coEvery {
                settingsRepository.putRandomizeFileNames(any())
            } returns Result.failure(Exception(""))
            containerHost.storeRandomizeFileNames(true).join()
            coVerify(exactly = 2) {
                settingsRepository.putRandomizeFileNames(any())
            }
            expectState {
                copy(isRandomizeFileNamesEnabled = true)
            }
            expectSideEffect(SettingsSideEffect.RandomizeFileNames.Success)
            expectSideEffect(SettingsSideEffect.RandomizeFileNames.Failure)
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
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.success(testUri)
            containerHost.handleDefaultPathOpen().join()
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.failure(Exception(""))
            containerHost.handleDefaultPathOpen().join()
            coVerify(exactly = 2) {
                settingsRepository.getPrivilegedDefaultOpenPath()
            }
            expectSideEffect(SettingsSideEffect.DefaultOpenPath.Select.Success(testUri))
            expectSideEffect(SettingsSideEffect.DefaultOpenPath.Select.Failure)
        }
    }

    @Test
    fun test_clearDefaultPathOpen() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState(defaultOpenPathName = TEST_DEFAULT_PATH_OPEN_NAME)
        ) {
            expectInitialState()
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.success(testUri)
            coEvery {
                settingsRepository.putDefaultOpenPath(any(), any())
            } returns Result.success(Unit)
            containerHost.clearDefaultPathOpen().join()
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.success(testUri)
            coEvery {
                settingsRepository.putDefaultOpenPath(any(), any())
            } returns Result.failure(Exception(""))
            containerHost.clearDefaultPathOpen().join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.getPrivilegedDefaultOpenPath()
                settingsRepository.putDefaultOpenPath(any(), any())
                settingsRepository.getPrivilegedDefaultOpenPath()
                settingsRepository.putDefaultOpenPath(any(), any())
            }
            expectState {
                copy(defaultOpenPathName = "")
            }
            expectSideEffect(SettingsSideEffect.DefaultOpenPath.Clear.Success)
            expectSideEffect(SettingsSideEffect.DefaultOpenPath.Clear.Failure)
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
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.success(testUri)
            coEvery {
                settingsRepository.putDefaultOpenPath(any(), any())
            } returns Result.success(Unit)
            coEvery {
                settingsRepository.getDefaultOpenPathName()
            } returns TEST_DEFAULT_PATH_OPEN_NAME
            containerHost.storeDefaultPathOpen(testUri).join()
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.success(testUri)
            coEvery {
                settingsRepository.putDefaultOpenPath(any(), any())
            } returns Result.failure(Exception(""))
            coEvery {
                settingsRepository.getDefaultOpenPathName()
            } returns TEST_DEFAULT_PATH_OPEN_NAME
            containerHost.storeDefaultPathOpen(testUri).join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.getPrivilegedDefaultOpenPath()
                settingsRepository.putDefaultOpenPath(any(), any())
                settingsRepository.getDefaultOpenPathName()
                settingsRepository.getPrivilegedDefaultOpenPath()
                settingsRepository.putDefaultOpenPath(any(), any())
                settingsRepository.getDefaultOpenPathName()
            }
            expectState {
                copy(defaultOpenPathName = TEST_DEFAULT_PATH_OPEN_NAME)
            }
            expectSideEffect(SettingsSideEffect.DefaultOpenPath.Store.Success)
            expectSideEffect(SettingsSideEffect.DefaultOpenPath.Store.Failure)
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
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.success(testUri)
            containerHost.handleDefaultPathSave().join()
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.failure(Exception(""))
            containerHost.handleDefaultPathSave().join()
            coVerify(exactly = 2) {
                settingsRepository.getPrivilegedDefaultSavePath()
            }
            expectSideEffect(SettingsSideEffect.DefaultSavePath.Select.Success(uri = testUri))
            expectSideEffect(SettingsSideEffect.DefaultSavePath.Select.Failure)
        }
    }

    @Test
    fun test_clearDefaultPathSave() = runTest {
        SettingsViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SettingsState(defaultSavePathName = TEST_DEFAULT_PATH_SAVE_NAME)
        ) {
            expectInitialState()
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.success(testUri)
            coEvery {
                settingsRepository.putDefaultSavePath(any(), any())
            } returns Result.success(Unit)
            containerHost.clearDefaultPathSave().join()
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.success(testUri)
            coEvery {
                settingsRepository.putDefaultSavePath(any(), any())
            } returns Result.failure(Exception(""))
            containerHost.clearDefaultPathSave().join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.getPrivilegedDefaultSavePath()
                settingsRepository.putDefaultSavePath(any(), any())
                settingsRepository.getPrivilegedDefaultSavePath()
                settingsRepository.putDefaultSavePath(any(), any())
            }
            expectState {
                copy(defaultSavePathName = "")
            }
            expectSideEffect(SettingsSideEffect.DefaultSavePath.Clear.Success)
            expectSideEffect(SettingsSideEffect.DefaultSavePath.Clear.Failure)
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
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.success(testUri)
            coEvery {
                settingsRepository.putDefaultSavePath(any(), any())
            } returns Result.success(Unit)
            coEvery {
                settingsRepository.getDefaultSavePathName()
            } returns TEST_DEFAULT_PATH_SAVE_NAME
            containerHost.storeDefaultPathSave(testUri).join()
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.success(testUri)
            coEvery {
                settingsRepository.putDefaultSavePath(any(), any())
            } returns Result.failure(Exception(""))
            coEvery {
                settingsRepository.getDefaultSavePathName()
            } returns TEST_DEFAULT_PATH_SAVE_NAME
            containerHost.storeDefaultPathSave(testUri).join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.getPrivilegedDefaultSavePath()
                settingsRepository.putDefaultSavePath(any(), any())
                settingsRepository.getDefaultSavePathName()
                settingsRepository.getPrivilegedDefaultSavePath()
                settingsRepository.putDefaultSavePath(any(), any())
                settingsRepository.getDefaultSavePathName()
            }
            expectState {
                copy(defaultSavePathName = TEST_DEFAULT_PATH_SAVE_NAME)
            }
            expectSideEffect(SettingsSideEffect.DefaultSavePath.Store.Success)
            expectSideEffect(SettingsSideEffect.DefaultSavePath.Store.Failure)
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
            coEvery {
                settingsRepository.putAutoDelete(any())
            } returns Result.success(Unit)
            containerHost.storeAutoDelete(true).join()
            coEvery {
                settingsRepository.putAutoDelete(any())
            } returns Result.failure(Exception(""))
            containerHost.storeAutoDelete(true).join()
            coVerify(exactly = 2) {
                settingsRepository.putAutoDelete(any())
            }
            expectState {
                copy(isAutoDeleteEnabled = true)
            }
            expectSideEffect(SettingsSideEffect.AutoDelete.Success)
            expectSideEffect(SettingsSideEffect.AutoDelete.Failure)
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
            coEvery {
                settingsRepository.putPreserveOrientation(any())
            } returns Result.success(Unit)
            containerHost.storePreserveOrientation(true).join()
            coEvery {
                settingsRepository.putPreserveOrientation(any())
            } returns Result.failure(Exception(""))
            containerHost.storePreserveOrientation(true).join()
            coVerify(exactly = 2) {
                settingsRepository.putPreserveOrientation(any())
            }
            expectState {
                copy(isPreserveOrientationEnabled = true)
            }
            expectSideEffect(SettingsSideEffect.PreserveOrientation.Success)
            expectSideEffect(SettingsSideEffect.PreserveOrientation.Failure)
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
            coEvery {
                settingsRepository.putShareByDefault(any())
            } returns Result.success(Unit)
            containerHost.storeShareByDefault(true).join()
            coEvery {
                settingsRepository.putShareByDefault(any())
            } returns Result.failure(Exception(""))
            containerHost.storeShareByDefault(true).join()
            coVerify(exactly = 2) {
                settingsRepository.putShareByDefault(any())
            }
            expectState {
                copy(isShareByDefaultEnabled = true)
            }
            expectSideEffect(SettingsSideEffect.ShareByDefault.Success)
            expectSideEffect(SettingsSideEffect.ShareByDefault.Failure)
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
            coEvery {
                settingsRepository.getDefaultDisplayNameSuffix()
            } returns TEST_DEFAULT_DISPLAY_NAME_SUFFIX
            containerHost.handleDefaultDisplayNameSuffix().join()
            coVerify(exactly = 1) {
                settingsRepository.getDefaultDisplayNameSuffix()
            }
            expectSideEffect(
                SettingsSideEffect.Navigate.ToDefaultDisplayNameSuffix(
                    defaultDisplayNameSuffix = TEST_DEFAULT_DISPLAY_NAME_SUFFIX
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
            coEvery {
                settingsRepository.putDefaultDisplayNameSuffix(any())
            } returns Result.success(Unit)
            containerHost.storeDefaultDisplayNameSuffix(TEST_DEFAULT_DISPLAY_NAME_SUFFIX).join()
            coEvery {
                settingsRepository.putDefaultDisplayNameSuffix(any())
            } returns Result.failure(Exception(""))
            containerHost.storeDefaultDisplayNameSuffix(TEST_DEFAULT_DISPLAY_NAME_SUFFIX).join()
            coVerify(exactly = 2) {
                settingsRepository.putDefaultDisplayNameSuffix(any())
            }
            expectState {
                copy(defaultDisplayNameSuffix = TEST_DEFAULT_DISPLAY_NAME_SUFFIX)
            }
            expectSideEffect(SettingsSideEffect.DefaultDisplayNameSuffix.Success)
            expectSideEffect(SettingsSideEffect.DefaultDisplayNameSuffix.Failure)
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
            coEvery {
                settingsRepository.putSelectImagesLegacy(any())
            } returns Result.success(Unit)
            containerHost.storeLegacyImageSelection(true).join()
            coEvery {
                settingsRepository.putSelectImagesLegacy(any())
            } returns Result.failure(Exception(""))
            containerHost.storeLegacyImageSelection(true).join()
            coVerify(exactly = 2) {
                settingsRepository.putSelectImagesLegacy(any())
            }
            expectState {
                copy(isLegacyImageSelectionEnabled = true)
            }
            expectSideEffect(SettingsSideEffect.LegacyImageSelection.Success)
            expectSideEffect(SettingsSideEffect.LegacyImageSelection.Failure)
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
            coEvery {
                settingsRepository.putSavePathSelectionSkip(any())
            } returns Result.success(Unit)
            containerHost.storeSavePathSelectionSkip(true).join()
            coEvery {
                settingsRepository.putSavePathSelectionSkip(any())
            } returns Result.failure(Exception(""))
            containerHost.storeSavePathSelectionSkip(true).join()
            coVerify(exactly = 2) {
                settingsRepository.putSavePathSelectionSkip(any())
            }
            expectState {
                copy(isSkipSavePathSelectionEnabled = true)
            }
            expectSideEffect(SettingsSideEffect.DefaultSavePath.SelectionSkip.Success)
            expectSideEffect(SettingsSideEffect.DefaultSavePath.SelectionSkip.Failure)
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
            coEvery {
                settingsRepository.getDefaultNightMode()
            } returns defaultNightMode
            containerHost.handleDefaultNightMode().join()
            coVerify(exactly = 1) {
                settingsRepository.getDefaultNightMode()
            }
            expectSideEffect(
                SettingsSideEffect.Navigate.ToDefaultNightMode(defaultNightMode = defaultNightMode)
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
            coEvery {
                settingsRepository.putDefaultNightMode(any())
            } returns Result.success(Unit)
            coEvery {
                settingsRepository.getDefaultNightModeName()
            } returns TEST_DEFAULT_NIGHT_MODE_NAME
            containerHost.storeDefaultNightMode(defaultNightMode).join()
            coEvery {
                settingsRepository.putDefaultNightMode(any())
            } returns Result.failure(Exception(""))
            coEvery {
                settingsRepository.getDefaultNightModeName()
            } returns TEST_DEFAULT_NIGHT_MODE_NAME
            containerHost.storeDefaultNightMode(defaultNightMode).join()
            coVerify(ordering = Ordering.ALL) {
                settingsRepository.putDefaultNightMode(any())
                settingsRepository.getDefaultNightModeName()
                settingsRepository.putDefaultNightMode(any())
                settingsRepository.getDefaultNightModeName()
            }
            expectState {
                copy(defaultNightModeName = TEST_DEFAULT_NIGHT_MODE_NAME)
            }
            expectSideEffect(SettingsSideEffect.DefaultNightMode.Success)
            expectSideEffect(SettingsSideEffect.DefaultNightMode.Failure)
        }
    }
}
