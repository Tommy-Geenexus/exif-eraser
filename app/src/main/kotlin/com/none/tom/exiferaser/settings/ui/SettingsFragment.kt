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

package com.none.tom.exiferaser.settings.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.extension.isNotNullOrEmpty
import com.none.tom.exiferaser.core.extension.setupToolbar
import com.none.tom.exiferaser.core.ui.BaseFragment
import com.none.tom.exiferaser.core.ui.RecyclerViewVerticalDividerItemDecoration
import com.none.tom.exiferaser.core.util.KEY_DEFAULT_DISPLAY_NAME_SUFFIX
import com.none.tom.exiferaser.core.util.KEY_DEFAULT_NIGHT_MODE
import com.none.tom.exiferaser.databinding.FragmentSettingsBinding
import com.none.tom.exiferaser.settings.business.SettingsSideEffect
import com.none.tom.exiferaser.settings.business.SettingsState
import com.none.tom.exiferaser.settings.business.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.launch

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
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(toolbar = binding.appbarMediumCollapsing.toolbar, title = R.string.settings)
        binding.preferences.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = SettingsAdapter(listener = this@SettingsFragment)
            itemAnimator = null
            addItemDecoration(RecyclerViewVerticalDividerItemDecoration(context))
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updateLayoutParams<FrameLayout.LayoutParams> {
                binding.appbarMediumCollapsing.appbarLayout.setPadding(0, insets.top, 0, 0)
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
        setFragmentResultListener(KEY_DEFAULT_DISPLAY_NAME_SUFFIX) { _, bundle: Bundle ->
            val value = bundle
                .getString(KEY_DEFAULT_DISPLAY_NAME_SUFFIX)
                .orEmpty()
            viewModel.storeDefaultDisplayNameSuffix(value)
        }
        setFragmentResultListener(KEY_DEFAULT_NIGHT_MODE) { _, bundle: Bundle ->
            val value = bundle.getInt(KEY_DEFAULT_NIGHT_MODE)
            AppCompatDelegate.setDefaultNightMode(value)
            viewModel.storeDefaultNightMode(value)
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

    override fun bindLayout(view: View) = FragmentSettingsBinding.bind(view)

    override fun onRandomizeFileNamesChanged(value: Boolean) {
        viewModel.storeRandomizeFileNames(value)
    }

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

    override fun onAutoDeleteChanged(value: Boolean) {
        viewModel.storeAutoDelete(value)
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

    override fun onLegacyImageSelectionChanged(value: Boolean) {
        viewModel.storeLegacyImageSelection(value)
    }

    override fun onSavePathSelectionSkipChanged(value: Boolean) {
        viewModel.storeSavePathSelectionSkip(value)
    }

    override fun onDefaultNightModeSelected() {
        viewModel.handleDefaultNightMode()
    }

    private fun renderState(state: SettingsState) {
        (binding.preferences.adapter as? SettingsAdapter)?.run {
            val index = AtomicInteger()
            val legacyImageSelection = state.isLegacyImageSelectionEnabled.toString()
            submitList(
                listOf(
                    index.getAndIncrement() to state.isRandomizeFileNamesEnabled.toString(),
                    index.getAndIncrement() to state.defaultOpenPathName,
                    index.getAndIncrement() to state.defaultSavePathName,
                    index.getAndIncrement() to legacyImageSelection,
                    index.getAndIncrement() to state.isAutoDeleteEnabled.toString(),
                    index.getAndIncrement() to state.isPreserveOrientationEnabled.toString(),
                    index.getAndIncrement() to state.isShareByDefaultEnabled.toString(),
                    index.getAndIncrement() to state.defaultDisplayNameSuffix,
                    index.getAndIncrement() to legacyImageSelection,
                    index.getAndIncrement() to state.defaultSavePathName.isNotEmpty().toString(),
                    index.getAndIncrement() to state.isSkipSavePathSelectionEnabled.toString(),
                    index.getAndIncrement() to state.defaultNightModeName
                )
            )
        }
    }

    private fun handleSideEffect(sideEffect: SettingsSideEffect) {
        when (sideEffect) {
            SettingsSideEffect.AutoDelete.Failure -> {}
            SettingsSideEffect.AutoDelete.Success -> {}
            SettingsSideEffect.DefaultOpenPath.Clear.Failure -> {}
            SettingsSideEffect.DefaultOpenPath.Clear.Success -> {}
            SettingsSideEffect.DefaultOpenPath.Select.Failure -> {
                defaultPathOpen.launch(Uri.EMPTY)
            }
            is SettingsSideEffect.DefaultOpenPath.Select.Success -> {
                defaultPathOpen.launch(sideEffect.uri)
            }
            SettingsSideEffect.DefaultOpenPath.Store.Failure -> {}
            SettingsSideEffect.DefaultOpenPath.Store.Success -> {}
            SettingsSideEffect.DefaultSavePath.Clear.Failure -> {}
            SettingsSideEffect.DefaultSavePath.Clear.Success -> {}
            SettingsSideEffect.DefaultSavePath.Select.Failure -> {
                defaultPathSave.launch(Uri.EMPTY)
            }
            SettingsSideEffect.DefaultSavePath.SelectionSkip.Failure -> {}
            is SettingsSideEffect.DefaultSavePath.Select.Success -> {
                defaultPathSave.launch(sideEffect.uri)
            }
            SettingsSideEffect.DefaultSavePath.SelectionSkip.Success -> {}
            SettingsSideEffect.DefaultSavePath.Store.Failure -> {}
            SettingsSideEffect.DefaultSavePath.Store.Success -> {}
            SettingsSideEffect.LegacyImageSelection.Failure -> {}
            SettingsSideEffect.LegacyImageSelection.Success -> {}
            is SettingsSideEffect.Navigate.ToDefaultDisplayNameSuffix -> {
                findNavController().navigate(
                    SettingsFragmentDirections.settingsToDefaultDisplayNameSuffix(
                        sideEffect.defaultDisplayNameSuffix
                    )
                )
            }
            is SettingsSideEffect.Navigate.ToDefaultNightMode -> {
                findNavController().navigate(
                    SettingsFragmentDirections.settingsToDefaultNightMode(
                        sideEffect.defaultNightMode
                    )
                )
            }
            SettingsSideEffect.PreserveOrientation.Failure -> {}
            SettingsSideEffect.PreserveOrientation.Success -> {}
            SettingsSideEffect.RandomizeFileNames.Failure -> {}
            SettingsSideEffect.RandomizeFileNames.Success -> {}
            SettingsSideEffect.ShareByDefault.Failure -> {}
            SettingsSideEffect.ShareByDefault.Success -> {}
            SettingsSideEffect.DefaultDisplayNameSuffix.Failure -> {}
            SettingsSideEffect.DefaultDisplayNameSuffix.Success -> {}
            SettingsSideEffect.DefaultNightMode.Failure -> {}
            SettingsSideEffect.DefaultNightMode.Success -> {}
        }
    }
}
