/*
 * Copyright (c) 2018-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.main.data

import android.net.Uri
import androidx.datastore.core.DataStore
import com.none.tom.exiferaser.SelectionProto
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.core.di.DispatcherDefault
import com.none.tom.exiferaser.core.di.DispatcherIo
import com.none.tom.exiferaser.core.extension.isNotEmpty
import com.none.tom.exiferaser.core.extension.isNotNullOrEmpty
import com.none.tom.exiferaser.core.extension.suspendRunCatching
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SelectionRepository @Inject constructor(
    private val dataStore: DataStore<SelectionProto>,
    @DispatcherDefault private val dispatcherDefault: CoroutineDispatcher,
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher
) {

    suspend fun getSelection(fromIndex: Int): List<UserImageSelectionProto> = dataStore
        .data
        .catch { exception ->
            Timber.e(exception)
            emit(SelectionProto())
        }
        .map { selection ->
            mutableListOf<UserImageSelectionProto>().apply {
                selection
                    .user_image_selection_proto
                    ?.let { proto ->
                        if (fromIndex == 0) {
                            add(proto)
                        }
                    }
                selection
                    .user_images_selection_proto
                    ?.user_images_selection
                    ?.drop(fromIndex)
                    ?.let { protos -> addAll(protos) }
            }
        }
        .flowOn(dispatcherDefault)
        .firstOrNull()
        ?: emptyList()

    suspend fun putSelection(uri: Uri?, isFromCamera: Boolean = false): Result<Unit> =
        withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                check(uri.isNotNullOrEmpty()) {
                    "Empty selection"
                }
                dataStore.updateData { proto ->
                    proto.copy(
                        user_image_selection_proto = UserImageSelectionProto(
                            image_path = uri.toString(),
                            from_camera = isFromCamera
                        ),
                        user_images_selection_proto = null
                    )
                }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }

    suspend fun putSelection(uris: List<Uri>): Result<Unit> = withContext(dispatcherIo) {
        coroutineContext.suspendRunCatching {
            val urisNonEmpty = uris.filter { uri -> uri.isNotEmpty() }
            check(urisNonEmpty.isNotEmpty()) {
                "Empty selection"
            }
            dataStore.updateData { proto ->
                proto.copy(
                    user_image_selection_proto = null,
                    user_images_selection_proto = UserImagesSelectionProto(
                        user_images_selection = urisNonEmpty.map { uri ->
                            UserImageSelectionProto(image_path = uri.toString())
                        }
                    )
                )
            }
            Result.success(Unit)
        }.getOrElse { exception ->
            Timber.e(exception)
            Result.failure(exception)
        }
    }
}
