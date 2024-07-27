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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.util.FileUtilsKt
import com.none.tom.exiferaser.core.util.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.databinding.FragmentDefaultDisplayNameSuffixBinding

class DefaultDisplayNameSuffixFragment : DialogFragment() {

    companion object {

        const val KEY_DEFAULT_DISPLAY_NAME_SUFFIX =
            TOP_LEVEL_PACKAGE_NAME + "DEFAULT_DISPLAY_NAME_SUFFIX"
    }

    private val args: DefaultDisplayNameSuffixFragmentArgs by navArgs()

    private var _binding: FragmentDefaultDisplayNameSuffixBinding? = null
    private val binding: FragmentDefaultDisplayNameSuffixBinding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentDefaultDisplayNameSuffixBinding.inflate(layoutInflater)
        binding.defaultDisplayNameSuffix.setText(
            args.defaultDisplayNameSuffix,
            TextView.BufferType.EDITABLE
        )
        binding.defaultDisplayNameSuffix.doOnTextChanged { text, _, _, _ ->
            binding.defaultDisplayNameSuffixLayout.error = if (
                !FileUtilsKt.isValidExtFilename(text.toString())
            ) {
                getString(R.string.suffix_invalid)
            } else {
                null
            }
            (dialog as? AlertDialog)?.getButton(DialogInterface.BUTTON_POSITIVE)?.isEnabled =
                binding.defaultDisplayNameSuffixLayout.error.isNullOrEmpty()
        }
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
            .apply {
                setOnShowListener {
                    binding.defaultDisplayNameSuffix.run {
                        requestFocus()
                        setSelection(text?.length ?: 0)
                    }
                    window?.let { w ->
                        WindowCompat
                            .getInsetsController(w, binding.defaultDisplayNameSuffix)
                            .show(WindowInsetsCompat.Type.ime())
                    }
                }
            }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        _binding = null
    }
}
