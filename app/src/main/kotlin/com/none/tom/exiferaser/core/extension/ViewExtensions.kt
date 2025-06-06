/*
 * Copyright (c) 2024-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.core.extension

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import com.google.android.material.button.MaterialButton

inline fun MaterialButton.addIconAnimation(
    @DrawableRes animatedVectorDrawable: Int,
    @DrawableRes animatedVectorDrawableInverse: Int,
    crossinline showAnimatedVectorDrawableCondition: () -> Boolean
) {
    setIconResource(animatedVectorDrawable)
    val callback = object : Animatable2Compat.AnimationCallback() {

        override fun onAnimationEnd(drawableEnd: Drawable?) {
            AnimatedVectorDrawableCompat.unregisterAnimationCallback(drawableEnd, this)
            setIconResource(
                if (showAnimatedVectorDrawableCondition()) {
                    animatedVectorDrawable
                } else {
                    animatedVectorDrawableInverse
                }
            )
            AnimatedVectorDrawableCompat.registerAnimationCallback(icon, this)
        }
    }
    val lifecycle = findViewTreeLifecycleOwner()?.lifecycle
    lifecycle?.addObserver(
        object : LifecycleEventObserver {

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        AnimatedVectorDrawableCompat.registerAnimationCallback(
                            icon,
                            callback
                        )
                    }
                    Lifecycle.Event.ON_STOP -> {
                        if (icon is Animatable) {
                            (icon as Animatable).stop()
                        }
                        AnimatedVectorDrawableCompat.unregisterAnimationCallback(
                            icon,
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

fun ItemTouchHelper.attachTo(recyclerView: RecyclerView) {
    val lifecycle = recyclerView.findViewTreeLifecycleOwner()?.lifecycle
    lifecycle?.addObserver(
        object : LifecycleEventObserver {

            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
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
