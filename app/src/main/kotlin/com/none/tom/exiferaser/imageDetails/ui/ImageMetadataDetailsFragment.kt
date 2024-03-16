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

package com.none.tom.exiferaser.imageDetails.ui

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.databinding.FragmentImageMetadataDetailsBinding
import com.none.tom.exiferaser.imageDetails.business.ImageMetadataDetailsState
import com.none.tom.exiferaser.imageDetails.business.ImageMetadataDetailsViewModel
import com.none.tom.exiferaser.imageProcessing.data.ImageMetadataSnapshot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImageMetadataDetailsFragment : DialogFragment() {

    companion object {

        const val TAG = "DetailsFragment"

        const val KEY_DISPLAY_NAME = TOP_LEVEL_PACKAGE_NAME + "DISPLAY_NAME"
        const val KEY_EXTENSION = TOP_LEVEL_PACKAGE_NAME + "EXTENSION"
        const val KEY_MIME_TYPE = TOP_LEVEL_PACKAGE_NAME + "MIME_TYPE"
        const val KEY_IMAGE_METADATA_SNAPSHOT = TOP_LEVEL_PACKAGE_NAME + "IMAGE_META_SNAPSHOT"

        fun newInstance(
            displayName: String,
            extension: String,
            mimeType: String,
            imageMetadataSnapshot: ImageMetadataSnapshot
        ) = ImageMetadataDetailsFragment().apply {
            arguments = bundleOf(
                KEY_DISPLAY_NAME to displayName,
                KEY_EXTENSION to extension,
                KEY_MIME_TYPE to mimeType,
                KEY_IMAGE_METADATA_SNAPSHOT to imageMetadataSnapshot
            )
        }
    }

    private val viewModelImageModified: ImageMetadataDetailsViewModel by viewModels()
    private var _binding: FragmentImageMetadataDetailsBinding? = null
    private val binding: FragmentImageMetadataDetailsBinding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentImageMetadataDetailsBinding.inflate(layoutInflater)
        val displayName = arguments?.getString(KEY_DISPLAY_NAME).orEmpty()
        if (displayName.isEmpty()) {
            binding.title.isVisible = false
        } else {
            binding.title.text = displayName
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModelImageModified.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        return MaterialAlertDialogBuilder(requireActivity())
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .apply {
                setOnShowListener {
                    arguments?.let { args ->
                        viewModelImageModified.handleImageDetails(
                            extension = args.getString(KEY_EXTENSION).orEmpty(),
                            mimeType = args.getString(KEY_MIME_TYPE).orEmpty(),
                            imageMetadataSnapshot = BundleCompat
                                .getParcelable(
                                    args,
                                    KEY_IMAGE_METADATA_SNAPSHOT,
                                    ImageMetadataSnapshot::class.java
                                )
                                ?: ImageMetadataSnapshot()
                        )
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderState(state: ImageMetadataDetailsState) {
        binding.extension.text = state.extension
        binding.extension.isVisible = state.extension.isNotEmpty()
        binding.iccp.isVisible = state.imageMetadataSnapshot.isIccProfileContained
        binding.exif.isVisible = state.imageMetadataSnapshot.isExifContained
        binding.xmp.isVisible = state.imageMetadataSnapshot.isXmpContained
        binding.extendedXmp.isVisible = state.imageMetadataSnapshot.isExtendedXmpContained
        binding.photoshop.isVisible = state.imageMetadataSnapshot.isPhotoshopImageResourcesContained
    }
}
