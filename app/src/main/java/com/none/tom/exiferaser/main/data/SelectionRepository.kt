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

package com.none.tom.exiferaser.main.data

import android.net.Uri
import androidx.annotation.IntRange
import androidx.datastore.core.DataStore
import com.none.tom.exiferaser.SelectionProto
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.di.DispatcherIo
import com.none.tom.exiferaser.isNotEmpty
import com.none.tom.exiferaser.isNullOrEmpty
import com.none.tom.exiferaser.suspendRunCatching
import com.squareup.wire.AnyMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SelectionRepository @Inject constructor(
    private val dataStore: DataStore<SelectionProto>,
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher
) {

    suspend fun getSelection(@IntRange(from = 0) dropFirstN: Int): Flow<AnyMessage?> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
                emit(SelectionProto())
            }
            .map { proto ->
                when {
                    proto.user_image_selection_proto != null -> {
                        if (dropFirstN == 0) {
                            AnyMessage.pack(proto.user_image_selection_proto)
                        } else {
                            null
                        }
                    }
                    proto.user_images_selection_proto != null -> {
                        val selection = proto.user_images_selection_proto.user_images_selection
                        when {
                            dropFirstN == 0 -> {
                                AnyMessage.pack(proto.user_images_selection_proto)
                            }
                            dropFirstN < selection.size -> {
                                AnyMessage.pack(
                                    UserImagesSelectionProto(
                                        user_images_selection = selection.drop(dropFirstN)
                                    )
                                )
                            }
                            else -> {
                                null
                            }
                        }
                    }
                    else -> {
                        null
                    }
                }
            }
            .flowOn(dispatcherIo)
    }

    suspend fun putSelection(
        uri: Uri?,
        fromCamera: Boolean = false
    ): Boolean {
        if (uri.isNullOrEmpty()) {
            return false
        }
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.updateData { proto ->
                    proto.copy(
                        user_image_selection_proto = UserImageSelectionProto(
                            image_path = uri.toString(),
                            from_camera = fromCamera
                        ),
                        user_images_selection_proto = null
                    )
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    suspend fun putSelection(
        uris: List<Uri>? = null,
        urisFromIntent: Array<Uri>? = null
    ): Boolean {
        val selection = uris ?: urisFromIntent?.toList()
        if (selection.isNullOrEmpty()) {
            return false
        }
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.updateData { proto ->
                    proto.copy(
                        user_image_selection_proto = null,
                        user_images_selection_proto = UserImagesSelectionProto(
                            user_images_selection = selection
                                .filter { uri -> uri.isNotEmpty() }
                                .map { uri ->
                                    UserImageSelectionProto(image_path = uri.toString())
                                }
                        )
                    )
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }

    suspend fun putSelection(message: AnyMessage?): Boolean {
        if (message == null) {
            return false
        }
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.updateData { proto ->
                    var imageProto: UserImageSelectionProto? = null
                    var imagesProto: UserImagesSelectionProto? = null
                    if (message.typeUrl == UserImageSelectionProto.ADAPTER.typeUrl) {
                        imageProto = message.unpackOrNull(UserImageSelectionProto.ADAPTER)
                    }
                    if (message.typeUrl == UserImagesSelectionProto.ADAPTER.typeUrl) {
                        imagesProto = message.unpackOrNull(UserImagesSelectionProto.ADAPTER)
                    }
                    proto.copy(
                        user_image_selection_proto = imageProto,
                        user_images_selection_proto = imagesProto
                    )
                }
                true
            }.getOrElse { exception ->
                Timber.e(exception)
                false
            }
        }
    }
}
