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

package com.none.tom.exiferaser.main.ui

import android.content.Context
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.drawable.Animatable
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.cash.exhaustive.Exhaustive
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGE
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGES
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGE_DIR
import com.none.tom.exiferaser.INTENT_EXTRA_CONSUMED
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentMainBinding
import com.none.tom.exiferaser.main.MarginItemDecoration
import com.none.tom.exiferaser.main.OpenDocument
import com.none.tom.exiferaser.main.OpenMultipleDocuments
import com.none.tom.exiferaser.main.TakePicture
import com.none.tom.exiferaser.main.addItemTouchHelper
import com.none.tom.exiferaser.main.business.MainSideEffect
import com.none.tom.exiferaser.main.business.MainState
import com.none.tom.exiferaser.main.business.MainViewModel
import com.none.tom.exiferaser.main.setupScaleAndIconAnimation
import com.none.tom.exiferaser.setTransitions
import com.none.tom.exiferaser.setupToolbar
import com.none.tom.exiferaser.supportedMimeTypes
import com.squareup.wire.AnyMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect

@AndroidEntryPoint
class MainFragment :
    BaseFragment<FragmentMainBinding>(R.layout.fragment_main),
    MainAdapter.Listener {

    private companion object {
        const val GRID_LAYOUT_SPAN_CNT = 2

        @DrawableRes
        const val IMAGE_SOURCES_AVD_REORDER = R.drawable.avd_drag_to_done_all

        @DrawableRes
        const val IMAGE_SOURCES_AVD_PUT = R.drawable.avd_done_all_to_drag
    }

    private val args: MainFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()
    private val chooseImage = registerForActivityResult(OpenDocument()) { result ->
        viewModel.preparePutSelection(result)
        viewModel.putImageSelection(imageUri = result)
    }
    private val chooseImages = registerForActivityResult(OpenMultipleDocuments()) { result ->
        viewModel.preparePutSelection(result)
        viewModel.putImagesSelection(imageUris = result)
    }
    private val chooseImageDirectory =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            viewModel.preparePutSelection(result)
            viewModel.putImageDirectorySelection(treeUri = result)
        }
    private val launchCamera = registerForActivityResult(TakePicture()) { result ->
        viewModel.preparePutSelection(result)
        viewModel.putImageSelection(
            imageUri = result,
            fromCamera = true
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        requireActivity().setupToolbar(
            fragment = this,
            toolbar = binding.toolbar,
            titleRes = R.string.app_name
        )
        val orientation = resources.configuration.orientation
        binding.title.text = if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
            getString(
                R.string.choose_your_preferred_image_source_placeholder,
                getString(R.string.choose_your),
                getString(R.string.preferred_image_source)
            )
        } else {
            getString(R.string.choose_your) + ' ' + getString(R.string.preferred_image_source)
        }
        binding.imageSources.apply {
            layoutManager = StaggeredGridLayoutManager(
                GRID_LAYOUT_SPAN_CNT,
                RecyclerView.HORIZONTAL
            )
            adapter = MainAdapter(listener = this@MainFragment)
            addItemDecoration(MarginItemDecoration(resources.getDimension(R.dimen.spacing_micro)))
            addItemTouchHelper(
                viewLifecycleOwner,
                ItemTouchHelper(
                    SimpleItemTouchHelperCallback(
                        callback = adapter as MainAdapter,
                        canMoveItem = {
                            binding.imageSourcesReorder.tag == IMAGE_SOURCES_AVD_PUT
                        }
                    )
                )
            )
        }
        binding.imageSourcesReorder.apply {
            setupScaleAndIconAnimation(
                viewLifecycleOwner,
                iconResStart = IMAGE_SOURCES_AVD_REORDER,
                iconResEnd = IMAGE_SOURCES_AVD_PUT,
                textResStart = R.string.reorder,
                textResEnd = R.string.confirm
            )
            setOnClickListener {
                if (tag == IMAGE_SOURCES_AVD_REORDER) {
                    viewModel.prepareReorderImageSources()
                } else {
                    (binding.imageSources.adapter as? MainAdapter)
                        ?.currentList
                        ?.let { imageSources ->
                            viewModel.preparePutImageSources()
                            viewModel.putImageSources(imageSources)
                        }
                }
            }
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
        consumeDeepLinkIfPresent()
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                viewModel.handleSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun bindLayout(view: View) = FragmentMainBinding.bind(view)

    override fun onImageItemSelected() {
        viewModel.prepareChooseImagesOrLaunchCamera()
        viewModel.chooseImage()
    }

    override fun onImagesItemSelected() {
        viewModel.prepareChooseImagesOrLaunchCamera()
        viewModel.chooseImages()
    }

    override fun onImageDirectoryItemSelected() {
        viewModel.prepareChooseImagesOrLaunchCamera()
        viewModel.chooseImageDirectory()
    }

    override fun onCameraItemSelected() {
        viewModel.prepareChooseImagesOrLaunchCamera()
        viewModel.launchCamera(displayName = System.currentTimeMillis().toString())
    }

    override fun onImageSourceMoved(
        imageSources: MutableList<AnyMessage>,
        oldIndex: Int,
        newIndex: Int
    ) {
        viewModel.reorderImageSources(imageSources, oldIndex, newIndex)
    }

    private fun renderState(state: MainState) {
        binding.run {
            spinner.isVisible = state.imageSourcesFetching ||
                state.imageSourcesPersisting ||
                state.selectionPersisting ||
                state.accessingPreferences
            imageSources.apply {
                (adapter as? MainAdapter)?.submitList(state.imageSources)
                isVisible = !spinner.isVisible
            }
            val reorder =
                state.imageSourcesReorder && imageSourcesReorder.tag != IMAGE_SOURCES_AVD_PUT
            val putReorder =
                state.imageSourcesPersisted && imageSourcesReorder.tag != IMAGE_SOURCES_AVD_REORDER
            if (reorder || putReorder) {
                (binding.imageSourcesReorder.icon as? Animatable)?.start()
            }
        }
    }

    private fun handleSideEffect(sideEffect: MainSideEffect) {
        @Exhaustive
        when (sideEffect) {
            is MainSideEffect.ChooseImage -> {
                (chooseImage.contract as? OpenDocument)?.initialUri = sideEffect.openPath
                chooseImage.launch(supportedMimeTypes)
            }
            is MainSideEffect.ChooseImages -> {
                (chooseImages.contract as? OpenMultipleDocuments)?.initialUri = sideEffect.openPath
                chooseImages.launch(supportedMimeTypes)
            }
            is MainSideEffect.ChooseImageDirectory -> {
                chooseImageDirectory.launch(sideEffect.openPath)
            }
            is MainSideEffect.LaunchCamera -> {
                launchCamera.launch(sideEffect.fileProviderImagePath)
            }
            is MainSideEffect.NavigateToSelectionSavePath -> {
                setTransitions(
                    transitionExit = MaterialSharedAxis(MaterialSharedAxis.X, true),
                    transitionReenter = MaterialSharedAxis(MaterialSharedAxis.X, false)
                )
                findNavController().navigate(MainFragmentDirections.mainToSelectionSavePath())
            }
            is MainSideEffect.NavigateToSettings -> {
                setTransitions(
                    transitionExit = MaterialSharedAxis(MaterialSharedAxis.Z, true),
                    transitionReenter = MaterialSharedAxis(MaterialSharedAxis.Z, false)
                )
                findNavController().navigate(MainFragmentDirections.mainToSettings())
            }
            is MainSideEffect.ShortcutHandle -> {
                handleShortcutIntent(sideEffect.shortcutAction)
            }
            is MainSideEffect.ShortcutReportUsed -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    reportShortcutUsed(sideEffect.shortcutAction)
                }
            }
        }
    }

    private fun consumeDeepLinkIfPresent() {
        if (requireActivity().intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        val shortcut = args.shortcut
        val imageSelection = args.imageSelection
        val imagesSelection = args.imagesSelection
        when {
            shortcut != null -> {
                requireActivity().intent.putExtra(INTENT_EXTRA_CONSUMED, true)
                viewModel.reportShortcutUsed(shortcut)
                viewModel.handleShortcut(shortcutAction = shortcut)
            }
            imageSelection != null -> {
                requireActivity().intent.putExtra(INTENT_EXTRA_CONSUMED, true)
                viewModel.preparePutSelection(imageSelection)
                viewModel.putImageSelection(imageUri = imageSelection)
            }
            imagesSelection != null -> {
                requireActivity().intent.putExtra(INTENT_EXTRA_CONSUMED, true)
                viewModel.preparePutSelection(imagesSelection)
                viewModel.putImagesSelection(intentImageUris = imagesSelection)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun reportShortcutUsed(action: String) {
        val shortcutManager =
            (requireContext().getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager)
        shortcutManager
            ?.manifestShortcuts
            ?.find { shortcutInfo -> shortcutInfo.intent?.action == action }
            ?.let { shortcutInfo -> shortcutManager.reportShortcutUsed(shortcutInfo.id) }
    }

    private fun handleShortcutIntent(action: String) {
        when (action) {
            INTENT_ACTION_CHOOSE_IMAGE -> {
                onImageItemSelected()
            }
            INTENT_ACTION_CHOOSE_IMAGES -> {
                onImagesItemSelected()
            }
            INTENT_ACTION_CHOOSE_IMAGE_DIR -> {
                onImageDirectoryItemSelected()
            }
            else -> {
                onCameraItemSelected()
            }
        }
    }
}
