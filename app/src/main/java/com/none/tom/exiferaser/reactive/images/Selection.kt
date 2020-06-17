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

package com.none.tom.exiferaser.reactive.images

import android.net.Uri
import com.none.tom.exiferaser.EMPTY_STRING

sealed class Selection(
    open var isStarted: Boolean = false,
    var isFinished: Boolean = false
)

object EmptySelection : Selection()

class ImageSelection(
    val uri: Uri,
    var uriModified: Uri = Uri.EMPTY,
    var displayName: String = EMPTY_STRING,
    var handled: Boolean = false,
    var modified: Boolean = false,
    var isSaving: Boolean = false
) : Selection()

class ImagesSelection(
    val images: List<ImageSelection>,
    override var isStarted: Boolean = false
) : Selection()

class ImageDirectorySelection(val treeUri: Uri) : Selection()
