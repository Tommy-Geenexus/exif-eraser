// Copyright (c) 2018-2020, Tom Geiselmann
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software
// and associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.none.tom.exiferaser.ui.settings

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import com.none.tom.exiferaser.R

@Suppress("unused")
class NightModeListPreference : ListPreference {

    init {
        val isMinQ = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
        setEntries(if (isMinQ) R.array.night_mode_entries_q else R.array.night_mode_entries)
        setEntryValues(if (isMinQ) R.array.night_mode_entry_values_q else R.array.night_mode_entry_values)
        setValueIndex(entryValues.indexOf(AppCompatDelegate.getDefaultNightMode().toString()).let { index ->
            entryValues.getOrNull(index).let { value ->
                if (value.isNullOrEmpty()) {
                    entryValues.indexOf(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM.toString()).let { newIndex ->
                        if (newIndex < 0) {
                            // Pre-Android Q devices lack AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                            entryValues.indexOf(AppCompatDelegate.MODE_NIGHT_NO.toString())
                        } else {
                            newIndex
                        }
                    }
                } else {
                    index
                }
            }
        })
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
