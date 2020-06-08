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
import android.provider.DocumentsContract
import androidx.activity.result.contract.ActivityResultContract
import com.none.tom.exiferaser.MIME_TYPE_JPEG

class CreateDocument : ActivityResultContract<Pair<String, Uri?>, Uri>() {

    override fun createIntent(
        context: Context,
        input: Pair<String, Uri?>?
    ): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = MIME_TYPE_JPEG
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(MIME_TYPE_JPEG))
            if (input != null) {
                putExtra(Intent.EXTRA_TITLE, input.first)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && input.second != null) {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, input.second)
                }
            }
        }
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): Uri? {
        return if (resultCode != Activity.RESULT_OK || intent == null) {
            null
        } else {
            return intent.data
        }
    }
}
