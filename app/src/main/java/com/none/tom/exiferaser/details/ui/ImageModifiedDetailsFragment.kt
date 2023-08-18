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

package com.none.tom.exiferaser.details.ui

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.databinding.FragmentImageModifiedDetailsBinding
import com.none.tom.exiferaser.details.business.ImageModifiedDetailsState
import com.none.tom.exiferaser.details.business.ImageModifiedDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImageModifiedDetailsFragment : DialogFragment() {

    companion object {

        const val TAG = "DetailsFragment"

        const val KEY_DISPLAY_NAME = TOP_LEVEL_PACKAGE_NAME + "DISPLAY_NAME"
        const val KEY_EXTENSION = TOP_LEVEL_PACKAGE_NAME + "EXTENSION"
        const val KEY_MIME_TYPE = TOP_LEVEL_PACKAGE_NAME + "MIME_TYPE"
        const val KEY_ICCP = TOP_LEVEL_PACKAGE_NAME + "ICCP"
        const val KEY_EXIF = TOP_LEVEL_PACKAGE_NAME + "EXIF"
        const val KEY_PHOTOSHOP = TOP_LEVEL_PACKAGE_NAME + "PHOTOSHOP"
        const val KEY_XMP = TOP_LEVEL_PACKAGE_NAME + "XMP"
        const val KEY_EXTENDED_XMP = TOP_LEVEL_PACKAGE_NAME + "EXTENDED_XMP"

        fun newInstance(
            displayName: String,
            extension: String,
            mimeType: String,
            containsIccProfile: Boolean,
            containsExif: Boolean,
            containsPhotoshopImageResources: Boolean,
            containsXmp: Boolean,
            containsExtendedXmp: Boolean
        ) = ImageModifiedDetailsFragment().apply {
            arguments = bundleOf(
                KEY_DISPLAY_NAME to displayName,
                KEY_EXTENSION to extension,
                KEY_MIME_TYPE to mimeType,
                KEY_ICCP to containsIccProfile,
                KEY_EXIF to containsExif,
                KEY_PHOTOSHOP to containsPhotoshopImageResources,
                KEY_XMP to containsXmp,
                KEY_EXTENDED_XMP to containsExtendedXmp
            )
        }
    }

    private val viewModelImageModified: ImageModifiedDetailsViewModel by viewModels()
    private var _binding: FragmentImageModifiedDetailsBinding? = null
    private val binding: FragmentImageModifiedDetailsBinding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentImageModifiedDetailsBinding.inflate(layoutInflater)
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
                            containsIccProfile = args.getBoolean(KEY_ICCP),
                            containsExif = args.getBoolean(KEY_EXIF),
                            containsPhotoshopImageResources = args.getBoolean(KEY_PHOTOSHOP),
                            containsXmp = args.getBoolean(KEY_XMP),
                            containsExtendedXmp = args.getBoolean(KEY_EXTENDED_XMP)
                        )
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderState(state: ImageModifiedDetailsState) {
        binding.extension.text = state.extension
        binding.extension.isVisible = state.extension.isNotEmpty()
        binding.iccp.isVisible = state.containsIccProfile
        binding.exif.isVisible = state.containsExif
        binding.xmp.isVisible = state.containsXmp
        binding.extendedXmp.isVisible = state.containsExtendedXmp
        binding.photoshop.isVisible = state.containsPhotoshopImageResources
    }
}
