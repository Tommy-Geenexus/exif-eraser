/*
 * Copyright (c) 2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.core.util

const val TOP_LEVEL_PACKAGE_NAME = "com.none.tom.exiferaser."

const val KEY_CAMERA_IMAGE_DELETE = TOP_LEVEL_PACKAGE_NAME + "CAM_CAMERA_IMAGE_DELETE"
const val KEY_DEFAULT_DISPLAY_NAME_SUFFIX = TOP_LEVEL_PACKAGE_NAME + "DEFAULT_DISPLAY_NAME_SUFFIX"
const val KEY_DEFAULT_NIGHT_MODE = TOP_LEVEL_PACKAGE_NAME + "DEFAULT_NIGHT_MODE"
const val KEY_DISPLAY_NAME = TOP_LEVEL_PACKAGE_NAME + "DISPLAY_NAME"
const val KEY_EXTENSION = TOP_LEVEL_PACKAGE_NAME + "EXTENSION"
const val KEY_IMAGE_METADATA_SNAPSHOT = TOP_LEVEL_PACKAGE_NAME + "IMAGE_META_SNAPSHOT"
const val KEY_IMAGE_PATH = TOP_LEVEL_PACKAGE_NAME + "IMAGE_PATH"
const val KEY_MIME_TYPE = TOP_LEVEL_PACKAGE_NAME + "MIME_TYPE"
const val KEY_NAV_DESTINATION_ID = TOP_LEVEL_PACKAGE_NAME + "NAV_DESTINATION_ID"
const val KEY_STATE_REPORT = TOP_LEVEL_PACKAGE_NAME + "REPORT"

const val IMAGE_PROCESSING_REPORT_IMAGES_MAX = 100

const val INDEX_DEFAULT_IMAGE_FILE = 0
const val INDEX_DEFAULT_IMAGE_FILES = 1
const val INDEX_DEFAULT_IMAGE_DIRECTORY = 2
const val INDEX_DEFAULT_CAMERA = 3
const val IMAGE_SOURCES_COUNT = 4
const val INDEX_START_ITEM_TYPE_IMAGE = 3
const val INDEX_START_ITEM_TYPE_UI = 8

const val INTENT_ACTION_CHOOSE_IMAGE = TOP_LEVEL_PACKAGE_NAME + "ACTION_CHOOSE_IMAGE"
const val INTENT_ACTION_CHOOSE_IMAGES = TOP_LEVEL_PACKAGE_NAME + "ACTION_CHOOSE_IMAGES"
const val INTENT_ACTION_CHOOSE_IMAGE_DIR = TOP_LEVEL_PACKAGE_NAME + "ACTION_CHOOSE_IMAGE_DIRECTORY"
const val INTENT_ACTION_LAUNCH_CAM = TOP_LEVEL_PACKAGE_NAME + "ACTION_LAUNCH_CAMERA"

const val INTENT_EXTRA_CONSUMED = TOP_LEVEL_PACKAGE_NAME + "EXTRA_CONSUMED"

const val ITEM_TYPE_FS = 1
const val ITEM_TYPE_IMAGE = 2
const val ITEM_TYPE_UI = 3

const val NAV_ARG_IMAGE_PROCESSING_SUMMARIES = "nav_arg_image_processing_summaries"
const val NAV_ARG_IMAGE_SELECTION = "nav_arg_image_selection"
const val NAV_ARG_SHORTCUT = "nav_arg_shortcut"

const val MIME_TYPE_IMAGE = "image/*"

// Do not change SETTINGS_* for backwards compatibility
const val SETTINGS_KEY_AUTO_DELETE = "auto_delete"
const val SETTINGS_KEY_DEFAULT_DISPLAY_NAME_SUFFIX = "default_display_name_suffix"
const val SETTINGS_KEY_DEFAULT_NIGHT_MODE = "night_mode"
const val SETTINGS_KEY_DEFAULT_OPEN_PATH = "default_path_open"
const val SETTINGS_KEY_DEFAULT_SAVE_PATH = "default_path_save"
const val SETTINGS_KEY_LEGACY_IMAGE_SELECTION = "legacy_image_selection"
const val SETTINGS_KEY_PRESERVE_ORIENTATION = "image_orientation"
const val SETTINGS_KEY_RANDOMIZE_FILE_NAMES = "randomize_file_names"
const val SETTINGS_KEY_SAVE_PATH_SELECTION_SKIP = "save_path_selection_skip"
const val SETTINGS_KEY_SHARE_BY_DEFAULT = "image_share_by_default"

const val TAG_ITEM_HELP_TRANSLATE = TOP_LEVEL_PACKAGE_NAME + "ITEM_HELP_TRANSLATE"
const val TAG_ITEM_FEEDBACK = TOP_LEVEL_PACKAGE_NAME + "ITEM_FEEDBACK"

const val URL_ISSUES = "https://github.com/Tommy-Geenexus/exif-eraser/issues"
const val URL_LOCALISATION =
    "https://crowdin.com/project/exif-eraser/invite?h=7814ed7d3215d8221577cc5b8aa661ee2190921"
