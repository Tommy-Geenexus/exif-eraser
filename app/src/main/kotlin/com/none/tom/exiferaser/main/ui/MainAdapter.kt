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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.window.core.layout.WindowHeightSizeClass
import com.none.tom.exiferaser.ImageDirectoryProto
import com.none.tom.exiferaser.ImageFileProto
import com.none.tom.exiferaser.ImageFilesProto
import com.none.tom.exiferaser.databinding.ItemImageSourceBinding
import com.squareup.wire.AnyMessage

class MainAdapter(
    private val listener: Listener,
    private val windowHeightSizeClass: WindowHeightSizeClass
) : ListAdapter<AnyMessage, MainViewHolder>(
    object : DiffUtil.ItemCallback<AnyMessage>() {

        override fun areItemsTheSame(oldItem: AnyMessage, newItem: AnyMessage) = oldItem == newItem

        override fun areContentsTheSame(oldItem: AnyMessage, newItem: AnyMessage) =
            oldItem == newItem
    }
),
    MainItemTouchHelperCallback.OnRecyclerViewItemMoveListener {

    interface Listener {
        fun onImageItemSelected()
        fun onImagesItemSelected()
        fun onImageDirectoryItemSelected()
        fun onCameraItemSelected()
        fun onImageSourceMoved(imageSources: List<AnyMessage>, oldIndex: Int, newIndex: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        return MainViewHolder(
            binding = ItemImageSourceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            listener = listener,
            windowHeightSizeClass = windowHeightSizeClass
        )
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        when (currentList.getOrNull(position)?.typeUrl) {
            ImageFileProto.ADAPTER.typeUrl -> {
                holder.bindSelectImageItem()
            }
            ImageFilesProto.ADAPTER.typeUrl -> {
                holder.bindSelectImagesItem()
            }
            ImageDirectoryProto.ADAPTER.typeUrl -> {
                holder.bindSelectImageDirectoryItem()
            }
            else -> {
                holder.bindCameraItem()
            }
        }
    }

    override fun onRecyclerViewItemMove(oldIndex: Int, newIndex: Int) {
        listener.onImageSourceMoved(currentList, oldIndex, newIndex)
    }
}
