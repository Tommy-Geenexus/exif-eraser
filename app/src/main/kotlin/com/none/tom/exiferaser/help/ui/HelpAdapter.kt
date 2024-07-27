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

package com.none.tom.exiferaser.help.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.none.tom.exiferaser.databinding.PreferenceBinding

class HelpAdapter(
    private val listener: Listener
) : RecyclerView.Adapter<HelpViewHolder>() {

    interface Listener {

        fun onHelpTranslateSelected()
        fun onFeedbackSelected()
    }

    private companion object {

        const val ITEM_TYPE_IMAGE_FORMATS = 0
        const val ITEM_TYPE_TRANSLATE = 1
        const val ITEM_TYPE_FEEDBACK = 2
        const val ITEM_TYPE_VERSION = 3
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HelpViewHolder(
        binding = PreferenceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ),
        listener = listener
    )

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        when (getItemViewType(position)) {
            ITEM_TYPE_IMAGE_FORMATS -> holder.bindItemSupportedImageFormats()
            ITEM_TYPE_TRANSLATE -> holder.bindItemHelpTranslate()
            ITEM_TYPE_FEEDBACK -> holder.bindItemFeedback()
            ITEM_TYPE_VERSION -> holder.bindItemBuildVersion()
            else -> {
            }
        }
    }

    override fun getItemCount() = 4

    override fun getItemId(position: Int): Long {
        return when (position) {
            0 -> ITEM_TYPE_IMAGE_FORMATS.toLong()
            1 -> ITEM_TYPE_TRANSLATE.toLong()
            2 -> ITEM_TYPE_FEEDBACK.toLong()
            3 -> ITEM_TYPE_VERSION.toLong()
            else -> 0
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> ITEM_TYPE_IMAGE_FORMATS
            1 -> ITEM_TYPE_TRANSLATE
            2 -> ITEM_TYPE_FEEDBACK
            3 -> ITEM_TYPE_VERSION
            else -> 0
        }
    }
}
