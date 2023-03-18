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
import android.provider.MediaStore
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.none.tom.exiferaser.MIME_TYPE_IMAGE

class PickMultipleVisualMedia2(
    private val usePhotoPicker: () -> Boolean
) : ActivityResultContracts.PickMultipleVisualMedia() {

    @SuppressLint("MissingSuperCall", "InlinedApi")
    override fun createIntent(
        context: Context,
        input: PickVisualMediaRequest
    ): Intent {
        return if (usePhotoPicker() &&
            ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable()
        ) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = MIME_TYPE_IMAGE
                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, MediaStore.getPickImagesMaxLimit())
            }
        } else {
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = MIME_TYPE_IMAGE
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
        }
    }
}
