/*
 * Copyright (c) 2018-2020, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.report.ui

import android.content.res.ColorStateList
import android.net.Uri
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.clear
import coil.load
import com.google.android.material.color.MaterialColors
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentReportCardViewBinding

class ReportViewHolder(
    private val binding: FragmentReportCardViewBinding,
    private val listener: ReportAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.imageCropped.setOnClickListener {
            listener.onImageThumbnailSelected(absoluteAdapterPosition)
        }
    }

    fun bindItem(
        imagePath: Uri,
        imageModified: Boolean,
        imageSaved: Boolean
    ) {
        binding.apply {
            imageCropped.run {
                load(imagePath) {
                    listener(
                        onError = { _, _ ->
                            scaleType = ImageView.ScaleType.CENTER
                        }
                    )
                    error(R.drawable.ic_image_not_supported)
                }
                modified.apply {
                    chipStrokeColor = ColorStateList.valueOf(
                        MaterialColors.getColor(
                            root,
                            if (imageModified) R.attr.colorOk else R.attr.colorError
                        )
                    )
                    setText(if (imageModified) R.string.modified else R.string.unmodified)
                }
                saved.apply {
                    chipStrokeColor = ColorStateList.valueOf(
                        MaterialColors.getColor(
                            root,
                            if (imageSaved) R.attr.colorOk else R.attr.colorError
                        )
                    )
                    setText(if (imageSaved) R.string.saved else R.string.unsaved)
                }
            }
        }
    }

    fun releaseResources() {
        binding.imageCropped.clear()
    }
}
