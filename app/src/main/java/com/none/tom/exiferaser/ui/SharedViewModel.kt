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

package com.none.tom.exiferaser.ui

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.none.tom.exiferaser.data.ImageRepository
import com.none.tom.exiferaser.data.SharedPrefsDelegate
import com.none.tom.exiferaser.data.SharedPrefsRepository
import com.none.tom.exiferaser.launchIfNotActive
import com.none.tom.exiferaser.reactive.Event
import com.none.tom.exiferaser.reactive.images.EmptySelection
import com.none.tom.exiferaser.reactive.images.ImageDirectorySelection
import com.none.tom.exiferaser.reactive.images.ImageModifyResult
import com.none.tom.exiferaser.reactive.images.ImageSaveResult
import com.none.tom.exiferaser.reactive.images.ImageSelection
import com.none.tom.exiferaser.reactive.images.ImagesModifiedResult
import com.none.tom.exiferaser.reactive.images.ImagesSelection
import com.none.tom.exiferaser.reactive.images.Selection
import kotlinx.coroutines.Job

class SharedViewModel(
    private val imageRepository: ImageRepository,
    private val sharedPrefsRepository: SharedPrefsRepository
) : ViewModel(),
    SharedPrefsDelegate by sharedPrefsRepository {

    val imageModified: MutableLiveData<Event<ImageModifyResult>> = MutableLiveData()
    val imagesModified: MutableLiveData<Event<ImagesModifiedResult>> = MutableLiveData()
    val imageSaved: MutableLiveData<Event<ImageSaveResult>> = MutableLiveData()

    private var job: Job? = null

    lateinit var selection: Selection

    fun modifyImageOrImagesSelectionOrResolveImageDirectory() {
        when (selection) {
            is EmptySelection -> imagesModified.postValue(Event(ImagesModifiedResult(0, 0)))
            is ImageSelection -> modifyImageSelection()
            is ImagesSelection -> modifyImagesSelection()
            is ImageDirectorySelection -> resolveImageDirectory()
        }
    }

    private fun modifyImageSelection(force: Boolean = false) {
        if (selection is ImageSelection) {
            (selection as ImageSelection)
                .let { selection ->
                    if (!selection.handled) {
                        job = viewModelScope.launchIfNotActive(job, force = force) {
                            val preserveOrientation = sharedPrefsRepository.shouldPreserveImageOrientation()
                            imageRepository
                                .modifyImage(selection, preserveOrientation)
                                .let { result ->
                                    imageModified.postValue(Event(result))
                                    if (result.image == null) {
                                        imagesModified.postValue(Event(ImagesModifiedResult(0, 1)))
                                    }
                                }
                        }
                    } else {
                        val modified = if (selection.modified) 1 else 0
                        imagesModified.postValue(Event(ImagesModifiedResult(modified, 1)))
                    }
                }
        }
    }

    private fun modifyImagesSelection(force: Boolean = false) {
        if (selection is ImagesSelection) {
            (selection as ImagesSelection)
                .images
                .let { imageSelections ->
                    imageSelections
                        .firstOrNull { image -> !image.handled }
                        .let { firstOrNextImage ->
                            if (firstOrNextImage != null) {
                                job = viewModelScope.launchIfNotActive(job, force = force) {
                                    val preserveOrientation =
                                        sharedPrefsRepository.shouldPreserveImageOrientation()
                                    imageRepository
                                        .modifyImage(firstOrNextImage, preserveOrientation)
                                        .let { result ->
                                            imageModified.postValue(Event(result))
                                            if (result.image == null) {
                                                modifyImagesSelection(true)
                                            }
                                        }
                                }
                            } else {
                                val modified = imageSelections.filter { image -> image.modified }.count()
                                imagesModified.postValue(Event(ImagesModifiedResult(modified, imageSelections.size)))
                            }
                        }
                }
        }
    }

    private fun resolveImageDirectory() {
        if (selection is ImageDirectorySelection) {
            job = viewModelScope.launchIfNotActive(job) {
                imageRepository
                    .resolveImageDirectory((selection as ImageDirectorySelection).treeUri)
                    .let { result ->
                        selection = result
                        when (selection) {
                            is EmptySelection -> imagesModified.postValue(Event(ImagesModifiedResult(0, 0)))
                            is ImageSelection -> modifyImageSelection(true)
                            is ImagesSelection -> modifyImagesSelection(true)
                        }
                    }
            }
        }
    }

    fun saveImage(destination: Uri) {
        imageModified.value
            ?.peekContent()
            ?.image
            ?.let { saveImage ->
                job = viewModelScope.launchIfNotActive(job) {
                    imageSaved.postValue(Event(imageRepository.saveImage(destination, saveImage)))
                    if (selection is ImageSelection) {
                        (selection as ImageSelection)
                            .let { selection ->
                                selection.uriModified = destination
                                val modified = if (selection.modified) 1 else 0
                                imagesModified.postValue(Event(ImagesModifiedResult(modified, 1)))
                            }
                    } else if (selection is ImagesSelection) {
                        (selection as ImagesSelection)
                            .images
                            .lastOrNull { image -> image.handled }
                            ?.let { image ->
                                image.uriModified = destination
                                modifyImagesSelection(true)
                            }
                    }
                }
            }
    }

    fun getImageDisplayNameAndSuffix(displayName: String): String {
        return sharedPrefsRepository.getDefaultDisplayNameSuffix()
            .takeIf { suffix -> suffix.isNotEmpty() }
            ?.let { suffix -> displayName.plus('_'.plus(suffix)) }
            ?: displayName
    }

    fun isFinishedAndModifiedImageOrImages(): Boolean {
        return when (selection) {
            is ImageSelection -> {
                (selection as ImageSelection).let { selection ->
                    selection.handled && selection.modified
                }
            }
            is ImagesSelection -> {
                (selection as ImagesSelection)
                    .images
                    .let { selection ->
                        selection.none { image -> !image.handled } && selection.any { image -> image.modified }
                    }
            }
            is ImageDirectorySelection -> false
            is EmptySelection -> false
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}
