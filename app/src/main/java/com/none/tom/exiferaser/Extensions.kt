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
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import com.google.android.material.snackbar.Snackbar
import dev.chrisbanes.insetter.applyInsetter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun View.applyInsetMargins() {
    applyInsetter {
        type(
            navigationBars = true,
            statusBars = true
        ) {
            margin()
        }
    }
}

fun View.showSnackbar(
    anchor: View? = null,
    msg: String,
    @StringRes actionMsg: Int = 0,
    onActionClick: View.OnClickListener? = null,
    length: Int = Snackbar.LENGTH_SHORT
) {
    var backingSnackbar: Snackbar? = Snackbar
        .make(this, msg, length)
        .setAnchorView(anchor)
        .setAction(if (actionMsg != 0) context.getString(actionMsg) else null, onActionClick)
    val snackbar = backingSnackbar
    val lifecycle = findViewTreeLifecycleOwner()?.lifecycle
    if (snackbar != null && lifecycle != null) {
        lifecycle.addObserver(
            object : LifecycleEventObserver {

                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_STOP -> {
                            lifecycle.removeObserver(this)
                            snackbar.dismiss()
                            backingSnackbar = null
                        }
                        else -> {
                        }
                    }
                }
            }
        )
        snackbar.show()
    } else {
        backingSnackbar = null
    }
}

@ExperimentalContracts
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

@ExperimentalContracts
fun ClipData.addUrisToSet(resultSet: LinkedHashSet<Uri>) {
    for (i in 0 until itemCount) {
        val uri = getItemAt(i)?.uri
        if (uri.isNotNullOrEmpty()) {
            resultSet.add(uri)
        }
    }
}

@Suppress("BooleanMethodIsAlwaysInverted")
fun ClipDescription.areMimeTypesSupported(): Boolean {
    for (i in 0 until mimeTypeCount) {
        val mimeType = getMimeType(i)
        if (mimeType != MIME_TYPE_IMAGE && !supportedMimeTypes.contains(mimeType)) {
            return false
        }
    }
    return true
}

fun Uri?.isNullOrEmpty() = this == null || this == Uri.EMPTY

@ExperimentalContracts
fun Uri?.isNotNullOrEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullOrEmpty != null)
    }
    return this != null && isNotEmpty()
}

@Suppress("BooleanMethodIsAlwaysInverted")
fun Uri.isNotEmpty() = this != Uri.EMPTY
