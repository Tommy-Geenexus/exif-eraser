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

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import com.none.tom.exiferaser.settings.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.contracts.ExperimentalContracts
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.assert
import org.orbitmvi.orbit.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class SavePathViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>()

    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()

    @Test
    fun test_verifyHasPrivilegedDefaultSavePath() {
        val initialState = SavePathState()
        val viewModel = SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        coEvery {
            settingsRepository.hasPrivilegedDefaultSavePath()
        } returns true
        viewModel.verifyHasPrivilegedDefaultSavePath()
        coVerify(exactly = 1) {
            settingsRepository.hasPrivilegedDefaultSavePath()
        }
        coEvery {
            settingsRepository.hasPrivilegedDefaultSavePath()
        } returns false
        viewModel.verifyHasPrivilegedDefaultSavePath()
        coVerify(exactly = 2) {
            settingsRepository.hasPrivilegedDefaultSavePath()
        }
        viewModel.assert(initialState) {
            states(
                {
                    copy(hasPrivilegedDefaultSavePath = true)
                },
                {
                    copy(hasPrivilegedDefaultSavePath = false)
                }
            )
        }
    }

    @Test
    fun test_chooseSelectionSavePath() {
        val initialState = SavePathState()
        val viewModel = SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.chooseSelectionSavePath(testUri)
        coEvery {
            settingsRepository.getDefaultOpenPathSuspending()
        } returns testUri
        viewModel.chooseSelectionSavePath(Uri.EMPTY)
        coVerify(exactly = 1) {
            settingsRepository.getDefaultOpenPathSuspending()
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                SavePathSideEffect.ChooseSavePath(openPath = testUri),
                SavePathSideEffect.ChooseSavePath(openPath = testUri)
            )
        }
    }

    @ExperimentalContracts
    @Test
    fun test_navigateToSelection() {
        val initialState = SavePathState()
        val viewModel = SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.navigateToSelection(null)
        viewModel.navigateToSelection(testUri)
        coEvery {
            settingsRepository.getDefaultSavePathSuspending()
        } returns testUri
        viewModel.navigateToSelection(Uri.EMPTY)
        coVerify(exactly = 1) {
            settingsRepository.getDefaultSavePathSuspending()
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                SavePathSideEffect.NavigateToSelection(savePath = testUri),
                SavePathSideEffect.NavigateToSelection(savePath = testUri)
            )
        }
    }
}
