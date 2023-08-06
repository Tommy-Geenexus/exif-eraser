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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.orbitmvi.orbit.test.test

class UpdateViewModelTest {

    private val updateRepository = mockk<UpdateRepository>()

    @Test
    fun test_beginOrResumeAppUpdate() = runTest {
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
        UpdateViewModel(updateRepository).test(
            testScope = this,
            initialState = UpdateState
        ) {
            expectInitialState()
            invokeIntent {
                beginOrResumeAppUpdate(
                    info = info,
                    onBeginUpdate = onBeginUpdate
                )
            }
            expectSideEffect(UpdateSideEffect.UpdateFailed)
            expectSideEffect(UpdateSideEffect.UpdateInProgress(progress = PROGRESS_MIN))
            expectSideEffect(UpdateSideEffect.UpdateInProgress(progress = PROGRESS_MAX))
            expectSideEffect(UpdateSideEffect.UpdateReadyToInstall)
        }
    }

    @Test
    fun test_checkAppUpdateAvailability() = runTest {
        UpdateViewModel(updateRepository).test(
            testScope = this,
            initialState = UpdateState
        ) {
            expectInitialState()
            val info = mockk<AppUpdateInfo>()
            invokeIntent {
                coEvery {
                    updateRepository.getAppUpdateInfo()
                } returns info
                coEvery {
                    updateRepository.isAppUpdateAvailableOrInProgress(info)
                } returns true
                coEvery {
                    updateRepository.getAppUpdatePriority(info)
                } returns UpdatePriority.Low
                checkAppUpdateAvailability()
            }.join()
            invokeIntent {
                coEvery {
                    updateRepository.getAppUpdatePriority(info)
                } returns UpdatePriority.Medium
                checkAppUpdateAvailability()
            }.join()
            invokeIntent {
                coEvery {
                    updateRepository.getAppUpdatePriority(info)
                } returns UpdatePriority.High
                checkAppUpdateAvailability()
            }.join()
            invokeIntent {
                coEvery {
                    updateRepository.isAppUpdateAvailableOrInProgress(info)
                } returns false
                checkAppUpdateAvailability()
            }.join()
            expectSideEffect(
                UpdateSideEffect.UpdateAvailable(
                    info = info,
                    immediateUpdate = false
                )
            )
            expectSideEffect(
                UpdateSideEffect.UpdateAvailable(
                    info = info,
                    immediateUpdate = false
                )
            )
            expectSideEffect(
                UpdateSideEffect.UpdateAvailable(
                    info = info,
                    immediateUpdate = true
                )
            )
        }
    }

    @Test
    fun test_handleAppUpdateResult() = runTest {
        UpdateViewModel(updateRepository).test(
            testScope = this,
            initialState = UpdateState
        ) {
            expectInitialState()
            invokeIntent {
                handleAppUpdateResult(
                    result = -1, // Activity.RESULT_OK
                    immediateUpdate = false
                )
            }.join()
            invokeIntent {
                handleAppUpdateResult(
                    result = -1, // Activity.RESULT_OK
                    immediateUpdate = true
                )
            }.join()
            invokeIntent {
                handleAppUpdateResult(
                    result = 0, // Activity.RESULT_CANCELLED
                    immediateUpdate = false
                )
            }.join()
            invokeIntent {
                handleAppUpdateResult(
                    result = 0, // Activity.RESULT_CANCELLED
                    immediateUpdate = true
                )
            }.join()
            expectSideEffect(UpdateSideEffect.UpdateCancelled)
        }
    }
}
