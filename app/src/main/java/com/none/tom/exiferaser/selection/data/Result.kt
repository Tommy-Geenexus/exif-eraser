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

package com.none.tom.exiferaser.selection.data

import android.os.Parcelable
import androidx.annotation.IntRange
import com.none.tom.exiferaser.PROGRESS_MAX
import com.none.tom.exiferaser.PROGRESS_MIN
import kotlinx.parcelize.Parcelize

sealed class Result : Parcelable {

    @Parcelize
    data object Empty : Result()

    @Parcelize
    data class Report(
        val summary: Summary = Summary()
    ) : Result()

    @Parcelize
    data class Handled(
        @IntRange(from = PROGRESS_MIN.toLong(), to = PROGRESS_MAX.toLong())
        val progress: Int = PROGRESS_MIN
    ) : Result()

    @Parcelize
    data object HandledAll : Result()
}
