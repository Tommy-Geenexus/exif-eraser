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

package com.none.tom.exiferaser

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.fragment.NavHostFragment
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.google.android.material.elevation.SurfaceColors
import com.none.tom.exiferaser.databinding.ActivityExifEraserBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExifEraserActivity : AppCompatActivity() {

    companion object {

        // See MainFragmentArgs
        private const val KEY_IMAGES_SELECTION = "images_selection"

        // See MainFragmentArgs
        private const val KEY_SHORTCUT = "shortcut"
    }

    lateinit var windowSizeClass: WindowSizeClass

    init {
        addOnNewIntentListener { intent ->
            setIntent(intent)
            handleSupportedIntent()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityExifEraserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        computeWindowSizeClasses()
        (supportFragmentManager.findFragmentById(R.id.nav_controller) as NavHostFragment?)
            ?.navController
            ?.addOnDestinationChangedListener { _, destination, _ ->
                if (windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.EXPANDED) {
                    if (destination.id == R.id.fragment_main) {
                        window.navigationBarColor = SurfaceColors.SURFACE_2.getColor(this)
                    } else {
                        window.navigationBarColor = SurfaceColors.SURFACE_0.getColor(this)
                    }
                }
            }
        binding.layout.addView(object : View(this) {
            override fun onConfigurationChanged(newConfig: Configuration?) {
                super.onConfigurationChanged(newConfig)
                computeWindowSizeClasses()
            }
        })
        handleSupportedIntent()
    }

    private fun computeWindowSizeClasses() {
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        val heightDp = metrics.bounds.height() / resources.displayMetrics.density
        windowSizeClass = WindowSizeClass.compute(widthDp, heightDp)
    }

    private fun handleSupportedIntent() {
        if (intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        val sendIntentActions = listOf(
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE
        )
        val shortcutIntentActions = listOf(
            INTENT_ACTION_CHOOSE_IMAGE,
            INTENT_ACTION_CHOOSE_IMAGES,
            INTENT_ACTION_CHOOSE_IMAGE_DIR,
            INTENT_ACTION_LAUNCH_CAM
        )
        if (sendIntentActions.contains(intent.action)) {
            val uris = mutableListOf<Uri>()
            val clipData = intent.clipData
            if (clipData != null) {
                uris.addAll(clipData.supportedImageUrisToList())
            }
            if (uris.isNotEmpty()) {
                (supportFragmentManager.findFragmentById(R.id.nav_controller) as NavHostFragment)
                    .navController
                    .navigate(
                        R.id.global_to_main,
                        bundleOf(KEY_IMAGES_SELECTION to uris.toTypedArray())
                    )
            }
        } else if (shortcutIntentActions.contains(intent.action)) {
            val args = bundleOf(KEY_SHORTCUT to intent.action)
            (supportFragmentManager.findFragmentById(R.id.nav_controller) as NavHostFragment)
                .navController
                .createDeepLink()
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.fragment_main)
                .setArguments(args)
                .createTaskStackBuilder()
                .startActivities()
        }
    }
}
