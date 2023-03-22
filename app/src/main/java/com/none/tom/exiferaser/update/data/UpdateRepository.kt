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

package com.none.tom.exiferaser.update.data

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.IntRange
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.color.MaterialColors
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestCompleteUpdate
import com.google.android.play.core.ktx.requestUpdateFlow
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.di.DispatcherIo
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.PROGRESS_MIN
import com.none.tom.exiferaser.suspendRunCatching
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateManager: AppUpdateManager,
    private val notificationManager: NotificationManagerCompat,
    @DispatcherIo private val dispatcher: CoroutineDispatcher
) {

    private companion object {

        const val CHANNEL_ID = TOP_LEVEL_PACKAGE_NAME + "CHANNEL_ID"

        const val NOTIFICATION_ID = 0

        const val APP_UPDATE_REQUEST_CODE = 0

        const val APP_UPDATE_THRESHOLD_DAYS = 90
    }

    suspend fun getAppUpdateInfo(): AppUpdateInfo? {
        return withContext(dispatcher) {
            coroutineContext.suspendRunCatching {
                appUpdateManager.requestAppUpdateInfo()
            }.getOrElse { exception ->
                Timber.e(exception)
                null
            }
        }
    }

    suspend fun completeFlexibleAppUpdate(): Boolean {
        notificationManager.cancel(NOTIFICATION_ID)
        return withContext(dispatcher) {
            coroutineContext.suspendRunCatching {
                appUpdateManager.requestCompleteUpdate()
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun beginOrResumeAppUpdate(
        info: AppUpdateInfo,
        onBeginUpdate: (AppUpdateManager, AppUpdateInfo, Int, Int) -> Boolean
    ): Flow<UpdateResult>? {
        return try {
            val updatePriority = getAppUpdatePriority(info)
            if (updatePriority == UpdatePriority.Low) {
                return null
            }
            val appUpdateType = if (updatePriority == UpdatePriority.High) {
                AppUpdateType.IMMEDIATE
            } else {
                AppUpdateType.FLEXIBLE
            }
            onBeginUpdate(
                appUpdateManager,
                info,
                appUpdateType,
                APP_UPDATE_REQUEST_CODE
            )
            if (updatePriority == UpdatePriority.Medium) {
                appUpdateManager
                    .requestUpdateFlow()
                    .map { result ->
                        when (result) {
                            AppUpdateResult.NotAvailable -> UpdateResult.NotAvailable
                            is AppUpdateResult.Available -> UpdateResult.Available
                            is AppUpdateResult.InProgress -> {
                                val progress = with(result.installState) {
                                    val current = bytesDownloaded()
                                    val total = totalBytesToDownload()
                                    if (current != 0L && total != 0L) {
                                        current.toDouble() / total.toDouble()
                                    } else {
                                        0L.toDouble()
                                    }
                                }.roundToInt()
                                UpdateResult.InProgress(progress)
                            }
                            is AppUpdateResult.Downloaded -> UpdateResult.ReadyToInstall
                        }
                    }
                    .catch { exception ->
                        Timber.e(exception)
                        emit(UpdateResult.FailedToInstall)
                    }
                    .flowOn(dispatcher)
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    fun getAppUpdatePriority(info: AppUpdateInfo): UpdatePriority {
        return when (info.updatePriority()) {
            0, 1 -> UpdatePriority.Low
            2, 3 -> {
                val staleness = info.clientVersionStalenessDays() ?: -1
                if (staleness >= APP_UPDATE_THRESHOLD_DAYS &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    UpdatePriority.High
                } else if (info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                    UpdatePriority.Medium
                } else {
                    UpdatePriority.Low
                }
            }
            else -> {
                when {
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> {
                        UpdatePriority.High
                    }
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        UpdatePriority.Medium
                    }
                    else -> {
                        UpdatePriority.Low
                    }
                }
            }
        }
    }

    fun isAppUpdateAvailableOrInProgress(info: AppUpdateInfo): Boolean {
        val availability = info.updateAvailability()
        return availability == UpdateAvailability.UPDATE_AVAILABLE ||
            availability == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
    }

    @SuppressLint("MissingPermission")
    fun showAppUpdateProgressNotification(
        @IntRange(
            from = PROGRESS_MIN.toLong(),
            to = PROGRESS_MAX.toLong()
        ) progress: Int = PROGRESS_MIN,
        failed: Boolean = false
    ) {
        registerNotificationChannel()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            notificationManager.areNotificationsEnabled()
        ) {
            notificationManager.notify(
                NOTIFICATION_ID,
                when {
                    failed -> {
                        createAppUpdateFailedNotification()
                    }
                    progress >= PROGRESS_MAX -> {
                        createAppUpdateReadyToInstallNotification()
                    }
                    else -> {
                        createAppUpdateInProgressNotification(progress)
                    }
                }
            )
        }
    }

    private fun createAppUpdateFailedNotification(): Notification {
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setColor(MaterialColors.getColor(context, R.attr.colorSecondary, null))
            .setContentTitle(context.getString(R.string.app_update_failed))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .build()
    }

    private fun createAppUpdateInProgressNotification(
        @IntRange(from = PROGRESS_MIN.toLong(), to = PROGRESS_MAX.toLong()) progress: Int
    ): Notification {
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setColor(MaterialColors.getColor(context, R.attr.colorSecondary, null))
            .setContentTitle(context.getString(R.string.app_update_download))
            .setProgress(PROGRESS_MAX, progress, false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(false)
            .build()
    }

    private fun createAppUpdateReadyToInstallNotification(): Notification {
        return NotificationCompat
            .Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo)
            .setColor(MaterialColors.getColor(context, R.attr.colorSecondary, null))
            .setContentTitle(context.getString(R.string.app_update_downloaded))
            .setContentText(context.getString(R.string.app_update_install))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    context.packageManager.getLaunchIntentForPackage(context.packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setAutoCancel(true)
            .build()
    }

    private fun registerNotificationChannel() {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
    }
}
