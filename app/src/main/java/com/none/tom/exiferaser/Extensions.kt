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

package com.none.tom.exiferaser

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.contracts.contract
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

suspend fun <T> CoroutineContext.suspendRunCatching(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (exception: Exception) {
    ensureActive()
    Result.failure(exception)
}

fun Context.getClipImages(): List<Uri> {
    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val resultSet = linkedSetOf<Uri>()
    if (cm.hasPrimaryClip() && cm.primaryClipDescription?.areMimeTypesSupported() == true) {
        cm.primaryClip?.addUrisToSet(resultSet)
    }
    return resultSet.toList()
}

fun Intent.getClipDataUris(): Array<Uri> {
    val resultSet = linkedSetOf<Uri>()
    val d = data
    if (d != null) {
        resultSet.add(d)
    }
    val c = clipData
    if (c == null && resultSet.isEmpty()) {
        return emptyArray()
    } else if (c?.description?.areMimeTypesSupported() == true) {
        c.addUrisToSet(resultSet)
    }
    return resultSet.toTypedArray()
}

fun ClipData.addUrisToSet(resultSet: LinkedHashSet<Uri>) {
    for (i in 0 until itemCount) {
        val uri = getItemAt(i)?.uri
        if (uri.isNotNullOrEmpty()) {
            resultSet.add(uri)
        }
    }
}

fun ClipDescription.areMimeTypesSupported(): Boolean {
    for (i in 0 until mimeTypeCount) {
        val mimeType = getMimeType(i)
        if (mimeType != MIME_TYPE_IMAGE && !supportedMimeTypes.contains(mimeType)) {
            return false
        }
    }
    return true
}

fun Uri.isFileProviderUri() = pathSegments.any { path -> path == "my_images" }

fun Uri?.isNullOrEmpty() = this == null || this == Uri.EMPTY

fun Uri?.isNotNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && isNotEmpty()
}

fun Uri.isNotEmpty() = this != Uri.EMPTY

@Throws(IOException::class)
suspend fun ContentResolver.openInputStreamOrThrow(
    coroutineContext: CoroutineContext,
    uri: Uri
): InputStream {
    return withContext(coroutineContext) {
        openInputStream(uri) ?: throw IOException()
    }
}

@Throws(IOException::class)
suspend fun ContentResolver.openOutputStreamOrThrow(
    coroutineContext: CoroutineContext,
    uri: Uri
): OutputStream {
    return withContext(coroutineContext) {
        openOutputStream(uri) ?: throw IOException()
    }
}

@Throws(IOException::class)
suspend fun ContentResolver.queryOrThrow(
    coroutineContext: CoroutineContext,
    uri: Uri
): Cursor {
    return withContext(coroutineContext) {
        query(uri, null, null, null, null) ?: throw IOException()
    }
}

fun Boolean.toInt() = if (this) 1 else 0

fun Int.toPercent() = toString().plus('%')

fun Int.toProgress(max: Int) = (this * PROGRESS_MAX) / max

fun <E> MutableList<E>.addOrShift(
    element: E,
    shiftAtSize: Int = REPORT_SUMMARIES_MAX
): List<E> {
    if (size >= shiftAtSize) {
        removeFirstOrNull() ?: return this
    }
    add(element)
    return this
}

fun String.getExtensionFromMimeTypeOrEmpty(): String {
    return when (this) {
        MIME_TYPE_JPEG -> EXTENSION_JPEG
        MIME_TYPE_PNG -> EXTENSION_PNG
        MIME_TYPE_WEBP -> EXTENSION_WEBP
        else -> ""
    }
}
