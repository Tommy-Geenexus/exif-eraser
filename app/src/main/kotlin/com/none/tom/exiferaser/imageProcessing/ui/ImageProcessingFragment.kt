/*
 * Copyright (c) 2018-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.imageProcessing.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.window.core.layout.WindowSizeClass
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.contract.ActivityResultContractShareImages
import com.none.tom.exiferaser.core.image.ImageProcessingStep
import com.none.tom.exiferaser.core.ui.BaseFragment
import com.none.tom.exiferaser.core.util.NAV_ARG_IMAGE_PROCESSING_SUMMARIES
import com.none.tom.exiferaser.databinding.FragmentImageProcessingBinding
import com.none.tom.exiferaser.imageProcessing.business.ImageProcessingSideEffect
import com.none.tom.exiferaser.imageProcessing.business.ImageProcessingState
import com.none.tom.exiferaser.imageProcessing.business.ImageProcessingViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImageProcessingFragment : BaseFragment<FragmentImageProcessingBinding>(
    R.layout.fragment_image_processing
) {
    private val viewModel: ImageProcessingViewModel by viewModels()
    private val activityResultContractShareImages = registerForActivityResult(
        ActivityResultContractShareImages()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        exitTransition = MaterialElevationScale(false)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        reenterTransition = MaterialElevationScale(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updateLayoutParams<FrameLayout.LayoutParams> { bottomMargin = insets.bottom }
            WindowInsetsCompat.CONSUMED
        }
        val menuProvider = object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_selection, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_share).apply {
                    isVisible = binding.done.isVisible &&
                        viewModel.container.stateFlow.value.imagesSavedCount > 0
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return if (menuItem.itemId == R.id.action_share) {
                    viewModel.shareImages()
                    true
                } else {
                    false
                }
            }
        }
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner)
        setupAdaptiveLayout()
        binding.details.setOnClickListener { viewModel.handleImageProcessingDetails() }
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

    override fun bindLayout(view: View) = FragmentImageProcessingBinding.bind(view)

    private fun renderState(state: ImageProcessingState) {
        if (binding.progressLayout.isVisible) {
            binding.progress.text = getString(
                R.string.progress_percent,
                state.progress.displayValue
            )
            binding.progressIndicator.setProgressCompat(state.progress.displayValue, true)
        }
        if (state.imageImageProcessingStep is ImageProcessingStep.FinishedBulk) {
            binding.subheading.text = getString(
                R.string.images_modified_placeholder,
                state.imagesWithMetadataCount,
                getString(R.string.of),
                state.imagesProcessedCount,
                getString(R.string.images_modified)
            )
            binding.progressLayout.isVisible = false
            binding.detailsLayout.isVisible = state.imageProcessingSummaries.isNotEmpty()
            binding.done.isVisible = true
            if (state.imagesProcessedCount > 0) {
                requireActivity().invalidateOptionsMenu()
            }
        }
    }

    private fun handleSideEffect(sideEffect: ImageProcessingSideEffect) {
        when (sideEffect) {
            is ImageProcessingSideEffect.Handle.UserImagesSelection -> {
                viewModel.handleUserImagesSelection(sideEffect.protos, sideEffect.treeUri)
            }
            ImageProcessingSideEffect.Handle.UnsupportedSelection -> {
                viewModel.handleUnsupportedSelection()
            }
            is ImageProcessingSideEffect.Navigate.ToImageProcessingDetails -> {
                findNavController().navigate(
                    R.id.image_processing_to_image_processing_details,
                    bundleOf(
                        NAV_ARG_IMAGE_PROCESSING_SUMMARIES to sideEffect.imageProcessingSummaries
                    ),
                    null,
                    FragmentNavigatorExtras(
                        binding.details to getString(R.string.shared_element_details)
                    )
                )
            }
            is ImageProcessingSideEffect.ShareImages -> {
                activityResultContractShareImages.launch(
                    getString(R.string.share_via) to sideEffect.imageUris
                )
            }
        }
    }

    private fun setupAdaptiveLayout() {
        binding.image.updateLayoutParams {
            val dimen = if (isHeightAtLeastBreakpoint(
                    heightDpBreakpoint = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
                )
            ) {
                resources.getDimension(R.dimen.icon_expanded).toInt()
            } else if (isHeightAtLeastBreakpoint(
                    heightDpBreakpoint = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
                )
            ) {
                resources.getDimension(R.dimen.icon_medium).toInt()
            } else {
                resources.getDimension(R.dimen.icon_compact).toInt()
            }
            height = dimen
            width = dimen
        }
        if (isHeightAtLeastBreakpoint(
                heightDpBreakpoint = WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
            )
        ) {
            binding.heading.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge
            )
            binding.subheading.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
            )
        } else if (isHeightAtLeastBreakpoint(
                heightDpBreakpoint = WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
            )
        ) {
            binding.heading.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium
            )
            binding.subheading.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
            )
        } else {
            binding.heading.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall
            )
            binding.subheading.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodySmall
            )
        }
    }
}
