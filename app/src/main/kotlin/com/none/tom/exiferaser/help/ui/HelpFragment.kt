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

package com.none.tom.exiferaser.help.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.contract.ActivityResultContractViewUrl
import com.none.tom.exiferaser.core.extension.setupToolbar
import com.none.tom.exiferaser.core.ui.BaseFragment
import com.none.tom.exiferaser.core.util.URL_ISSUES
import com.none.tom.exiferaser.core.util.URL_LOCALISATION
import com.none.tom.exiferaser.databinding.FragmentHelpBinding

class HelpFragment :
    BaseFragment<FragmentHelpBinding>(R.layout.fragment_help),
    HelpAdapter.Listener {

    private val viewUrl = registerForActivityResult(ActivityResultContractViewUrl()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(toolbar = binding.appbarMediumCollapsing.toolbar, title = R.string.help)
        binding.helpAndFeedback.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = HelpAdapter(listener = this@HelpFragment)
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
    }

    override fun bindLayout(view: View) = FragmentHelpBinding.bind(view)

    override fun onHelpTranslateSelected() {
        viewUrl.launch(URL_LOCALISATION.toUri())
    }

    override fun onFeedbackSelected() {
        viewUrl.launch(URL_ISSUES.toUri())
    }
}
