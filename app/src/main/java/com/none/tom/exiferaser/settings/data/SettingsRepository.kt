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

package com.none.tom.exiferaser.settings.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.isNotEmpty
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsDelegate {

    private val defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    fun getDefaultNightMode(): Int {
        return defaultSharedPrefs
            .getString(context.getString(R.string.key_night_mode), null)
            ?.toIntOrNull()
            ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    override fun putDefaultOpenPath(
        path: Uri,
        releaseUriPermissions: Boolean
    ) {
        if (releaseUriPermissions) {
            releasePersistablePermissions(
                resolver = context.contentResolver,
                uri = getDefaultOpenPath(),
                read = true,
                write = false
            )
        }
        putUri(R.string.key_default_path_open, path)
    }

    override fun getDefaultOpenPath() = getUri(R.string.key_default_path_open)

    override fun getDefaultOpenPathSummary(): String {
        val path = getDefaultOpenPath()
        return runCatching {
            if (path.isNotEmpty()) {
                DocumentFile.fromTreeUri(context, path)?.name ?: context.getString(R.string.custom)
            } else {
                context.getString(R.string.none)
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            context.getString(R.string.custom)
        }
    }

    override fun putDefaultSavePath(
        path: Uri,
        releaseUriPermissions: Boolean
    ) {
        if (path.isNotEmpty()) {
            takePersistablePermissions(
                resolver = context.contentResolver,
                uri = path,
                read = true,
                write = true
            )
        } else if (releaseUriPermissions) {
            releasePersistablePermissions(
                resolver = context.contentResolver,
                uri = getDefaultSavePath(),
                read = true,
                write = true
            )
        }
        putUri(R.string.key_default_path_save, path)
    }

    override fun getDefaultSavePath() = getUri(R.string.key_default_path_save)

    override fun getDefaultSavePathSummary(): String {
        val path = getDefaultSavePath()
        return runCatching {
            if (path.isNotEmpty()) {
                DocumentFile.fromTreeUri(context, path)?.name ?: context.getString(R.string.custom)
            } else {
                context.getString(R.string.none)
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            context.getString(R.string.custom)
        }
    }

    override fun shouldPreserveImageOrientation(): Boolean {
        return defaultSharedPrefs.getBoolean(
            context.getString(R.string.key_image_orientation),
            false
        )
    }

    override fun shouldShareImagesByDefault(): Boolean {
        return defaultSharedPrefs.getBoolean(
            context.getString(R.string.key_image_share_by_default),
            false
        )
    }

    override fun isBatchImageProcessingEnabled(): Boolean {
        return defaultSharedPrefs.getBoolean(
            context.getString(R.string.key_batch_image_processing),
            true
        )
    }

    fun hasPrivilegedDefaultSavePath(): Boolean {
        val path = getDefaultSavePath()
        return path.isNotEmpty() &&
            hasPersistablePermissions(
                resolver = context.contentResolver,
                uri = path,
                read = true,
                write = true
            )
    }

    fun getDefaultDisplayNameSuffix(): String {
        return defaultSharedPrefs
            .getString(
                context.getString(R.string.key_default_display_name_suffix),
                String.Empty
            )
            .orEmpty()
    }

    private fun putUri(
        @StringRes id: Int,
        uri: Uri
    ) {
        defaultSharedPrefs.edit {
            putString(context.getString(id), uri.toString())
        }
    }

    private fun getUri(@StringRes id: Int): Uri {
        return defaultSharedPrefs
            .getString(context.getString(id), String.Empty)
            .let { uri -> if (!uri.isNullOrEmpty()) Uri.parse(uri) else Uri.EMPTY }
    }

    @Suppress("SameParameterValue")
    private fun hasPersistablePermissions(
        resolver: ContentResolver,
        uri: Uri,
        read: Boolean,
        write: Boolean
    ): Boolean {
        return resolver.persistedUriPermissions.any { permission ->
            if (permission.uri != uri) {
                false
            } else {
                if (read && !permission.isReadPermission) {
                    return@any false
                }
                if (write && !permission.isWritePermission) {
                    return@any false
                }
                true
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun takePersistablePermissions(
        resolver: ContentResolver,
        uri: Uri,
        read: Boolean,
        write: Boolean
    ) {
        var flags = 0
        if (read) {
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        if (write) {
            flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        runCatching {
            resolver.takePersistableUriPermission(uri, flags)
        }.getOrElse { exception ->
            Timber.e(exception)
        }
    }

    @Suppress("SameParameterValue")
    private fun releasePersistablePermissions(
        resolver: ContentResolver,
        uri: Uri,
        read: Boolean,
        write: Boolean
    ) {
        var flags = 0
        if (read) {
            flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        if (write) {
            flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
        runCatching {
            resolver.releasePersistableUriPermission(uri, flags)
        }.getOrElse { exception ->
            Timber.e(exception)
        }
    }
}
