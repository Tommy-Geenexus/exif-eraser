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
import com.none.tom.exiferaser.isNotEmpty
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
import kotlinx.coroutines.launch

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
        selection.isStarted = true
            .also {
                when (selection) {
                    is EmptySelection -> imageOrImagesModified()
                    is ImageSelection -> modifyImageSelection()
                    is ImagesSelection -> modifyImagesSelection()
                    is ImageDirectorySelection -> resolveImageDirectory()
                }
            }
    }

    private fun modifyImageSelection() {
        (selection as ImageSelection)
            .let { selection ->
                if (!selection.handled) {
                    job = viewModelScope.launch {
                        val preserveOrientation = sharedPrefsRepository.shouldPreserveImageOrientation()
                        imageRepository
                            .modifyImage(selection, preserveOrientation)
                            .let { result ->
                                imageModified.postValue(Event(result))
                                if (result.image != null) {
                                    selection.isSaving = !selection.isSaving
                                } else {
                                    imageOrImagesModified()
                                }
                            }
                    }
                } else if (!selection.isSaving) {
                    imageOrImagesModified()
                }
            }
    }

    private fun modifyImagesSelection() {
        (selection as ImagesSelection)
            .images
            .let { imageSelections ->
                imageSelections
                    .firstOrNull { image -> !image.handled }
                    ?.let { firstOrNextImage ->
                        job = viewModelScope.launch {
                            val preserveOrientation = sharedPrefsRepository.shouldPreserveImageOrientation()
                            imageRepository
                                .modifyImage(firstOrNextImage, preserveOrientation)
                                .let { result ->
                                    imageModified.postValue(Event(result))
                                    if (result.image != null) {
                                        firstOrNextImage.isSaving = !firstOrNextImage.isSaving
                                    } else {
                                        modifyImagesSelection()
                                    }
                                }
                        }
                    }
                    ?: imageSelections
                        .lastOrNull { image -> image.handled }
                        ?.let { lastImage ->
                            if (!lastImage.isSaving) {
                                imageOrImagesModified()
                            }
                        }
            }
    }

    private fun resolveImageDirectory() {
        job = viewModelScope.launch {
            imageRepository
                .resolveImageDirectory((selection as ImageDirectorySelection).treeUri)
                .let { result ->
                    selection = result
                    when (selection) {
                        is EmptySelection -> imageOrImagesModified()
                        is ImageSelection -> modifyImageSelection()
                        is ImagesSelection -> modifyImagesSelection()
                    }
                }
        }
    }

    private fun imageOrImagesModified() {
        selection.isFinished = true
            .also {
                imagesModified.postValue(Event((when (selection) {
                    is EmptySelection -> ImagesModifiedResult(0, 0)
                    is ImageSelection -> ImagesModifiedResult(if ((selection as ImageSelection).modified) 1 else 0, 1)
                    is ImagesSelection -> {
                        (selection as ImagesSelection)
                            .images
                            .let { images ->
                                images
                                    .filter { image -> image.modified }
                                    .count()
                                    .let { modified ->
                                        ImagesModifiedResult(modified, images.size)
                                    }
                            }
                    }
                    is ImageDirectorySelection -> ImagesModifiedResult(0, 0)
                })))
            }
    }

    fun saveImage(destination: Uri) {
        imageModified.value
            ?.peekContent()
            ?.image
            ?.let { saveImage ->
                job = viewModelScope.launch {
                    imageSaved.postValue(Event(imageRepository.saveImage(destination, saveImage)))
                    if (selection is ImageSelection) {
                        (selection as ImageSelection)
                            .let { selection ->
                                selection.run {
                                    isSaving = !isSaving
                                    uriModified = destination
                                    imageOrImagesModified()
                                }
                            }
                    } else if (selection is ImagesSelection) {
                        (selection as ImagesSelection)
                            .images
                            .lastOrNull { image -> image.handled }
                            ?.let { image ->
                                image.apply {
                                    isSaving = !isSaving
                                    uriModified = destination
                                }
                                modifyImagesSelection()
                            }
                    }
                }
            }
    }

    fun getImagesModifiedUris(): List<Uri> {
        return (selection as ImagesSelection)
            .images
            .filter { image -> image.uriModified.isNotEmpty() }
            .map { image -> image.uriModified }
    }

    fun getImageDisplayNameAndSuffix(displayName: String): String {
        return sharedPrefsRepository.getDefaultDisplayNameSuffix()
            .takeIf { suffix -> suffix.isNotEmpty() }
            ?.let { suffix ->
                displayName
                    .plus('_')
                    .plus(suffix)
            }
            ?: displayName
    }

    fun isFinishedAndModifiedImageOrImages(): Boolean {
        return when (selection) {
            is EmptySelection -> false
            is ImageSelection -> {
                (selection as ImageSelection)
                    .let { image -> image.handled && image.uriModified.isNotEmpty() }
            }
            is ImagesSelection -> {
                (selection as ImagesSelection)
                    .images
                    .let { images ->
                        images.none { image -> !image.handled } &&
                                images.any { image -> image.uriModified.isNotEmpty() }
                    }
            }
            is ImageDirectorySelection -> false
        }
    }

    override fun onCleared() {
        super.onCleared()
        job?.cancel()
    }
}
