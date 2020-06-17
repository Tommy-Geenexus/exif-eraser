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

package com.none.tom.exiferaser.data

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import com.none.tom.exiferaser.EMPTY_STRING
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.isNotEmpty
import com.none.tom.exiferaser.revokePermissions

class SharedPrefsRepository(
    private val context: Context
) : SharedPrefsDelegate {

    companion object {
        const val KEY_IMAGE_POSITION = "image_position"
        const val KEY_IMAGES_POSITION = "images_position"
        const val KEY_IMAGE_DIRECTORY_POSITION = "image_directory_position"
    }

    private val sharedPrefs = context.getSharedPreferences(
        context.packageName + "_preferences_private",
        Context.MODE_PRIVATE
    )
    private val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val imageSources = mutableMapOf(
        KEY_IMAGE_POSITION to 0,
        KEY_IMAGES_POSITION to 1,
        KEY_IMAGE_DIRECTORY_POSITION to 2
    )
    private var openPath = Uri.EMPTY
    private var savePath = Uri.EMPTY

    override fun getImageSourcePositions(): Map<String, Int> {
        return with(sharedPrefs) {
            mapOf(
                KEY_IMAGE_POSITION to getInt(KEY_IMAGE_POSITION, 0),
                KEY_IMAGES_POSITION to getInt(KEY_IMAGES_POSITION, 1),
                KEY_IMAGE_DIRECTORY_POSITION to getInt(KEY_IMAGE_DIRECTORY_POSITION, 2)
            )
        }
    }

    @SuppressLint("NewApi")
    override fun putPreliminaryImageSourcePositions() {
        if (imageSources != getImageSourcePositions()) {
            with(sharedPrefs.edit()) {
                putInt(KEY_IMAGE_POSITION, imageSources.getOrDefault(KEY_IMAGE_POSITION, 0))
                putInt(KEY_IMAGES_POSITION, imageSources.getOrDefault(KEY_IMAGES_POSITION, 1))
                putInt(KEY_IMAGE_DIRECTORY_POSITION, imageSources.getOrDefault(KEY_IMAGE_DIRECTORY_POSITION, 2))
                apply()
            }
        }
    }

    override fun updateImageSourcePositions(
        oldTag: String,
        newTag: String,
        oldPosition: Int,
        newPosition: Int
    ) {
        if (oldPosition != newPosition) {
            imageSources[newTag] = oldPosition
            imageSources[oldTag] = newPosition
        }
    }

    override fun putDefaultOpenPath(uri: Uri) {
        putUri(R.string.key_default_path_open, uri).also {
            uri.revokePermissions(context)
            openPath = uri
        }
    }

    override fun getDefaultOpenPath(): Uri {
        return if (openPath.isNotEmpty()) {
            openPath
        } else {
            getUri(R.string.key_default_path_open).also { uri -> openPath = uri }
        }
    }

    override fun getDefaultOpenPathSummary(): String {
        return getDefaultOpenPath().let { path ->
            context.getString(
                if (path == Uri.EMPTY) {
                    R.string.none
                } else {
                    R.string.custom
                }
            )
        }
    }

    override fun putDefaultSavePath(uri: Uri) {
        putUri(R.string.key_default_path_save, uri).also {
            uri.revokePermissions(context)
            savePath = uri
        }
    }

    override fun getDefaultSavePath(): Uri {
        return if (savePath.isNotEmpty()) {
            savePath
        } else {
            getUri(R.string.key_default_path_save).also { uri -> savePath = uri }
        }
    }

    override fun getDefaultSavePathSummary(): String {
        return getDefaultSavePath().let { path ->
            context.getString(
                if (path == Uri.EMPTY) {
                    R.string.none
                } else {
                    R.string.custom
                }
            )
        }
    }

    override fun shouldPreserveImageOrientation(): Boolean {
        return defaultSharedPrefs.getBoolean(context.getString(R.string.key_image_orientation), false)
    }

    override fun putPreserveImageOrientation(preserveOrientation: Boolean) {
        with(defaultSharedPrefs.edit()) {
            putBoolean(context.getString(R.string.key_image_orientation), preserveOrientation)
            apply()
        }
    }

    override fun shouldShareImagesByDefault(): Boolean {
        return defaultSharedPrefs.getBoolean(context.getString(R.string.key_image_share_by_default), false)
    }

    fun getDefaultDisplayNameSuffix(): String {
        return defaultSharedPrefs.getString(
            context.getString(R.string.key_default_display_name_suffix),
            EMPTY_STRING
        )!!
    }

    private fun putUri(
        @StringRes id: Int,
        uri: Uri
    ) {
        with(defaultSharedPrefs.edit()) {
            putString(context.getString(id), uri.toString())
            apply()
        }
    }

    private fun getUri(@StringRes id: Int): Uri {
        return defaultSharedPrefs
            .getString(context.getString(id), EMPTY_STRING)
            .let { uri ->
                if (!uri.isNullOrEmpty()) {
                    Uri.parse(uri)
                } else {
                    Uri.EMPTY
                }
            }
    }
}
