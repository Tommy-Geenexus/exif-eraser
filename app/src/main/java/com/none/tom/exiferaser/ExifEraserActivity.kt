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

package com.none.tom.exiferaser

import android.content.Intent
import android.content.IntentSender
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.result.IntentSenderRequest
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.material.elevation.SurfaceColors
import com.none.tom.exiferaser.databinding.ActivityExifEraserBinding
import com.none.tom.exiferaser.update.StartIntentSenderForResult
import com.none.tom.exiferaser.update.business.UpdateSideEffect
import com.none.tom.exiferaser.update.business.UpdateViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@AndroidEntryPoint
class ExifEraserActivity : AppCompatActivity() {

    companion object {

        // See MainFragmentArgs
        private const val KEY_IMAGE_SELECTION = "image_selection"

        // See MainFragmentArgs
        private const val KEY_IMAGES_SELECTION = "images_selection"

        // See MainFragmentArgs
        private const val KEY_SHORTCUT = "shortcut"

        const val KEY_UPDATE_FAILED = TOP_LEVEL_PACKAGE_NAME + "UPDATE_FAILED"

        const val KEY_UPDATE_IN_PROGRESS = TOP_LEVEL_PACKAGE_NAME + "UPDATE_IN_PROGRESS"

        const val KEY_UPDATE_READY_TO_INSTALL = TOP_LEVEL_PACKAGE_NAME + "UPDATE_READY_TO_INSTALL"
    }

    private val viewModel: UpdateViewModel by viewModels()
    private val contract = StartIntentSenderForResult()
    private val updateResult = registerForActivityResult(contract) { result ->
        if (result != null) {
            viewModel.handleAppUpdateResult(
                result = result.resultCode,
                immediateUpdate = contract.immediateUpdate
            )
        }
    }

    internal var windowSizeClassHeight: WindowSizeClass = WindowSizeClass.Unspecified
    internal var windowSizeClassWidth: WindowSizeClass = WindowSizeClass.Unspecified

    init {
        addOnNewIntentListener { intent ->
            setIntent(intent)
            handleSendIntent()
            handleShortcutIntent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = SurfaceColors.SURFACE_2.getColor(this)
        val binding = ActivityExifEraserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.layout.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()
        handleSendIntent()
        handleShortcutIntent()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.sideEffectFlow.collect { sideEffect ->
                    handleSideEffect(sideEffect)
                }
            }
        }
        viewModel.checkAppUpdateAvailability()
    }

    private fun handleSideEffect(sideEffect: UpdateSideEffect) {
        when (sideEffect) {
            is UpdateSideEffect.UpdateAvailable -> {
                viewModel.beginOrResumeAppUpdate(sideEffect.info) { um, info, type, requestCode ->
                    um.startUpdateFlowForResult(
                        info,
                        type,
                        { intentSender: IntentSender, _, _, _, _, _, _ ->
                            contract.immediateUpdate = sideEffect.immediateUpdate
                            updateResult.launch(IntentSenderRequest.Builder(intentSender).build())
                        },
                        requestCode
                    )
                }
            }
            UpdateSideEffect.UpdateCancelled -> {
                finish()
            }
            UpdateSideEffect.UpdateFailed -> {
                supportFragmentManager
                    .primaryNavigationFragment
                    ?.childFragmentManager
                    ?.setFragmentResult(KEY_UPDATE_FAILED, bundleOf())
            }
            is UpdateSideEffect.UpdateInProgress -> {
                supportFragmentManager
                    .primaryNavigationFragment
                    ?.childFragmentManager
                    ?.setFragmentResult(
                        KEY_UPDATE_IN_PROGRESS,
                        bundleOf(KEY_UPDATE_IN_PROGRESS to sideEffect.progress)
                    )
            }
            UpdateSideEffect.UpdateReadyToInstall -> {
                supportFragmentManager
                    .primaryNavigationFragment
                    ?.childFragmentManager
                    ?.setFragmentResult(KEY_UPDATE_READY_TO_INSTALL, bundleOf())
            }
        }
    }

    private fun handleSendIntent() {
        if (!intent.hasExtra(INTENT_EXTRA_CONSUMED) &&
            isSendIntent() &&
            isSupportedSendImageIntent()
        ) {
            val imageUris = intent.getClipDataUris()
            if (imageUris.isNotEmpty()) {
                val args = if (imageUris.size > 1) {
                    bundleOf(KEY_IMAGES_SELECTION to imageUris)
                } else {
                    bundleOf(KEY_IMAGE_SELECTION to imageUris.first())
                }
                (supportFragmentManager.findFragmentById(R.id.nav_controller) as NavHostFragment?)
                    ?.navController
                    ?.navigate(R.id.global_to_main, args)
            }
        }
    }

    private fun handleShortcutIntent() {
        if (!intent.hasExtra(INTENT_EXTRA_CONSUMED) && isShortcutIntent()) {
            val args = bundleOf(KEY_SHORTCUT to intent.action)
            (supportFragmentManager.findFragmentById(R.id.nav_controller) as NavHostFragment?)
                ?.navController
                ?.createDeepLink()
                ?.setGraph(R.navigation.nav_graph)
                ?.setDestination(R.id.fragment_main)
                ?.setArguments(args)
                ?.createTaskStackBuilder()
                ?.startActivities()
        }
    }

    private fun isSendIntent(): Boolean {
        return intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE
    }

    private fun isSupportedSendImageIntent(): Boolean {
        return (
            intent.type == MIME_TYPE_IMAGE ||
                supportedMimeTypes.contains(intent.type)
            ) &&
            (intent.data != null || intent.clipData != null)
    }

    private fun isShortcutIntent(): Boolean {
        return intent.action == INTENT_ACTION_CHOOSE_IMAGE ||
            intent.action == INTENT_ACTION_CHOOSE_IMAGES ||
            intent.action == INTENT_ACTION_CHOOSE_IMAGE_DIR ||
            intent.action == INTENT_ACTION_LAUNCH_CAM
    }

    private fun computeWindowSizeClasses() {
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        val heightDp = metrics.bounds.height() / resources.displayMetrics.density
        windowSizeClassWidth = WindowSizeClass.calculate(widthDp, height = false)
        windowSizeClassHeight = WindowSizeClass.calculate(heightDp, height = true)
    }
}
