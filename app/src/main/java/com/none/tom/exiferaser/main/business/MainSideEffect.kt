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

package com.none.tom.exiferaser.main.business

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.IntRange
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.PROGRESS_MIN
import kotlinx.parcelize.Parcelize

sealed class MainSideEffect : Parcelable {

    @Parcelize
    data class ChooseImage(
        val openPath: Uri
    ) : MainSideEffect()

    @Parcelize
    data class ChooseImages(
        val openPath: Uri
    ) : MainSideEffect()

    @Parcelize
    data class ChooseImageDirectory(
        val openPath: Uri
    ) : MainSideEffect()

    @Parcelize
    data class DefaultNightMode(
        val value: Int
    ) : MainSideEffect()

    @Parcelize
    object DeleteCameraImages : MainSideEffect()

    @Parcelize
    data class ExternalPicturesDeleted(
        val success: Boolean
    ) : MainSideEffect()

    @Parcelize
    data class FlexibleUpdateInProgress(
        @IntRange(from = PROGRESS_MIN.toLong(), to = PROGRESS_MAX.toLong()) val progress: Int
    ) : MainSideEffect()

    @Parcelize
    object FlexibleUpdateReadyToInstall : MainSideEffect()

    @Parcelize
    object FlexibleUpdateFailed : MainSideEffect()

    @Parcelize
    object ImageSourcesReadComplete : MainSideEffect()

    @Parcelize
    data class LaunchCamera(
        val fileProviderImagePath: Uri
    ) : MainSideEffect()

    @Parcelize
    data class NavigateToSelection(
        val savePath: Uri
    ) : MainSideEffect()

    @Parcelize
    object NavigateToSelectionSavePath : MainSideEffect()

    @Parcelize
    object NavigateToSettings : MainSideEffect()

    @Parcelize
    object NavigateToHelp : MainSideEffect()

    @Parcelize
    data class PasteImages(
        val uris: List<Uri>
    ) : MainSideEffect()

    @Parcelize
    object PasteImagesNone : MainSideEffect()

    @Parcelize
    data class ReceivedImage(
        val uri: Uri
    ) : MainSideEffect()

    @Parcelize
    data class ReceivedImages(
        val uris: List<Uri>
    ) : MainSideEffect()

    sealed class Shortcut : MainSideEffect() {

        @Parcelize
        data class Handle(
            val shortcutAction: String
        ) : MainSideEffect()

        @Parcelize
        data class ReportUsage(
            val shortcutAction: String
        ) : MainSideEffect()
    }
}
