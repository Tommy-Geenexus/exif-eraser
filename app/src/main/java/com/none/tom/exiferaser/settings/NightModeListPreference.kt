/*
 * Copyright (c) 2018-2020, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.settings

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import com.none.tom.exiferaser.R

@Suppress("unused")
class NightModeListPreference : ListPreference {

    init {
        val entries = context.resources.getStringArray(R.array.night_mode_entries).apply {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                dropWhile { entry -> entry == context.getString(R.string.automatically) }
            }
        }
        val values = context.resources.getStringArray(R.array.night_mode_entry_values).apply {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                dropWhile { value ->
                    value == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()
                }
            }
        }
        val default = AppCompatDelegate.getDefaultNightMode().toString()
        val fallback = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.indexOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString())
        } else {
            // Pre-Android Q devices lack AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            values.indexOf(AppCompatDelegate.MODE_NIGHT_NO.toString())
        }
        setEntries(entries)
        entryValues = values
        setValueIndex(if (!values.contains(default)) fallback else entryValues.indexOf(default))
    }

    constructor(context: Context) : super(context)

    constructor(
        context: Context,
        attributeSet: AttributeSet
    ) : super(context, attributeSet)

    constructor(
        context: Context,
        attributeSet: AttributeSet,
        defStyleAttr: Int
    ) : super(
        context,
        attributeSet,
        defStyleAttr
    )

    constructor(
        context: Context,
        attributeSet: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(
        context,
        attributeSet,
        defStyleAttr,
        defStyleRes
    )
}
