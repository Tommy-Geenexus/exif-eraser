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

package com.none.tom.exiferaser.settings.ui

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.ItemFsBinding

class ItemFsViewHolder(
    private val binding: ItemFsBinding,
    private val listener: SettingsAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.randomizeFileNames.apply {
            layoutPreferenceCheckBox.setOnClickListener {
                onOff.isChecked = !onOff.isChecked
            }
            onOff.setOnCheckedChangeListener { _, value ->
                listener.onRandomizeFileNamesChanged(value)
            }
            iconPreferenceCheckBox.setImageResource(R.drawable.ic_shuffle)
            titlePreferenceCheckBox.setText(R.string.randomize_file_names)
        }
        binding.defaultPathOpen.apply {
            layoutPreferenceFs.setOnClickListener {
                listener.onDefaultPathOpenSelected()
            }
            clear.setOnClickListener {
                listener.onDefaultPathOpenClear()
            }
            iconPreferenceFs.setImageResource(R.drawable.ic_folder_open)
            titlePreferenceFs.setText(R.string.default_path_open)
        }
        binding.defaultPathSave.apply {
            layoutPreferenceFs.setOnClickListener {
                listener.onDefaultPathSaveSelected()
            }
            clear.setOnClickListener {
                listener.onDefaultPathSaveClear()
            }
            iconPreferenceFs.setImageResource(R.drawable.ic_folder_special)
            titlePreferenceFs.setText(R.string.default_path_save)
        }
        binding.fs.category.setText(R.string.file_system)
    }

    fun bindItemFs(
        randomizeFileNames: Boolean,
        defaultOpenPathName: String,
        defaultSavePathName: String
    ) {
        binding.randomizeFileNames.apply {
            summaryPreferenceCheckBox.setText(
                if (randomizeFileNames) {
                    R.string.randomize_file_names_on
                } else {
                    R.string.randomize_file_names_off
                }
            )
            onOff.isChecked = randomizeFileNames
        }
        binding.defaultPathOpen.apply {
            val hasPath = defaultOpenPathName.isNotEmpty()
            clear.isVisible = hasPath
            summaryPreferenceFs.text = if (hasPath) {
                defaultOpenPathName
            } else {
                itemView.context.getString(R.string.none)
            }
        }
        binding.defaultPathSave.apply {
            val hasPath = defaultSavePathName.isNotEmpty()
            clear.isVisible = hasPath
            summaryPreferenceFs.text = if (hasPath) {
                defaultSavePathName
            } else {
                itemView.context.getString(R.string.none)
            }
        }
    }
}
