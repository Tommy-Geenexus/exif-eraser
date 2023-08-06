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

package com.none.tom.exiferaser.savepath.business

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
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

@Config(sdk = [Build.VERSION_CODES.TIRAMISU])
@RunWith(RobolectricTestRunner::class)
class SavePathViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>()
    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()

    @Test
    fun test_verifyHasPrivilegedDefaultSavePath() = runTest {
        SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SavePathState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                } returns testUri
                verifyHasPrivilegedDefaultSavePath()
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
            }
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                } returns Uri.EMPTY
                verifyHasPrivilegedDefaultSavePath()
            }.join()
            coVerify(exactly = 2) {
                settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
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
        SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SavePathState()
        ) {
            expectInitialState()
            invokeIntent {
                chooseSelectionSavePath(testUri)
            }.join()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
                } returns testUri
                chooseSelectionSavePath(Uri.EMPTY)
            }.join()
            coVerify(exactly = 1) {
                settingsRepository.getPrivilegedDefaultPathOpenOrEmpty()
            }
            expectSideEffect(SavePathSideEffect.ChooseSavePath(openPath = testUri))
            expectSideEffect(SavePathSideEffect.ChooseSavePath(openPath = testUri))
        }
    }

    @Test
    fun test_handleSelection() = runTest {
        SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            testScope = this,
            initialState = SavePathState()
        ) {
            expectInitialState()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                } returns Uri.EMPTY
                handleSelection(null)
            }.join()
            invokeIntent {
                coEvery {
                    settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
                } returns testUri
                handleSelection(testUri)
            }.join()
            invokeIntent {
                handleSelection(Uri.EMPTY)
            }.join()
            coVerify(exactly = 2) {
                settingsRepository.getPrivilegedDefaultPathSaveOrEmpty()
            }
            expectSideEffect(SavePathSideEffect.NavigateToSelection(savePath = testUri))
            expectSideEffect(SavePathSideEffect.NavigateToSelection(savePath = testUri))
        }
    }
}
