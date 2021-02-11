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

import android.net.Uri
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.none.tom.exiferaser.ImageDirectoryProto
import com.none.tom.exiferaser.ImageFileProto
import com.none.tom.exiferaser.ImageFilesProto
import com.none.tom.exiferaser.databinding.FragmentMainCardViewBinding
import com.none.tom.exiferaser.main.ACTIVITY_EXPANDED
import com.squareup.wire.AnyMessage

class MainAdapter(
    private val listener: Listener
) : ListAdapter<AnyMessage, MainViewHolder>(
    object : DiffUtil.ItemCallback<AnyMessage>() {

        override fun areItemsTheSame(
            oldItem: AnyMessage,
            newItem: AnyMessage
        ) = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: AnyMessage,
            newItem: AnyMessage
        ) = oldItem == newItem
    }
),
    SimpleItemTouchHelperCallback.OnRecyclerViewItemMoveListener {

    interface Listener {
        fun onImageItemSelected()
        fun onImagesItemSelected()
        fun onImageDirectoryItemSelected()
        fun onCameraItemSelected()
        fun onImageSourceMoved(
            imageSources: MutableList<AnyMessage>,
            oldIndex: Int,
            newIndex: Int
        )
        fun onImageDragged(
            dragEvent: DragEvent,
            uri: Uri
        )
    }

    var screenHeightRatio: Float = ACTIVITY_EXPANDED

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MainViewHolder {
        return MainViewHolder(
            binding = FragmentMainCardViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            listener = listener
        )
    }

    override fun onBindViewHolder(
        holder: MainViewHolder,
        position: Int
    ) {
        when (currentList.getOrNull(position)?.typeUrl) {
            ImageFileProto.ADAPTER.typeUrl -> {
                holder.bindSelectImageItem(screenHeightRatio)
            }
            ImageFilesProto.ADAPTER.typeUrl -> {
                holder.bindSelectImagesItem(screenHeightRatio)
            }
            ImageDirectoryProto.ADAPTER.typeUrl -> {
                holder.bindSelectImageDirectoryItem(screenHeightRatio)
            }
            else -> {
                holder.bindCameraItem(screenHeightRatio)
            }
        }
    }

    override fun onRecyclerViewItemMove(
        oldIndex: Int,
        newIndex: Int
    ) {
        listener.onImageSourceMoved(currentList.toMutableList(), oldIndex, newIndex)
    }
}
