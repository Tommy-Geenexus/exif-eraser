/*
 * Copyright (c) 2018-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.draganddrop.DropHelper
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.core.contract.ActivityResultContractTakePicture
import com.none.tom.exiferaser.core.extension.addIconAnimation
import com.none.tom.exiferaser.core.extension.attachTo
import com.none.tom.exiferaser.core.image.supportedImageFormats
import com.none.tom.exiferaser.core.receiver.DragAndDropContentReceiver
import com.none.tom.exiferaser.core.ui.BaseFragment
import com.none.tom.exiferaser.core.ui.RecyclerViewMarginItemDecoration
import com.none.tom.exiferaser.core.util.INTENT_ACTION_CHOOSE_IMAGE
import com.none.tom.exiferaser.core.util.INTENT_ACTION_CHOOSE_IMAGES
import com.none.tom.exiferaser.core.util.INTENT_ACTION_CHOOSE_IMAGE_DIR
import com.none.tom.exiferaser.core.util.INTENT_EXTRA_CONSUMED
import com.none.tom.exiferaser.core.util.KEY_CAMERA_IMAGE_DELETE
import com.none.tom.exiferaser.databinding.FragmentMainBinding
import com.none.tom.exiferaser.main.business.MainSideEffect
import com.none.tom.exiferaser.main.business.MainState
import com.none.tom.exiferaser.main.business.MainViewModel
import com.squareup.wire.AnyMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment :
    BaseFragment<FragmentMainBinding>(R.layout.fragment_main),
    MainAdapter.Listener {

    private val args: MainFragmentArgs by navArgs()
    private val viewModel: MainViewModel by viewModels()
    private val chooseImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { result ->
        viewModel.putImageSelection(uri = result)
    }
    private val chooseImageLegacy = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { result ->
        viewModel.putImageSelection(uri = result)
    }
    private val chooseImages = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
    ) { result ->
        viewModel.putImagesSelection(uris = result)
    }
    private val chooseImagesLegacy = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { result ->
        viewModel.putImagesSelection(uris = result)
    }
    private val chooseImageDirectory = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { result ->
        viewModel.putImageDirectorySelection(uri = result)
    }
    private val launchCamera = registerForActivityResult(
        ActivityResultContractTakePicture()
    ) { result ->
        viewModel.putImageSelection(uri = result, isFromCamera = true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsetsCompat ->
            val insets = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setLayoutParams(
                (view.layoutParams as FrameLayout.LayoutParams).apply {
                    topMargin = insets.top
                }
            )
            windowInsetsCompat
        }
        setupResponsiveAppBarLayout()
        setupResponsiveTitleLayout()
        setupResponsiveImageSourceLayout()
        DropHelper.configureView(
            requireActivity(),
            binding.layout,
            supportedImageFormats.map { f -> f.mimeType }.toTypedArray(),
            DragAndDropContentReceiver(
                onUrisReceived = { uris -> viewModel.handleReceivedImages(uris) }
            )
        )
        setFragmentResultListener(KEY_CAMERA_IMAGE_DELETE) { _, bundle: Bundle ->
            if (bundle.getBoolean(KEY_CAMERA_IMAGE_DELETE)) {
                viewModel.deleteCameraImages()
            }
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
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                findNavController().currentBackStackEntryFlow.collect { navBackStackEntry ->
                    val prevNavDestinationId = viewModel.navDestinationId
                    val nextNavDestinationId = navBackStackEntry.destination.id
                    if (nextNavDestinationId != prevNavDestinationId) {
                        viewModel.navDestinationId = nextNavDestinationId
                        exitTransition = if (
                            nextNavDestinationId == R.id.fragment_image_processing ||
                            nextNavDestinationId == R.id.fragment_image_save_path_selection
                        ) {
                            MaterialSharedAxis(MaterialSharedAxis.X, true)
                        } else {
                            MaterialSharedAxis(MaterialSharedAxis.Z, true)
                        }
                        reenterTransition = if (
                            prevNavDestinationId == R.id.fragment_image_processing ||
                            prevNavDestinationId == R.id.fragment_image_save_path_selection
                        ) {
                            MaterialSharedAxis(MaterialSharedAxis.X, false)
                        } else {
                            MaterialSharedAxis(MaterialSharedAxis.Z, false)
                        }
                    }
                }
            }
        }
        viewModel.readDefaultValues()
    }

    override fun bindLayout(view: View) = FragmentMainBinding.bind(view)

    override fun onImageItemSelected() {
        viewModel.chooseImage()
    }

    override fun onImagesItemSelected() {
        viewModel.chooseImages()
    }

    override fun onImageDirectoryItemSelected() {
        viewModel.chooseImageDirectory()
    }

    override fun onCameraItemSelected() {
        viewModel.launchCamera()
    }

    override fun onImageSourceMoved(imageSources: List<AnyMessage>, oldIndex: Int, newIndex: Int) {
        viewModel.reorderImageSources(imageSources, oldIndex, newIndex)
    }

    private fun renderState(state: MainState) {
        binding.progress.isVisible = state.loadingTasks > 0
        (binding.imageSources.adapter as? MainAdapter)?.submitList(state.imageSources)
    }

    private fun handleSideEffect(sideEffect: MainSideEffect) {
        when (sideEffect) {
            is MainSideEffect.DefaultNightMode -> {
                AppCompatDelegate.setDefaultNightMode(sideEffect.value)
            }
            is MainSideEffect.ImageSourceReordering -> {
                (binding.imageSourcesReorder.drawable as? Animatable)?.start()
                if (!sideEffect.isEnabled) {
                    viewModel.putImageSources(sideEffect.imageSources)
                }
            }
            is MainSideEffect.ImageSources.Image -> {
                if (sideEffect.isLegacyImageSelectionEnabled) {
                    chooseImageLegacy.launch(sideEffect.supportedMimeTypes)
                } else {
                    chooseImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
            MainSideEffect.ImageSources.ImageDirectory.Failure -> {
                chooseImageDirectory.launch(Uri.EMPTY)
            }
            is MainSideEffect.ImageSources.ImageDirectory.Success -> {
                chooseImageDirectory.launch(sideEffect.uri)
            }
            is MainSideEffect.ImageSources.Images -> {
                if (sideEffect.isLegacyImageSelectionEnabled) {
                    chooseImagesLegacy.launch(sideEffect.supportedMimeTypes)
                } else {
                    chooseImages.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            }
            MainSideEffect.ImageSources.Camera.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.image_sources_camera_failure,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            MainSideEffect.ImageSources.Put.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.image_sources_put_failure,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            is MainSideEffect.ImageSources.Camera.Success -> {
                launchCamera.launch(sideEffect.uri)
            }
            MainSideEffect.ImageSources.Put.Success -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.image_sources_put_success,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            MainSideEffect.Images.Delete.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.delete_camera_images_failed,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            MainSideEffect.Images.Delete.Success -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.delete_camera_images_success,
                        Snackbar.LENGTH_SHORT
                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            MainSideEffect.Images.Paste.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.clipboard_content_unsupported,
                        Snackbar.LENGTH_SHORT

                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            is MainSideEffect.Images.Paste.Success -> {
                viewModel.handleReceivedImages(sideEffect.uris)
            }
            is MainSideEffect.Images.Received.Multiple -> {
                viewModel.putImagesSelection(uris = sideEffect.uris)
            }
            MainSideEffect.Images.Received.None -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.clipboard_content_unsupported,
                        Snackbar.LENGTH_SHORT

                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            is MainSideEffect.Images.Received.Single -> {
                viewModel.putImageSelection(uri = sideEffect.uri)
            }
            MainSideEffect.ImageSources.Initialized -> {
                consumeSharedImages()
                consumeShortcutDeepLink()
            }
            MainSideEffect.Navigate.ToDeleteCameraImages -> {
                findNavController().navigate(MainFragmentDirections.mainToDeleteCameraImages())
            }
            MainSideEffect.Navigate.ToHelp -> {
                findNavController().navigate(MainFragmentDirections.mainToHelp())
            }
            is MainSideEffect.Navigate.ToSelection -> {
                findNavController().navigate(
                    MainFragmentDirections.mainToImageProcessing(sideEffect.savePath)
                )
            }
            MainSideEffect.Navigate.ToSelectionSavePath -> {
                findNavController().navigate(MainFragmentDirections.mainToImageSavePathSelection())
            }
            MainSideEffect.Navigate.ToSettings -> {
                findNavController().navigate(MainFragmentDirections.mainToSettings())
            }
            MainSideEffect.Selection.Image.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.image_sources_save_failure,
                        Snackbar.LENGTH_SHORT

                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            is MainSideEffect.Selection.Image.Success -> {
                viewModel.chooseSelectionNavigationRoute(sideEffect.isFromCamera)
            }
            MainSideEffect.Selection.ImageDirectory.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.image_sources_save_failure,
                        Snackbar.LENGTH_SHORT

                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            MainSideEffect.Selection.ImageDirectory.Success -> {
                viewModel.chooseSelectionNavigationRoute()
            }
            MainSideEffect.Selection.Images.Failure -> {
                Snackbar
                    .make(
                        requireView(),
                        R.string.image_sources_save_failure,
                        Snackbar.LENGTH_SHORT

                    )
                    .setAnchorView(binding.bottomBarLayout)
                    .show()
            }
            MainSideEffect.Selection.Images.Success -> {
                viewModel.chooseSelectionNavigationRoute()
            }
            is MainSideEffect.Shortcut.Handle -> {
                handleShortcutIntent(sideEffect.shortcutAction)
            }
            is MainSideEffect.Shortcut.ReportUsage -> {
                reportShortcutUsed(sideEffect.shortcutAction)
            }
        }
    }

    private fun consumeSharedImages() {
        if (requireActivity().intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        val uris = args.navArgImageSelection?.toList()
        if (uris != null) {
            requireActivity().intent.putExtra(INTENT_EXTRA_CONSUMED, true)
            viewModel.putImagesSelection(uris)
        }
    }

    private fun consumeShortcutDeepLink() {
        if (requireActivity().intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        val shortcut = args.navArgShortcut
        if (!shortcut.isNullOrEmpty()) {
            requireActivity().intent.putExtra(INTENT_EXTRA_CONSUMED, true)
            viewModel.reportShortcutUsed(shortcut)
            viewModel.handleShortcut(shortcut)
        }
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

    private fun reportShortcutUsed(action: String) {
        val sm = requireContext().getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager
        sm
            ?.manifestShortcuts
            ?.find { shortcutInfo -> shortcutInfo.intent?.action == action }
            ?.let { info -> sm.reportShortcutUsed(info.id) }
    }

    private fun setupResponsiveAppBarLayout() {
        setupToolbar(binding.toolbar, R.string.app_name)
        val menuProvider = object : MenuProvider {

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.content_paste -> {
                        viewModel.handlePasteImages()
                        true
                    }
                    R.id.action_delete_camera_images -> {
                        viewModel.handleDeleteCameraImages()
                        true
                    }
                    R.id.action_settings -> {
                        viewModel.handleSettings()
                        true
                    }
                    R.id.action_help -> {
                        viewModel.handleHelp()
                        true
                    }
                    else -> false
                }
            }
        }
        if (getWindowSizeClass().windowWidthSizeClass == WindowWidthSizeClass.EXPANDED) {
            requireActivity().addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.STARTED
            )
        } else {
            binding.bottomBar.addMenuProvider(
                menuProvider,
                viewLifecycleOwner,
                Lifecycle.State.STARTED
            )
            binding.bottomBarLayout.doOnLayout {
                val imageSourcesCoordinates = IntArray(size = 2) { 0 }
                val bottomBarCoordinates = IntArray(size = 2) { 0 }
                binding.imageSources.getLocationInWindow(imageSourcesCoordinates)
                binding.bottomBarLayout.getLocationInWindow(bottomBarCoordinates)
                val imageSourcesBottomY = imageSourcesCoordinates[1] +
                    binding.imageSources.measuredHeight
                if (imageSourcesBottomY > bottomBarCoordinates[1]) {
                    binding.bottomBarLayout.isVisible = false
                }
            }
            binding.imageSourcesReorder.apply {
                addIconAnimation(
                    animatedVectorDrawable = R.drawable.avd_drag_to_done_all,
                    animatedVectorDrawableInverse = R.drawable.avd_done_all_to_drag,
                    showAnimatedVectorDrawableCondition = {
                        !viewModel.container.stateFlow.value.isImageSourceReorderingEnabled
                    }
                )
                setOnClickListener {
                    viewModel.toggleImageSourceReorderingEnabled()
                }
            }
        }
        binding.bottomBarLayout.isVisible =
            getWindowSizeClass().windowWidthSizeClass != WindowWidthSizeClass.EXPANDED
    }

    private fun setupResponsiveImageSourceLayout() {
        binding.imageSources.apply {
            layoutManager = FlexboxLayoutManager(requireActivity()).apply {
                justifyContent = JustifyContent.CENTER
            }
            adapter = MainAdapter(
                listener = this@MainFragment,
                windowHeightSizeClass = getWindowSizeClass().windowHeightSizeClass
            )
            addItemDecoration(
                RecyclerViewMarginItemDecoration(
                    margin = resources.getDimension(R.dimen.spacing_micro).toInt()
                )
            )
            ItemTouchHelper(
                MainItemTouchHelperCallback(
                    callback = adapter as MainAdapter,
                    canMoveItem = {
                        viewModel.container.stateFlow.value.isImageSourceReorderingEnabled
                    }
                )
            ).attachTo(binding.imageSources)
        }
    }

    private fun setupResponsiveTitleLayout() {
        binding.title.gravity = when (getWindowSizeClass().windowWidthSizeClass) {
            WindowWidthSizeClass.COMPACT -> Gravity.END
            else -> Gravity.CENTER
        }
        binding.title.text = getString(
            R.string.choose_your_preferred_image_source_placeholder,
            getString(R.string.choose_your),
            getString(R.string.preferred_image_source)
        )
        binding.title.typeface = when (getWindowSizeClass().windowHeightSizeClass) {
            WindowHeightSizeClass.COMPACT -> {
                binding.title.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall
                )
                Typeface.DEFAULT_BOLD
            }
            WindowHeightSizeClass.MEDIUM -> {
                binding.title.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium
                )
                Typeface.DEFAULT_BOLD
            }
            else -> {
                binding.title.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_HeadlineLarge
                )
                Typeface.DEFAULT_BOLD
            }
        }
        binding.title.doOnLayout {
            val imageSourcesCoordinates = IntArray(size = 2) { 0 }
            val titleCoordinates = IntArray(size = 2) { 0 }
            binding.imageSources.getLocationInWindow(imageSourcesCoordinates)
            binding.title.getLocationInWindow(titleCoordinates)
            val titleBottomY = titleCoordinates[1] - binding.title.measuredHeight
            if (imageSourcesCoordinates[1] > titleBottomY) {
                binding.title.isVisible = false
            }
        }
    }
}
