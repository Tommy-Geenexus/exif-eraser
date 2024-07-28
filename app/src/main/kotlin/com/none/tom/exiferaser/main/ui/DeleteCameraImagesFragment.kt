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

package com.none.tom.exiferaser.main.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.none.tom.exiferaser.core.ui.BaseBottomSheetDialogFragment
import com.none.tom.exiferaser.core.util.KEY_CAMERA_IMAGE_DELETE
import com.none.tom.exiferaser.databinding.FragmentDeleteCameraImagesBinding

class DeleteCameraImagesFragment :
    BaseBottomSheetDialogFragment<FragmentDeleteCameraImagesBinding>() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.delete.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                KEY_CAMERA_IMAGE_DELETE,
                bundleOf(KEY_CAMERA_IMAGE_DELETE to true)
            )
            dismiss()
        }
        binding.cancel.setOnClickListener {
            dismiss()
        }
    }

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentDeleteCameraImagesBinding.inflate(inflater, container, false)
}
