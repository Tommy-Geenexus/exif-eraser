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

package com.none.tom.exiferaser.main

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.none.tom.exiferaser.MIME_TYPE_IMAGE

class PickMultipleVisualMedia2(
    private val usePhotoPicker: () -> Boolean
) : ActivityResultContracts.PickMultipleVisualMedia() {

    private companion object {

        const val ACTION_SYSTEM_FALLBACK_PICK_IMAGES =
            "androidx.activity.result.contract.action.PICK_IMAGES"

        const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX =
            "androidx.activity.result.contract.extra.PICK_IMAGES_MAX"

        const val GMS_ACTION_PICK_IMAGES =
            "com.google.android.gms.provider.action.PICK_IMAGES"
        const val GMS_EXTRA_PICK_IMAGES_MAX =
            "com.google.android.gms.provider.extra.PICK_IMAGES_MAX"
    }

    @SuppressLint("ClassVerificationFailure", "NewApi")
    private fun isSystemPickerAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2
        } else {
            false
        }
    }
    private fun isSystemFallbackPickerAvailable(context: Context): Boolean {
        return getSystemFallbackPicker(context) != null
    }

    @Suppress("DEPRECATION")
    private fun getSystemFallbackPicker(context: Context): ResolveInfo? {
        return context.packageManager.resolveActivity(
            Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES),
            PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY
        )
    }

    private fun isGmsPickerAvailable(context: Context): Boolean {
        return getGmsPicker(context) != null
    }

    @Suppress("DEPRECATION")
    private fun getGmsPicker(context: Context): ResolveInfo? {
        return context.packageManager.resolveActivity(
            Intent(GMS_ACTION_PICK_IMAGES),
            PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY
        )
    }

    @SuppressLint("MissingSuperCall", "InlinedApi")
    override fun createIntent(
        context: Context,
        input: PickVisualMediaRequest
    ): Intent {
        val usePhotoPicker = usePhotoPicker()
        return if (usePhotoPicker && isSystemPickerAvailable()) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = MIME_TYPE_IMAGE
                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MediaStore.getPickImagesMaxLimit())
            }
        } else if (usePhotoPicker && isSystemFallbackPickerAvailable(context)) {
            val fallbackPicker = checkNotNull(getSystemFallbackPicker(context)).activityInfo
            Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES).apply {
                setClassName(fallbackPicker.applicationInfo.packageName, fallbackPicker.name)
                type = MIME_TYPE_IMAGE
                putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX, MediaStore.getPickImagesMaxLimit())
            }
        } else if (usePhotoPicker && isGmsPickerAvailable(context)) {
            val gmsPicker = checkNotNull(getGmsPicker(context)).activityInfo
            Intent(GMS_ACTION_PICK_IMAGES).apply {
                setClassName(gmsPicker.applicationInfo.packageName, gmsPicker.name)
                type = MIME_TYPE_IMAGE
                putExtra(GMS_EXTRA_PICK_IMAGES_MAX, MediaStore.getPickImagesMaxLimit())
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = MIME_TYPE_IMAGE
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
    }
}
