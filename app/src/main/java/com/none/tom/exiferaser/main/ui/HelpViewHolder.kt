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

package com.none.tom.exiferaser.main.ui

import android.content.pm.PackageManager
import android.os.Build
import androidx.recyclerview.widget.RecyclerView
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.databinding.PreferenceBinding
import com.none.tom.exiferaser.supportImageFormats
import timber.log.Timber

class HelpViewHolder(
    private val binding: PreferenceBinding,
    private val listener: HelpAdapter.Listener
) : RecyclerView.ViewHolder(binding.root) {

    private companion object {

        const val TAG_ITEM_HELP_TRANSLATE = TOP_LEVEL_PACKAGE_NAME + "ITEM_HELP_TRANSLATE"
        const val TAG_ITEM_FEEDBACK = TOP_LEVEL_PACKAGE_NAME + "ITEM_FEEDBACK"
    }

    init {
        binding.layoutPreference.apply {
            setOnClickListener {
                if (tag == TAG_ITEM_HELP_TRANSLATE) {
                    listener.onHelpTranslateSelected()
                } else if (tag == TAG_ITEM_FEEDBACK) {
                    listener.onFeedbackSelected()
                }
            }
        }
    }

    fun bindItemSupportedImageFormats() {
        binding.apply {
            layoutPreference.tag = null
            iconPreference.setImageResource(R.drawable.ic_extension)
            titlePreference.setText(R.string.image_supported_formats)
            summaryPreference.text = supportImageFormats.joinToString()
        }
    }

    fun bindItemHelpTranslate() {
        binding.apply {
            layoutPreference.tag = TAG_ITEM_HELP_TRANSLATE
            iconPreference.setImageResource(R.drawable.ic_translate)
            titlePreference.setText(R.string.become_translator)
            summaryPreference.setText(R.string.onesky)
        }
    }

    fun bindItemFeedback() {
        binding.apply {
            layoutPreference.tag = TAG_ITEM_FEEDBACK
            iconPreference.setImageResource(R.drawable.ic_bug_report)
            titlePreference.setText(R.string.report_bug)
            summaryPreference.setText(R.string.github)
        }
    }

    fun bindItemBuildVersion() {
        binding.apply {
            layoutPreference.tag = null
            iconPreference.setImageResource(R.drawable.ic_build_circle)
            titlePreference.setText(R.string.version)
            summaryPreference.text = getBuildVersion()
        }
    }

    @Suppress("DEPRECATION")
    private fun getBuildVersion(): String {
        val context = itemView.context
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
        } catch (e: Exception) {
            Timber.e(e)
            String.Empty
        }
    }
}
