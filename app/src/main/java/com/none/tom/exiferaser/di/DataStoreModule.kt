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

package com.none.tom.exiferaser.di

import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.preferencesDataStore
import com.none.tom.exiferaser.main.data.ImageSourcesSerializer
import com.none.tom.exiferaser.main.data.SelectionSerializer
import com.none.tom.exiferaser.settings.data.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    fun provideDataStoreSelection(
        @ApplicationContext context: Context
    ) = context.imageSourcesDataStore

    @Provides
    fun provideDataStoreImageSources(
        @ApplicationContext context: Context
    ) = context.selectionDataStore

    @Provides
    fun provideDataStoreSettings(
        @ApplicationContext context: Context
    ) = context.settingsDataStore
}

private val Context.imageSourcesDataStore by dataStore(
    fileName = "image_sources.pb",
    serializer = ImageSourcesSerializer()
)

private val Context.selectionDataStore by dataStore(
    fileName = "selection.pb",
    serializer = SelectionSerializer()
)

private val Context.settingsDataStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = context.packageName + "_preferences",
                keysToMigrate = setOf(
                    SettingsRepository.KEY_DEFAULT_OPEN_PATH,
                    SettingsRepository.KEY_DEFAULT_SAVE_PATH,
                    SettingsRepository.KEY_PRESERVE_ORIENTATION,
                    SettingsRepository.KEY_SHARE_BY_DEFAULT,
                    SettingsRepository.KEY_DEFAULT_DISPLAY_NAME_SUFFIX
                )
            )
        )
    }
)
