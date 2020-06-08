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

import androidx.recyclerview.widget.RecyclerView
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.data.SharedPrefsRepository
import com.none.tom.exiferaser.databinding.FragmentMainCardViewBinding
import com.none.tom.exiferaser.ui.main.imagesource.MainAdapter.OnItemSelectedListener

class MainViewHolder(
    private val binding: FragmentMainCardViewBinding,
    private val listener: OnItemSelectedListener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.launch.setOnClickListener {
            with(itemView.context) {
                when (binding.method.text) {
                    getString(R.string.image) -> listener.onImageItemSelected()
                    getString(R.string.images) -> listener.onImagesItemSelected()
                    else -> listener.onImageDirectoryItemSelected()
                }
            }
        }
    }

    fun bindSelectImageItem() {
        binding.apply {
            image.setImageResource(R.drawable.ic_insert_photo)
            method.setText(R.string.image)
        }
        itemView.tag = SharedPrefsRepository.KEY_IMAGE_POSITION
    }

    fun bindSelectImagesItem() {
        binding.apply {
            image.setImageResource(R.drawable.ic_collections)
            method.setText(R.string.images)
        }
        itemView.tag = SharedPrefsRepository.KEY_IMAGES_POSITION
    }

    fun bindSelectImageDirectoryItem() {
        binding.apply {
            image.setImageResource(R.drawable.ic_photo_album)
            method.setText(R.string.image_directory)
        }
        itemView.tag = SharedPrefsRepository.KEY_IMAGE_DIRECTORY_POSITION
    }
}
