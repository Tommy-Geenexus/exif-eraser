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

package com.none.tom.exiferaser.settings.ui

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.Empty
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentSettingsBinding
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.settings.business.SettingsSideEffect
import com.none.tom.exiferaser.settings.business.SettingsState
import com.none.tom.exiferaser.settings.business.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@AndroidEntryPoint
class SettingsFragment :
    BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings),
    SettingsAdapter.Listener {

    private val viewModel: SettingsViewModel by viewModels()
    private val defaultPathOpen = registerForActivityResult(OpenDocumentTree()) { uri ->
        if (uri.isNotNullOrEmpty()) {
            viewModel.storeDefaultPathOpen(uri)
        }
    }
    private val defaultPathSave = registerForActivityResult(OpenDocumentTree()) { uri ->
        if (uri.isNotNullOrEmpty()) {
            viewModel.storeDefaultPathSave(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions(
            transitionEnter = MaterialSharedAxis(MaterialSharedAxis.Z, true),
            transitionReturn = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        setupToolbar(
            toolbar = binding.toolbarInclude.toolbar,
            titleRes = R.string.settings
        )
        binding.preferences.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = SettingsAdapter(listener = this@SettingsFragment)
            addItemDecoration(VerticalDividerItemDecoration(context))
            itemAnimator = null
        }
        setFragmentResultListener(
            DefaultDisplayNameSuffixFragment.KEY_DEFAULT_DISPLAY_NAME_SUFFIX
        ) { _, bundle: Bundle ->
            val value = bundle.getString(
                DefaultDisplayNameSuffixFragment.KEY_DEFAULT_DISPLAY_NAME_SUFFIX,
                String.Empty
            )
            viewModel.storeDefaultDisplayNameSuffix(value)
        }
        setFragmentResultListener(
            DefaultNightModeFragment.KEY_DEFAULT_NIGHT_MODE
        ) { _, bundle: Bundle ->
            val value = bundle.getInt(DefaultNightModeFragment.KEY_DEFAULT_NIGHT_MODE)
            AppCompatDelegate.setDefaultNightMode(value)
            viewModel.storeDefaultNightMode(value)
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

    override fun bindLayout(view: View) = FragmentSettingsBinding.bind(view)

    override fun onDefaultPathOpenSelected() {
        viewModel.handleDefaultPathOpen()
    }

    override fun onDefaultPathOpenClear() {
        viewModel.clearDefaultPathOpen()
    }

    override fun onDefaultPathSaveSelected() {
        viewModel.handleDefaultPathSave()
    }

    override fun onDefaultPathSaveClear() {
        viewModel.clearDefaultPathSave()
    }

    override fun onPreserveOrientationChanged(value: Boolean) {
        viewModel.storePreserveOrientation(value)
    }

    override fun onShareByDefaultChanged(value: Boolean) {
        viewModel.storeShareByDefault(value)
    }

    override fun onDefaultDisplayNameSuffixSelected() {
        viewModel.handleDefaultDisplayNameSuffix()
    }

    override fun onDefaultNightModeSelected() {
        viewModel.handleDefaultNightMode()
    }

    override fun onItemsUpdated() {
        startPostponedEnterTransition()
    }

    private fun renderState(state: SettingsState) {
        (binding.preferences.adapter as? SettingsAdapter)?.submitList(
            listOf(
                state.defaultPathOpenName,
                state.defaultPathSaveName,
                state.initialPreserveOrientation,
                state.initialShareByDefault,
                state.defaultDisplayNameSuffix,
                state.defaultNightModeName
            )
        )
    }

    private fun handleSideEffect(sideEffect: SettingsSideEffect) {
        @Exhaustive
        when (sideEffect) {
            is SettingsSideEffect.DefaultPathOpenClear -> {
            }
            is SettingsSideEffect.DefaultPathSaveClear -> {
            }
            is SettingsSideEffect.DefaultPathOpenSelect -> {
                defaultPathOpen.launch(sideEffect.uri)
            }
            is SettingsSideEffect.DefaultPathSaveSelect -> {
                defaultPathSave.launch(sideEffect.uri)
            }
            is SettingsSideEffect.DefaultPathOpenStore -> {
            }
            is SettingsSideEffect.DefaultPathSaveStore -> {
            }
            is SettingsSideEffect.NavigateToDefaultDisplayNameSuffix -> {
                navigate(
                    SettingsFragmentDirections.settingsToDefaultDisplayNameSuffix(
                        sideEffect.defaultDisplayNameSuffix
                    )
                )
            }
            is SettingsSideEffect.NavigateToDefaultNightMode -> {
                navigate(
                    SettingsFragmentDirections.settingsToDefaultNightMode(
                        sideEffect.defaultNightMode
                    )
                )
            }
            is SettingsSideEffect.PreserveOrientation -> {
            }
            is SettingsSideEffect.ShareByDefault -> {
            }
        }
    }
}
