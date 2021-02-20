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

package com.none.tom.exiferaser

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis

fun FragmentActivity.setupToolbar(
    fragment: Fragment,
    toolbar: Toolbar,
    @StringRes titleRes: Int = 0
) {
    toolbar.setTitle(titleRes)
    (this as? AppCompatActivity)?.setSupportActionBar(toolbar)
    fragment
        .requireParentFragment()
        .childFragmentManager
        .backStackEntryCount
        .takeIf { count -> count > 0 }
        ?.let {
            toolbar.apply {
                setNavigationIcon(R.drawable.ic_arrow_back)
                setNavigationOnClickListener {
                    findNavController().navigateUp()
                }
            }
        }
}

fun Fragment.navigate(navDirections: NavDirections) {
    val navController = findNavController()
    val action = navController.currentDestination?.getAction(navDirections.actionId)
    if (action != null && action.destinationId != 0) {
        navController.navigate(navDirections)
    }
}

fun Fragment.setTransitions(
    transitionEnter: MaterialSharedAxis? = null,
    transitionExit: MaterialSharedAxis? = null,
    transitionReturn: MaterialSharedAxis? = null,
    transitionReenter: MaterialSharedAxis? = null
) {
    enterTransition = transitionEnter?.apply {
        duration = resources.getInteger(R.integer.anim_time_medium).toLong()
    }
    exitTransition = transitionExit?.apply {
        duration = resources.getInteger(R.integer.anim_time_medium).toLong()
    }
    returnTransition = transitionReturn?.apply {
        duration = resources.getInteger(R.integer.anim_time_medium).toLong()
    }
    reenterTransition = transitionReenter?.apply {
        duration = resources.getInteger(R.integer.anim_time_medium).toLong()
    }
}

fun Fragment.isActivityInMultiWindowMode(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && requireActivity().isInMultiWindowMode
}

fun Intent.getClipDataUris(): Array<Uri> {
    val resultSet = linkedSetOf<Uri>()
    val d = data
    if (d != null) {
        resultSet.add(d)
    }
    val c = clipData
    if (c == null && resultSet.isEmpty()) {
        return emptyArray()
    } else if (c?.description?.areMimeTypesSupported() == true) {
        c.addUrisToSet(resultSet)
    }
    return resultSet.toTypedArray()
}

fun ClipData.addUrisToSet(resultSet: LinkedHashSet<Uri>) {
    for (i in 0 until itemCount) {
        val uri = getItemAt(i)?.uri
        if (uri != null) {
            resultSet.add(uri)
        }
    }
}

fun ClipDescription.areMimeTypesSupported(): Boolean {
    for (i in 0 until mimeTypeCount) {
        val mimeType = getMimeType(i)
        if (mimeType != MIME_TYPE_IMAGE && !supportedMimeTypes.contains(mimeType)) {
            return false
        }
    }
    return true
}

fun Uri?.isNotNullOrEmpty() = this != null && isNotEmpty()

fun Uri.isNotEmpty() = this != Uri.EMPTY
