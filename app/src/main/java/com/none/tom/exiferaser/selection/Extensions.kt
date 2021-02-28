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

package com.none.tom.exiferaser.selection

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.view.View
import androidx.annotation.WorkerThread
import androidx.core.view.isVisible
import com.none.tom.exiferaser.EXTENSION_JPEG
import com.none.tom.exiferaser.EXTENSION_PNG
import com.none.tom.exiferaser.EXTENSION_WEBP
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.MIME_TYPE_JPEG
import com.none.tom.exiferaser.MIME_TYPE_PNG
import com.none.tom.exiferaser.MIME_TYPE_WEBP
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

fun View.fadeIn() {
    alpha = 0f
    isVisible = true
    animate()
        .alpha(1f)
        .duration = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
}

fun View.fadeOut() {
    alpha = 0f
    isVisible = true
    animate()
        .alpha(0f)
        .setListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isVisible = false
                }
            }
        )
        .duration = resources.getInteger(android.R.integer.config_longAnimTime).toLong()
}

fun View.crossfade(view: View) {
    fadeIn()
    view.fadeOut()
}

fun Boolean.toInt() = if (this) 1 else 0

fun Int.toPercent() = toString().plus('%')

fun <E> Array<E>.setOrSkip(
    index: Int,
    element: E
) {
    if (index in 0..lastIndex) {
        set(index, element)
    }
}

fun String.getExtensionFromMimeTypeOrEmpty(): String {
    return when (this) {
        MIME_TYPE_JPEG -> EXTENSION_JPEG
        MIME_TYPE_PNG -> EXTENSION_PNG
        MIME_TYPE_WEBP -> EXTENSION_WEBP
        else -> String.Empty
    }
}

@WorkerThread
fun ContentResolver.openInputStreamOrThrow(uri: Uri): InputStream {
    return openInputStream(uri) ?: throw IOException()
}

@WorkerThread
fun ContentResolver.openOutputStreamOrThrow(uri: Uri): OutputStream {
    return openOutputStream(uri) ?: throw IOException()
}

@SuppressLint("Recycle")
@WorkerThread
fun ContentResolver.queryOrThrow(uri: Uri): Cursor {
    return query(uri, null, null, null, null) ?: throw IOException()
}
