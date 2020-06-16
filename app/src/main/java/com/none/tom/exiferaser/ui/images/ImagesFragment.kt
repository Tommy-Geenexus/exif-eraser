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

package com.none.tom.exiferaser.ui.images

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.Observer
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentImagesBinding
import com.none.tom.exiferaser.reactive.images.ImageSelection
import com.none.tom.exiferaser.reactive.images.ImagesSelection
import com.none.tom.exiferaser.showShortSnackbar
import com.none.tom.exiferaser.ui.BaseFragment

class ImagesFragment : BaseFragment<FragmentImagesBinding>(R.layout.fragment_images) {

    private val shareImage = registerForActivityResult(ShareImage()) {}
    private val shareImages = registerForActivityResult(ShareImages()) {}
    private val createDocument = registerForActivityResult(CreateDocument()) { uri -> viewModel.saveImage(uri) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.imageModified.observe(viewLifecycleOwner, Observer { event ->
            event.getContentOrNull()?.let { result ->
                if (result.image != null) {
                    val displayName = viewModel.getImageDisplayNameAndSuffix(result.fileName)
                    createDocument.launch(displayName to viewModel.getDefaultSavePath())
                }
            }
        })
        viewModel.imageSaved.observe(viewLifecycleOwner, Observer { event ->
            event.getContentOrNull()?.let { result ->
                if (!result.success) {
                    showShortSnackbar(R.string.image_save_failed)
                }
            }
        })
        viewModel.imagesModified.observe(viewLifecycleOwner, Observer { event ->
            event.getContentOrNull()?.let { result ->
                getBindingSafe().apply {
                    spinner.visibility = View.GONE
                    modified.text = resources.getQuantityString(
                        R.plurals.images_modified,
                        result.imagesModified,
                        result.imagesModified,
                        result.imagesTotal
                    )
                    val numberOfSkippedImages = result.imagesTotal - result.imagesModified
                    skipped.text = resources.getQuantityString(
                        R.plurals.images_skipped,
                        numberOfSkippedImages,
                        numberOfSkippedImages,
                        result.imagesTotal
                    )
                    done.visibility = View.VISIBLE
                }
                requireActivity().invalidateOptionsMenu()
                if (viewModel.shouldShareImagesByDefault() &&
                    viewModel.isFinishedAndModifiedImageOrImages() &&
                    savedInstanceState == null
                ) {
                    shareImageOrImages()
                }
            }
        })
        viewModel.modifyImageOrImagesSelectionOrResolveImageDirectory()
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_images, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_share).isVisible = viewModel.isFinishedAndModifiedImageOrImages()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_share) {
            shareImageOrImages()
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    override fun bindLayout(view: View): FragmentImagesBinding = FragmentImagesBinding.bind(view)

    private fun shareImageOrImages(): Boolean {
        return when (val selection = viewModel.selection) {
            is ImageSelection -> {
                shareImage.launch(selection.uriModified)
                true
            }
            is ImagesSelection -> {
                shareImages.launch(selection.images.map { image -> image.uriModified })
                true
            }
            else -> false
        }
    }
}
