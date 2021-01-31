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

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.none.tom.exiferaser.databinding.ActivityExifEraserBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExifEraserActivity : AppCompatActivity() {

    companion object {
        const val KEY_IMAGE_SELECTION = TOP_LEVEL_PACKAGE_NAME + "IMAGE_SELECTION"
        const val KEY_IMAGES_SELECTION = TOP_LEVEL_PACKAGE_NAME + "IMAGES_SELECTION"
        const val KEY_SHORTCUT = TOP_LEVEL_PACKAGE_NAME + "SHORTCUT"
        const val INTENT_EXTRA_CONSUMED = TOP_LEVEL_PACKAGE_NAME + "EXTRA_CONSUMED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_ExifEraser)
        super.onCreate(savedInstanceState)
        val binding = ActivityExifEraserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        consumeSendIntent()
        consumeShortcutIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeSendIntent()
        consumeShortcutIntent()
    }

    private fun consumeSendIntent() {
        if (!isSendIntent() || intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        intent.putExtra(INTENT_EXTRA_CONSUMED, true)
        if (isSupportedSendImageIntent()) {
            val imageUris = intent.getClipDataUris()
            if (imageUris.isEmpty()) {
                return
            }
            val resultKey = if (imageUris.size > 1) KEY_IMAGES_SELECTION else KEY_IMAGE_SELECTION
            supportFragmentManager
                .fragments
                .getOrNull(0)
                ?.childFragmentManager
                ?.setFragmentResult(resultKey, bundleOf(resultKey to imageUris))
        }
    }

    private fun consumeShortcutIntent() {
        if (!isShortcutIntent() || intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        intent.putExtra(INTENT_EXTRA_CONSUMED, true)
        supportFragmentManager
            .fragments
            .getOrNull(0)
            ?.childFragmentManager
            ?.setFragmentResult(KEY_SHORTCUT, bundleOf(KEY_SHORTCUT to intent.action))
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
