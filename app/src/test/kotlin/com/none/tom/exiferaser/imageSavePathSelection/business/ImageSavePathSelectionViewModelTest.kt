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
import androidx.datastore.core.CorruptionException
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.core.roboelectric.ROBOELECTRIC_BUILD_VERSION_CODE
import com.none.tom.exiferaser.core.util.testUri
import com.none.tom.exiferaser.settings.data.SettingsRepository
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
class ImageSavePathSelectionViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>()

    @Test
    fun test_verifyHasPrivilegedDefaultSavePath() = runTest {
        ImageSavePathSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = ImageSavePathSelectionState()
        ) {
            expectInitialState()
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.success(testUri)
            containerHost.verifyHasPrivilegedDefaultSavePath().join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultSavePath()
            }
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.failure(CorruptionException(""))
            containerHost.verifyHasPrivilegedDefaultSavePath().join()
            coVerify(exactly = 2) {
                settingsRepository.getPrivilegedDefaultSavePath()
            }
            expectState {
                copy(hasPrivilegedDefaultSavePath = true)
            }
            expectState {
                copy(hasPrivilegedDefaultSavePath = false)
            }
        }
    }

    @Test
    fun test_chooseSelectionSavePath() = runTest {
        ImageSavePathSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = ImageSavePathSelectionState()
        ) {
            expectInitialState()
            containerHost.chooseSelectionSavePath(testUri).join()
            coEvery {
                settingsRepository.getPrivilegedDefaultOpenPath()
            } returns Result.success(testUri)
            containerHost.chooseSelectionSavePath(Uri.EMPTY).join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultOpenPath()
            }
            expectSideEffect(ImageSavePathSelectionSideEffect.ChooseSavePath(openPath = testUri))
            expectSideEffect(ImageSavePathSelectionSideEffect.ChooseSavePath(openPath = testUri))
        }
    }

    @Test
    fun test_handleSelection() = runTest {
        ImageSavePathSelectionViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = ImageSavePathSelectionState()
        ) {
            expectInitialState()
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.failure(CorruptionException(""))
            containerHost.handleSelection(null).join()
            coEvery {
                settingsRepository.getPrivilegedDefaultSavePath()
            } returns Result.success(testUri)
            containerHost.handleSelection(testUri).join()
            containerHost.handleSelection(Uri.EMPTY).join()
            coVerify(exactly = 2) {
                settingsRepository.getPrivilegedDefaultSavePath()
            }
            expectSideEffect(
                ImageSavePathSelectionSideEffect.NavigateToSelection(savePath = testUri)
            )
            expectSideEffect(
                ImageSavePathSelectionSideEffect.NavigateToSelection(savePath = testUri)
            )
        }
    }
}
