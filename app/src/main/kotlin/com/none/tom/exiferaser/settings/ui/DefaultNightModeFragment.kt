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
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.util.TOP_LEVEL_PACKAGE_NAME

class DefaultNightModeFragment : DialogFragment() {

    companion object {

        const val KEY_DEFAULT_NIGHT_MODE = TOP_LEVEL_PACKAGE_NAME + "DEFAULT_NIGHT_MODE"
    }

    private val args: DefaultNightModeFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val defaultNightModes = requireContext().defaultNightModes()
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.night_mode)
            .setSingleChoiceItems(
                defaultNightModes.values.toTypedArray(),
                defaultNightModes.keys.indexOf(args.defaultNightMode).coerceAtLeast(0)
            ) { _, _: Int -> }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val list = (dialog as? AlertDialog)?.listView
                if (list != null && list.checkedItemCount > 0) {
                    dismiss()
                    val defaultNightMode = defaultNightModes
                        .keys
                        .toList()
                        .getOrElse(list.checkedItemPosition) { defaultNightMode }
                    parentFragmentManager.setFragmentResult(
                        KEY_DEFAULT_NIGHT_MODE,
                        bundleOf(KEY_DEFAULT_NIGHT_MODE to defaultNightMode)
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
    }
}

val defaultNightMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
} else {
    AppCompatDelegate.MODE_NIGHT_NO
}

fun Context.defaultNightModes() = mutableMapOf(
    AppCompatDelegate.MODE_NIGHT_YES to getString(R.string.always),
    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM to getString(R.string.automatically),
    AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY to getString(R.string.low_batt_only),
    AppCompatDelegate.MODE_NIGHT_NO to getString(R.string.never)
).apply {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        remove(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}.toMap()

fun Context.defaultNightModeDisplayValue() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    getString(R.string.automatically)
} else {
    getString(R.string.never)
}
