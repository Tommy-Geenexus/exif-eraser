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

package com.none.tom.exiferaser.selection.data

import android.os.FileUtils
import android.text.TextUtils
import java.nio.charset.StandardCharsets

/**
 * @see FileUtils
 */
object FileUtilsKt {

    fun isValidExtFilename(name: String?): Boolean {
        return name != null && name == buildValidExtFilename(name)
    }

    private fun isValidExtFilenameChar(c: Char): Boolean {
        return when (c) {
            '\u0000', '/' -> false
            else -> true
        }
    }

    private fun buildValidExtFilename(name: String): String {
        if (TextUtils.isEmpty(name) || "." == name || ".." == name) {
            return "(invalid)"
        }
        val res = StringBuilder(name.length)
        for (c in name) {
            if (isValidExtFilenameChar(c)) {
                res.append(c)
            } else {
                res.append('_')
            }
        }
        trimFilename(res, 255)
        return res.toString()
    }

    @Suppress("SameParameterValue")
    private fun trimFilename(
        res: StringBuilder,
        maxBytes: Int
    ) {
        var bytes = maxBytes
        var raw = res.toString().toByteArray(StandardCharsets.UTF_8)
        if (raw.size > bytes) {
            bytes -= 3
            while (raw.size > bytes) {
                res.deleteCharAt(res.length / 2)
                raw = res.toString().toByteArray(StandardCharsets.UTF_8)
            }
            res.insert(res.length / 2, "...")
        }
    }
}
