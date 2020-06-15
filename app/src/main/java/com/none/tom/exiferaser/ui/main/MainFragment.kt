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

package com.none.tom.exiferaser.ui.main

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentMainBinding
import com.none.tom.exiferaser.ui.BaseFragment
import com.none.tom.exiferaser.ui.main.imagesource.ItemDecoration
import com.none.tom.exiferaser.ui.main.imagesource.MainAdapter
import com.none.tom.exiferaser.ui.main.imagesource.SimpleItemTouchHelperCallback

class MainFragment : BaseFragment<FragmentMainBinding>(R.layout.fragment_main), MainAdapter.OnItemSelectedListener {

    companion object {
        const val KEY_REORDER = "reorder"
    }

    private val selectImage = registerForActivityResult(OpenDocuments()) { selection ->
        viewModel.selection = selection
        findNavController().navigate(MainFragmentDirections.mainToImages())
    }
    private val selectImages = registerForActivityResult(OpenDocuments()) { selection ->
        viewModel.selection = selection
        findNavController().navigate(MainFragmentDirections.mainToImages())
    }
    private val selectImageDirectory = registerForActivityResult(OpenDocumentTree()) { selection ->
        viewModel.selection = selection
        findNavController().navigate(MainFragmentDirections.mainToImages())
    }
    private var itemTouchHelper: ItemTouchHelper? = null
    private var animationCallback: Animatable2Compat.AnimationCallback? = null
    private var reorder = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        getBindingSafe().apply {
            imageSource.apply {
                layoutManager = StaggeredGridLayoutManager(2, RecyclerView.HORIZONTAL)
                adapter = MainAdapter(this@MainFragment, viewModel)
                addItemDecoration(ItemDecoration(resources.getDimension(R.dimen.spacing_micro)))
                setHasFixedSize(true)
            }
            reorder.apply {
                setOnClickListener { (drawable as Animatable).start() }
                setImageResource(
                    if (this@MainFragment.reorder) {
                        R.drawable.avd_done_all_to_drag
                    } else {
                        R.drawable.avd_drag_to_done_all
                    }
                )
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let { state -> reorder = state.getBoolean(KEY_REORDER, false) }
    }

    override fun onStart() {
        super.onStart()
        getBindingSafe().apply {
            AnimationCallback().let { callback ->
                AnimatedVectorDrawableCompat.registerAnimationCallback(reorder.drawable, callback)
                animationCallback = callback
            }
            ItemTouchHelper(SimpleItemTouchHelperCallback(imageSource.adapter as MainAdapter) {
                this@MainFragment.reorder
            }).let { helper ->
                helper.attachToRecyclerView(imageSource)
                itemTouchHelper = helper
            }
        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.menu_exif_eraser, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(MainFragmentDirections.mainToSettings())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_REORDER, reorder)
    }

    override fun onStop() {
        super.onStop()
        getBindingSafe().reorder.drawable.let { drawable ->
            (drawable as Animatable).stop()
            AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawable, animationCallback)
        }
        itemTouchHelper?.attachToRecyclerView(null)
        itemTouchHelper = null
        animationCallback = null
    }

    override fun bindLayout(view: View): FragmentMainBinding = FragmentMainBinding.bind(view)

    override fun onImageItemSelected() {
        selectImage.launch(false to viewModel.getDefaultOpenPath())
    }

    override fun onImagesItemSelected() {
        selectImages.launch(true to viewModel.getDefaultOpenPath())
    }

    override fun onImageDirectoryItemSelected() {
        selectImageDirectory.launch(viewModel.getDefaultOpenPath())
    }

    private inner class AnimationCallback : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawableOld: Drawable?) {
            reorder = !reorder
            if (!reorder) {
                viewModel.putPreliminaryImageSourcePositions()
            }
            AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawableOld, this)
            getBindingSafe().reorder.apply {
                setImageResource(
                    if (reorder) {
                        R.drawable.avd_done_all_to_drag
                    } else {
                        R.drawable.avd_drag_to_done_all
                    }
                )
                AnimatedVectorDrawableCompat.registerAnimationCallback(drawable, this@AnimationCallback)
            }
        }
    }
}
