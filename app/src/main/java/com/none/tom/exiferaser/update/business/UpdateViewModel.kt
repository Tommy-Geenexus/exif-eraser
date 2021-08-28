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

import androidx.lifecycle.ViewModel
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.none.tom.exiferaser.update.data.UpdatePriority
import com.none.tom.exiferaser.update.data.UpdateRepository
import com.none.tom.exiferaser.update.data.UpdateResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import org.orbitmvi.orbit.ContainerHost
import org.orbitmvi.orbit.syntax.simple.intent
import org.orbitmvi.orbit.syntax.simple.postSideEffect
import org.orbitmvi.orbit.viewmodel.container
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository
) : ContainerHost<UpdateState, UpdateSideEffect>,
    ViewModel() {

    override val container = container<UpdateState, UpdateSideEffect>(initialState = UpdateState)

    fun beginOrResumeAppUpdate(
        info: AppUpdateInfo,
        onBeginUpdate: (AppUpdateManager, AppUpdateInfo, Int, Int) -> Boolean
    ) = intent {
        updateRepository
            .beginOrResumeAppUpdate(
                info = info,
                onBeginUpdate = onBeginUpdate
            )
            ?.collect { result ->
                when (result) {
                    UpdateResult.FailedToInstall -> {
                        postSideEffect(UpdateSideEffect.UpdateFailed)
                    }
                    is UpdateResult.InProgress -> {
                        postSideEffect(UpdateSideEffect.UpdateInProgress(result.progress))
                    }
                    UpdateResult.ReadyToInstall -> {
                        postSideEffect(UpdateSideEffect.UpdateReadyToInstall)
                    }
                    else -> {
                    }
                }
            }
    }

    fun checkAppUpdateAvailability() = intent {
        val info = updateRepository.getAppUpdateInfo()
        if (info != null && updateRepository.isAppUpdateAvailableOrInProgress(info)) {
            postSideEffect(
                UpdateSideEffect.UpdateAvailable(
                    info = info,
                    immediateUpdate =
                    updateRepository.getAppUpdatePriority(info) == UpdatePriority.High
                )
            )
        }
    }

    fun handleAppUpdateResult(
        result: Int,
        immediateUpdate: Boolean
    ) = intent {
        if (result >= 0 && immediateUpdate) {
            postSideEffect(UpdateSideEffect.UpdateCancelled)
        }
    }
}
