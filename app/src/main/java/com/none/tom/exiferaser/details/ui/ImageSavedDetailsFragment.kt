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

package com.none.tom.exiferaser.details.ui

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImageSavedDetailsFragment : DialogFragment() {

    companion object {

        const val TAG = "SaveDetailsFragment"

        private const val KEY_IMAGE_PATH = TOP_LEVEL_PACKAGE_NAME + "IMAGE_PATH"

        fun newInstance(imagePath: String) = ImageSavedDetailsFragment().apply {
            arguments = bundleOf(KEY_IMAGE_PATH to imagePath)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val imagePath =
            requireArguments().getString(KEY_IMAGE_PATH) ?: getString(R.string.image_path)
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.image_path)
            .setMessage(imagePath)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }
}
