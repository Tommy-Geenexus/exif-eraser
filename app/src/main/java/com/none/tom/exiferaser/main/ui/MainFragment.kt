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

import android.content.Context
import android.content.pm.ShortcutManager
import android.graphics.Typeface
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.MenuProvider
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.draganddrop.DropHelper
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.none.tom.exiferaser.BaseFragment
import com.none.tom.exiferaser.ExifEraserActivity
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGE
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGES
import com.none.tom.exiferaser.INTENT_ACTION_CHOOSE_IMAGE_DIR
import com.none.tom.exiferaser.INTENT_EXTRA_CONSUMED
import com.none.tom.exiferaser.PROGRESS_MIN
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.WindowSizeClass
import com.none.tom.exiferaser.databinding.FragmentMainBinding
import com.none.tom.exiferaser.getClipImages
import com.none.tom.exiferaser.main.MainContentReceiver
import com.none.tom.exiferaser.main.MainItemTouchHelperCallback
import com.none.tom.exiferaser.main.PickMultipleVisualMedia2
import com.none.tom.exiferaser.main.PickVisualMedia2
import com.none.tom.exiferaser.main.RecyclerViewItemDecoration
import com.none.tom.exiferaser.main.TakePicture
import com.none.tom.exiferaser.main.business.MainSideEffect
import com.none.tom.exiferaser.main.business.MainState
import com.none.tom.exiferaser.main.business.MainViewModel
import com.none.tom.exiferaser.supportedMimeTypes
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
        PickVisualMedia2(
            usePhotoPicker = { !viewModel.container.stateFlow.value.legacyImageSelection }
        )
    ) { result ->
        viewModel.putImageSelection(
            uri = result,
            canReorderImageSources = canReorderImageSources()
        )
    }
    private val chooseImages = registerForActivityResult(
        PickMultipleVisualMedia2(
            usePhotoPicker = { !viewModel.container.stateFlow.value.legacyImageSelection }
        )
    ) { result ->
        viewModel.putImagesSelection(
            uris = result,
            canReorderImageSources = canReorderImageSources()
        )
    }
    private val chooseImageDirectory = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { result ->
        viewModel.putImageDirectorySelection(
            uri = result,
            canReorderImageSources = canReorderImageSources()
        )
    }
    private val launchCamera = registerForActivityResult(TakePicture()) { result ->
        viewModel.putImageSelection(
            uri = result,
            fromCamera = true,
            canReorderImageSources = canReorderImageSources()
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupResponsiveAppBarLayout()
        setupResponsiveToolbarTitleLayout()
        setupResponsiveImageSourceLayout()
        DropHelper.configureView(
            requireActivity(),
            binding.layout,
            supportedMimeTypes,
            MainContentReceiver(onUrisReceived = { uris -> viewModel.handleReceivedImages(uris) })
        )
        setFragmentResultListener(
            DeleteCameraImagesFragment.KEY_CAM_IMG_DELETE
        ) { _, bundle: Bundle ->
            if (bundle.getBoolean(DeleteCameraImagesFragment.KEY_CAM_IMG_DELETE)) {
                viewModel.deleteCameraImages()
            }
        }
        setFragmentResultListener(ExifEraserActivity.KEY_UPDATE_FAILED) { _, _ ->
            viewModel.handleFlexibleUpdateFailure()
        }
        setFragmentResultListener(
            ExifEraserActivity.KEY_UPDATE_IN_PROGRESS
        ) { key: String, args: Bundle ->
            viewModel.handleFlexibleUpdateInProgress(
                progress = args.getInt(key, PROGRESS_MIN),
                notify = true
            )
        }
        setFragmentResultListener(ExifEraserActivity.KEY_UPDATE_READY_TO_INSTALL) { _, _ ->
            viewModel.handleFlexibleUpdateReadyToInstall()
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
                        exitTransition = if (nextNavDestinationId == R.id.fragment_selection ||
                            nextNavDestinationId == R.id.fragment_save_path
                        ) {
                            MaterialSharedAxis(MaterialSharedAxis.X, true)
                        } else {
                            MaterialSharedAxis(MaterialSharedAxis.Z, true)
                        }
                        reenterTransition = if (prevNavDestinationId == R.id.fragment_selection ||
                            prevNavDestinationId == R.id.fragment_save_path
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
        viewModel.chooseImage(canReorderImageSources())
    }

    override fun onImagesItemSelected() {
        viewModel.chooseImages(canReorderImageSources())
    }

    override fun onImageDirectoryItemSelected() {
        viewModel.chooseImageDirectory(canReorderImageSources())
    }

    override fun onCameraItemSelected() {
        viewModel.launchCamera(
            fileProviderPackage = requireContext().getString(R.string.file_provider_package),
            displayName = System.currentTimeMillis().toString(),
            canReorderImageSources = canReorderImageSources()
        )
    }

    override fun onImageSourceMoved(
        imageSources: MutableList<AnyMessage>,
        oldIndex: Int,
        newIndex: Int
    ) {
        viewModel.reorderImageSources(imageSources, oldIndex, newIndex)
    }

    private fun renderState(state: MainState) {
        binding.progress.isVisible = state.loading
        (binding.imageSources.adapter as? MainAdapter)?.submitList(state.imageSources)
    }

    private fun handleSideEffect(sideEffect: MainSideEffect) {
        when (sideEffect) {
            is MainSideEffect.ChooseImage -> {
                chooseImage.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            is MainSideEffect.ChooseImages -> {
                chooseImages.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
            is MainSideEffect.ChooseImageDirectory -> {
                chooseImageDirectory.launch(sideEffect.openPath)
            }
            is MainSideEffect.DefaultNightMode -> {
                AppCompatDelegate.setDefaultNightMode(sideEffect.value)
            }
            is MainSideEffect.DeleteCameraImages -> {
                navigate(MainFragmentDirections.mainToDeleteCameraImages())
            }
            is MainSideEffect.ExternalPicturesDeleted -> {
                showSnackBar(
                    anchor = if (binding.bottomBarLayout.isVisible) {
                        binding.bottomBarLayout
                    } else {
                        null
                    },
                    msg = getString(
                        if (sideEffect.success) {
                            R.string.delete_camera_images_success
                        } else {
                            R.string.delete_camera_images_failed
                        }
                    )
                )
            }
            MainSideEffect.FlexibleUpdateReadyToInstall -> {
                showSnackBar(
                    anchor = if (binding.bottomBarLayout.isVisible) {
                        binding.bottomBarLayout
                    } else {
                        null
                    },
                    msg = getString(R.string.app_update_ready),
                    actionMsg = R.string.install,
                    onActionClick = {
                        viewModel.completeFlexibleUpdate()
                    },
                    length = Snackbar.LENGTH_INDEFINITE
                )
            }
            MainSideEffect.FlexibleUpdateFailed -> {
                showSnackBar(
                    anchor = if (binding.bottomBarLayout.isVisible) {
                        binding.bottomBarLayout
                    } else {
                        null
                    },
                    msg = getString(R.string.app_update_failed)
                )
            }
            is MainSideEffect.FlexibleUpdateInProgress -> {
                showSnackBar(
                    anchor = if (binding.bottomBarLayout.isVisible) {
                        binding.bottomBarLayout
                    } else {
                        null
                    },
                    msg = getString(R.string.app_update_download),
                    length = Snackbar.LENGTH_INDEFINITE
                )
            }
            MainSideEffect.ImageSourcesReadComplete -> {
                consumeSharedImages()
                consumeShortcutDeepLink()
            }
            is MainSideEffect.LaunchCamera -> {
                launchCamera.launch(sideEffect.fileProviderImagePath)
            }
            is MainSideEffect.NavigateToHelp -> {
                navigate(MainFragmentDirections.mainToHelp())
            }
            is MainSideEffect.NavigateToSelection -> {
                navigate(MainFragmentDirections.mainToSelection(savePath = sideEffect.savePath))
            }
            is MainSideEffect.NavigateToSelectionSavePath -> {
                navigate(MainFragmentDirections.mainToSelectionSavePath())
            }
            is MainSideEffect.NavigateToSettings -> {
                navigate(MainFragmentDirections.mainToSettings())
            }
            is MainSideEffect.PasteImages -> {
                viewModel.handleReceivedImages(sideEffect.uris)
            }
            is MainSideEffect.PasteImagesNone -> {
                showSnackBar(
                    anchor = if (binding.bottomBarLayout.isVisible) {
                        binding.bottomBarLayout
                    } else {
                        null
                    },
                    msg = getString(R.string.clipboard_content_unsupported)
                )
            }
            is MainSideEffect.ReceivedImage -> {
                viewModel.putImageSelection(
                    uri = sideEffect.uri,
                    canReorderImageSources = canReorderImageSources()
                )
            }
            is MainSideEffect.ReceivedImages -> {
                viewModel.putImagesSelection(
                    uris = sideEffect.uris,
                    canReorderImageSources = canReorderImageSources()
                )
            }
            is MainSideEffect.Shortcut.Handle -> {
                handleShortcutIntent(sideEffect.shortcutAction)
            }
            is MainSideEffect.Shortcut.ReportUsage -> {
                reportShortcutUsed(sideEffect.shortcutAction)
            }
        }
    }

    private fun canReorderImageSources(): Boolean {
        return binding.imageSourcesReorder.tag == R.drawable.avd_done_all_to_drag
    }

    private fun consumeSharedImages() {
        if (requireActivity().intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        val uri = args.imageSelection
        val uris = args.imagesSelection?.toList()
        if (uri != null) {
            requireActivity().intent.putExtra(INTENT_EXTRA_CONSUMED, true)
            viewModel.putImageSelection(uri)
        } else if (uris != null) {
            requireActivity().intent.putExtra(INTENT_EXTRA_CONSUMED, true)
            viewModel.putImagesSelection(uris)
        }
    }

    private fun consumeShortcutDeepLink() {
        if (requireActivity().intent.hasExtra(INTENT_EXTRA_CONSUMED)) {
            return
        }
        val shortcut = args.shortcut
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
        val sm = (requireContext().getSystemService(Context.SHORTCUT_SERVICE) as? ShortcutManager)
        val info = sm?.manifestShortcuts?.find { shortcutInfo ->
            shortcutInfo.intent?.action == action
        }
        if (info != null) {
            sm.reportShortcutUsed(info.id)
        }
    }

    private fun setupResponsiveAppBarLayout() {
        setupToolbar(toolbar = binding.toolbar)
        val menuProvider = object : MenuProvider {

            override fun onCreateMenu(
                menu: Menu,
                menuInflater: MenuInflater
            ) {
                menuInflater.inflate(R.menu.menu_main, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.content_paste -> {
                        viewModel.handlePasteImages(requireContext().getClipImages())
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
        if (getWindowSizeClassWidth() == WindowSizeClass.Expanded) {
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
            binding.imageSourcesReorder.addIconAnimation(
                animatedVectorDrawable = R.drawable.avd_drag_to_done_all,
                animatedVectorDrawableInverse = R.drawable.avd_done_all_to_drag
            )
            binding.imageSourcesReorder.setOnClickListener {
                (binding.imageSourcesReorder.drawable as? Animatable)?.start()
                if (binding.imageSourcesReorder.tag == R.drawable.avd_done_all_to_drag) {
                    val imageSources = (binding.imageSources.adapter as? MainAdapter)?.currentList
                    if (imageSources != null) {
                        viewModel.putImageSources(imageSources)
                    }
                }
            }
        }
        binding.bottomBarLayout.isVisible = getWindowSizeClassWidth() != WindowSizeClass.Expanded
    }

    private fun setupResponsiveImageSourceLayout() {
        binding.imageSources.apply {
            layoutManager = FlexboxLayoutManager(requireActivity()).apply {
                justifyContent = JustifyContent.CENTER
            }
            adapter = MainAdapter(
                listener = this@MainFragment,
                windowSizeClass = getWindowSizeClassHeight()
            )
            addItemDecoration(
                RecyclerViewItemDecoration(
                    margin = resources.getDimension(R.dimen.spacing_micro).toInt()
                )
            )
            ItemTouchHelper(
                MainItemTouchHelperCallback(
                    callback = adapter as MainAdapter,
                    canMoveItem = { canReorderImageSources() }
                )
            ).attachTo(binding.imageSources)
        }
    }

    private fun setupResponsiveToolbarTitleLayout() {
        binding.title.gravity = when (getWindowSizeClassWidth()) {
            WindowSizeClass.Unspecified,
            WindowSizeClass.Compact -> Gravity.END
            WindowSizeClass.Medium,
            WindowSizeClass.Expanded -> Gravity.CENTER
        }
        binding.title.text = getString(
            R.string.choose_your_preferred_image_source_placeholder,
            getString(R.string.choose_your),
            getString(R.string.preferred_image_source)
        )
        binding.title.typeface = when (getWindowSizeClassHeight()) {
            WindowSizeClass.Compact -> {
                binding.title.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_HeadlineSmall
                )
                Typeface.DEFAULT_BOLD
            }
            WindowSizeClass.Unspecified,
            WindowSizeClass.Medium -> {
                binding.title.setTextAppearance(
                    com.google.android.material.R.style.TextAppearance_Material3_HeadlineMedium
                )
                Typeface.DEFAULT_BOLD
            }
            WindowSizeClass.Expanded -> {
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

    private fun showSnackBar(
        anchor: View? = null,
        msg: String,
        @StringRes actionMsg: Int = 0,
        onActionClick: View.OnClickListener? = null,
        length: Int = Snackbar.LENGTH_SHORT
    ) {
        val view = requireView()
        var backingSnackbar: Snackbar? = Snackbar
            .make(view, msg, length)
            .setAnchorView(anchor)
            .setAction(if (actionMsg != 0) getString(actionMsg) else null, onActionClick)
        val snackbar = backingSnackbar
        val lifecycle = view.findViewTreeLifecycleOwner()?.lifecycle
        if (snackbar != null && lifecycle != null) {
            lifecycle.addObserver(
                object : LifecycleEventObserver {

                    override fun onStateChanged(
                        source: LifecycleOwner,
                        event: Lifecycle.Event
                    ) {
                        when (event) {
                            Lifecycle.Event.ON_STOP -> {
                                lifecycle.removeObserver(this)
                                snackbar.dismiss()
                                backingSnackbar = null
                            }
                            else -> {
                            }
                        }
                    }
                }
            )
            snackbar.show()
        } else {
            backingSnackbar = null
        }
    }

    private fun ItemTouchHelper.attachTo(recyclerView: RecyclerView) {
        val lifecycle = recyclerView.findViewTreeLifecycleOwner()?.lifecycle
        lifecycle?.addObserver(
            object : LifecycleEventObserver {

                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            attachToRecyclerView(recyclerView)
                        }
                        Lifecycle.Event.ON_STOP -> {
                            attachToRecyclerView(null)
                        }
                        Lifecycle.Event.ON_DESTROY -> {
                            lifecycle.removeObserver(this)
                        }
                        else -> {
                        }
                    }
                }
            }
        )
    }

    private fun FloatingActionButton.addIconAnimation(
        @DrawableRes animatedVectorDrawable: Int,
        @DrawableRes animatedVectorDrawableInverse: Int
    ) {
        setImageResource(animatedVectorDrawable)
        tag = animatedVectorDrawable
        val callback = object : Animatable2Compat.AnimationCallback() {

            override fun onAnimationEnd(drawableEnd: Drawable?) {
                AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawableEnd, this)
                tag = if (tag == animatedVectorDrawableInverse) {
                    setImageResource(animatedVectorDrawable)
                    animatedVectorDrawable
                } else {
                    setImageResource(animatedVectorDrawableInverse)
                    animatedVectorDrawableInverse
                }
                AnimatedVectorDrawableCompat.registerAnimationCallback(drawable, this)
            }
        }
        val lifecycle = findViewTreeLifecycleOwner()?.lifecycle
        lifecycle?.addObserver(
            object : LifecycleEventObserver {

                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event
                ) {
                    when (event) {
                        Lifecycle.Event.ON_START -> {
                            AnimatedVectorDrawableCompat.registerAnimationCallback(
                                drawable,
                                callback
                            )
                        }
                        Lifecycle.Event.ON_STOP -> {
                            if (drawable is Animatable) {
                                (drawable as Animatable).stop()
                            }
                            AnimatedVectorDrawableCompat.unregisterAnimationCallback(
                                drawable,
                                callback
                            )
                        }
                        Lifecycle.Event.ON_DESTROY -> {
                            lifecycle.removeObserver(this)
                        }
                        else -> {
                        }
                    }
                }
            }
        )
    }
}
