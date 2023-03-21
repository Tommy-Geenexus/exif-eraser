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

package com.none.tom.exiferaser.report.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.AttrRes
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.color.MaterialColors
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.MIME_TYPE_IMAGE
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.databinding.FragmentReportBinding
import com.none.tom.exiferaser.details.ui.ImageModifiedDetailsFragment
import com.none.tom.exiferaser.details.ui.ImageSavedDetailsFragment
import com.none.tom.exiferaser.report.ReportConstraintLayout
import com.none.tom.exiferaser.report.business.ReportSideEffect
import com.none.tom.exiferaser.report.business.ReportState
import com.none.tom.exiferaser.report.business.ReportViewModel
import com.none.tom.exiferaser.report.lerp
import com.none.tom.exiferaser.report.lerpArgb
import com.none.tom.exiferaser.selection.MaterialColor
import com.none.tom.exiferaser.selection.data.Summary
import com.none.tom.exiferaser.selection.ui.SelectionFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlin.contracts.ExperimentalContracts
import kotlinx.coroutines.launch

@ExperimentalContracts
@AndroidEntryPoint
class ReportFragment :
    BaseFragment<FragmentReportBinding>(R.layout.fragment_report),
    ReportAdapter.Listener {

    companion object {
        private const val KEY_STATE_BEHAVIOUR = TOP_LEVEL_PACKAGE_NAME + "STATE_BEHAVIOUR"
        private const val KEY_STATE_REPORT = TOP_LEVEL_PACKAGE_NAME + "REPORT"
        const val KEY_REPORT_SLIDE = TOP_LEVEL_PACKAGE_NAME + "REPORT_SLIDE"
        const val KEY_OFFSET_SLIDE = TOP_LEVEL_PACKAGE_NAME + "OFFSET_SLIDE"
        const val KEY_FRACTION_END = TOP_LEVEL_PACKAGE_NAME + "FRACTION_END"
        private const val FRACTION_IN_START = 0.2f
        const val FRACTION_OUT_START = 0f
        private const val FRACTION_OUT_END = 0.19f
    }

    private var _viewModel: ReportViewModel? = null
    private val viewModel: ReportViewModel get() = _viewModel!!
    private val viewImage = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}
    private val colorBackgroundStart by lazy(LazyThreadSafetyMode.NONE) {
        MaterialColors.getColor(requireView(), R.attr.colorPrimaryContainer)
    }
    private val colorBackgroundEnd by lazy(LazyThreadSafetyMode.NONE) {
        MaterialColors.getColor(requireView(), R.attr.colorSurface)
    }
    private val endFraction by lazy(LazyThreadSafetyMode.NONE) {
        val actionBarSize = resolveThemeAttribute(android.R.attr.actionBarSize).toFloat()
        val layoutHeight = binding.layout.height.toFloat()
        1 - (actionBarSize / layoutHeight)
    }
    private val backCallback by lazy(LazyThreadSafetyMode.NONE) {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            enabled = false
        ) {
            behaviour.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }
    private var _reportCallback: BottomSheetBehavior.BottomSheetCallback? = null
    private val reportCallback get() = _reportCallback!!
    private var _behaviour: ReportFragmentBehaviour<ReportConstraintLayout>? = null
    private val behaviour get() = _behaviour!!

    @Suppress("DEPRECATION")
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        _viewModel = ViewModelProvider(this).get()
        setFragmentResultListener(SelectionFragment.KEY_REPORT_PREPARE) { _, args: Bundle ->
            viewModel.handleImageSummaries(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    args.getParcelableArrayList(
                        SelectionFragment.KEY_REPORT_PREPARE,
                        Summary::class.java
                    ).orEmpty()
                } else {
                    args.getParcelableArrayList<Summary>(
                        SelectionFragment.KEY_REPORT_PREPARE
                    ).orEmpty()
                }
            )
        }
        _behaviour = BottomSheetBehavior.from(binding.layout) as
            ReportFragmentBehaviour<ReportConstraintLayout>
        var translationMax = 0f
        binding.layout.apply {
            doOnLayout {
                behaviour.apply {
                    val peekWidth = with(binding.details) { width + marginStart + marginEnd }
                    val peekHeight = with(binding.details) { height + marginBottom + marginTop }
                    translationMax = (width - peekWidth).toFloat()
                    translationX = translationMax
                    setPeekHeight(peekHeight)
                    addBottomSheetCallback(reportCallback)
                    state = savedInstanceState
                        ?.getInt(KEY_STATE_BEHAVIOUR)
                        ?: BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
        binding.report.apply {
            doOnLayout {
                (layoutManager as? LinearLayoutManager)
                    ?.onRestoreInstanceState(savedInstanceState?.getBundle(KEY_STATE_REPORT))
            }
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
                handleReportSlide(slideOffset, translationMax)
            }
        }
        binding.expand.setOnClickListener {
            behaviour.state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding.report.apply {
            addItemDecoration(DividerItemDecoration(requireContext(), RecyclerView.VERTICAL))
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ReportAdapter(listener = this@ReportFragment)
        }
        binding.toolbar.setNavigationOnClickListener {
            backCallback.handleOnBackPressed()
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
        outState.apply {
            putInt(KEY_STATE_BEHAVIOUR, behaviour.state)
            putParcelable(
                KEY_STATE_REPORT,
                (binding.report.layoutManager as? LinearLayoutManager)?.onSaveInstanceState()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewModel = null
        behaviour.removeBottomSheetCallback(reportCallback)
        _reportCallback = null
        _behaviour = null
    }

    override fun bindLayout(view: View) = FragmentReportBinding.bind(view)

    override fun onImageThumbnailSelected(position: Int) {
        viewModel.handleViewImage(position)
    }

    override fun onImageModifiedSelected(position: Int) {
        viewModel.handleImageModifiedDetails(position)
    }

    override fun onImageSavedSelected(position: Int) {
        viewModel.handleImageSavedDetails(position)
    }

    private fun renderState(state: ReportState) {
        (binding.report.adapter as? ReportAdapter)?.submitList(state.imageSummaries)
    }

    private fun handleSideEffect(sideEffect: ReportSideEffect) {
        when (sideEffect) {
            is ReportSideEffect.ViewImage -> {
                viewImage.launch(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndTypeAndNormalize(sideEffect.imageUri, MIME_TYPE_IMAGE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            }
            is ReportSideEffect.NavigateToImageModifiedDetails -> {
                ImageModifiedDetailsFragment.newInstance(
                    displayName = sideEffect.displayName,
                    extension = sideEffect.extension,
                    mimeType = sideEffect.mimeType,
                    containsIccProfile = sideEffect.containsIccProfile,
                    containsExif = sideEffect.containsExif,
                    containsPhotoshopImageResources = sideEffect.containsPhotoshopImageResources,
                    containsXmp = sideEffect.containsXmp,
                    containsExtendedXmp = sideEffect.containsExtendedXmp
                ).show(childFragmentManager, ImageModifiedDetailsFragment.TAG)
            }
            is ReportSideEffect.NavigateToImageSavedDetails -> {
                ImageSavedDetailsFragment
                    .newInstance(sideEffect.imagePath)
                    .show(childFragmentManager, ImageSavedDetailsFragment.TAG)
            }
        }
    }

    private fun handleReportSlide(
        slideOffset: Float,
        translationMax: Float
    ) {
        binding.layout.apply {
            translationX = lerp(
                startValue = translationMax,
                endValue = FRACTION_OUT_START,
                startFraction = FRACTION_OUT_START,
                endFraction = FRACTION_OUT_END,
                fraction = slideOffset
            )
        }
        binding.details.apply {
            alpha = lerp(
                startValue = MaterialColors.ALPHA_FULL,
                endValue = MaterialColor.ALPHA_TRANSPARENT,
                startFraction = FRACTION_OUT_START,
                endFraction = FRACTION_OUT_END,
                fraction = slideOffset
            )
            isVisible = slideOffset <= FRACTION_OUT_END
        }
        binding.expand.apply {
            alpha = lerp(
                startValue = MaterialColors.ALPHA_FULL,
                endValue = MaterialColor.ALPHA_TRANSPARENT,
                startFraction = FRACTION_OUT_START,
                endFraction = FRACTION_OUT_END,
                fraction = slideOffset
            )
            isVisible = slideOffset <= FRACTION_OUT_END
        }
        binding.toolbar.apply {
            alpha = lerp(
                startValue = MaterialColor.ALPHA_TRANSPARENT,
                endValue = MaterialColors.ALPHA_FULL,
                startFraction = FRACTION_IN_START,
                endFraction = endFraction,
                fraction = slideOffset
            )
            isVisible = slideOffset >= FRACTION_IN_START
        }
        binding.report.apply {
            alpha = lerp(
                startValue = MaterialColor.ALPHA_TRANSPARENT,
                endValue = MaterialColors.ALPHA_FULL,
                startFraction = FRACTION_OUT_START,
                endFraction = endFraction,
                fraction = slideOffset
            )
            isVisible = slideOffset >= FRACTION_IN_START
        }
        binding.layout.backgroundTintList = ColorStateList.valueOf(
            lerpArgb(
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

    private fun handleReportStateChanged(newState: Int) {
        backCallback.isEnabled = newState == BottomSheetBehavior.STATE_EXPANDED
    }

    private fun resolveThemeAttribute(
        @AttrRes attr: Int,
        typedValue: TypedValue = TypedValue(),
        resolveRefs: Boolean = true
    ): Int {
        requireActivity().theme.resolveAttribute(attr, typedValue, resolveRefs)
        return TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
    }
}
