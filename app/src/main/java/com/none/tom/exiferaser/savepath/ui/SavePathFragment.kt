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

package com.none.tom.exiferaser.savepath.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentSavePathBinding
import com.none.tom.exiferaser.navigate
import com.none.tom.exiferaser.savepath.business.SavePathSideEffect
import com.none.tom.exiferaser.savepath.business.SavePathState
import com.none.tom.exiferaser.savepath.business.SavePathViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.contracts.ExperimentalContracts
import kotlinx.coroutines.flow.collect

@ExperimentalContracts
@AndroidEntryPoint
class SavePathFragment : BottomSheetDialogFragment() {

    private val viewModel: SavePathViewModel by viewModels()
    private val chooseSavePath = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { savePath ->
        viewModel.navigateToSelection(savePath)
    }
    private var _binding: FragmentSavePathBinding? = null
    private val binding: FragmentSavePathBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavePathBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.pathSaveDefault.setOnClickListener {
            viewModel.navigateToSelection(savePath = Uri.EMPTY)
        }
        binding.pathSaveCustom.setOnClickListener {
            viewModel.chooseSelectionSavePath()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getTheme() = R.style.ThemeOverlay_ExifEraser_Sheet_Bottom

    private fun renderState(state: SavePathState) {
        binding.pathSaveDefault.isVisible = state.hasPrivilegedDefaultSavePath
    }

    private fun handleSideEffect(sideEffect: SavePathSideEffect) {
        @Exhaustive
        when (sideEffect) {
            is SavePathSideEffect.ChooseSavePath -> {
                chooseSavePath.launch(sideEffect.openPath)
            }
            is SavePathSideEffect.NavigateToSelection -> {
                navigate(
                    SavePathFragmentDirections.selectionSavePathToSelection(sideEffect.savePath)
                )
            }
        }
    }
}
