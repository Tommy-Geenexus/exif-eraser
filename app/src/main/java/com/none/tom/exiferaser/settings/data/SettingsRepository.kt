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
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.di.DispatcherIo
import com.none.tom.exiferaser.isNotEmpty
import com.none.tom.exiferaser.settings.defaultNightMode
import com.none.tom.exiferaser.settings.defaultNightModeValue
import com.none.tom.exiferaser.settings.name
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @DispatcherIo private val dispatcher: CoroutineDispatcher
) {

    companion object {

        // Keys copied from settings SharedPreferences for DataStore migration
        const val KEY_DEFAULT_OPEN_PATH = "default_path_open"
        const val KEY_DEFAULT_SAVE_PATH = "default_path_save"
        const val KEY_PRESERVE_ORIENTATION = "image_orientation"
        const val KEY_SHARE_BY_DEFAULT = "image_share_by_default"
        const val KEY_DEFAULT_DISPLAY_NAME_SUFFIX = "default_display_name_suffix"
        const val KEY_DEFAULT_NIGHT_MODE = "night_mode"
    }

    private val keyDefaultOpenPath = stringPreferencesKey(KEY_DEFAULT_OPEN_PATH)
    private val keyDefaultSavePath = stringPreferencesKey(KEY_DEFAULT_SAVE_PATH)
    private val keyPreserveOrientation = booleanPreferencesKey(KEY_PRESERVE_ORIENTATION)
    private val keyShareByDefault = booleanPreferencesKey(KEY_SHARE_BY_DEFAULT)
    private val keyDefaultDisplayNameSuffix = stringPreferencesKey(KEY_DEFAULT_DISPLAY_NAME_SUFFIX)
    private val keyDefaultNightMode = intPreferencesKey(KEY_DEFAULT_NIGHT_MODE)

    suspend fun putDefaultPathOpen(
        defaultPathOpenNew: Uri,
        defaultPathOpenCurrent: Uri,
        releaseUriPermissions: Boolean
    ): Boolean {
        if (defaultPathOpenNew.isNotEmpty()) {
            takePersistablePermissions(
                resolver = context.contentResolver,
                uri = defaultPathOpenNew,
                read = true,
                write = false
            )
        } else if (releaseUriPermissions) {
            releasePersistablePermissions(
                resolver = context.contentResolver,
                uri = defaultPathOpenCurrent,
                read = true,
                write = false
            )
        }
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyDefaultOpenPath] = defaultPathOpenNew.toString()
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun getDefaultPathOpen(): Flow<Uri> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
                Uri.EMPTY
            }
            .map { preferences ->
                preferences[keyDefaultOpenPath]?.toUri() ?: Uri.EMPTY
            }
            .flowOn(dispatcher)
    }

    suspend fun getDefaultPathOpenName(): String {
        val defaultPathOpen = getDefaultPathOpen().first()
        return runCatching {
            if (defaultPathOpen.isNotEmpty()) {
                DocumentFile.fromTreeUri(context, defaultPathOpen)?.name.orEmpty()
            } else {
                String.Empty
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            String.Empty
        }
    }

    suspend fun putDefaultPathSave(
        defaultPathSaveNew: Uri,
        defaultPathSaveCurrent: Uri,
        releaseUriPermissions: Boolean
    ): Boolean {
        if (defaultPathSaveNew.isNotEmpty()) {
            takePersistablePermissions(
                resolver = context.contentResolver,
                uri = defaultPathSaveNew,
                read = true,
                write = true
            )
        } else if (releaseUriPermissions) {
            releasePersistablePermissions(
                resolver = context.contentResolver,
                uri = defaultPathSaveCurrent,
                read = true,
                write = true
            )
        }
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyDefaultSavePath] = defaultPathSaveNew.toString()
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun getDefaultPathSave(): Flow<Uri> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
                Uri.EMPTY
            }
            .map { preferences ->
                preferences[keyDefaultSavePath]?.toUri() ?: Uri.EMPTY
            }
            .flowOn(dispatcher)
    }

    suspend fun getDefaultPathSaveName(): String {
        val defaultPathSave = getDefaultPathSave().first()
        return runCatching {
            if (defaultPathSave.isNotEmpty()) {
                DocumentFile.fromTreeUri(context, defaultPathSave)?.name.orEmpty()
            } else {
                String.Empty
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            String.Empty
        }
    }

    suspend fun hasPrivilegedDefaultPathSave(defaultSavePath: Uri): Boolean {
        return withContext(dispatcher) {
            defaultSavePath.isNotEmpty() &&
                hasPersistablePermissions(
                    resolver = context.contentResolver,
                    uri = defaultSavePath,
                    read = true,
                    write = true
                )
        }
    }

    suspend fun putPreserveOrientation(value: Boolean): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyPreserveOrientation] = value
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun shouldPreserveOrientation(): Flow<Boolean> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
            }
            .map { preferences ->
                preferences[keyPreserveOrientation] ?: false
            }
            .flowOn(dispatcher)
    }

    suspend fun putShareByDefault(value: Boolean): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyShareByDefault] = value
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun shouldShareByDefault(): Flow<Boolean> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
            }
            .map { preferences ->
                preferences[keyShareByDefault] ?: false
            }
            .flowOn(dispatcher)
    }

    suspend fun putDefaultDisplayNameSuffix(value: String): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyDefaultDisplayNameSuffix] = value
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun getDefaultDisplayNameSuffix(): Flow<String> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
                String.Empty
            }
            .map { preferences ->
                preferences[keyDefaultDisplayNameSuffix] ?: String.Empty
            }
            .flowOn(dispatcher)
    }

    fun getDefaultNightMode(): Flow<Int> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
                defaultNightModeValue
            }
            .map { preferences ->
                preferences[keyDefaultNightMode] ?: defaultNightModeValue
            }
            .flowOn(dispatcher)
    }

    suspend fun getDefaultNightModeName(): String {
        val defaultNightMode by context.defaultNightMode()
        val defaultNightModeCurrent = getDefaultNightMode().first()
        return defaultNightMode
            .entries
            .find { entry -> entry.value == defaultNightModeCurrent }
            ?.key
            ?: defaultNightMode.name()
    }

    suspend fun putDefaultNightMode(value: Int): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyDefaultNightMode] = value
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
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
