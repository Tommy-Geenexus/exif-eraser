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

package com.none.tom.exiferaser.report.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import com.github.heyalex.cornersheet.behavior.CornerSheetBehavior
import com.github.heyalex.cornersheet.interpolate
import com.github.heyalex.cornersheet.interpolateArgb
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.color.MaterialColors
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.MIME_TYPE_IMAGE
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.databinding.FragmentReportBinding
import com.none.tom.exiferaser.details.ui.DetailsFragment
import com.none.tom.exiferaser.report.business.ReportSideEffect
import com.none.tom.exiferaser.report.business.ReportState
import com.none.tom.exiferaser.report.business.ReportViewModel
import com.none.tom.exiferaser.report.resolveThemeAttr
import com.none.tom.exiferaser.selection.MaterialColor
import com.none.tom.exiferaser.selection.data.Summary
import com.none.tom.exiferaser.selection.ui.SelectionFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@Suppress("unused")
@AndroidEntryPoint
class ReportFragment :
    BaseFragment<FragmentReportBinding>(R.layout.fragment_report),
    ReportAdapter.Listener {

    companion object {
        private const val KEY_STATE_BEHAVIOUR = TOP_LEVEL_PACKAGE_NAME + "STATE_BEHAVIOUR"
        const val KEY_REPORT_SLIDE = TOP_LEVEL_PACKAGE_NAME + "REPORT_SLIDE"
        const val KEY_OFFSET_SLIDE = TOP_LEVEL_PACKAGE_NAME + "OFFSET_SLIDE"
        const val KEY_FRACTION_END = TOP_LEVEL_PACKAGE_NAME + "FRACTION_END"
        private const val FRACTION_IN_START = 0.2f
        const val FRACTION_OUT_START = 0f
        private const val FRACTION_OUT_END = 0.19f
    }

    private val viewModel: ReportViewModel by viewModels()
    private val colorBackgroundStart by lazy(LazyThreadSafetyMode.NONE) {
        MaterialColors.getColor(requireView(), R.attr.colorAccent)
    }
    private val colorBackgroundEnd by lazy(LazyThreadSafetyMode.NONE) {
        MaterialColors.getColor(requireView(), android.R.attr.colorBackground)
    }
    private val endFraction by lazy {
        1 - (
            requireContext().resolveThemeAttr(android.R.attr.actionBarSize).toFloat() /
                binding.layout.height.toFloat()
            )
    }
    private val backCallback by lazy(LazyThreadSafetyMode.NONE) {
        requireActivity()
            .onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, enabled = false) {
                behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
            }
    }
    private var _reportCallback: BottomSheetBehavior.BottomSheetCallback? = null
    private val reportCallback get() = _reportCallback!!
    private var _behaviour: CornerSheetBehavior<ConstraintLayout>? = null
    private val behaviour get() = _behaviour!!

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(SelectionFragment.KEY_REPORT_PREPARE) { _, args: Bundle ->
            viewModel.handleImageSummaries(
                args
                    .getParcelableArrayList<Summary>(SelectionFragment.KEY_REPORT_PREPARE)
                    .orEmpty()
            )
        }
        _reportCallback = object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                newState: Int
            ) {
                handleReportStateChanged(newState)
            }

            override fun onSlide(
                bottomSheet: View,
                slideOffset: Float
            ) {
                handleReportSlide(slideOffset)
            }
        }
        _behaviour =
            BottomSheetBehavior.from(binding.layout) as CornerSheetBehavior<ConstraintLayout>
        behaviour.apply {
            addBottomSheetCallback(reportCallback)
            binding.unfold.apply {
                doOnLayout {
                    setHorizontalPeekHeight(width + marginStart + marginEnd, false)
                    setPeekHeight(height + marginBottom + marginTop, false)
                    savedInstanceState?.let { savedState ->
                        state = savedState.getInt(KEY_STATE_BEHAVIOUR)
                    }
                }
            }
        }
        binding.apply {
            report.apply {
                addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
                layoutManager = LinearLayoutManager(requireContext())
                adapter = ReportAdapter(listener = this@ReportFragment)
                toolbar.setNavigationOnClickListener {
                    backCallback.handleOnBackPressed()
                }
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.container.stateFlow.collect { state ->
                renderState(state)
            }
        }
        lifecycleScope.launchWhenCreated {
            viewModel.container.sideEffectFlow.collect { sideEffect ->
                handleSideEffect(sideEffect)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STATE_BEHAVIOUR, behaviour.state)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        behaviour.removeBottomSheetCallback(reportCallback)
        _reportCallback = null
        _behaviour = null
    }

    override fun bindLayout(view: View) = FragmentReportBinding.bind(view)

    override fun onImageThumbnailSelected(position: Int) {
        viewModel.handleViewImage(position)
    }

    override fun onModifiedSelected(position: Int) {
        viewModel.handleImageDetails(position)
    }

    private fun renderState(state: ReportState) {
        (binding.report.adapter as? ReportAdapter)?.submitList(state.imageSummaries)
    }

    private fun handleSideEffect(sideEffect: ReportSideEffect) {
        @Exhaustive
        when (sideEffect) {
            is ReportSideEffect.ViewImage -> {
                startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndTypeAndNormalize(sideEffect.imageUri, MIME_TYPE_IMAGE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            }
            is ReportSideEffect.NavigateToDetails -> {
                DetailsFragment.newInstance(
                    displayName = sideEffect.displayName,
                    extension = sideEffect.extension,
                    mimeType = sideEffect.mimeType,
                    containsIccProfile = sideEffect.containsIccProfile,
                    containsExif = sideEffect.containsExif,
                    containsPhotoshopImageResources = sideEffect.containsPhotoshopImageResources,
                    containsXmp = sideEffect.containsXmp,
                    containsExtendedXmp = sideEffect.containsExtendedXmp
                ).show(childFragmentManager, DetailsFragment.TAG)
            }
        }
    }

    private fun handleReportSlide(slideOffset: Float) {
        binding.apply {
            unfold.apply {
                alpha = interpolate(
                    startValue = MaterialColors.ALPHA_FULL,
                    endValue = MaterialColor.ALPHA_TRANSPARENT,
                    startFraction = FRACTION_OUT_START,
                    endFraction = FRACTION_OUT_END,
                    fraction = slideOffset
                )
                isVisible = slideOffset <= FRACTION_OUT_END
            }
            toolbar.apply {
                alpha = interpolate(
                    startValue = MaterialColor.ALPHA_TRANSPARENT,
                    endValue = MaterialColors.ALPHA_FULL,
                    startFraction = FRACTION_IN_START,
                    endFraction = endFraction,
                    fraction = slideOffset
                )
                isVisible = slideOffset >= FRACTION_IN_START
            }
            report.apply {
                alpha = interpolate(
                    startValue = MaterialColor.ALPHA_TRANSPARENT,
                    endValue = MaterialColors.ALPHA_FULL,
                    startFraction = FRACTION_OUT_START,
                    endFraction = endFraction,
                    fraction = slideOffset
                )
                isVisible = slideOffset >= FRACTION_IN_START
            }
            layout.backgroundTintList = ColorStateList.valueOf(
                interpolateArgb(
                    startColor = colorBackgroundStart,
                    endColor = colorBackgroundEnd,
                    startFraction = FRACTION_OUT_START,
                    endFraction = FRACTION_OUT_END,
                    fraction = slideOffset
                )
            )
            requireParentFragment().parentFragmentManager.setFragmentResult(
                KEY_REPORT_SLIDE,
                bundleOf(
                    KEY_FRACTION_END to endFraction,
                    KEY_OFFSET_SLIDE to slideOffset
                )
            )
        }
    }

    private fun handleReportStateChanged(newState: Int) {
        backCallback.isEnabled = newState == BottomSheetBehavior.STATE_EXPANDED
    }
}
