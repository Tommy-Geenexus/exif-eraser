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

package com.none.tom.exiferaser.update.business

import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.PROGRESS_MIN
import com.none.tom.exiferaser.update.data.UpdatePriority
import com.none.tom.exiferaser.update.data.UpdateRepository
import com.none.tom.exiferaser.update.data.UpdateResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.orbitmvi.orbit.test

@ExperimentalCoroutinesApi
class UpdateViewModelTest {

    private val updateRepository = mockk<UpdateRepository>()

    @Test
    fun test_beginOrResumeAppUpdate() = runBlockingTest {
        val initialState = UpdateState
        val viewModel = UpdateViewModel(updateRepository).test(initialState = initialState)
        val info = mockk<AppUpdateInfo>()
        val onBeginUpdate = { _: AppUpdateManager, _: AppUpdateInfo, _: Int, _: Int -> true }
        coEvery {
            updateRepository.beginOrResumeAppUpdate(info, onBeginUpdate)
        } returns flow {
            emit(UpdateResult.NotAvailable)
            emit(UpdateResult.Available)
            emit(UpdateResult.FailedToInstall)
            emit(UpdateResult.InProgress(progress = PROGRESS_MIN))
            emit(UpdateResult.InProgress(progress = PROGRESS_MAX))
            emit(UpdateResult.ReadyToInstall)
        }
        viewModel.testIntent {
            beginOrResumeAppUpdate(
                info = info,
                onBeginUpdate = onBeginUpdate
            )
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                UpdateSideEffect.UpdateFailed,
                UpdateSideEffect.UpdateInProgress(progress = PROGRESS_MIN),
                UpdateSideEffect.UpdateInProgress(progress = PROGRESS_MAX),
                UpdateSideEffect.UpdateReadyToInstall
            )
        }
    }

    @Test
    fun test_checkAppUpdateAvailability() = runBlockingTest {
        val initialState = UpdateState
        val viewModel = UpdateViewModel(updateRepository).test(
            initialState = initialState,
            isolateFlow = false
        )
        val info = mockk<AppUpdateInfo>()
        coEvery {
            updateRepository.getAppUpdateInfo()
        } returns info
        coEvery {
            updateRepository.isAppUpdateAvailableOrInProgress(info)
        } returns true
        coEvery {
            updateRepository.getAppUpdatePriority(info)
        } returns UpdatePriority.Low
        viewModel.testIntent {
            checkAppUpdateAvailability()
        }
        coEvery {
            updateRepository.getAppUpdatePriority(info)
        } returns UpdatePriority.Medium
        viewModel.testIntent {
            checkAppUpdateAvailability()
        }
        coEvery {
            updateRepository.getAppUpdatePriority(info)
        } returns UpdatePriority.High
        viewModel.testIntent {
            checkAppUpdateAvailability()
        }
        coEvery {
            updateRepository.isAppUpdateAvailableOrInProgress(info)
        } returns false
        viewModel.testIntent {
            checkAppUpdateAvailability()
        }
        viewModel.assert(initialState) {
            postedSideEffects(
                UpdateSideEffect.UpdateAvailable(
                    info = info,
                    immediateUpdate = false
                ),
                UpdateSideEffect.UpdateAvailable(
                    info = info,
                    immediateUpdate = false
                ),
                UpdateSideEffect.UpdateAvailable(
                    info = info,
                    immediateUpdate = true
                )
            )
        }
    }

    @Test
    fun test_handleAppUpdateResult() = runBlockingTest {
        val initialState = UpdateState
        val viewModel = UpdateViewModel(updateRepository).test(
            initialState = initialState,
            isolateFlow = false
        )
        viewModel.testIntent {
            handleAppUpdateResult(
                result = -1, // Activity.RESULT_OK
                immediateUpdate = false
            )
        }
        viewModel.testIntent {
            handleAppUpdateResult(
                result = -1, // Activity.RESULT_OK
                immediateUpdate = true
            )
        }
        viewModel.testIntent {
            handleAppUpdateResult(
                result = 0, // Activity.RESULT_CANCELLED
                immediateUpdate = false
            )
        }
        viewModel.testIntent {
            handleAppUpdateResult(
                result = 0, // Activity.RESULT_CANCELLED
                immediateUpdate = true
            )
        }
        viewModel.assert(initialState) {
            postedSideEffects(UpdateSideEffect.UpdateCancelled)
        }
    }
}
