/*
 * Copyright (c) 2018-2021, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.databinding.FragmentDetailsBinding
import com.none.tom.exiferaser.details.business.DetailsState
import com.none.tom.exiferaser.details.business.DetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class DetailsFragment : DialogFragment() {

    companion object {

        const val TAG = "DetailsFragment"
        const val KEY_DISPLAY_NAME = TOP_LEVEL_PACKAGE_NAME + "display_name"
        const val KEY_MIME_TYPE = TOP_LEVEL_PACKAGE_NAME + "mime_type"
        const val KEY_ICCP = TOP_LEVEL_PACKAGE_NAME + "iccp"
        const val KEY_EXIF = TOP_LEVEL_PACKAGE_NAME + "exif"
        const val KEY_PHOTOSHOP = TOP_LEVEL_PACKAGE_NAME + "photoshop"
        const val KEY_XMP = TOP_LEVEL_PACKAGE_NAME + "xmp"
        const val KEY_EXTENDED_XMP = TOP_LEVEL_PACKAGE_NAME + "extended_xmp"

        @Suppress("LongParameterList")
        fun newInstance(
            displayName: String,
            mimeType: String,
            containsIccProfile: Boolean,
            containsExif: Boolean,
            containsPhotoshopImageResources: Boolean,
            containsXmp: Boolean,
            containsExtendedXmp: Boolean
        ) = DetailsFragment().apply {
            arguments = bundleOf(
                KEY_DISPLAY_NAME to displayName,
                KEY_MIME_TYPE to mimeType,
                KEY_ICCP to containsIccProfile,
                KEY_EXIF to containsExif,
                KEY_PHOTOSHOP to containsPhotoshopImageResources,
                KEY_XMP to containsXmp,
                KEY_EXTENDED_XMP to containsExtendedXmp
            )
        }
    }

    private val viewModel: DetailsViewModel by viewModels()
    private var _binding: FragmentDetailsBinding? = null
    private val binding: FragmentDetailsBinding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = FragmentDetailsBinding.inflate(LayoutInflater.from(requireContext()))
        lifecycleScope.launchWhenCreated {
            viewModel.container.stateFlow.collect { state ->
                renderState(state)
            }
        }
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(arguments?.getString(KEY_DISPLAY_NAME, String.Empty))
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()
            .apply {
                setOnShowListener {
                    arguments?.let { args ->
                        viewModel.handleImageDetails(
                            mimeType = args.getString(KEY_MIME_TYPE, String.Empty),
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

    @Suppress("LongMethod")
    private fun renderState(state: DetailsState) {
        binding.apply {
            extension.apply {
                if (state.extension.isNotEmpty()) {
                    text = state.extension
                    isVisible = true
                } else {
                    isVisible = false
                }
            }
            val colorOk = ColorStateList.valueOf(MaterialColors.getColor(root, R.attr.colorOk))
            val colorError = ColorStateList.valueOf(
                MaterialColors.getColor(root, R.attr.colorError)
            )
            iccp.apply {
                if (state.containsIccProfile) {
                    chipStrokeColor = colorOk
                    chipIconTint = colorOk
                    setChipIconResource(R.drawable.ic_check)
                } else {
                    chipStrokeColor = colorError
                    chipIconTint = colorError
                    setChipIconResource(R.drawable.ic_clear)
                }
            }
            exif.apply {
                if (state.containsExif) {
                    chipStrokeColor = colorOk
                    chipIconTint = colorOk
                    setChipIconResource(R.drawable.ic_check)
                } else {
                    chipStrokeColor = colorError
                    chipIconTint = colorError
                    setChipIconResource(R.drawable.ic_clear)
                }
            }
            xmp.apply {
                if (state.containsXmp) {
                    chipStrokeColor = colorOk
                    chipIconTint = colorOk
                    setChipIconResource(R.drawable.ic_check)
                } else {
                    chipStrokeColor = colorError
                    chipIconTint = colorError
                    setChipIconResource(R.drawable.ic_clear)
                }
            }
            photoshop.apply {
                if (state.containsPhotoshopImageResources) {
                    chipStrokeColor = colorOk
                    chipIconTint = colorOk
                    setChipIconResource(R.drawable.ic_check)
                } else {
                    chipStrokeColor = colorError
                    chipIconTint = colorError
                    setChipIconResource(R.drawable.ic_clear)
                }
                isVisible = state.jpegImage
            }
            extendedXmp.apply {
                if (state.containsExtendedXmp) {
                    chipStrokeColor = colorOk
                    chipIconTint = colorOk
                    setChipIconResource(R.drawable.ic_check)
                } else {
                    chipStrokeColor = colorError
                    chipIconTint = colorError
                    setChipIconResource(R.drawable.ic_clear)
                }
                isVisible = state.jpegImage
            }
        }
    }
}
