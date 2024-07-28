/*
 * Copyright (c) 2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.core.extension

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatDelegate
import com.none.tom.exiferaser.R

fun Activity.resolveThemeAttribute(@AttrRes attrRes: Int): Int {
    val tv = TypedValue()
    theme.resolveAttribute(attrRes, tv, true)
    return tv.data
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
