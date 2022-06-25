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

package com.none.tom.exiferaser.selection.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.applyInsetMargins
import com.none.tom.exiferaser.databinding.FragmentSelectionBinding
import com.none.tom.exiferaser.report.lerp
import com.none.tom.exiferaser.report.ui.ReportFragment
import com.none.tom.exiferaser.selection.ShareImages
import com.none.tom.exiferaser.selection.business.SelectionSideEffect
import com.none.tom.exiferaser.selection.business.SelectionState
import com.none.tom.exiferaser.selection.business.SelectionViewModel
import com.none.tom.exiferaser.selection.crossfade
import com.none.tom.exiferaser.selection.fadeIn
import com.none.tom.exiferaser.selection.toPercent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@AndroidEntryPoint
class SelectionFragment : BaseFragment<FragmentSelectionBinding>(R.layout.fragment_selection) {

    companion object {
        const val KEY_REPORT_PREPARE = TOP_LEVEL_PACKAGE_NAME + "REPORT_PREPARE"
    }

    private val args: SelectionFragmentArgs by navArgs()
    private val viewModel: SelectionViewModel by viewModels()
    private val shareImages = registerForActivityResult(ShareImages()) {}
    private val elevationNone by lazy(LazyThreadSafetyMode.NONE) {
        resources.getDimension(R.dimen.elevation_none)
    }
    private val elevationMicro by lazy(LazyThreadSafetyMode.NONE) {
        resources.getDimension(R.dimen.elevation_micro)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions(
            transitionEnter = MaterialSharedAxis(MaterialSharedAxis.X, true),
            transitionReturn = MaterialSharedAxis(MaterialSharedAxis.X, false)
        )
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
                    isVisible = binding.done.isVisible && viewModel.hasSavedImages()
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
        setFragmentResultListener(ReportFragment.KEY_REPORT_SLIDE) { _, bundle: Bundle ->
            adjustToolbarElevation(
                endFraction = bundle.getFloat(ReportFragment.KEY_FRACTION_END),
                slideOffset = bundle.getFloat(ReportFragment.KEY_OFFSET_SLIDE)
            )
        }
        binding.layout.applyInsetMargins()
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
        showProgress(state.progress)
        if (state.handledAll) {
            showReport(state.imagesModified, state.imagesTotal)
        }
    }

    private fun handleSideEffect(sideEffect: SelectionSideEffect) {
        when (sideEffect) {
            is SelectionSideEffect.PrepareReport -> {
                childFragmentManager.setFragmentResult(
                    KEY_REPORT_PREPARE,
                    bundleOf(KEY_REPORT_PREPARE to sideEffect.imageSummaries)
                )
                binding.fragmentReport.fadeIn()
            }
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

    private fun showProgress(currentProgress: Int) {
        binding.apply {
            progress.text = currentProgress.toPercent()
            progressIndicator.setProgressCompat(currentProgress, true)
        }
    }

    private fun showReport(
        modified: Int,
        total: Int
    ) {
        binding.run {
            subheading.text = getString(
                R.string.images_modified_placeholder,
                modified,
                getString(R.string.of),
                total,
                getString(R.string.images_modified)
            )
            done.crossfade(progressLayout)
            if (total > 0) {
                viewModel.prepareReport()
                requireActivity().invalidateOptionsMenu()
            }
        }
    }

    private fun adjustToolbarElevation(
        endFraction: Float,
        slideOffset: Float
    ) {
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.elevation = lerp(
            startValue = elevationMicro,
            endValue = elevationNone,
            startFraction = ReportFragment.FRACTION_OUT_START,
            endFraction = endFraction,
            fraction = slideOffset
        )
    }
}
