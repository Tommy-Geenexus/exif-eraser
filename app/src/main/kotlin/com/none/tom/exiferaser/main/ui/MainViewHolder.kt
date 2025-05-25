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

package com.none.tom.exiferaser.main.ui

import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.window.core.layout.WindowSizeClass
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.ItemImageSourceBinding

class MainViewHolder(
    private val binding: ItemImageSourceBinding,
    private val listener: MainAdapter.Listener,
    private val isHeightAtLeastBreakpoint: (Int) -> Boolean
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.image.updateLayoutParams {
            binding.image.layoutParams.apply {
                val dimen = if (isHeightAtLeastBreakpoint(
                        WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                    )
                ) {
                    itemView.context.resources.getDimension(R.dimen.icon_expanded).toInt()
                } else if (isHeightAtLeastBreakpoint(
                        WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                    )
                ) {
                    itemView.context.resources.getDimension(R.dimen.icon_medium).toInt()
                } else {
                    itemView.context.resources.getDimension(R.dimen.icon_compact).toInt()
                }
                height = dimen
                width = dimen
            }
        }
        binding.method.setTextAppearance(
            if (isHeightAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                )
            ) {
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            } else if (isHeightAtLeastBreakpoint(
                    WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                )
            ) {
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
            } else {
                com.google.android.material.R.style.TextAppearance_Material3_BodySmall
            }
        )
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

    fun bindSelectImageItem() {
        binding.apply {
            image.setImageResource(R.drawable.ic_image)
            method.setText(R.string.image_file)
            imageSource.tag = binding.method.text
        }
    }

    fun bindSelectImagesItem() {
        binding.apply {
            image.setImageResource(R.drawable.ic_photo_library)
            method.setText(R.string.image_files)
            imageSource.tag = method.text
        }
    }

    fun bindSelectImageDirectoryItem() {
        binding.apply {
            image.setImageResource(R.drawable.ic_photo_album)
            method.setText(R.string.image_directory)
            imageSource.tag = method.text
        }
    }

    fun bindCameraItem() {
        binding.apply {
            image.setImageResource(R.drawable.ic_camera)
            method.setText(R.string.camera)
            imageSource.tag = method.text
        }
    }
}
