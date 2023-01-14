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

package com.none.tom.exiferaser.main

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

class MainItemTouchHelperCallback(
    private val callback: OnRecyclerViewItemMoveListener,
    private val canMoveItem: () -> Boolean
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or
        ItemTouchHelper.DOWN or
        ItemTouchHelper.START or
        ItemTouchHelper.END,
    0
) {

    interface OnRecyclerViewItemMoveListener {
        fun onRecyclerViewItemMove(
            oldIndex: Int,
            newIndex: Int
        )
    }

    @Suppress("ComplexCondition")
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        with(viewHolder.itemView) {
            val topY = top + dY
            val bottomY = topY + height
            val startX = left + dX
            val endX = startX + width
            var offsetX = dX
            var offsetY = dY
            if (startX < 0) {
                offsetX = 0f
            }
            val width = recyclerView.width.toFloat()
            if (endX > width) {
                offsetX = 0f
            }
            if (topY < 0) {
                offsetY = 0f
            }
            if (bottomY > recyclerView.height) {
                offsetY = 0f
            }
            MainItemTouchUiUtil.onDraw(
                c,
                recyclerView,
                viewHolder.itemView,
                offsetX,
                offsetY,
                actionState,
                isCurrentlyActive
            )
        }
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder,
        target: ViewHolder
    ): Boolean {
        return if (canMoveItem()) {
            callback.onRecyclerViewItemMove(
                viewHolder.absoluteAdapterPosition,
                target.absoluteAdapterPosition
            )
            true
        } else {
            false
        }
    }

    @Suppress("EmptyFunctionBlock")
    override fun onSwiped(
        viewHolder: ViewHolder,
        direction: Int
    ) {
    }
}
