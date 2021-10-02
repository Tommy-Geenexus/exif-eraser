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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith
import org.orbitmvi.orbit.test
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.P])
@RunWith(RobolectricTestRunner::class)
class SavePathViewModelTest {

    private val settingsRepository = mockk<SettingsRepository>()

    private val testUri = ContentResolver.SCHEME_CONTENT.toUri()

    @Test
    fun test_verifyHasPrivilegedDefaultSavePath() = runBlockingTest {
        val initialState = SavePathState()
        val viewModel = SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        coEvery {
            settingsRepository.getDefaultPathSave()
        } returns flowOf(testUri)
        coEvery {
            settingsRepository.hasPrivilegedDefaultPathSave(any())
        } returns true
        viewModel.testIntent {
            verifyHasPrivilegedDefaultSavePath()
        }
        coVerify(exactly = 1) {
            settingsRepository.hasPrivilegedDefaultPathSave(any())
        }
        coEvery {
            settingsRepository.hasPrivilegedDefaultPathSave(any())
        } returns false
        viewModel.testIntent {
            verifyHasPrivilegedDefaultSavePath()
        }
        coVerify(exactly = 2) {
            settingsRepository.hasPrivilegedDefaultPathSave(any())
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
    fun test_chooseSelectionSavePath() = runBlockingTest {
        val initialState = SavePathState()
        val viewModel = SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.testIntent {
            chooseSelectionSavePath(testUri)
        }
        coEvery {
            settingsRepository.getDefaultPathOpen()
        } returns flowOf(testUri)
        viewModel.testIntent {
            chooseSelectionSavePath(Uri.EMPTY)
        }
        coVerify(exactly = 1) {
            settingsRepository.getDefaultPathOpen()
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
    fun test_handleSelection() = runBlockingTest {
        val initialState = SavePathState()
        val viewModel = SavePathViewModel(
            savedStateHandle = SavedStateHandle(),
            settingsRepository = settingsRepository
        ).test(
            initialState = initialState,
            isolateFlow = false
        )
        coEvery {
            settingsRepository.getDefaultPathSave()
        } returns flowOf()
        viewModel.testIntent {
            handleSelection(null)
        }
        coEvery {
            settingsRepository.getDefaultPathSave()
        } returns flowOf(testUri)
        viewModel.testIntent {
            handleSelection(testUri)
        }
        viewModel.testIntent {
            handleSelection(Uri.EMPTY)
        }
        coVerify(exactly = 2) {
            settingsRepository.getDefaultPathSave()
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                SavePathSideEffect.NavigateToSelection(savePath = testUri),
                SavePathSideEffect.NavigateToSelection(savePath = testUri)
            )
        }
    }
}
