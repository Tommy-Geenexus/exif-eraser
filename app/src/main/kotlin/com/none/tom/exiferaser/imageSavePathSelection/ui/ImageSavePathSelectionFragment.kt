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

package com.none.tom.exiferaser.imageSavePathSelection.ui

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.none.tom.exiferaser.BaseBottomSheetDialogFragment
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentImageSavePathSelectionBinding
import com.none.tom.exiferaser.imageSavePathSelection.business.ImageSavePathSelectionSideEffect
import com.none.tom.exiferaser.imageSavePathSelection.business.ImageSavePathSelectionState
import com.none.tom.exiferaser.imageSavePathSelection.business.ImageSavePathSelectionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImageSavePathSelectionFragment :
    BaseBottomSheetDialogFragment<FragmentImageSavePathSelectionBinding>() {

    private val viewModel: ImageSavePathSelectionViewModel by viewModels()
    private val chooseSavePath = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { savePath ->
        viewModel.handleSelection(savePath)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            val value = TypedValue()
            requireActivity().theme.resolveAttribute(R.attr.colorSecondary, value, true)
            binding.pathSaveDefault.setBackgroundColor(value.data)
        }
        binding.pathSaveDefault.setOnClickListener {
            viewModel.handleSelection(savePath = Uri.EMPTY)
        }
        binding.pathSaveCustom.setOnClickListener {
            viewModel.chooseSelectionSavePath()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.stateFlow.collect { state ->
                    renderState(state)
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.container.sideEffectFlow.collect { sideEffect ->
                    handleSideEffect(sideEffect)
                }
            }
        }
    }

    override fun inflateLayout(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentImageSavePathSelectionBinding.inflate(inflater, container, false)

    private fun renderState(state: ImageSavePathSelectionState) {
        binding.pathSaveDefault.isVisible = state.hasPrivilegedDefaultSavePath
    }

    private fun handleSideEffect(sideEffect: ImageSavePathSelectionSideEffect) {
        when (sideEffect) {
            is ImageSavePathSelectionSideEffect.ChooseSavePath -> {
                chooseSavePath.launch(sideEffect.openPath)
            }
            is ImageSavePathSelectionSideEffect.NavigateToSelection -> {
                navigate(
                    ImageSavePathSelectionFragmentDirections.selectionSavePathToImageProcessing(
                        sideEffect.savePath
                    )
                )
            }
        }
    }
}
