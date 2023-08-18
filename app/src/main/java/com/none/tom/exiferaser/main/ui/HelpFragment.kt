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

package com.none.tom.exiferaser.main.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.URL_ISSUES
import com.none.tom.exiferaser.URL_LOCALISATION
import com.none.tom.exiferaser.databinding.FragmentHelpBinding
import com.none.tom.exiferaser.settings.ViewUrl

class HelpFragment :
    BaseFragment<FragmentHelpBinding>(R.layout.fragment_help),
    HelpAdapter.Listener {

    private val viewUrl = registerForActivityResult(ViewUrl()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(
            toolbar = binding.includeToolbar.toolbar,
            titleRes = R.string.help
        )
        binding.helpAndFeedback.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = HelpAdapter(listener = this@HelpFragment)
        }
    }

    override fun bindLayout(view: View) = FragmentHelpBinding.bind(view)

    override fun onHelpTranslateSelected() {
        viewUrl.launch(Uri.parse(URL_LOCALISATION))
    }

    override fun onFeedbackSelected() {
        viewUrl.launch(URL_ISSUES.toUri())
    }
}
