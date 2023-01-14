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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.databinding.FragmentDefaultDisplayNameSuffixBinding

class DefaultDisplayNameSuffixFragment : DialogFragment() {

    internal companion object {

        const val KEY_DEFAULT_DISPLAY_NAME_SUFFIX =
            TOP_LEVEL_PACKAGE_NAME + "DEFAULT_DISPLAY_NAME_SUFFIX"
    }

    private val args: DefaultDisplayNameSuffixFragmentArgs by navArgs()

    private var _binding: FragmentDefaultDisplayNameSuffixBinding? = null
    internal val binding: FragmentDefaultDisplayNameSuffixBinding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentDefaultDisplayNameSuffixBinding.inflate(layoutInflater)
        binding.defaultDisplayNameSuffix.setText(
            args.defaultDisplayNameSuffix,
            TextView.BufferType.EDITABLE
        )
        return MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    KEY_DEFAULT_DISPLAY_NAME_SUFFIX,
                    bundleOf(
                        KEY_DEFAULT_DISPLAY_NAME_SUFFIX to
                            binding.defaultDisplayNameSuffix.text?.trim().toString()
                    )
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        _binding = null
    }
}
