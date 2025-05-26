/*
 * Copyright (c) 2018-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import androidx.window.layout.WindowMetricsCalculator
import com.none.tom.exiferaser.core.extension.resolveThemeAttribute
import com.none.tom.exiferaser.core.extension.setCutoutForegroundColor
import com.none.tom.exiferaser.core.extension.supportedImageUrisToList
import com.none.tom.exiferaser.core.util.INTENT_ACTION_CHOOSE_IMAGE
import com.none.tom.exiferaser.core.util.INTENT_ACTION_CHOOSE_IMAGES
import com.none.tom.exiferaser.core.util.INTENT_ACTION_CHOOSE_IMAGE_DIR
import com.none.tom.exiferaser.core.util.INTENT_ACTION_LAUNCH_CAM
import com.none.tom.exiferaser.core.util.INTENT_EXTRA_CONSUMED
import com.none.tom.exiferaser.core.util.NAV_ARG_IMAGE_SELECTION
import com.none.tom.exiferaser.core.util.NAV_ARG_SHORTCUT
import com.none.tom.exiferaser.databinding.ActivityExifEraserBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExifEraserActivity : AppCompatActivity() {

    lateinit var windowSizeClass: WindowSizeClass

    init {
        addOnNewIntentListener { intent ->
            setIntent(intent)
            handleSupportedIntent()
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        with(ActivityExifEraserBinding.inflate(layoutInflater)) {
            setContentView(root)
            computeWindowSizeClasses()
            setCutoutForegroundColor(colorRes = android.R.color.black)
            ViewCompat.setOnApplyWindowInsetsListener(root) { _, windowInsetsCompat ->
                val insets = windowInsetsCompat.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                root.updateLayoutParams<FrameLayout.LayoutParams> {
                    leftMargin = insets.left
                    rightMargin = insets.right
                }
                windowInsetsCompat
            }
            root.addView(
                object : View(this@ExifEraserActivity) {
                    override fun onConfigurationChanged(newConfig: Configuration?) {
                        super.onConfigurationChanged(newConfig)
                        computeWindowSizeClasses()
                    }
                }
            )
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            findNavController().addOnDestinationChangedListener { _, destination, _ ->
                if (!windowSizeClass.isWidthAtLeastBreakpoint(
                        widthDpBreakpoint = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                    )
                ) {
                    window.navigationBarColor = resolveThemeAttribute(
                        if (destination.id == R.id.fragment_main) {
                            com.google.android.material.R.attr.colorSurfaceContainer
                        } else {
                            com.google.android.material.R.attr.colorSurface
                        }
                    )
                }
            }
        }
        handleSupportedIntent()
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController().navigateUp() || super.onSupportNavigateUp()
    }

    private fun computeWindowSizeClasses() {
        val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this)
        val widthDp = metrics.bounds.width() / resources.displayMetrics.density
        val heightDp = metrics.bounds.height() / resources.displayMetrics.density
        windowSizeClass = WindowSizeClass.BREAKPOINTS_V1.computeWindowSizeClass(widthDp, heightDp)
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
                findNavController().navigate(
                    R.id.global_to_main,
                    bundleOf(NAV_ARG_IMAGE_SELECTION to uris.toTypedArray())
                )
            }
        } else if (shortcutIntentActions.contains(intent.action)) {
            findNavController()
                .createDeepLink()
                .setGraph(R.navigation.nav_graph)
                .setDestination(R.id.fragment_main)
                .setArguments(bundleOf(NAV_ARG_SHORTCUT to intent.action))
                .createTaskStackBuilder()
                .startActivities()
        }
    }

    private fun findNavController(): NavController {
        return (supportFragmentManager.findFragmentById(R.id.container) as NavHostFragment)
            .navController
    }
}
