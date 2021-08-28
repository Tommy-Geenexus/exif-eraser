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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.none.tom.exiferaser.MIME_TYPE_IMAGE

class ShareImages : ActivityResultContract<Pair<String, ArrayList<Uri>>, Boolean>() {

    override fun createIntent(
        context: Context,
        input: Pair<String, ArrayList<Uri>>?
    ): Intent {
        return Intent.createChooser(
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                setTypeAndNormalize(MIME_TYPE_IMAGE)
                if (input != null) {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, input.second)
                }
            },
            input?.first.orEmpty()
        )
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ) = resultCode == Activity.RESULT_OK
}
