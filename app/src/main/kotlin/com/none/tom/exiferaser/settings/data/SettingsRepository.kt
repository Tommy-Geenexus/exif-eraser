/*
 * Copyright (c) 2018-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import com.none.tom.exiferaser.core.di.DispatcherIo
import com.none.tom.exiferaser.core.extension.isNotEmpty
import com.none.tom.exiferaser.core.extension.isNotNullOrEmpty
import com.none.tom.exiferaser.core.extension.suspendRunCatching
import com.none.tom.exiferaser.settings.ui.defaultNightMode
import com.none.tom.exiferaser.settings.ui.defaultNightModeDisplayValue
import com.none.tom.exiferaser.settings.ui.defaultNightModes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher
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

    suspend fun putDefaultOpenPath(
        newDefaultOpenPath: Uri,
        currentDefaultOpenPath: Uri
    ): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                if (newDefaultOpenPath.isNotEmpty()) {
                    if (takePersistablePermissions(
                            contentResolver = context.contentResolver,
                            uri = newDefaultOpenPath
                        ).isFailure
                    ) {
                        error("takePersistablePermissions() failed")
                    }
                }
                if (currentDefaultOpenPath.isNotEmpty()) {
                    releasePersistablePermissions(context.contentResolver, currentDefaultOpenPath)
                }
                dataStore.edit { preferences ->
                    preferences[keyDefaultOpenPath] = newDefaultOpenPath.toString()
                }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                if (newDefaultOpenPath.isNotEmpty()) {
                    releasePersistablePermissions(context.contentResolver, newDefaultOpenPath)
                }
                Result.failure(exception)
            }
        }
    }

    private suspend fun getDefaultOpenPath(): Uri {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keyDefaultOpenPath]?.toUri() }
            .firstOrNull()
            ?: Uri.EMPTY
    }

    suspend fun getDefaultOpenPathName(): String {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val defaultOpenPath = getDefaultOpenPath()
                if (!defaultOpenPath.isNotNullOrEmpty()) {
                    error("defaultOpenPath is null or empty")
                }
                DocumentFile.fromTreeUri(context, defaultOpenPath)?.name.orEmpty()
            }.getOrElse { exception ->
                Timber.e(exception)
                ""
            }
        }
    }

    suspend fun getPrivilegedDefaultOpenPath(): Result<Uri> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val defaultOpenPath = getDefaultOpenPath()
                if (!defaultOpenPath.isNotNullOrEmpty()) {
                    error("defaultOpenPath is null or empty")
                }
                val exists = DocumentFile.fromTreeUri(context, defaultOpenPath)?.exists() == true
                if (!exists) {
                    releasePersistablePermissions(context.contentResolver, defaultOpenPath)
                    error("defaultOpenPath does not exist anymore")
                }
                if (!hasPersistablePermissions(context.contentResolver, defaultOpenPath)) {
                    error("defaultOpenPath is missing persistable permissions")
                }
                Result.success(defaultOpenPath)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun putDefaultSavePath(
        newDefaultSavePath: Uri,
        currentDefaultSavePath: Uri
    ): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                if (newDefaultSavePath.isNotEmpty()) {
                    val result = takePersistablePermissions(
                        contentResolver = context.contentResolver,
                        uri = newDefaultSavePath,
                        write = true
                    )
                    if (result.isFailure) {
                        error("takePersistablePermissions() failed")
                    }
                }
                if (currentDefaultSavePath.isNotEmpty()) {
                    releasePersistablePermissions(
                        contentResolver = context.contentResolver,
                        uri = currentDefaultSavePath,
                        write = true
                    )
                }
                dataStore.edit { preferences ->
                    preferences[keyDefaultSavePath] = newDefaultSavePath.toString()
                }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                if (newDefaultSavePath.isNotEmpty()) {
                    releasePersistablePermissions(
                        contentResolver = context.contentResolver,
                        uri = newDefaultSavePath,
                        write = true
                    )
                }
                Result.failure(exception)
            }
        }
    }

    private suspend fun getDefaultSavePath(): Uri {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keyDefaultSavePath]?.toUri() }
            .firstOrNull()
            ?: Uri.EMPTY
    }

    suspend fun getDefaultSavePathName(): String {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val defaultSavePath = getDefaultSavePath()
                if (!defaultSavePath.isNotNullOrEmpty()) {
                    error("defaultSavePath is null or empty")
                }
                DocumentFile.fromTreeUri(context, defaultSavePath)?.name.orEmpty()
            }.getOrElse { exception ->
                Timber.e(exception)
                ""
            }
        }
    }

    suspend fun isRandomizeFileNamesEnabled(): Boolean {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keyRandomizeFileNames] }
            .firstOrNull()
            ?: false
    }

    suspend fun putRandomizeFileNames(value: Boolean): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.edit { preferences -> preferences[keyRandomizeFileNames] = value }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun isLegacyImageSelectionEnabled(): Boolean {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keyLegacyImageSelection] }
            .firstOrNull()
            ?: false
    }

    suspend fun putSelectImagesLegacy(value: Boolean): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.edit { preferences -> preferences[keyLegacyImageSelection] = value }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun getPrivilegedDefaultSavePath(): Result<Uri> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val defaultSavePath = getDefaultSavePath()
                if (!defaultSavePath.isNotNullOrEmpty()) {
                    error("defaultSavePath is null or empty")
                }
                val exists = DocumentFile.fromTreeUri(context, defaultSavePath)?.exists() == true
                if (!exists) {
                    releasePersistablePermissions(
                        contentResolver = context.contentResolver,
                        uri = defaultSavePath,
                        write = true
                    )
                    error("defaultSavePath does not exist anymore")
                }
                if (!hasPersistablePermissions(
                        resolver = context.contentResolver,
                        uri = defaultSavePath,
                        write = true
                    )
                ) {
                    error("defaultSavePath is missing persistable permissions")
                }
                Result.success(defaultSavePath)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun putAutoDelete(value: Boolean): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.edit { preferences -> preferences[keyAutoDelete] = value }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun isAutoDeleteEnabled(): Boolean {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keyAutoDelete] }
            .firstOrNull()
            ?: false
    }

    suspend fun putPreserveOrientation(value: Boolean): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.edit { preferences -> preferences[keyPreserveOrientation] = value }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun isPreserveOrientationEnabled(): Boolean {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keyPreserveOrientation] }
            .firstOrNull()
            ?: false
    }

    suspend fun putShareByDefault(value: Boolean): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.edit { preferences -> preferences[keyShareByDefault] = value }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun isShareByDefaultEnabled(): Boolean {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keyShareByDefault] }
            .firstOrNull()
            ?: false
    }

    suspend fun putDefaultDisplayNameSuffix(value: String): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.edit { preferences -> preferences[keyDefaultDisplayNameSuffix] = value }
                Result.success((Unit))
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun getDefaultDisplayNameSuffix(): String {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keyDefaultDisplayNameSuffix] }
            .firstOrNull()
            .orEmpty()
    }

    suspend fun putSavePathSelectionSkip(value: Boolean): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.edit { preferences -> preferences[keySavePathSelectionSkip] = value }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun isSkipSavePathSelectionEnabled(): Boolean {
        return dataStore
            .data
            .catch { exception -> Timber.e(exception) }
            .map { preferences -> preferences[keySavePathSelectionSkip] }
            .firstOrNull()
            ?: false
    }

    suspend fun getDefaultNightMode(): Int {
        return dataStore
            .data
            .map { preferences -> preferences[keyDefaultNightMode] }
            .firstOrNull()
            ?: defaultNightMode
    }

    suspend fun getDefaultNightModeName(): String {
        return withContext(dispatcherIo) {
            context
                .defaultNightModes()
                .getOrDefault(getDefaultNightMode(), context.defaultNightModeDisplayValue())
        }
    }

    suspend fun putDefaultNightMode(value: Int): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.edit { preferences -> preferences[keyDefaultNightMode] = value }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    private suspend fun hasPersistablePermissions(
        resolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false
    ): Boolean {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
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

    private suspend fun takePersistablePermissions(
        contentResolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false
    ): Result<Unit> {
        return withContext(dispatcherIo) {
            var flags = 0
            if (read) {
                flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            if (write) {
                flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
            coroutineContext.suspendRunCatching {
                contentResolver.takePersistableUriPermission(uri, flags)
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    private suspend fun releasePersistablePermissions(
        contentResolver: ContentResolver,
        uri: Uri,
        read: Boolean = true,
        write: Boolean = false
    ): Result<Unit> {
        return withContext(dispatcherIo) {
            var flags = 0
            if (read) {
                flags = flags or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            if (write) {
                flags = flags or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            }
            coroutineContext.suspendRunCatching {
                contentResolver.releasePersistableUriPermission(uri, flags)
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }
}
