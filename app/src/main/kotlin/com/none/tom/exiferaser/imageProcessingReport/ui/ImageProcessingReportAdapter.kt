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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.none.tom.exiferaser.core.image.ImageProcessingSummary
import com.none.tom.exiferaser.databinding.ItemImageProcessingReportBinding

class ImageProcessingReportAdapter(
    private val listener: Listener
) : ListAdapter<ImageProcessingSummary, ImageProcessingReportViewHolder>(
    object : DiffUtil.ItemCallback<ImageProcessingSummary>() {

        override fun areItemsTheSame(
            oldItem: ImageProcessingSummary,
            newItem: ImageProcessingSummary
        ) = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: ImageProcessingSummary,
            newItem: ImageProcessingSummary
        ) = oldItem == newItem
    }
) {

    interface Listener {
        fun onImageThumbnailSelected(position: Int)
        fun onViewImageMetadataSelected(position: Int)
        fun onViewImagePathSelected(position: Int)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageProcessingReportViewHolder {
        return ImageProcessingReportViewHolder(
            ItemImageProcessingReportBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            listener
        )
    }

    override fun onBindViewHolder(holder: ImageProcessingReportViewHolder, position: Int) {
        currentList.getOrNull(position)?.let { summary ->
            holder.bindItem(
                uri = summary.uri,
                isMetadataContained = summary.imageMetadataSnapshot.isMetadataContained(),
                isImageSaved = summary.isImageSaved
            )
        }
    }

    override fun onViewRecycled(holder: ImageProcessingReportViewHolder) {
        holder.releaseResources()
    }
}
