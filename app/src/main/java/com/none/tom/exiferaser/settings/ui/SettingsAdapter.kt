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

package com.none.tom.exiferaser.settings.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.databinding.ItemFsBinding
import com.none.tom.exiferaser.databinding.ItemImageBinding
import com.none.tom.exiferaser.databinding.ItemUiBinding

class SettingsAdapter(
    private val listener: Listener
) : ListAdapter<Any, RecyclerView.ViewHolder>(
    object : DiffUtil.ItemCallback<Any>() {

        override fun areItemsTheSame(
            oldItem: Any,
            newItem: Any
        ): Boolean = false

        override fun areContentsTheSame(
            oldItem: Any,
            newItem: Any
        ): Boolean = false
    }
) {

    private companion object {

        const val ITEM_TYPE_FS = 1
        const val ITEM_TYPE_IMAGE = 2
        const val ITEM_TYPE_UI = 3
    }

    interface Listener {

        fun onDefaultPathOpenSelected()
        fun onDefaultPathOpenClear()
        fun onDefaultPathSaveSelected()
        fun onDefaultPathSaveClear()
        fun onPreserveOrientationChanged(value: Boolean)
        fun onShareByDefaultChanged(value: Boolean)
        fun onDefaultDisplayNameSuffixSelected()
        fun onDefaultNightModeSelected()
        fun onItemsUpdated()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_FS -> {
                ItemFsViewHolder(
                    binding = ItemFsBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
            ITEM_TYPE_IMAGE -> {
                ItemImageViewHolder(
                    binding = ItemImageBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
            else -> {
                ItemUiViewHolder(
                    binding = ItemUiBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ),
                    listener = listener
                )
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (getItemViewType(position)) {
            ITEM_TYPE_FS -> {
                (holder as ItemFsViewHolder).bindItemFs(
                    defaultOpenPathName = (currentList.firstOrNull() as? String) ?: String.Empty,
                    defaultSavePathName = (currentList.getOrNull(1) as? String) ?: String.Empty
                )
            }
            ITEM_TYPE_IMAGE -> {
                (holder as ItemImageViewHolder).bindImageItem(
                    initialPreserveOrientation = currentList.getOrNull(2) as? Boolean ?: false,
                    initialShareByDefault = currentList.getOrNull(3) as? Boolean ?: false,
                    defaultDisplayNameSuffix = (currentList.getOrNull(4) as? String) ?: String.Empty
                )
            }
            ITEM_TYPE_UI -> {
                (holder as ItemUiViewHolder).bindUiItem(
                    defaultNightModeName = (currentList.getOrNull(5) as? String) ?: String.Empty
                )
            }
            else -> {
            }
        }
    }

    override fun onCurrentListChanged(
        previousList: MutableList<Any>,
        currentList: MutableList<Any>
    ) {
        if (currentList.isNotEmpty()) {
            listener.onItemsUpdated()
        }
    }

    override fun getItemCount() = 3

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> ITEM_TYPE_FS
            1 -> ITEM_TYPE_IMAGE
            2 -> ITEM_TYPE_UI
            else -> 0
        }
    }
}
