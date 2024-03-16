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

package com.none.tom.exiferaser.imageProcessingReport.ui

import android.net.Uri
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.dispose
import coil.load
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.ItemImageProcessingReportBinding

class ImageProcessingReportViewHolder(
    private val binding: ItemImageProcessingReportBinding,
    private val listener: ImageProcessingReportAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.imageCropped.setOnClickListener {
            listener.onImageThumbnailSelected(absoluteAdapterPosition)
        }
        binding.metadata.setOnClickListener {
            listener.onViewImageMetadataSelected(absoluteAdapterPosition)
        }
        binding.savePath.setOnClickListener {
            listener.onViewImagePathSelected(absoluteAdapterPosition)
        }
    }

    fun bindItem(uri: Uri, isMetadataContained: Boolean, isImageSaved: Boolean) {
        binding.imageCropped.load(uri) {
            allowHardware(binding.imageCropped.isHardwareAccelerated)
            listener(
                onError = { _, _ -> binding.imageCropped.scaleType = ImageView.ScaleType.CENTER }
            )
            error(R.drawable.ic_image_not_supported)
        }
        binding.imageCropped.isClickable = isImageSaved
        binding.metadata.isEnabled = isMetadataContained
        binding.savePath.isEnabled = isImageSaved
    }

    fun releaseResources() {
        binding.imageCropped.dispose()
    }
}
