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

package com.none.tom.exiferaser.main.ui

import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentMainCardViewBinding

class MainViewHolder(
    private val binding: FragmentMainCardViewBinding,
    private val listener: MainAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.imageSource.setOnClickListener {
            with(itemView.context) {
                when (binding.method.text) {
                    getString(R.string.image_file) -> listener.onImageItemSelected()
                    getString(R.string.image_files) -> listener.onImagesItemSelected()
                    getString(R.string.image_directory) -> listener.onImageDirectoryItemSelected()
                    else -> listener.onCameraItemSelected()
                }
            }
        }
    }

    fun bindSelectImageItem(screenHeightRatio: Float) {
        binding.apply {
            method.setText(R.string.image_file)
            bindItemByScreenHeightRatio(
                screenHeightRatio = screenHeightRatio,
                drawableRes = R.drawable.ic_image,
                drawableResXLarge = R.drawable.ic_image_xlarge
            )
        }
    }

    fun bindSelectImagesItem(screenHeightRatio: Float) {
        binding.apply {
            method.setText(R.string.image_files)
            bindItemByScreenHeightRatio(
                screenHeightRatio = screenHeightRatio,
                drawableRes = R.drawable.ic_photo_library,
                drawableResXLarge = R.drawable.ic_photo_library_xlarge
            )
        }
    }

    fun bindSelectImageDirectoryItem(screenHeightRatio: Float) {
        binding.apply {
            method.setText(R.string.image_directory)
            bindItemByScreenHeightRatio(
                screenHeightRatio = screenHeightRatio,
                drawableRes = R.drawable.ic_photo_album,
                drawableResXLarge = R.drawable.ic_photo_album_xlarge
            )
        }
    }

    fun bindCameraItem(screenHeightRatio: Float) {
        binding.apply {
            method.setText(R.string.camera)
            bindItemByScreenHeightRatio(
                screenHeightRatio = screenHeightRatio,
                drawableRes = R.drawable.ic_camera,
                drawableResXLarge = R.drawable.ic_camera_xlarge
            )
        }
    }

    private fun bindItemByScreenHeightRatio(
        screenHeightRatio: Float,
        @DrawableRes drawableRes: Int,
        @DrawableRes drawableResXLarge: Int
    ) {
        val margin: Int
        if (screenHeightRatio <= MainFragment.MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_MAX) {
            margin = itemView.context.resources.getDimension(R.dimen.spacing_small).toInt()
            binding.image.setImageResource(drawableRes)
        } else if (screenHeightRatio > MainFragment.MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_MAX &&
            screenHeightRatio <= MainFragment.MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_DEFAULT
        ) {
            margin = itemView.context.resources.getDimension(R.dimen.spacing_normal).toInt()
            binding.image.setImageResource(drawableRes)
        } else {
            margin = itemView.context.resources.getDimension(R.dimen.spacing_normal).toInt()
            binding.image.setImageResource(drawableResXLarge)
        }
        (binding.image.layoutParams as? ConstraintLayout.LayoutParams)
            ?.setMargins(margin, margin, margin, margin)
        (binding.method.layoutParams as? ConstraintLayout.LayoutParams)
            ?.setMargins(margin, margin, margin, margin)
    }
}
