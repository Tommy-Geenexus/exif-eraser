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

package com.none.tom.exiferaser

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.none.tom.exiferaser.reactive.images.EmptySelection
import com.none.tom.exiferaser.reactive.images.ImageSelection
import com.none.tom.exiferaser.reactive.images.ImagesSelection
import com.none.tom.exiferaser.reactive.images.Selection

fun Activity.showLongToast(
    @StringRes id: Int
) {
    Toast.makeText(this, id, Toast.LENGTH_LONG)
        .show()
}

fun Fragment.showShortSnackbar(
    @StringRes id: Int,
    @IdRes anchor: Int = 0
) {
    with(Snackbar.make(requireView(), id, Snackbar.LENGTH_SHORT)) {
        if (anchor != 0) {
            setAnchorView(anchor)
        }
        show()
    }
}

fun Intent.isSupported(): Boolean {
    return if (action.isNullOrEmpty()) {
        false
    } else {
        (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) &&
                type == MIME_TYPE_JPEG &&
                clipData != null
    }
}

fun Intent.isMimeTypeUnsupported(): Boolean {
    return if (type.isNullOrEmpty()) {
        false
    } else {
        (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) &&
                type != MIME_TYPE_JPEG
    }
}

fun Intent.toImageOrImagesSelection(): Selection {
    return when {
        clipData != null -> {
            mutableListOf<ImageSelection>().let { selection ->
                for (i in 0 until clipData!!.itemCount) {
                    clipData!!.getItemAt(i).uri.also { uri ->
                        if (uri != null) {
                            selection.add(ImageSelection(uri))
                        }
                    }
                }
                when {
                    selection.isEmpty() -> EmptySelection
                    selection.size < 2 -> selection[0]
                    else -> ImagesSelection(selection)
                }
            }
        }
        data != null -> ImageSelection(data!!)
        else -> EmptySelection
    }
}

fun Uri.revokePermissions(
    context: Context
) {
    context.revokeUriPermission(
        this,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    )
}

fun Uri.isNotEmpty() = this != Uri.EMPTY
