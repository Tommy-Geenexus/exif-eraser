/*
 * Copyright (c) 2018-2025, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

package com.none.tom.exiferaser.settings.business

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class SettingsSideEffect : Parcelable {

    sealed class AutoDelete : SettingsSideEffect() {

        @Parcelize
        data object Failure : AutoDelete()

        @Parcelize
        data object Success : AutoDelete()
    }

    sealed class DefaultDisplayNameSuffix : SettingsSideEffect() {

        @Parcelize
        data object Failure : DefaultDisplayNameSuffix()

        @Parcelize
        data object Success : DefaultDisplayNameSuffix()
    }

    sealed class DefaultNightMode : SettingsSideEffect() {

        @Parcelize
        data object Failure : DefaultNightMode()

        @Parcelize
        data object Success : DefaultNightMode()
    }

    sealed class DefaultOpenPath : SettingsSideEffect() {

        sealed class Clear : DefaultOpenPath() {

            @Parcelize
            data object Failure : Clear()

            @Parcelize
            data object Success : Clear()
        }

        sealed class Select : DefaultOpenPath() {

            @Parcelize
            data object Failure : Select()

            @Parcelize
            data class Success(val uri: Uri) : Select()
        }

        sealed class Store : DefaultOpenPath() {

            @Parcelize
            data object Failure : Store()

            @Parcelize
            data object Success : Store()
        }
    }

    sealed class DefaultSavePath : SettingsSideEffect() {

        sealed class Clear : DefaultSavePath() {

            @Parcelize
            data object Failure : Clear()

            @Parcelize
            data object Success : Clear()
        }

        sealed class Select : DefaultSavePath() {

            @Parcelize
            data object Failure : Select()

            @Parcelize
            data class Success(val uri: Uri) : Select()
        }

        sealed class SelectionSkip : DefaultSavePath() {

            @Parcelize
            data object Failure : Select()

            @Parcelize
            data object Success : Select()
        }

        sealed class Store : DefaultSavePath() {

            @Parcelize
            data object Failure : Store()

            @Parcelize
            data object Success : Store()
        }
    }

    sealed class LegacyImageSelection : SettingsSideEffect() {

        @Parcelize
        data object Failure : LegacyImageSelection()

        @Parcelize
        data object Success : LegacyImageSelection()
    }

    sealed class Navigate : SettingsSideEffect() {

        @Parcelize
        data class ToDefaultDisplayNameSuffix(val defaultDisplayNameSuffix: String) : Navigate()

        @Parcelize
        data class ToDefaultNightMode(val defaultNightMode: Int) : Navigate()
    }

    sealed class PreserveOrientation : SettingsSideEffect() {

        @Parcelize
        data object Failure : PreserveOrientation()

        @Parcelize
        data object Success : PreserveOrientation()
    }

    sealed class RandomizeFileNames : SettingsSideEffect() {

        @Parcelize
        data object Failure : RandomizeFileNames()

        @Parcelize
        data object Success : RandomizeFileNames()
    }

    sealed class ShareByDefault : SettingsSideEffect() {

        @Parcelize
        data object Failure : ShareByDefault()

        @Parcelize
        data object Success : ShareByDefault()
    }
}
