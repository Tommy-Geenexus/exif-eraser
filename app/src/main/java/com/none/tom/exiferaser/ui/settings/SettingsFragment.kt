// Copyright (c) 2018-2020, Tom Geiselmann
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software
// and associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
// INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
// IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY,WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
// CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.none.tom.exiferaser.ui.settings

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.activityViewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.URL_ISSUE
import com.none.tom.exiferaser.data.ImageRepository
import com.none.tom.exiferaser.data.SharedPrefsRepository
import com.none.tom.exiferaser.ui.SharedViewModel
import com.none.tom.exiferaser.ui.SharedViewModelFactory
import com.none.tom.exiferaser.ui.main.ViewUrl
import kotlinx.coroutines.Dispatchers

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val TAG_PREFERENCE_TEXT_INPUT_EDIT_TEXT = "text_preference_text_input_edit_text"
    }

    private val viewModel: SharedViewModel by activityViewModels {
        requireContext().applicationContext.let { context ->
            SharedViewModelFactory(
                ImageRepository(context, Dispatchers.IO),
                SharedPrefsRepository(context)
            )
        }
    }
    private val defaultOpenPath = registerForActivityResult(OpenDocumentTree()) { uri ->
        if (uri != null) {
            defaultOpenPathPreference?.apply {
                setSummary(R.string.custom)
                shouldShowButton = !shouldShowButton
            }
            viewModel.putDefaultOpenPath(uri)
            listView.adapter?.notifyItemChanged(0)
        }
    }
    private val defaultSavePath = registerForActivityResult(OpenDocumentTree()) { uri ->
        if (uri != null) {
            defaultSavePathPreference?.apply {
                setSummary(R.string.custom)
                shouldShowButton = !shouldShowButton
            }
            viewModel.putDefaultSavePath(uri)
            listView.adapter?.notifyItemChanged(1)
        }
    }
    private val viewUrl = registerForActivityResult(ViewUrl()) {}
    private var defaultOpenPathPreference: ImageButtonPreference? = null
    private var defaultSavePathPreference: ImageButtonPreference? = null
    private var imageOrientationPreference: SwitchPreference? = null
    private var defaultDisplayNameSuffixPreference: EditTextPreference? = null
    private var nightModePreference: ListPreference? = null

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
        findPreference<PreferenceCategory>(getString(R.string.key_file_system))?.let { preferenceCategory ->
            preferenceCategory.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        }
        findPreference<ImageButtonPreference>(getString(R.string.key_default_path_open))?.let { preference ->
            preference.apply {
                summary = viewModel.getDefaultOpenPathSummary()
                shouldShowButton = summary?.equals(getString(R.string.custom)) ?: false
                doOnClear = {
                    setSummary(R.string.none)
                    viewModel.putDefaultOpenPath(Uri.EMPTY)
                    shouldShowButton = !shouldShowButton
                    listView.adapter?.notifyItemChanged(0)
                }
            }
            defaultOpenPathPreference = preference
        }
        findPreference<ImageButtonPreference>(getString(R.string.key_default_path_save))?.let { preference ->
            preference.apply {
                summary = viewModel.getDefaultSavePathSummary()
                shouldShowButton = summary?.equals(getString(R.string.custom)) ?: false
                doOnClear = {
                    setSummary(R.string.none)
                    viewModel.putDefaultSavePath(Uri.EMPTY)
                    shouldShowButton = !shouldShowButton
                    listView.adapter?.notifyItemChanged(1)
                }
            }
            defaultSavePathPreference = preference
        }
        findPreference<SwitchPreference>(getString(R.string.key_image_orientation))?.let { preference ->
            preference.setOnPreferenceChangeListener { _, newValue ->
                viewModel.putPreserveImageOrientation(newValue as Boolean)
                true
            }
            imageOrientationPreference = preference
        }
        findPreference<EditTextPreference>(getString(R.string.key_default_display_name_suffix))?.let { preference ->
            preference.apply {
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
            defaultDisplayNameSuffixPreference = preference
        }
        findPreference<ListPreference>(getString(R.string.key_night_mode))?.let { preference ->
            preference.apply {
                setSummaryProvider { preference.entry }
                setOnPreferenceChangeListener { _, nightMode ->
                    AppCompatDelegate.setDefaultNightMode(nightMode.toString().toInt())
                    true
                }
            }
            nightModePreference = preference
        }
        findPreference<Preference>(getString(R.string.key_version))?.let { preference ->
            preference.summary = with(requireContext()) {
                packageManager
                    .getPackageInfo(packageName, 0)
                    .versionName
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is TextInputEditTextPreference) {
            if (parentFragmentManager.findFragmentByTag(TAG_PREFERENCE_TEXT_INPUT_EDIT_TEXT) == null) {
                TextInputEditTextPreferenceDialogFragmentCompat
                    .newInstance(preference.getKey())
                    .let { fragment ->
                        // TODO: Fix deprecation once upstream gets updated
                        @Suppress("DEPRECATION")
                        fragment.setTargetFragment(this, 0)
                        fragment.show(parentFragmentManager, TAG_PREFERENCE_TEXT_INPUT_EDIT_TEXT)
                    }
            }
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            getString(R.string.key_default_path_open) -> {
                defaultOpenPath.launch(viewModel.getDefaultOpenPath())
                true
            }
            getString(R.string.key_default_path_save) -> {
                defaultSavePath.launch(viewModel.getDefaultSavePath())
                true
            }
            getString(R.string.key_bug_report) -> {
                viewUrl.launch(URL_ISSUE)
                true
            }
            else -> super.onPreferenceTreeClick(preference)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        imageOrientationPreference?.onPreferenceChangeListener = null
        defaultDisplayNameSuffixPreference?.onPreferenceChangeListener = null
        nightModePreference?.onPreferenceChangeListener = null
        defaultOpenPathPreference = null
        defaultSavePathPreference = null
        imageOrientationPreference = null
        nightModePreference = null
    }
}
