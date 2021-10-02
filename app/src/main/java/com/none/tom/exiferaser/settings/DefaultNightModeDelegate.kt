/*
 * Copyright (c) 2018-2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.R
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class DefaultNightModeDelegate(
    private val context: Context
) : ReadOnlyProperty<Nothing?, Map<String, Int>> {

    override fun getValue(
        thisRef: Nothing?,
        property: KProperty<*>
    ): Map<String, Int> = getEntries()

    private fun getEntries(): Map<String, Int> {
        val value = mapOf(
            context.getString(R.string.always) to AppCompatDelegate.MODE_NIGHT_YES,
            context.getString(R.string.automatically) to AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            context.getString(R.string.low_batt_only) to AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
            context.getString(R.string.never) to AppCompatDelegate.MODE_NIGHT_NO
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            value
        } else {
            value
                .entries
                .dropWhile { entry ->
                    entry.value == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                .associate { entry -> entry.key to entry.value }
        }
    }
}

fun Map<String, Int>.name(): String {
    return entries
        .firstOrNull { entry ->
            entry.value == defaultNightModeValue
        }
        ?.key
        ?: String.Empty
}

fun Context.defaultNightMode(): DefaultNightModeDelegate {
    return DefaultNightModeDelegate(this.applicationContext)
}

val defaultNightModeValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
} else {
    AppCompatDelegate.MODE_NIGHT_NO
}
