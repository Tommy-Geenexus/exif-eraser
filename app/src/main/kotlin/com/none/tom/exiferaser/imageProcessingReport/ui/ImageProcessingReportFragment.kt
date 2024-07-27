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

package com.none.tom.exiferaser.imageProcessingReport.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Keep
import androidx.core.os.BundleCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.color.MaterialColors
import com.google.android.material.elevation.SurfaceColors
import com.google.android.material.snackbar.Snackbar
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.image.ImageProcessingSummary
import com.none.tom.exiferaser.core.ui.BaseFragment
import com.none.tom.exiferaser.core.ui.ChildDispatchConstraintLayout
import com.none.tom.exiferaser.core.util.MIME_TYPE_IMAGE
import com.none.tom.exiferaser.core.util.TOP_LEVEL_PACKAGE_NAME
import com.none.tom.exiferaser.core.util.lerp
import com.none.tom.exiferaser.core.util.lerpArgb
import com.none.tom.exiferaser.databinding.FragmentImageProcessingReportBinding
import com.none.tom.exiferaser.imageDetails.ui.ImageMetadataDetailsFragment
import com.none.tom.exiferaser.imageDetails.ui.ImageSavePathDetailsFragment
import com.none.tom.exiferaser.imageProcessing.ui.ImageProcessingFragment
import com.none.tom.exiferaser.imageProcessingReport.business.ImageProcessingReportSideEffect
import com.none.tom.exiferaser.imageProcessingReport.business.ImageProcessingReportState
import com.none.tom.exiferaser.imageProcessingReport.business.ImageProcessingReportViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Keep
@AndroidEntryPoint
class ImageProcessingReportFragment :
    BaseFragment<FragmentImageProcessingReportBinding>(R.layout.fragment_image_processing_report),
    ImageProcessingReportAdapter.Listener {

    private companion object {
        const val KEY_STATE_BEHAVIOUR = TOP_LEVEL_PACKAGE_NAME + "STATE_BEHAVIOUR"
        const val KEY_STATE_REPORT = TOP_LEVEL_PACKAGE_NAME + "REPORT"
        const val FRACTION_IN_START = 0.2f
        const val FRACTION_OUT_START = 0f
        const val FRACTION_OUT_END = 0.19f
        const val ALPHA_TRANSPARENT = 0f
    }

    private val viewModel: ImageProcessingReportViewModel by viewModels()
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
        val value = TypedValue()
        requireActivity().theme.resolveAttribute(android.R.attr.actionBarSize, value, true)
        val actionBarSize = TypedValue
            .complexToDimensionPixelSize(value.data, resources.displayMetrics)
            .toFloat()
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
    private var _behaviour:
        ImageProcessingReportFragmentBehaviour<ChildDispatchConstraintLayout>? = null
    private val behaviour get() = _behaviour!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setFragmentResultListener(ImageProcessingFragment.KEY_REPORT) { _, args: Bundle ->
            viewModel.handleImageProcessingSummaries(
                BundleCompat.getParcelableArrayList(
                    args,
                    ImageProcessingFragment.KEY_REPORT,
                    ImageProcessingSummary::class.java
                ).orEmpty()
            )
        }
        binding.toolbar.setBackgroundColor(SurfaceColors.SURFACE_0.getColor(requireActivity()))
        _behaviour = BottomSheetBehavior.from(binding.layout) as
            ImageProcessingReportFragmentBehaviour<ChildDispatchConstraintLayout>
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
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                handleReportStateChanged(newState)
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                handleReportSlide(slideOffset, translationMax)
            }
        }
        binding.expand.setOnClickListener {
            behaviour.state = BottomSheetBehavior.STATE_EXPANDED
        }
        binding.report.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ImageProcessingReportAdapter(listener = this@ImageProcessingReportFragment)
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
        behaviour.removeBottomSheetCallback(reportCallback)
        _reportCallback = null
        _behaviour = null
    }

    override fun bindLayout(view: View) = FragmentImageProcessingReportBinding.bind(view)

    override fun onImageThumbnailSelected(position: Int) {
        viewModel.handleViewImage(position)
    }

    override fun onViewImageMetadataSelected(position: Int) {
        viewModel.handleImageModifiedDetails(position)
    }

    override fun onViewImagePathSelected(position: Int) {
        viewModel.handleImageSavedDetails(position)
    }

    private fun renderState(state: ImageProcessingReportState) {
        (binding.report.adapter as? ImageProcessingReportAdapter)
            ?.submitList(state.imageProcessingSummaries)
    }

    private fun handleSideEffect(sideEffect: ImageProcessingReportSideEffect) {
        when (sideEffect) {
            is ImageProcessingReportSideEffect.ViewImage -> {
                viewImage.launch(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndTypeAndNormalize(sideEffect.imageUri, MIME_TYPE_IMAGE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            }
            ImageProcessingReportSideEffect.ImageSaved.Unsupported -> {
                Snackbar.make(requireView(), R.string.image_private, Snackbar.LENGTH_SHORT).show()
            }
            is ImageProcessingReportSideEffect.Navigate.ToImageModifiedDetails -> {
                ImageMetadataDetailsFragment.newInstance(
                    displayName = sideEffect.displayName,
                    extension = sideEffect.extension,
                    mimeType = sideEffect.mimeType,
                    imageMetadataSnapshot = sideEffect.imageMetadataSnapshot
                ).show(childFragmentManager, ImageMetadataDetailsFragment.TAG)
            }
            is ImageProcessingReportSideEffect.Navigate.ToImageSavedDetails -> {
                ImageSavePathDetailsFragment
                    .newInstance(sideEffect.name)
                    .show(childFragmentManager, ImageSavePathDetailsFragment.TAG)
            }
        }
    }

    private fun handleReportSlide(slideOffset: Float, translationMax: Float) {
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
                endValue = ALPHA_TRANSPARENT,
                startFraction = FRACTION_OUT_START,
                endFraction = FRACTION_OUT_END,
                fraction = slideOffset
            )
            isVisible = slideOffset <= FRACTION_OUT_END
        }
        binding.expand.apply {
            alpha = lerp(
                startValue = MaterialColors.ALPHA_FULL,
                endValue = ALPHA_TRANSPARENT,
                startFraction = FRACTION_OUT_START,
                endFraction = FRACTION_OUT_END,
                fraction = slideOffset
            )
            isVisible = slideOffset <= FRACTION_OUT_END
        }
        binding.toolbar.apply {
            alpha = lerp(
                startValue = ALPHA_TRANSPARENT,
                endValue = MaterialColors.ALPHA_FULL,
                startFraction = FRACTION_IN_START,
                endFraction = endFraction,
                fraction = slideOffset
            )
            isVisible = slideOffset >= FRACTION_IN_START
        }
        binding.report.apply {
            alpha = lerp(
                startValue = ALPHA_TRANSPARENT,
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
    }

    private fun handleReportStateChanged(newState: Int) {
        backCallback.isEnabled = newState == BottomSheetBehavior.STATE_EXPANDED
    }
}
