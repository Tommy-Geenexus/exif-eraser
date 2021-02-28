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

import android.content.res.ColorStateList
import android.view.DragEvent
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.core.view.ContentInfoCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.areMimeTypesSupported
import com.none.tom.exiferaser.databinding.FragmentMainCardViewBinding
import com.none.tom.exiferaser.main.MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_DEFAULT
import com.none.tom.exiferaser.main.MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_MAX
import com.none.tom.exiferaser.main.MainContentReceiver
import com.none.tom.exiferaser.main.canReceiveContent
import kotlin.contracts.ExperimentalContracts
import kotlin.math.roundToInt

@ExperimentalContracts
class MainViewHolder(
    private val binding: FragmentMainCardViewBinding,
    private val listener: MainAdapter.Listener,
    private val receiver: MainContentReceiver
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.imageSource.setOnClickListener {
            with(itemView.context) {
                when (binding.method.text) {
                    getString(R.string.image_file) -> listener.onImageItemSelected()
                    getString(R.string.image_files) -> listener.onImagesItemSelected()
                    getString(R.string.image_directory) -> listener.onImageDirectoryItemSelected()
                    else -> listener.onCameraItemSelected()
                }
            }
        }
        setupDragAndDrop()
    }

    fun bindSelectImageItem(screenHeightRatio: Float) {
        binding.apply {
            method.setText(R.string.image_file)
            imageSource.tag = binding.method.text
        }
        bindItemByScreenHeightRatio(
            screenHeightRatio = screenHeightRatio,
            drawableRes = R.drawable.ic_image,
            drawableResXLarge = R.drawable.ic_image_xlarge
        )
    }

    fun bindSelectImagesItem(screenHeightRatio: Float) {
        binding.apply {
            method.setText(R.string.image_files)
            imageSource.tag = method.text
        }
        bindItemByScreenHeightRatio(
            screenHeightRatio = screenHeightRatio,
            drawableRes = R.drawable.ic_photo_library,
            drawableResXLarge = R.drawable.ic_photo_library_xlarge
        )
    }

    fun bindSelectImageDirectoryItem(screenHeightRatio: Float) {
        binding.apply {
            method.setText(R.string.image_directory)
            imageSource.tag = method.text
        }
        bindItemByScreenHeightRatio(
            screenHeightRatio = screenHeightRatio,
            drawableRes = R.drawable.ic_photo_album,
            drawableResXLarge = R.drawable.ic_photo_album_xlarge
        )
    }

    fun bindCameraItem(screenHeightRatio: Float) {
        binding.apply {
            method.setText(R.string.camera)
            imageSource.tag = method.text
        }
        bindItemByScreenHeightRatio(
            screenHeightRatio = screenHeightRatio,
            drawableRes = R.drawable.ic_camera,
            drawableResXLarge = R.drawable.ic_camera_xlarge
        )
    }

    private fun setupDragAndDrop() {
        binding.imageSource.setOnDragListener { view, event ->
            val isDragAndDropSupported = view.canReceiveContent()
            val alphaLow = (MaterialColors.ALPHA_LOW * 255).roundToInt()
            val colorAccept = ColorStateList.valueOf(
                MaterialColors.compositeARGBWithAlpha(
                    MaterialColors.getColor(view, R.attr.colorAccept),
                    alphaLow
                )
            )
            val colorOk = ColorStateList.valueOf(
                MaterialColors.compositeARGBWithAlpha(
                    MaterialColors.getColor(view, R.attr.colorOk),
                    alphaLow
                )
            )
            val colorError = ColorStateList.valueOf(
                MaterialColors.compositeARGBWithAlpha(
                    MaterialColors.getColor(view, R.attr.colorError),
                    alphaLow
                )
            )
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    if (event.clipDescription.areMimeTypesSupported()) {
                        binding.imageSource.setCardForegroundColor(
                            if (isDragAndDropSupported) colorAccept else colorError
                        )
                        true
                    } else {
                        false
                    }
                }
                DragEvent.ACTION_DRAG_ENTERED -> {
                    if (isDragAndDropSupported) {
                        binding.imageSource.setCardForegroundColor(colorOk)
                    }
                    isDragAndDropSupported
                }
                DragEvent.ACTION_DRAG_LOCATION -> {
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> {
                    if (isDragAndDropSupported) {
                        binding.imageSource.setCardForegroundColor(colorAccept)
                    }
                    true
                }
                DragEvent.ACTION_DROP -> {
                    receiver.onReceiveContent(
                        view,
                        ContentInfoCompat.Builder(
                            event.clipData,
                            ContentInfoCompat.SOURCE_DRAG_AND_DROP
                        )
                            .setExtras(bundleOf(DragEvent.ACTION_DROP.toString() to event))
                            .build()
                    )
                    true
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    binding.imageSource.setCardForegroundColor(null)
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

    private fun bindItemByScreenHeightRatio(
        screenHeightRatio: Float,
        @DrawableRes drawableRes: Int,
        @DrawableRes drawableResXLarge: Int
    ) {
        val margin: Int
        if (screenHeightRatio <= MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_MAX) {
            margin = itemView.context.resources.getDimension(R.dimen.spacing_small).toInt()
            binding.image.setImageResource(drawableRes)
        } else if (screenHeightRatio > MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_MAX &&
            screenHeightRatio <= MODE_MULTI_WINDOW_ACTIVITY_COLLAPSED_DEFAULT
        ) {
            margin = itemView.context.resources.getDimension(R.dimen.spacing_normal).toInt()
            binding.image.setImageResource(drawableRes)
        } else {
            margin = itemView.context.resources.getDimension(R.dimen.spacing_normal).toInt()
            binding.image.setImageResource(drawableResXLarge)
        }
        (binding.image.layoutParams as? ConstraintLayout.LayoutParams)
            ?.setMargins(margin, margin, margin, margin)
        (binding.method.layoutParams as? ConstraintLayout.LayoutParams)
            ?.setMargins(margin, margin, margin, margin)
    }
}
