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

package com.none.tom.exiferaser.main.data

import androidx.datastore.core.DataStore
import com.none.tom.exiferaser.CameraProto
import com.none.tom.exiferaser.ImageDirectoryProto
import com.none.tom.exiferaser.ImageFileProto
import com.none.tom.exiferaser.ImageFilesProto
import com.none.tom.exiferaser.ImageSourcesProto
import com.none.tom.exiferaser.core.di.DispatcherIo
import com.none.tom.exiferaser.core.extension.suspendRunCatching
import com.squareup.wire.AnyMessage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class ImageSourcesRepository @Inject constructor(
    private val dataStore: DataStore<ImageSourcesProto>,
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher
) {

    private companion object {
        const val INDEX_DEFAULT_IMAGE_FILE = 0
        const val INDEX_DEFAULT_IMAGE_FILES = 1
        const val INDEX_DEFAULT_IMAGE_DIRECTORY = 2
        const val INDEX_DEFAULT_CAMERA = 3
        const val IMAGE_SOURCES_COUNT = 4
    }

    suspend fun getImageSources(): List<AnyMessage> {
        return dataStore
            .data
            .map { proto ->
                val imageFileProto = proto.image_file_proto
                    ?: ImageFileProto(index = INDEX_DEFAULT_IMAGE_FILE)
                val imageFilesProto = proto.image_files_proto
                    ?: ImageFilesProto(index = INDEX_DEFAULT_IMAGE_FILES)
                val imageDirectoryProto = proto.image_directory_proto
                    ?: ImageDirectoryProto(index = INDEX_DEFAULT_IMAGE_DIRECTORY)
                val cameraProto = proto.camera_proto
                    ?: CameraProto(index = INDEX_DEFAULT_CAMERA)
                Array(
                    size = IMAGE_SOURCES_COUNT,
                    init = { index ->
                        when (index) {
                            imageFileProto.index -> AnyMessage.pack(imageFileProto)
                            imageFilesProto.index -> AnyMessage.pack(imageFilesProto)
                            imageDirectoryProto.index -> AnyMessage.pack(imageDirectoryProto)
                            else -> AnyMessage.pack(cameraProto)
                        }
                    }
                ).toList()
            }
            .catch { exception -> Timber.e(exception) }
            .flowOn(dispatcherIo)
            .firstOrNull()
            ?: coroutineContext.suspendRunCatching {
                listOf(
                    AnyMessage.pack(ImageFileProto(index = INDEX_DEFAULT_IMAGE_FILE)),
                    AnyMessage.pack(ImageFilesProto(index = INDEX_DEFAULT_IMAGE_FILES)),
                    AnyMessage.pack(ImageDirectoryProto(index = INDEX_DEFAULT_IMAGE_DIRECTORY)),
                    AnyMessage.pack(CameraProto(index = INDEX_DEFAULT_CAMERA))
                )
            }.getOrElse { exception ->
                Timber.e(exception)
                emptyList()
            }
    }

    suspend fun putImageSources(imageSources: List<AnyMessage>): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                dataStore.updateData { proto ->
                    var imageFileProto: ImageFileProto? = null
                    var imageFilesProto: ImageFilesProto? = null
                    var imageDirectoryProto: ImageDirectoryProto? = null
                    var cameraProto: CameraProto? = null
                    imageSources.forEachIndexed { index: Int, imageSource: AnyMessage ->
                        when (imageSource.typeUrl) {
                            ImageFileProto.ADAPTER.typeUrl -> {
                                imageFileProto = ImageFileProto(index = index)
                            }
                            ImageFilesProto.ADAPTER.typeUrl -> {
                                imageFilesProto = ImageFilesProto(index = index)
                            }
                            ImageDirectoryProto.ADAPTER.typeUrl -> {
                                imageDirectoryProto = ImageDirectoryProto(index = index)
                            }
                            else -> {
                                cameraProto = CameraProto(index = index)
                            }
                        }
                    }
                    proto.copy(
                        image_file_proto = imageFileProto,
                        image_files_proto = imageFilesProto,
                        image_directory_proto = imageDirectoryProto,
                        camera_proto = cameraProto
                    )
                }
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }
}
