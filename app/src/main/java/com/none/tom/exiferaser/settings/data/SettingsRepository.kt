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
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.settings.defaultNightMode
import com.none.tom.exiferaser.settings.defaultNightModeValue
import com.none.tom.exiferaser.settings.name
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.contracts.ExperimentalContracts

@OptIn(ExperimentalContracts::class)
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

        const val KEY_RANDOMIZE_FILE_NAMES = "randomize_file_names"
        const val KEY_AUTO_DELETE = "auto_delete"
        const val KEY_LEGACY_IMAGE_SELECTION = "legacy_image_selection"
        const val KEY_SAVE_PATH_SELECTION_SKIP = "save_path_selection_skip"
    }

    private val keyRandomizeFileNames = booleanPreferencesKey(KEY_RANDOMIZE_FILE_NAMES)
    private val keyDefaultOpenPath = stringPreferencesKey(KEY_DEFAULT_OPEN_PATH)
    private val keyDefaultSavePath = stringPreferencesKey(KEY_DEFAULT_SAVE_PATH)
    private val keyAutoDelete = booleanPreferencesKey(KEY_AUTO_DELETE)
    private val keyPreserveOrientation = booleanPreferencesKey(KEY_PRESERVE_ORIENTATION)
    private val keyShareByDefault = booleanPreferencesKey(KEY_SHARE_BY_DEFAULT)
    private val keyDefaultDisplayNameSuffix = stringPreferencesKey(KEY_DEFAULT_DISPLAY_NAME_SUFFIX)
    private val keyLegacyImageSelection = booleanPreferencesKey(KEY_LEGACY_IMAGE_SELECTION)
    private val keySavePathSelectionSkip = booleanPreferencesKey(KEY_SAVE_PATH_SELECTION_SKIP)
    private val keyDefaultNightMode = intPreferencesKey(KEY_DEFAULT_NIGHT_MODE)

    suspend fun putDefaultPathOpen(
        defaultPathOpenNew: Uri,
        defaultPathOpenCurrent: Uri
    ): Boolean {
        if (defaultPathOpenNew.isNotEmpty()) {
            if (!takePersistablePermissions(context.contentResolver, defaultPathOpenNew)) {
                return false
            }
        }
        if (defaultPathOpenCurrent.isNotEmpty()) {
            releasePersistablePermissions(context.contentResolver, defaultPathOpenCurrent)
        }
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyDefaultOpenPath] = defaultPathOpenNew.toString()
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                if (defaultPathOpenNew.isNotEmpty()) {
                    releasePersistablePermissions(context.contentResolver, defaultPathOpenNew)
                }
                false
            }
        }
    }

    private fun getDefaultPathOpen(): Flow<Uri> {
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
        val defaultPathOpen = getDefaultPathOpen().firstOrNull()
        return runCatching {
            if (defaultPathOpen.isNotNullOrEmpty()) {
                DocumentFile.fromTreeUri(context, defaultPathOpen)?.name.orEmpty()
            } else {
                String.Empty
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            String.Empty
        }
    }

    suspend fun getPrivilegedDefaultPathOpenOrEmpty(): Uri {
        return withContext(dispatcher) {
            runCatching {
                val defaultPathOpen = getDefaultPathOpen().firstOrNull()
                if (defaultPathOpen == null || defaultPathOpen == Uri.EMPTY) {
                    return@runCatching Uri.EMPTY
                }
                val exists = DocumentFile.fromTreeUri(context, defaultPathOpen)?.exists() == true
                if (!exists) {
                    releasePersistablePermissions(
                        resolver = context.contentResolver,
                        uri = defaultPathOpen
                    )
                    return@runCatching Uri.EMPTY
                }
                if (hasPersistablePermissions(
                        resolver = context.contentResolver,
                        uri = defaultPathOpen
                    )
                ) {
                    defaultPathOpen
                } else {
                    Uri.EMPTY
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                Uri.EMPTY
            }
        }
    }

    suspend fun putDefaultPathSave(
        defaultPathSaveNew: Uri,
        defaultPathSaveCurrent: Uri
    ): Boolean {
        if (defaultPathSaveNew.isNotEmpty()) {
            val success = takePersistablePermissions(
                resolver = context.contentResolver,
                uri = defaultPathSaveNew,
                write = true
            )
            if (!success) {
                return false
            }
        }
        if (defaultPathSaveCurrent.isNotEmpty()) {
            releasePersistablePermissions(
                resolver = context.contentResolver,
                uri = defaultPathSaveCurrent,
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
                if (defaultPathSaveNew.isNotEmpty()) {
                    releasePersistablePermissions(
                        resolver = context.contentResolver,
                        uri = defaultPathSaveNew,
                        write = true
                    )
                }
                false
            }
        }
    }

    private fun getDefaultPathSave(): Flow<Uri> {
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
        val defaultPathSave = getDefaultPathSave().firstOrNull()
        return runCatching {
            if (defaultPathSave.isNotNullOrEmpty()) {
                DocumentFile.fromTreeUri(context, defaultPathSave)?.name.orEmpty()
            } else {
                String.Empty
            }
        }.getOrElse { exception ->
            Timber.e(exception)
            String.Empty
        }
    }

    fun shouldRandomizeFileNames(): Flow<Boolean> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
            }
            .map { preferences ->
                preferences[keyRandomizeFileNames] ?: false
            }
            .flowOn(dispatcher)
    }

    suspend fun putRandomizeFileNames(value: Boolean): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyRandomizeFileNames] = value
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun shouldSelectImagesLegacy(): Flow<Boolean> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
            }
            .map { preferences ->
                preferences[keyLegacyImageSelection] ?: false
            }
            .flowOn(dispatcher)
    }

    suspend fun putSelectImagesLegacy(value: Boolean): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyLegacyImageSelection] = value
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    suspend fun getPrivilegedDefaultPathSaveOrEmpty(): Uri {
        return withContext(dispatcher) {
            runCatching {
                val defaultPathSave = getDefaultPathSave().firstOrNull()
                if (defaultPathSave == null || defaultPathSave == Uri.EMPTY) {
                    return@runCatching Uri.EMPTY
                }
                val exists = DocumentFile.fromTreeUri(context, defaultPathSave)?.exists() == true
                if (!exists) {
                    releasePersistablePermissions(
                        resolver = context.contentResolver,
                        uri = defaultPathSave,
                        write = true
                    )
                    return@runCatching Uri.EMPTY
                }
                if (hasPersistablePermissions(
                        resolver = context.contentResolver,
                        uri = defaultPathSave,
                        write = true
                    )
                ) {
                    defaultPathSave
                } else {
                    Uri.EMPTY
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                Uri.EMPTY
            }
        }
    }

    suspend fun putAutoDelete(value: Boolean): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keyAutoDelete] = value
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun shouldAutoDelete(): Flow<Boolean> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
            }
            .map { preferences ->
                preferences[keyAutoDelete] ?: false
            }
            .flowOn(dispatcher)
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

    suspend fun putSavePathSelectionSkip(value: Boolean): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.edit { preferences ->
                    preferences[keySavePathSelectionSkip] = value
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    fun shouldSkipSavePathSelection(): Flow<Boolean> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
            }
            .map { preferences ->
                preferences[keySavePathSelectionSkip] ?: false
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
        val defaultNightModeCurrent = getDefaultNightMode().firstOrNull()
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

    private suspend fun hasPersistablePermissions(
        resolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false
    ): Boolean {
        return withContext(dispatcher) {
            runCatching {
                resolver.persistedUriPermissions.any { permission ->
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
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    @Suppress("SameParameterValue")
    private suspend fun takePersistablePermissions(
        resolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false
    ): Boolean {
        return withContext(dispatcher) {
            var flags = 0
            if (read) {
                flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            if (write) {
                flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
            runCatching {
                resolver.takePersistableUriPermission(uri, flags)
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    @Suppress("SameParameterValue")
    private suspend fun releasePersistablePermissions(
        resolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false
    ) {
        withContext(dispatcher) {
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
}
