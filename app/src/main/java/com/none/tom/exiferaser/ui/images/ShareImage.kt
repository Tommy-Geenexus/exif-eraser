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

package com.none.tom.exiferaser.ui.images

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.none.tom.exiferaser.MIME_TYPE_JPEG
import com.none.tom.exiferaser.R

class ShareImage : ActivityResultContract<Uri, Boolean>() {

    override fun createIntent(
        context: Context,
        input: Uri?
    ): Intent {
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = MIME_TYPE_JPEG
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(MIME_TYPE_JPEG))
                if (input != null) {
                    putExtra(Intent.EXTRA_STREAM, input)
                }
            },
            context.getString(R.string.share_via)
        )
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ) = resultCode == Activity.RESULT_OK
}