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
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.settings.defaultNightMode

class DefaultNightModeFragment : DialogFragment() {

    internal companion object {

        const val KEY_DEFAULT_NIGHT_MODE = TOP_LEVEL_PACKAGE_NAME + "DEFAULT_NIGHT_MODE"
    }

    private val args: DefaultNightModeFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val defaultNightMode by requireContext().defaultNightMode()
        val values = defaultNightMode.values.toList()
        val checkedPosition = values.indexOf(args.defaultNightMode)
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.night_mode)
            .setSingleChoiceItems(
                defaultNightMode.keys.toTypedArray(),
                checkedPosition
            ) { _, _: Int ->
            }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val list = (dialog as? AlertDialog)?.listView
                if (list != null && list.checkedItemCount > 0) {
                    dismiss()
                    parentFragmentManager.setFragmentResult(
                        KEY_DEFAULT_NIGHT_MODE,
                        bundleOf(
                            KEY_DEFAULT_NIGHT_MODE to values[list.checkedItemPosition]
                        )
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}
