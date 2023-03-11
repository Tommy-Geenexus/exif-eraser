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

package com.none.tom.exiferaser.settings.ui

import androidx.recyclerview.widget.RecyclerView
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.ItemUiBinding

class ItemUiViewHolder(
    private val binding: ItemUiBinding,
    private val listener: SettingsAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.savePathCustomSkip.apply {
            layoutPreferenceCheckBox.setOnClickListener {
                onOff.isChecked = !onOff.isChecked
            }
            onOff.setOnCheckedChangeListener { _, value ->
                listener.onSavePathSelectionSkipChanged(value)
            }
            iconPreferenceCheckBox.setImageResource(R.drawable.ic_straight)
            titlePreferenceCheckBox.setText(R.string.save_path_custom_skip)
        }
        binding.nightMode.apply {
            layoutPreference.setOnClickListener {
                listener.onDefaultNightModeSelected()
            }
            iconPreference.setImageResource(R.drawable.ic_dark_mode)
            titlePreference.setText(R.string.night_mode)
        }
        binding.ui.category.setText(R.string.ui)
    }

    fun bindUiItem(
        skipSavePathSelection: Boolean,
        defaultNightModeName: String
    ) {
        binding.savePathCustomSkip.apply {
            summaryPreferenceCheckBox.setText(
                if (skipSavePathSelection) {
                    R.string.save_path_custom_skip_on
                } else {
                    R.string.save_path_custom_skip_off
                }
            )
            onOff.isChecked = skipSavePathSelection
        }
        binding.nightMode.summaryPreference.text = defaultNightModeName
    }
}
