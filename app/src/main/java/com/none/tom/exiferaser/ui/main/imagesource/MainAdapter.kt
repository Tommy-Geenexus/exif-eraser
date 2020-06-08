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

package com.none.tom.exiferaser.ui.main.imagesource

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.none.tom.exiferaser.data.SharedPrefsRepository
import com.none.tom.exiferaser.databinding.FragmentMainCardViewBinding
import com.none.tom.exiferaser.ui.SharedViewModel

class MainAdapter(
    private val listener: OnItemSelectedListener,
    private val viewModel: SharedViewModel
) : RecyclerView.Adapter<MainViewHolder>(), SimpleItemTouchHelperCallback.OnRecyclerViewItemMoveListener {

    interface OnItemSelectedListener {
        fun onImageItemSelected()
        fun onImagesItemSelected()
        fun onImageDirectoryItemSelected()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MainViewHolder {
        return MainViewHolder(
            FragmentMainCardViewBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )
    }

    override fun getItemCount() = viewModel.getImageSourcePositions().size

    override fun onBindViewHolder(
        holder: MainViewHolder,
        position: Int
    ) {
        val imageSourcePositions = viewModel.getImageSourcePositions()
        when (position) {
            imageSourcePositions[SharedPrefsRepository.KEY_IMAGE_POSITION] -> holder.bindSelectImageItem()
            imageSourcePositions[SharedPrefsRepository.KEY_IMAGES_POSITION] -> holder.bindSelectImagesItem()
            else -> holder.bindSelectImageDirectoryItem()
        }
    }

    override fun onRecyclerViewItemMove(
        oldTag: String,
        newTag: String,
        oldPosition: Int,
        newPosition: Int
    ) {
        notifyItemMoved(oldPosition, newPosition)
        viewModel.updateImageSourcePositions(oldTag, newTag, oldPosition, newPosition)
    }
}
