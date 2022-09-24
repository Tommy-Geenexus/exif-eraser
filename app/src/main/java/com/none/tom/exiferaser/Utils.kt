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

@file:Suppress("SameReturnValue")

package com.none.tom.exiferaser

const val TOP_LEVEL_PACKAGE_NAME = "com.none.tom.exiferaser."

const val INTENT_ACTION_CHOOSE_IMAGE = TOP_LEVEL_PACKAGE_NAME + "ACTION_CHOOSE_IMAGE"
const val INTENT_ACTION_CHOOSE_IMAGES = TOP_LEVEL_PACKAGE_NAME + "ACTION_CHOOSE_IMAGES"
const val INTENT_ACTION_CHOOSE_IMAGE_DIR =
    TOP_LEVEL_PACKAGE_NAME + "ACTION_CHOOSE_IMAGE_DIRECTORY"
const val INTENT_ACTION_LAUNCH_CAM = TOP_LEVEL_PACKAGE_NAME + "ACTION_LAUNCH_CAMERA"

const val INTENT_EXTRA_CONSUMED = TOP_LEVEL_PACKAGE_NAME + "EXTRA_CONSUMED"

const val URL_ISSUES = "https://github.com/Tommy-Geenexus/exif-eraser/issues"
const val URL_LOCALISATION = "https://tomgappdev.oneskyapp.com/collaboration/project?id=375350"

const val MIME_TYPE_IMAGE = "image/*"
const val MIME_TYPE_JPEG = "image/jpeg"
const val MIME_TYPE_PNG = "image/png"
const val MIME_TYPE_WEBP = "image/webp"

const val EXTENSION_JPEG = ".jpg"
const val EXTENSION_PNG = ".png"
const val EXTENSION_WEBP = ".webp"

val String.Companion.Empty: String get() = ""
