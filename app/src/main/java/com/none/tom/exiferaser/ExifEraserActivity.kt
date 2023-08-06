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
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.material.elevation.SurfaceColors
import com.none.tom.exiferaser.databinding.ActivityExifEraserBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExifEraserActivity : AppCompatActivity() {

    private companion object {

        // See MainFragmentArgs
        const val KEY_IMAGE_SELECTION = "image_selection"

        // See MainFragmentArgs
        const val KEY_IMAGES_SELECTION = "images_selection"

        // See MainFragmentArgs
        const val KEY_SHORTCUT = "shortcut"
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
        (supportFragmentManager.findFragmentById(R.id.nav_controller) as NavHostFragment?)
            ?.navController
            ?.addOnDestinationChangedListener { navController, destination, _ ->
                val prevDestinationId = navController.previousBackStackEntry?.destination?.id
                val colorSurface0 = SurfaceColors.SURFACE_0.getColor(this@ExifEraserActivity)
                val colorSurface2 = SurfaceColors.SURFACE_2.getColor(this@ExifEraserActivity)
                if (destination.id == R.id.fragment_main) {
                    window.statusBarColor = colorSurface0
                    window.navigationBarColor = colorSurface2
                } else if (prevDestinationId == R.id.fragment_main) {
                    window.statusBarColor = colorSurface2
                    window.navigationBarColor = colorSurface0
                }
            }
        binding.layout.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        computeWindowSizeClasses()
        handleSendIntent()
        handleShortcutIntent()
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
