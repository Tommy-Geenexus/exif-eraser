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

package com.none.tom.exiferaser.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import com.none.tom.exiferaser.data.ImageRepository
import com.none.tom.exiferaser.data.SharedPrefsRepository
import kotlinx.coroutines.Dispatchers

abstract class BaseFragment<B : ViewBinding>(
    @LayoutRes layoutRes: Int
) : Fragment(layoutRes) {

    internal val viewModel: SharedViewModel by activityViewModels {
        requireContext().applicationContext.let { context ->
            SharedViewModelFactory(
                ImageRepository(context, Dispatchers.IO),
                SharedPrefsRepository(context)
            )
        }
    }
    private var binding: B? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = bindLayout(view)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    internal fun getBindingSafe(): B {
        return if (binding != null) {
            binding!!
        } else {
            throw IllegalStateException("binding not available before onCreateView() or after onDestroyView()")
        }
    }

    abstract fun bindLayout(view: View): B
}
