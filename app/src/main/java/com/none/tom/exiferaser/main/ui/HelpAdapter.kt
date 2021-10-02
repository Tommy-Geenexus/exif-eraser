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

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = HelpViewHolder(
        binding = PreferenceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ),
        listener = listener
    )

    override fun onBindViewHolder(
        holder: HelpViewHolder,
        position: Int
    ) {
        when (position) {
            0 -> holder.bindItemSupportedImageFormats()
            1 -> holder.bindItemHelpTranslate()
            2 -> holder.bindItemFeedback()
            3 -> holder.bindItemBuildVersion()
            else -> {
            }
        }
    }

    override fun getItemCount() = 4
}
