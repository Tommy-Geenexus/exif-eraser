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

package com.none.tom.exiferaser

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavDeepLinkBuilder
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

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ExifEraser)
        super.onCreate(savedInstanceState)
        val binding = ActivityExifEraserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleSendIntent()
        handleShortcutIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSendIntent()
        handleShortcutIntent()
    }

    private fun handleSendIntent() {
        if (!isSendIntent() || intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        if (isSupportedSendImageIntent()) {
            val imageUris = intent.getClipDataUris()
            if (imageUris.isEmpty()) {
                return
            }
            val args = if (imageUris.size > 1) {
                KEY_IMAGES_SELECTION to imageUris
            } else {
                KEY_IMAGE_SELECTION to imageUris.first()
            }
            sendPendingIntent(bundleOf(args))
        }
    }

    private fun handleShortcutIntent() {
        if (!isShortcutIntent() || intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        sendPendingIntent(bundleOf(KEY_SHORTCUT to intent.action))
    }

    private fun sendPendingIntent(args: Bundle) {
        NavDeepLinkBuilder(this)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.fragment_main)
            .setArguments(args)
            .createPendingIntent()
            .send(
                this,
                Activity.RESULT_OK,
                Intent().apply {
                    fillIn(intent, 0)
                }
            )
    }

    private fun isSendIntent(): Boolean {
        return intent.action == Intent.ACTION_SEND ||
            intent.action == Intent.ACTION_SEND_MULTIPLE
    }

    private fun isSupportedSendImageIntent(): Boolean {
        return (intent.type == MIME_TYPE_IMAGE || supportedMimeTypes.contains(intent.type)) &&
            (intent.data != null || intent.clipData != null)
    }

    private fun isShortcutIntent(): Boolean {
        return intent.action == INTENT_ACTION_CHOOSE_IMAGE ||
            intent.action == INTENT_ACTION_CHOOSE_IMAGES ||
            intent.action == INTENT_ACTION_CHOOSE_IMAGE_DIR ||
            intent.action == INTENT_ACTION_LAUNCH_CAM
    }
}
