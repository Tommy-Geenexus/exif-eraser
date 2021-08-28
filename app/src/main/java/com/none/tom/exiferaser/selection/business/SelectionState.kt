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

package com.none.tom.exiferaser.selection.business

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.IntRange
import com.none.tom.exiferaser.selection.HISTORY_SIZE
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.PROGRESS_MIN
import com.none.tom.exiferaser.selection.data.Result
import com.none.tom.exiferaser.selection.data.Summary
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelectionState(
    val imageResult: Result = Result.Empty,
    val imageSummaries: Array<Summary?> = arrayOfNulls(HISTORY_SIZE),
    val imageUris: Array<Uri?> = arrayOfNulls(HISTORY_SIZE),
    val imagesModified: Int = 0,
    val imagesSaved: Int = 0,
    val imagesTotal: Int = 0,
    @IntRange(from = PROGRESS_MIN.toLong(), to = PROGRESS_MAX.toLong())
    val progress: Int = PROGRESS_MIN,
    val handledAll: Boolean = false
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SelectionState

        if (imageResult != other.imageResult) return false
        if (!imageSummaries.contentEquals(other.imageSummaries)) return false
        if (!imageUris.contentEquals(other.imageUris)) return false
        if (imagesModified != other.imagesModified) return false
        if (imagesSaved != other.imagesSaved) return false
        if (imagesTotal != other.imagesTotal) return false
        if (progress != other.progress) return false
        if (handledAll != other.handledAll) return false

        return true
    }

    override fun hashCode(): Int {
        var result = imageResult.hashCode()
        result = 31 * result + imageSummaries.contentHashCode()
        result = 31 * result + imageUris.contentHashCode()
        result = 31 * result + imagesModified
        result = 31 * result + imagesSaved
        result = 31 * result + imagesTotal
        result = 31 * result + progress
        result = 31 * result + handledAll.hashCode()
        return result
    }
}
