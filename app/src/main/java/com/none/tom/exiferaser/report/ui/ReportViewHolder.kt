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

package com.none.tom.exiferaser.report.ui

import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.ItemReportBinding

class ReportViewHolder(
    private val binding: ItemReportBinding,
    private val listener: ReportAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.imageCropped.setOnClickListener {
            listener.onImageThumbnailSelected(absoluteAdapterPosition)
        }
        binding.modified.setOnClickListener {
            listener.onImageModifiedSelected(absoluteAdapterPosition)
        }
        binding.saved.setOnClickListener {
            listener.onImageSavedSelected(absoluteAdapterPosition)
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val value = TypedValue()
            itemView.context.theme.resolveAttribute(R.attr.colorSecondary, value, true)
            binding.modified.setChipIconTintResource(value.resourceId)
            binding.saved.setChipIconTintResource(value.resourceId)
        }
    }

    fun bindItem(
        imageUri: Uri,
        imageModified: Boolean,
        imageSaved: Boolean
    ) {
        binding.imageCropped.load(imageUri) {
            allowHardware(binding.imageCropped.isHardwareAccelerated)
            listener(
                onError = { _, _ -> binding.imageCropped.scaleType = ImageView.ScaleType.CENTER }
            )
            error(R.drawable.ic_image_not_supported)
        }
        binding.imageCropped.isClickable = imageSaved
        binding.modified.isVisible = imageModified
        binding.unmodified.isVisible = !imageModified
        binding.saved.isVisible = imageSaved
        binding.unsaved.isVisible = !imageSaved
    }

    fun releaseResources() {
        binding.imageCropped.dispose()
    }
}
