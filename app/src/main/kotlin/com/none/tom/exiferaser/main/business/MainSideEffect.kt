/*
 * Copyright (c) 2018-2024, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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
import com.squareup.wire.AnyMessage
import kotlinx.parcelize.Parcelize

sealed class MainSideEffect : Parcelable {

    @Parcelize
    data class DefaultNightMode(
        val value: Int
    ) : MainSideEffect()

    @Parcelize
    data class ImageSourceReordering(
        val imageSources: List<AnyMessage>,
        val isEnabled: Boolean
    ) : MainSideEffect()

    sealed class ImageSources : MainSideEffect() {

        @Parcelize
        data class Image(
            val supportedMimeTypes: Array<String>,
            val isLegacyImageSelectionEnabled: Boolean
        ) : ImageSources() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Image

                if (!supportedMimeTypes.contentEquals(other.supportedMimeTypes)) {
                    return false
                }
                if (isLegacyImageSelectionEnabled != other.isLegacyImageSelectionEnabled) {
                    return false
                }

                return true
            }

            override fun hashCode(): Int {
                var result = supportedMimeTypes.contentHashCode()
                result = 31 * result + isLegacyImageSelectionEnabled.hashCode()
                return result
            }
        }

        @Parcelize
        data class Images(
            val supportedMimeTypes: Array<String>,
            val isLegacyImageSelectionEnabled: Boolean
        ) : ImageSources() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Images

                if (!supportedMimeTypes.contentEquals(other.supportedMimeTypes)) {
                    return false
                }
                if (isLegacyImageSelectionEnabled != other.isLegacyImageSelectionEnabled) {
                    return false
                }

                return true
            }

            override fun hashCode(): Int {
                var result = supportedMimeTypes.contentHashCode()
                result = 31 * result + isLegacyImageSelectionEnabled.hashCode()
                return result
            }
        }

        sealed class ImageDirectory : ImageSources() {

            @Parcelize
            data object Failure : ImageDirectory()

            @Parcelize
            data class Success(
                val uri: Uri
            ) : ImageDirectory()
        }

        sealed class Camera : ImageSources() {

            @Parcelize
            data object Failure : Put()

            @Parcelize
            data class Success(
                val uri: Uri
            ) : Put()
        }

        @Parcelize
        data object Initialized : MainSideEffect()

        sealed class Put : ImageSources() {

            @Parcelize
            data object Failure : Put()

            @Parcelize
            data object Success : Put()
        }
    }

    sealed class Images : MainSideEffect() {

        sealed class Delete : Images() {

            @Parcelize
            data object Failure : Delete()

            @Parcelize
            data object Success : Delete()
        }

        sealed class Paste : Images() {

            @Parcelize
            data object Failure : Paste()

            @Parcelize
            data class Success(
                val uris: List<Uri>
            ) : Paste()
        }

        sealed class Received : Images() {

            @Parcelize
            data object None : Received()

            @Parcelize
            data class Single(
                val uri: Uri
            ) : Received()

            @Parcelize
            data class Multiple(
                val uris: List<Uri>
            ) : Received()
        }
    }

    sealed class Navigate : MainSideEffect() {

        @Parcelize
        data object ToDeleteCameraImages : Navigate()

        @Parcelize
        data object ToHelp : Navigate()

        @Parcelize
        data class ToSelection(
            val savePath: Uri
        ) : Navigate()

        @Parcelize
        data object ToSelectionSavePath : Navigate()

        @Parcelize
        data object ToSettings : Navigate()
    }

    sealed class Selection : MainSideEffect() {

        sealed class Image : Selection() {

            @Parcelize
            data object Failure : Image()

            @Parcelize
            data class Success(
                val isFromCamera: Boolean
            ) : Image()
        }

        sealed class Images : Selection() {

            @Parcelize
            data object Failure : Images()

            @Parcelize
            data object Success : Images()
        }

        sealed class ImageDirectory : Selection() {

            @Parcelize
            data object Failure : ImageDirectory()

            @Parcelize
            data object Success : ImageDirectory()
        }
    }

    sealed class Shortcut : MainSideEffect() {

        @Parcelize
        data class Handle(
            val shortcutAction: String
        ) : Shortcut()

        @Parcelize
        data class ReportUsage(
            val shortcutAction: String
        ) : Shortcut()
    }
}
