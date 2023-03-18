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
import com.none.tom.exiferaser.databinding.ItemImageBinding

class ItemImageViewHolder(
    private val binding: ItemImageBinding,
    private val listener: SettingsAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.image.category.setText(R.string.image)
        binding.autoDelete.apply {
            layoutPreferenceCheckBox.setOnClickListener {
                onOff.isChecked = !onOff.isChecked
            }
            onOff.setOnCheckedChangeListener { _, value ->
                listener.onAutoDeleteChanged(value)
            }
            iconPreferenceCheckBox.setImageResource(R.drawable.ic_auto_delete)
            titlePreferenceCheckBox.setText(R.string.image_auto_delete)
        }
        binding.preserveOrientation.apply {
            layoutPreferenceCheckBox.setOnClickListener {
                onOff.isChecked = !onOff.isChecked
            }
            onOff.setOnCheckedChangeListener { _, value ->
                listener.onPreserveOrientationChanged(value)
            }
            iconPreferenceCheckBox.setImageResource(R.drawable.ic_looks)
            titlePreferenceCheckBox.setText(R.string.image_orientation)
        }
        binding.shareByDefault.apply {
            layoutPreferenceCheckBox.setOnClickListener {
                onOff.isChecked = !onOff.isChecked
            }
            onOff.setOnCheckedChangeListener { _, value ->
                listener.onShareByDefaultChanged(value)
            }
            iconPreferenceCheckBox.setImageResource(R.drawable.ic_share)
            titlePreferenceCheckBox.setText(R.string.image_share_by_default)
        }
        binding.defaultDisplayNameSuffix.apply {
            layoutPreference.setOnClickListener {
                listener.onDefaultDisplayNameSuffixSelected()
            }
            iconPreference.setImageResource(R.drawable.ic_title)
            titlePreference.setText(R.string.default_display_name_suffix)
        }
    }

    fun bindImageItem(
        legacyImageSelection: Boolean,
        autoDelete: Boolean,
        preserveOrientation: Boolean,
        shareByDefault: Boolean,
        defaultDisplayNameSuffix: String
    ) {
        binding.autoDelete.apply {
            summaryPreferenceCheckBox.setText(
                if (autoDelete) {
                    R.string.auto_delete_image_on
                } else {
                    R.string.auto_delete_image_off
                }
            )
            layoutPreferenceCheckBox.isEnabled = legacyImageSelection
            iconPreferenceCheckBox.isEnabled = legacyImageSelection
            titlePreferenceCheckBox.isEnabled = legacyImageSelection
            summaryPreferenceCheckBox.isEnabled = legacyImageSelection
            onOff.isEnabled = legacyImageSelection
            onOff.isChecked = autoDelete
        }
        binding.preserveOrientation.apply {
            summaryPreferenceCheckBox.setText(
                if (preserveOrientation) {
                    R.string.preserve_image_orientation_on
                } else {
                    R.string.preserve_image_orientation_off
                }
            )
            onOff.isChecked = preserveOrientation
        }
        binding.shareByDefault.apply {
            summaryPreferenceCheckBox.setText(
                if (shareByDefault) {
                    R.string.share_images_by_default_on
                } else {
                    R.string.share_images_by_default_off
                }
            )
            onOff.isChecked = shareByDefault
        }
        binding.defaultDisplayNameSuffix.summaryPreference.text = defaultDisplayNameSuffix.ifEmpty {
            itemView.context.getString(R.string.none)
        }
    }
}
