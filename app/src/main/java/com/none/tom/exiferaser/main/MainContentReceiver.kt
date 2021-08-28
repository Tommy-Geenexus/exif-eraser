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

package com.none.tom.exiferaser.main

import android.net.Uri
import android.os.Build
import android.view.DragEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import com.none.tom.exiferaser.addUrisToSet
import com.none.tom.exiferaser.areMimeTypesSupported
import com.none.tom.exiferaser.isNotNullOrEmpty
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
class MainContentReceiver(
    private val listener: Listener
) : OnReceiveContentListener {

    interface Listener {

        @RequiresApi(Build.VERSION_CODES.N)
        fun onUrisReceived(
            event: DragEvent,
            uris: List<Uri>
        )
    }

    override fun onReceiveContent(
        view: View,
        payload: ContentInfoCompat
    ): ContentInfoCompat? {
        if (!view.canReceiveContent()) {
            return payload
        }
        val split = payload.partition { item ->
            item.uri.isNotNullOrEmpty()
        }
        val content = split.first
        if (content != null) {
            val event = payload.extras?.getParcelable<DragEvent>(DragEvent.ACTION_DROP.toString())
            if (event != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val clipData = content.clip
                if (clipData.description.areMimeTypesSupported()) {
                    val uris = linkedSetOf<Uri>().apply {
                        clipData.addUrisToSet(this)
                    }
                    if (uris.isNotEmpty()) {
                        listener.onUrisReceived(
                            event = event,
                            uris = uris.toList()
                        )
                        return split.second
                    }
                }
            }
        }
        return payload
    }
}
