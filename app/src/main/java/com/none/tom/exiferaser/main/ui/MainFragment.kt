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
import android.graphics.Point
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.DragEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.Surface
import android.view.View
import android.view.WindowInsets
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import app.cash.exhaustive.Exhaustive
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.ExifEraserActivity
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGE
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGES
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGE_DIR
import com.none.tom.exiferaser.INTENT_EXTRA_CONSUMED
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.databinding.FragmentMainBinding
import com.none.tom.exiferaser.isActivityInMultiWindowMode
import com.none.tom.exiferaser.main.ACTIVITY_EXPANDED
import com.none.tom.exiferaser.main.MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_DEFAULT
import com.none.tom.exiferaser.main.MainContentReceiver
import com.none.tom.exiferaser.main.MarginItemDecoration
import com.none.tom.exiferaser.main.SimpleItemTouchHelperCallback
import com.none.tom.exiferaser.main.TakePicture
import com.none.tom.exiferaser.main.addItemTouchHelper
import com.none.tom.exiferaser.main.addScaleAndIconAnimation
import com.none.tom.exiferaser.main.business.MainSideEffect
import com.none.tom.exiferaser.main.business.MainState
import com.none.tom.exiferaser.main.business.MainViewModel
import com.none.tom.exiferaser.main.getClipImages
import com.none.tom.exiferaser.navigate
import com.none.tom.exiferaser.selection.PROGRESS_MIN
import com.none.tom.exiferaser.setTransitions
import com.none.tom.exiferaser.setupToolbar
import com.none.tom.exiferaser.showSnackbar
import com.none.tom.exiferaser.supportedMimeTypes
import com.squareup.wire.AnyMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@AndroidEntryPoint
class MainFragment :
    BaseFragment<FragmentMainBinding>(R.layout.fragment_main),
    MainAdapter.Listener,
    MainContentReceiver.Listener {

    private companion object {
        const val GRID_LAYOUT_SPAN_CNT = 2

        @DrawableRes
        const val IMAGE_SOURCES_AVD_REORDER = R.drawable.avd_drag_to_done_all

        @DrawableRes
        const val IMAGE_SOURCES_AVD_DRAG = R.drawable.avd_done_all_to_drag
    }

    private val args: MainFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()
    private val chooseImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { result ->
        viewModel.preparePutSelection(result)
        viewModel.putImageSelection(imageUri = result)
    }
    private val chooseImages = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { result ->
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
        setFragmentResultListener(ExifEraserActivity.KEY_UPDATE_FAILED) { _, _ ->
            viewModel.handleFlexibleUpdateFailure()
        }
        setFragmentResultListener(
            ExifEraserActivity.KEY_UPDATE_IN_PROGRESS
        ) { key: String, args: Bundle ->
            viewModel.handleFlexibleUpdateInProgress(
                progress = args.getInt(key, PROGRESS_MIN),
                notify = binding.imageSourcesReorderLayout.childCount < 2
            )
        }
        setFragmentResultListener(ExifEraserActivity.KEY_UPDATE_READY_TO_INSTALL) { _, _ ->
            viewModel.handleFlexibleUpdateReadyToInstall()
        }
        binding.title.text = if (isOrientationPortrait()) {
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
            adapter = MainAdapter(
                listener = this@MainFragment,
                receiver = MainContentReceiver(this@MainFragment)
            )
            addItemDecoration(MarginItemDecoration(resources.getDimension(R.dimen.spacing_micro)))
            addItemTouchHelper(
                ItemTouchHelper(
                    SimpleItemTouchHelperCallback(
                        callback = adapter as MainAdapter,
                        canMoveItem = {
                            binding.imageSourcesReorder.tag == IMAGE_SOURCES_AVD_DRAG
                        }
                    )
                )
            )
        }
        binding.imageSourcesReorder.apply {
            addScaleAndIconAnimation(
                iconResStart = IMAGE_SOURCES_AVD_REORDER,
                iconResEnd = IMAGE_SOURCES_AVD_DRAG,
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
        consumeDeepLinkIfPresent()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        val transition = when (viewModel.sharedTransitionAxis) {
            MaterialSharedAxis.X -> MaterialSharedAxis(MaterialSharedAxis.X, false)
            MaterialSharedAxis.Y -> MaterialSharedAxis(MaterialSharedAxis.Y, false)
            MaterialSharedAxis.Z -> MaterialSharedAxis(MaterialSharedAxis.Z, false)
            else -> null
        }
        if (transition != null) {
            setTransitions(transitionReenter = transition)
        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu,
        inflater: MenuInflater
    ) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.content_paste -> {
                viewModel.handlePasteImages(requireContext().getClipImages())
                true
            }
            R.id.action_settings -> {
                viewModel.handleSettings()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        viewModel.handleMultiWindowMode(isInMultiWindowMode)
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
        viewModel.launchCamera(
            fileProviderPackage = requireContext().getString(R.string.file_provider_package),
            displayName = System.currentTimeMillis().toString()
        )
    }

    override fun onImageSourceMoved(
        imageSources: MutableList<AnyMessage>,
        oldIndex: Int,
        newIndex: Int
    ) {
        viewModel.reorderImageSources(imageSources, oldIndex, newIndex)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onUrisReceived(
        event: DragEvent,
        uris: List<Uri>
    ) {
        if (requireActivity().requestDragAndDropPermissions(event) != null) {
            viewModel.handleReceivedImages(uris)
        }
    }

    private fun renderState(state: MainState) {
        val isActivityInMultiWindowMode = isActivityInMultiWindowMode()
        val accessingStorage = state.imageSourcesFetching ||
            state.imageSourcesPersisting ||
            state.selectionPersisting ||
            state.accessingPreferences
        val currentScreenHeightRatio = calculateScreenHeightRatio(isActivityInMultiWindowMode)
        binding.apply {
            spinner.isVisible = accessingStorage
            title.setTextAppearance(
                getTitleTextAppearance(
                    screenHeightRatio = currentScreenHeightRatio,
                    isActivityInMultiWindowMode = isActivityInMultiWindowMode
                )
            )
            (imageSources.adapter as? MainAdapter?)?.run {
                screenHeightRatio = currentScreenHeightRatio
                submitList(state.imageSources)
            }
            imageSources.isVisible = !binding.spinner.isVisible
            imageSourcesReorder.isVisible = !isActivityInMultiWindowMode
        }
        val isReorderShown = binding.imageSourcesReorder.tag == IMAGE_SOURCES_AVD_REORDER
        val isDragShown = binding.imageSourcesReorder.tag == IMAGE_SOURCES_AVD_DRAG
        val animateImageSources = (state.imageSourcesReorder && isReorderShown) ||
            (state.imageSourcesPersisted && isDragShown)
        if (animateImageSources && !isActivityInMultiWindowMode) {
            (binding.imageSourcesReorder.icon as? Animatable)?.start()
        }
    }

    private fun handleSideEffect(sideEffect: MainSideEffect) {
        @Exhaustive
        when (sideEffect) {
            MainSideEffect.FlexibleUpdateReadyToInstall -> {
                binding.imageSourcesReorderLayout.showSnackbar(
                    msg = getString(R.string.app_update_ready),
                    actionMsg = R.string.install,
                    onActionClick = {
                        viewModel.completeFlexibleUpdate()
                    },
                    length = Snackbar.LENGTH_INDEFINITE
                )
            }
            MainSideEffect.FlexibleUpdateFailed -> {
                binding.imageSourcesReorderLayout.showSnackbar(
                    msg = getString(R.string.app_update_failed)
                )
            }
            is MainSideEffect.FlexibleUpdateInProgress -> {
                binding.imageSourcesReorderLayout.showSnackbar(
                    msg = getString(R.string.app_update_download),
                    length = Snackbar.LENGTH_INDEFINITE
                )
            }
            is MainSideEffect.ChooseImage -> {
                chooseImage.launch(supportedMimeTypes)
            }
            is MainSideEffect.ChooseImages -> {
                chooseImages.launch(supportedMimeTypes)
            }
            is MainSideEffect.ChooseImageDirectory -> {
                chooseImageDirectory.launch(sideEffect.openPath)
            }
            is MainSideEffect.LaunchCamera -> {
                launchCamera.launch(sideEffect.fileProviderImagePath)
            }
            is MainSideEffect.NavigateToSelection -> {
                setTransitions(
                    transitionExit = MaterialSharedAxis(MaterialSharedAxis.X, true),
                    transitionReenter = MaterialSharedAxis(MaterialSharedAxis.X, false)
                )
                viewModel.sharedTransitionAxis = MaterialSharedAxis.X
                navigate(MainFragmentDirections.mainToSelection(savePath = Uri.EMPTY))
            }
            is MainSideEffect.NavigateToSelectionSavePath -> {
                setTransitions(
                    transitionExit = MaterialSharedAxis(MaterialSharedAxis.X, true),
                    transitionReenter = MaterialSharedAxis(MaterialSharedAxis.X, false)
                )
                viewModel.sharedTransitionAxis = MaterialSharedAxis.X
                navigate(MainFragmentDirections.mainToSelectionSavePath())
            }
            is MainSideEffect.NavigateToSettings -> {
                setTransitions(
                    transitionExit = MaterialSharedAxis(MaterialSharedAxis.Z, true),
                    transitionReenter = MaterialSharedAxis(MaterialSharedAxis.Z, false)
                )
                viewModel.sharedTransitionAxis = MaterialSharedAxis.Z
                navigate(MainFragmentDirections.mainToSettings())
            }
            is MainSideEffect.PasteImages -> {
                viewModel.handleReceivedImages(sideEffect.uris)
            }
            is MainSideEffect.PasteImagesNone -> {
                binding.imageSourcesReorderLayout.showSnackbar(
                    msg = getString(R.string.clipboard_content_unsupported)
                )
            }
            is MainSideEffect.ReceivedImage -> {
                viewModel.preparePutSelection(sideEffect.uri)
                viewModel.putImageSelection(sideEffect.uri)
            }
            is MainSideEffect.ReceivedImages -> {
                viewModel.preparePutSelection(sideEffect.uris)
                viewModel.putImagesSelection(sideEffect.uris)
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

    private fun calculateScreenHeightRatio(isActivityInMultiWindowMode: Boolean): Float {
        if (!isActivityInMultiWindowMode) {
            return ACTIVITY_EXPANDED
        }
        return if (isRotationNormalOrInversed()) {
            // The activity is fully resizeable, calculate the relative size
            calculateMultiWindowModeRatioPortrait()
        } else {
            // The activity is fully expandable or collapsible only
            MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_DEFAULT
        }
    }

    @Suppress("DEPRECATION")
    private fun calculateMultiWindowModeRatioPortrait(): Float {
        val wm = requireActivity().windowManager
        val currentHeightPx: Int
        val maxHeightPx: Int
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = wm.currentWindowMetrics
            val mask = WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout()
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(mask)
            currentHeightPx = metrics.bounds.height() - insets.top - insets.bottom
            maxHeightPx = wm.maximumWindowMetrics.bounds.height()
        } else {
            val point = Point()
            val metrics = DisplayMetrics()
            val defaultDisplay = wm.defaultDisplay
            defaultDisplay.getSize(point)
            defaultDisplay.getRealMetrics(metrics)
            currentHeightPx = point.y
            maxHeightPx = metrics.heightPixels
        }
        return currentHeightPx.toFloat() / maxHeightPx.toFloat()
    }

    @StyleRes
    private fun getTitleTextAppearance(
        screenHeightRatio: Float,
        isActivityInMultiWindowMode: Boolean
    ): Int {
        val isActivityCollapsed = screenHeightRatio <= MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_DEFAULT
        return if (isActivityInMultiWindowMode && isActivityCollapsed) {
            R.style.TextAppearance_ExifEraser_Title_Medium
        } else {
            R.style.TextAppearance_ExifEraser_Title_Large
        }
    }

    private fun isOrientationPortrait(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    @Suppress("DEPRECATION")
    private fun isRotationNormalOrInversed(): Boolean {
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().display?.rotation
        } else {
            requireActivity().windowManager.defaultDisplay.rotation
        }
        return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
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
