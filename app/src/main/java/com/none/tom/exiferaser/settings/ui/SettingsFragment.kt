/*
 * Copyright (c) 2018-2020, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.get
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.URL_LOCALISATION
import com.none.tom.exiferaser.databinding.FragmentSettingsBinding
import com.none.tom.exiferaser.isNotEmpty
import com.none.tom.exiferaser.isNotNullOrEmpty
import com.none.tom.exiferaser.setTransitions
import com.none.tom.exiferaser.settings.ImageButtonPreference
import com.none.tom.exiferaser.settings.ViewUrl
import com.none.tom.exiferaser.settings.business.SettingsViewModel
import com.none.tom.exiferaser.setupToolbar
import com.none.tom.exiferaser.supportImageFormatShortcuts
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat() {

    private val viewModel: SettingsViewModel by viewModels()
    private val defaultOpenPath = registerForActivityResult(OpenDocumentTree()) { path ->
        if (path.isNotNullOrEmpty()) {
            preferences.forEachIndexed { index, preference ->
                if (preference.key == getString(R.string.key_default_path_open)) {
                    setDefaultOpenPath(
                        preference = preference as? ImageButtonPreference,
                        layoutIndex = index,
                        path = path
                    )
                }
            }
        }
    }
    private val defaultSavePath = registerForActivityResult(OpenDocumentTree()) { path ->
        if (path.isNotNullOrEmpty()) {
            preferences.forEachIndexed { index, preference ->
                if (preference.key == getString(R.string.key_default_path_save)) {
                    setDefaultSavePath(
                        preference = preference as ImageButtonPreference,
                        layoutIndex = index,
                        path = path
                    )
                }
            }
        }
    }
    private val viewUrl = registerForActivityResult(ViewUrl()) {}
    private var _binding: FragmentSettingsBinding? = null
    internal val binding: FragmentSettingsBinding get() = _binding!!
    private var _preferences: MutableList<Preference>? = null
    private val preferences: List<Preference> get() = _preferences!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTransitions(
            transitionEnter = MaterialSharedAxis(MaterialSharedAxis.Z, true),
            transitionReturn = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        )
    }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)
        requireActivity().setupToolbar(
            fragment = this,
            toolbar = binding.toolbarInclude.toolbar,
            titleRes = R.string.settings
        )
        _preferences = mutableListOf<Preference>().apply {
            for (categoryIndex in 0 until preferenceScreen.preferenceCount) {
                val category = preferenceScreen[categoryIndex]
                if (category is PreferenceCategory) {
                    if (category.key == getString(R.string.key_file_system)) {
                        setupPreferenceCategoryFileSystem(category)
                    }
                    for (preferenceIndex in 0 until category.preferenceCount) {
                        add(category[preferenceIndex])
                    }
                }
            }
        }
        preferences.forEachIndexed { index, preference ->
            when (preference.key) {
                getString(R.string.key_default_path_open) -> {
                    setupPreferenceDefaultPathOpen(
                        preference = preference as? ImageButtonPreference,
                        layoutIndex = index
                    )
                }
                getString(R.string.key_default_path_save) -> {
                    setupPreferenceDefaultPathSave(
                        preference = preference as? ImageButtonPreference,
                        layoutIndex = index
                    )
                }
                getString(R.string.key_default_display_name_suffix) -> {
                    setupPreferenceDefaultDisplayNameSuffix(preference as? EditTextPreference)
                }
                getString(R.string.key_night_mode) -> {
                    setupPreferenceDefaultNightMode(preference as? ListPreference)
                }
                getString(R.string.key_translate) -> {
                    setupPreferenceTranslate(preference)
                }
                getString(R.string.key_bug_report) -> {
                    setupPreferenceBugReport(preference)
                }
                getString(R.string.key_image_supported_formats) -> {
                    setupPreferenceImageSupportedFormats(preference)
                }
                getString(R.string.key_version) -> {
                    setupPreferenceVersion(preference)
                }
                else -> {
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _preferences?.forEach { preference ->
            preference.apply {
                onPreferenceClickListener = null
                onPreferenceChangeListener = null
            }
        }
        _preferences = null
        _binding = null
    }

    private fun setupPreferenceCategoryFileSystem(preference: Preference) {
        preference.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    private fun setupPreferenceDefaultPathOpen(
        preference: ImageButtonPreference?,
        layoutIndex: Int
    ) {
        preference?.apply {
            summary = viewModel.getDefaultOpenPathSummary()
            setOnPreferenceClickListener {
                defaultOpenPath.launch(viewModel.getDefaultOpenPath())
                true
            }
            shouldShowClearButton = summary?.equals(getString(R.string.none))?.not() ?: false
            doOnClear = { setDefaultOpenPath(preference, layoutIndex, Uri.EMPTY) }
        }
    }

    private fun setupPreferenceDefaultPathSave(
        preference: ImageButtonPreference?,
        layoutIndex: Int
    ) {
        preference?.apply {
            summary = viewModel.getDefaultSavePathSummary()
            setOnPreferenceClickListener {
                defaultSavePath.launch(viewModel.getDefaultSavePath())
                true
            }
            shouldShowClearButton = summary?.equals(getString(R.string.none))?.not() ?: false
            doOnClear = { setDefaultSavePath(preference, layoutIndex, Uri.EMPTY) }
        }
    }

    private fun setupPreferenceDefaultDisplayNameSuffix(preference: EditTextPreference?) {
        preference?.apply {
            summary = if (text.isNullOrEmpty()) getString(R.string.none) else text
            setOnPreferenceChangeListener { _, newValue ->
                summary = if ((newValue as String).isEmpty()) {
                    getString(R.string.none)
                } else {
                    newValue
                }
                true
            }
        }
    }

    private fun setupPreferenceDefaultNightMode(preference: ListPreference?) {
        preference?.apply {
            setSummaryProvider { preference.entry }
            setOnPreferenceChangeListener { _, nightMode ->
                (nightMode as? String)?.toIntOrNull()?.let { defaultNightMode ->
                    AppCompatDelegate.setDefaultNightMode(defaultNightMode)
                }
                true
            }
        }
    }

    private fun setupPreferenceTranslate(preference: Preference) {
        preference.setOnPreferenceClickListener {
            viewUrl.launch(Uri.parse(URL_LOCALISATION))
            true
        }
    }

    private fun setupPreferenceBugReport(preference: Preference) {
        preference.setOnPreferenceClickListener {
            viewUrl.launch(Uri.parse(getString(R.string.url_issue)))
            true
        }
    }

    private fun setupPreferenceImageSupportedFormats(preference: Preference) {
        preference.summary = supportImageFormatShortcuts.joinToString()
    }

    private fun setupPreferenceVersion(preference: Preference) {
        preference.summary = viewModel.getPackageVersionName()
    }

    private fun setDefaultOpenPath(
        preference: ImageButtonPreference?,
        layoutIndex: Int,
        path: Uri
    ) {
        viewModel.putDefaultOpenPath(
            path = path,
            releaseUriPermissions = !path.isNotEmpty()
        )
        preference?.apply {
            summary = if (path.isNotEmpty()) {
                viewModel.getDefaultOpenPathSummary()
            } else {
                getString(R.string.none)
            }
            shouldShowClearButton = path.isNotEmpty()
        }
        listView.adapter?.notifyItemChanged(layoutIndex)
    }

    private fun setDefaultSavePath(
        preference: ImageButtonPreference?,
        layoutIndex: Int,
        path: Uri
    ) {
        viewModel.putDefaultSavePath(
            path = path,
            releaseUriPermissions = !path.isNotEmpty()
        )
        preference?.apply {
            summary = if (path.isNotEmpty()) {
                viewModel.getDefaultSavePathSummary()
            } else {
                getString(R.string.none)
            }
            shouldShowClearButton = path.isNotEmpty()
        }
        listView.adapter?.notifyItemChanged(layoutIndex)
    }
}
