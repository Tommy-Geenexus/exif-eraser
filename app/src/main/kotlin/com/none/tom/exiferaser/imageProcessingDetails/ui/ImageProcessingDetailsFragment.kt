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

package com.none.tom.exiferaser.imageProcessingDetails.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialContainerTransform
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.extension.resolveThemeAttribute
import com.none.tom.exiferaser.core.ui.BaseFragment
import com.none.tom.exiferaser.core.util.KEY_STATE_REPORT
import com.none.tom.exiferaser.core.util.MIME_TYPE_IMAGE
import com.none.tom.exiferaser.databinding.FragmentImageProcessingDetailsBinding
import com.none.tom.exiferaser.imageDetails.ui.ImageMetadataDetailsFragment
import com.none.tom.exiferaser.imageDetails.ui.ImageSavePathDetailsFragment
import com.none.tom.exiferaser.imageProcessingDetails.business.ImageProcessingDetailsSideEffect
import com.none.tom.exiferaser.imageProcessingDetails.business.ImageProcessingDetailsState
import com.none.tom.exiferaser.imageProcessingDetails.business.ImageProcessingDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImageProcessingDetailsFragment :
    BaseFragment<FragmentImageProcessingDetailsBinding>(R.layout.fragment_image_processing_details),
    ImageProcessingDetailsAdapter.Listener {

    private val viewModel: ImageProcessingDetailsViewModel by viewModels()
    private val viewImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform(requireActivity(), true).apply {
            startContainerColor = requireActivity().resolveThemeAttribute(
                com.google.android.material.R.attr.colorPrimaryContainer
            )
            endContainerColor = requireActivity().resolveThemeAttribute(
                com.google.android.material.R.attr.colorSurface
            )
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            // Animating elevation shadow causes performance issues
            isElevationShadowEnabled = false
        }
        sharedElementReturnTransition = MaterialContainerTransform(requireActivity(), false).apply {
            startContainerColor = requireActivity().resolveThemeAttribute(
                com.google.android.material.R.attr.colorSurface
            )
            endContainerColor = requireActivity().resolveThemeAttribute(
                com.google.android.material.R.attr.colorPrimaryContainer
            )
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            // Animating elevation shadow causes performance issues
            isElevationShadowEnabled = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        postponeEnterTransition()
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            )
            binding.toolbar.setLayoutParams(
                (binding.toolbar.layoutParams as LinearLayout.LayoutParams).apply {
                    topMargin = insets.top
                }
            )
            view.setLayoutParams(
                (view.layoutParams as FrameLayout.LayoutParams).apply {
                    bottomMargin = insets.bottom
                }
            )
            WindowInsetsCompat.CONSUMED
        }
        setupToolbar(binding.toolbar, R.string.details)
        binding.details.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ImageProcessingDetailsAdapter(listener = this@ImageProcessingDetailsFragment)
            doOnLayout {
                (layoutManager as? LinearLayoutManager)
                    ?.onRestoreInstanceState(savedInstanceState?.getBundle(KEY_STATE_REPORT))
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.sideEffectFlow.collect { sideEffect ->
                    handleSideEffect(sideEffect)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(
            KEY_STATE_REPORT,
            (binding.details.layoutManager as? LinearLayoutManager)?.onSaveInstanceState()
        )
    }

    override fun bindLayout(view: View) = FragmentImageProcessingDetailsBinding.bind(view)

    override fun onImageThumbnailLoaded(position: Int) {
        if (position >= (binding.details.layoutManager as LinearLayoutManager)
                .findLastVisibleItemPosition()
        ) {
            startPostponedEnterTransition()
        }
    }

    override fun onImageThumbnailSelected(position: Int) {
        viewModel.handleViewImage(position)
    }

    override fun onViewImageMetadataSelected(position: Int) {
        viewModel.handleImageModifiedDetails(position)
    }

    override fun onViewImagePathSelected(position: Int) {
        viewModel.handleImageSavedDetails(position)
    }

    private fun renderState(state: ImageProcessingDetailsState) {
        (binding.details.adapter as? ImageProcessingDetailsAdapter)
            ?.submitList(state.imageProcessingSummaries)
    }

    private fun handleSideEffect(sideEffect: ImageProcessingDetailsSideEffect) {
        when (sideEffect) {
            is ImageProcessingDetailsSideEffect.ViewImage -> {
                viewImage.launch(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndTypeAndNormalize(sideEffect.imageUri, MIME_TYPE_IMAGE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            }
            ImageProcessingDetailsSideEffect.ImageSaved.Unsupported -> {
                Snackbar.make(requireView(), R.string.image_private, Snackbar.LENGTH_SHORT).show()
            }
            is ImageProcessingDetailsSideEffect.Navigate.ToImageModifiedDetails -> {
                ImageMetadataDetailsFragment.newInstance(
                    displayName = sideEffect.displayName,
                    extension = sideEffect.extension,
                    mimeType = sideEffect.mimeType,
                    imageMetadataSnapshot = sideEffect.imageMetadataSnapshot
                ).show(childFragmentManager, ImageMetadataDetailsFragment.TAG)
            }
            is ImageProcessingDetailsSideEffect.Navigate.ToImageSavedDetails -> {
                ImageSavePathDetailsFragment
                    .newInstance(sideEffect.name)
                    .show(childFragmentManager, ImageSavePathDetailsFragment.TAG)
            }
        }
    }
}
