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

package com.none.tom.exiferaser.report.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.none.tom.exiferaser.databinding.FragmentReportCardViewBinding
import com.none.tom.exiferaser.selection.data.Summary

class ReportAdapter(
    private val listener: Listener
) : ListAdapter<Summary, ReportViewHolder>(
    object : DiffUtil.ItemCallback<Summary>() {

        override fun areItemsTheSame(
            oldItem: Summary,
            newItem: Summary
        ) = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: Summary,
            newItem: Summary
        ) = oldItem == newItem
    }
) {

    interface Listener {
        fun onImageThumbnailSelected(position: Int)
        fun onImageModifiedSelected(position: Int)
        fun onImageSavedSelected(position: Int)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReportViewHolder {
        return ReportViewHolder(
            FragmentReportCardViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            listener
        )
    }

    override fun onBindViewHolder(
        holder: ReportViewHolder,
        position: Int
    ) {
        currentList.getOrNull(position)?.let { summary ->
            holder.bindItem(
                imageUri = summary.imageUri,
                imageModified = summary.imageModified,
                imageSaved = summary.imageSaved
            )
        }
    }

    override fun onViewRecycled(holder: ReportViewHolder) {
        holder.releaseResources()
    }
}
