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

import android.content.ClipData
import android.net.Uri
import com.none.tom.exiferaser.core.image.supportedImageFormats
import com.none.tom.exiferaser.core.util.MIME_TYPE_IMAGE

fun ClipData.supportedImageUrisToList(): List<Uri> {
    for (i in 0 until description.mimeTypeCount) {
        val mimeType = description.getMimeType(i)
        if (mimeType != MIME_TYPE_IMAGE &&
            supportedImageFormats.none { f -> f.mimeType == mimeType }
        ) {
            return emptyList()
        }
    }
    val uris = mutableListOf<Uri>()
    for (i in 0 until itemCount) {
        val uri = getItemAt(i)?.uri
        if (uri.isNotNullOrEmpty()) {
            uris.add(uri)
        }
    }
    return uris.toList()
}
