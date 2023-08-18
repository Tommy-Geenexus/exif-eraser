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

package com.none.tom.exiferaser.selection.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.WindowSizeClass
import com.none.tom.exiferaser.databinding.FragmentSelectionBinding
import com.none.tom.exiferaser.selection.ShareImages
import com.none.tom.exiferaser.selection.business.SelectionSideEffect
import com.none.tom.exiferaser.selection.business.SelectionState
import com.none.tom.exiferaser.selection.business.SelectionViewModel
import com.none.tom.exiferaser.toPercent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelectionFragment : BaseFragment<FragmentSelectionBinding>(R.layout.fragment_selection) {

    companion object {
        const val KEY_REPORT = TOP_LEVEL_PACKAGE_NAME + "REPORT"
    }

    private val args: SelectionFragmentArgs by navArgs()
    private val viewModel: SelectionViewModel by viewModels()
    private val shareImages = registerForActivityResult(ShareImages()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val menuProvider = object : MenuProvider {

            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater
            ) {
                menuInflater.inflate(R.menu.menu_selection, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_share).apply {
                    isVisible = binding.done.isVisible &&
                        viewModel.container.stateFlow.value.imagesSaved > 0
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
        setupToolbar(
            toolbar = binding.toolbarInclude.toolbar,
            titleRes = R.string.summary
        )
        setupResponsiveLayout()
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

    override fun bindLayout(view: View) = FragmentSelectionBinding.bind(view)

    private fun renderState(state: SelectionState) {
        if (binding.progressLayout.isVisible) {
            binding.progress.text = state.progress.toPercent()
            binding.progressIndicator.setProgressCompat(state.progress, true)
        }
        if (state.handledAll) {
            binding.subheading.text = getString(
                R.string.images_modified_placeholder,
                state.imagesModified,
                getString(R.string.of),
                state.imagesTotal,
                getString(R.string.images_modified)
            )
            binding.progressLayout.isVisible = false
            binding.fragmentReport.isVisible = state.imageSummaries.isNotEmpty()
            binding.done.isVisible = true
            if (state.imagesTotal > 0) {
                childFragmentManager.setFragmentResult(
                    KEY_REPORT,
                    bundleOf(KEY_REPORT to ArrayList(state.imageSummaries))
                )
                requireActivity().invalidateOptionsMenu()
            }
        }
    }

    private fun handleSideEffect(sideEffect: SelectionSideEffect) {
        when (sideEffect) {
            is SelectionSideEffect.ReadComplete -> {
                viewModel.handleSelection(
                    selection = sideEffect.selection,
                    treeUri = args.savePath
                )
            }
            is SelectionSideEffect.SelectionHandled -> {
                viewModel.shareImagesByDefault()
            }
            is SelectionSideEffect.ShareImages -> {
                shareImages.launch(getString(R.string.share_via) to sideEffect.imageUris)
            }
        }
    }

    private fun setupResponsiveLayout() {
        binding.image.updateLayoutParams {
            val dimen = when (getWindowSizeClassHeight()) {
                WindowSizeClass.Compact -> {
                    resources.getDimension(R.dimen.icon_compact)
                }
                WindowSizeClass.Unspecified,
                WindowSizeClass.Medium -> {
                    resources.getDimension(R.dimen.icon_medium)
                }
                WindowSizeClass.Expanded -> {
                    resources.getDimension(R.dimen.icon_expanded)
                }
            }.toInt()
            height = dimen
            width = dimen
        }
        when (getWindowSizeClassHeight()) {
            WindowSizeClass.Unspecified,
            WindowSizeClass.Compact -> {
                binding.heading.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall
                )
                binding.subheading.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_BodySmall
                )
            }
            WindowSizeClass.Medium -> {
                binding.heading.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium
                )
                binding.subheading.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
                )
            }
            WindowSizeClass.Expanded -> {
                binding.heading.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge
                )
                binding.subheading.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_BodyLarge
                )
            }
        }
    }
}
