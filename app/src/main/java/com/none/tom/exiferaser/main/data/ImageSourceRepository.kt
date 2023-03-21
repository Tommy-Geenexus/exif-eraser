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

import androidx.datastore.core.DataStore
import com.none.tom.exiferaser.CameraProto
import com.none.tom.exiferaser.ImageDirectoryProto
import com.none.tom.exiferaser.ImageFileProto
import com.none.tom.exiferaser.ImageFilesProto
import com.none.tom.exiferaser.ImageSourcesProto
import com.none.tom.exiferaser.di.DispatcherIo
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
class ImageSourceRepository @Inject constructor(
    private val dataStore: DataStore<ImageSourcesProto>,
    @DispatcherIo private val dispatcher: CoroutineDispatcher
) {

    private companion object {
        const val INDEX_DEFAULT_IMAGE_FILE = 0
        const val INDEX_DEFAULT_IMAGE_FILES = 1
        const val INDEX_DEFAULT_IMAGE_DIRECTORY = 2
        const val INDEX_DEFAULT_CAMERA = 3
    }

    suspend fun getImageSources(): Flow<MutableList<AnyMessage>> {
        return dataStore
            .data
            .catch { exception ->
                Timber.e(exception)
                emit(
                    ImageSourcesProto(
                        image_file_proto = ImageFileProto(INDEX_DEFAULT_IMAGE_FILE),
                        image_files_proto = ImageFilesProto(INDEX_DEFAULT_IMAGE_FILES),
                        image_directory_proto =
                        ImageDirectoryProto(INDEX_DEFAULT_IMAGE_DIRECTORY),
                        camera_proto = CameraProto(INDEX_DEFAULT_CAMERA)
                    )
                )
            }
            .map { proto ->
                val imageFileProto =
                    proto.image_file_proto ?: ImageFileProto(INDEX_DEFAULT_IMAGE_FILE)
                val imageFilesProto =
                    proto.image_files_proto ?: ImageFilesProto(INDEX_DEFAULT_IMAGE_FILES)
                val imageDirectoryProto =
                    proto.image_directory_proto
                        ?: ImageDirectoryProto(INDEX_DEFAULT_IMAGE_DIRECTORY)
                val cameraProto = proto.camera_proto ?: CameraProto(INDEX_DEFAULT_CAMERA)
                val packedImageSources = mutableListOf(
                    AnyMessage.pack(imageFileProto),
                    AnyMessage.pack(imageFilesProto),
                    AnyMessage.pack(imageDirectoryProto),
                    AnyMessage.pack(cameraProto)
                )
                Array(
                    size = packedImageSources.size,
                    init = { index -> packedImageSources[index] }
                )
                    .apply {
                        set(
                            index = imageFileProto.index,
                            value = packedImageSources[INDEX_DEFAULT_IMAGE_FILE]
                        )
                        set(
                            index = imageFilesProto.index,
                            value = packedImageSources[INDEX_DEFAULT_IMAGE_FILES]
                        )
                        set(
                            index = imageDirectoryProto.index,
                            value = packedImageSources[INDEX_DEFAULT_IMAGE_DIRECTORY]
                        )
                        set(
                            index = cameraProto.index,
                            value = packedImageSources[INDEX_DEFAULT_CAMERA]
                        )
                    }
                    .toMutableList()
            }
            .flowOn(dispatcher)
    }

    suspend fun putImageSources(imageSources: List<AnyMessage>): Boolean {
        return withContext(dispatcher) {
            runCatching {
                dataStore.updateData { proto ->
                    var imageFileProto: ImageFileProto? = null
                    var imageFilesProto: ImageFilesProto? = null
                    var imageDirectoryProto: ImageDirectoryProto? = null
                    var cameraProto: CameraProto? = null
                    imageSources.forEachIndexed { newIndex: Int, imageSource: AnyMessage ->
                        when (imageSource.typeUrl) {
                            ImageFileProto.ADAPTER.typeUrl -> {
                                imageFileProto = ImageFileProto(index = newIndex)
                            }
                            ImageFilesProto.ADAPTER.typeUrl -> {
                                imageFilesProto = ImageFilesProto(index = newIndex)
                            }
                            ImageDirectoryProto.ADAPTER.typeUrl -> {
                                imageDirectoryProto = ImageDirectoryProto(index = newIndex)
                            }
                            else -> cameraProto = CameraProto(index = newIndex)
                        }
                    }
                    proto.copy(
                        image_file_proto = imageFileProto,
                        image_files_proto = imageFilesProto,
                        image_directory_proto = imageDirectoryProto,
                        camera_proto = cameraProto
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
