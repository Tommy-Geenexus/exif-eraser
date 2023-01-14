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

package com.none.tom.exiferaser.selection.data

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.IntRange
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.none.tom.exiferaser.EXTENSION_JPEG
import com.none.tom.exiferaser.EXTENSION_PNG
import com.none.tom.exiferaser.EXTENSION_WEBP
import com.none.tom.exiferaser.MIME_TYPE_JPEG
import com.none.tom.exiferaser.MIME_TYPE_PNG
import com.none.tom.exiferaser.MIME_TYPE_WEBP
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.UserImagesSelectionProto
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.PROGRESS_MIN
import com.none.tom.exiferaser.selection.toProgress
import com.squareup.wire.AnyMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import java.io.File
import java.io.FileOutputStream
import kotlin.contracts.ExperimentalContracts
import kotlin.time.ExperimentalTime

@ExperimentalCoroutinesApi
@ExperimentalTime
@ExperimentalContracts
@RunWith(AndroidJUnit4::class)
class ImageRepositoryInstrumentedTest {

    private companion object {
        const val CONTENT_URI_PREFIX = "content://com.none.tom.exiferaser.fileprovider/my_images/"
        const val CONTENT_URI_SUFFIX = "_Modified"
        const val JPEG_WITH_EXIF_WITH_XMP = "jpeg_with_exif_with_xmp"
        const val JPEG_WITHOUT_EXIF_WITHOUT_XMP = "jpeg_without_exif_without_xmp"
        const val PNG_WITH_EXIF = "png_with_exif_byte_order_ii"
        const val PNG_WITHOUT_EXIF = "png_without_exif"
        const val WEBP_WITH_EXIF = "webp_with_exif"
        const val WEBP_WITHOUT_EXIF_WITH_MAKE = "webp_without_exif"
    }

    private val resources = listOf(
        Triple(R.raw.jpeg_with_exif_with_xmp, JPEG_WITH_EXIF_WITH_XMP, EXTENSION_JPEG),
        Triple(R.raw.jpeg_without_exif_without_xmp, JPEG_WITHOUT_EXIF_WITHOUT_XMP, EXTENSION_JPEG),
        Triple(R.raw.png_with_exif_byte_order_ii, PNG_WITH_EXIF, EXTENSION_PNG),
        Triple(R.raw.png_without_exif, PNG_WITHOUT_EXIF, EXTENSION_PNG),
        Triple(R.raw.webp_with_exif, WEBP_WITH_EXIF, EXTENSION_WEBP),
        Triple(R.raw.webp_without_exif, WEBP_WITHOUT_EXIF_WITH_MAKE, EXTENSION_WEBP)
    )
    private val expectedImagePaths = listOf(
        Uri.parse(CONTENT_URI_PREFIX + JPEG_WITH_EXIF_WITH_XMP + EXTENSION_JPEG),
        Uri.parse(CONTENT_URI_PREFIX + JPEG_WITHOUT_EXIF_WITHOUT_XMP + EXTENSION_JPEG),
        Uri.parse(CONTENT_URI_PREFIX + PNG_WITH_EXIF + EXTENSION_PNG),
        Uri.parse(CONTENT_URI_PREFIX + PNG_WITHOUT_EXIF + EXTENSION_PNG),
        Uri.parse(CONTENT_URI_PREFIX + WEBP_WITH_EXIF + EXTENSION_WEBP),
        Uri.parse(CONTENT_URI_PREFIX + WEBP_WITHOUT_EXIF_WITH_MAKE + EXTENSION_WEBP)
    )
    private val expectedImagePathsModified = listOf(
        Uri.parse(
            CONTENT_URI_PREFIX + JPEG_WITH_EXIF_WITH_XMP + CONTENT_URI_SUFFIX + EXTENSION_JPEG
        ),
        Uri.parse(CONTENT_URI_PREFIX + JPEG_WITHOUT_EXIF_WITHOUT_XMP + EXTENSION_JPEG),
        Uri.parse(CONTENT_URI_PREFIX + PNG_WITH_EXIF + CONTENT_URI_SUFFIX + EXTENSION_PNG),
        Uri.parse(CONTENT_URI_PREFIX + PNG_WITHOUT_EXIF + EXTENSION_PNG),
        Uri.parse(CONTENT_URI_PREFIX + WEBP_WITH_EXIF + CONTENT_URI_SUFFIX + EXTENSION_WEBP),
        Uri.parse(
            CONTENT_URI_PREFIX + WEBP_WITHOUT_EXIF_WITH_MAKE + CONTENT_URI_SUFFIX + EXTENSION_WEBP
        )
    )
    private val expectedSummaries = listOf(
        Summary(
            displayName = JPEG_WITH_EXIF_WITH_XMP + CONTENT_URI_SUFFIX,
            extension = EXTENSION_JPEG,
            mimeType = MIME_TYPE_JPEG,
            imageUri = expectedImagePathsModified[0],
            imageModified = true,
            imageSaved = true,
            containsIccProfile = false,
            containsExif = true,
            containsPhotoshopImageResources = false,
            containsXmp = true,
            containsExtendedXmp = false
        ),
        Summary(
            displayName = JPEG_WITHOUT_EXIF_WITHOUT_XMP,
            extension = EXTENSION_JPEG,
            mimeType = MIME_TYPE_JPEG,
            imageUri = expectedImagePathsModified[1],
            imageModified = false,
            imageSaved = false,
            containsIccProfile = false,
            containsExif = false,
            containsPhotoshopImageResources = false,
            containsXmp = false,
            containsExtendedXmp = false
        ),
        Summary(
            displayName = PNG_WITH_EXIF + CONTENT_URI_SUFFIX,
            extension = EXTENSION_PNG,
            mimeType = MIME_TYPE_PNG,
            imageUri = expectedImagePathsModified[2],
            imageModified = true,
            imageSaved = true,
            containsIccProfile = false,
            containsExif = true,
            containsPhotoshopImageResources = false,
            containsXmp = true,
            containsExtendedXmp = false
        ),
        Summary(
            displayName = PNG_WITHOUT_EXIF,
            extension = EXTENSION_PNG,
            mimeType = MIME_TYPE_PNG,
            imageUri = expectedImagePathsModified[3],
            imageModified = false,
            imageSaved = false,
            containsIccProfile = false,
            containsExif = false,
            containsPhotoshopImageResources = false,
            containsXmp = false,
            containsExtendedXmp = false
        ),
        Summary(
            displayName = WEBP_WITH_EXIF + CONTENT_URI_SUFFIX,
            extension = EXTENSION_WEBP,
            mimeType = MIME_TYPE_WEBP,
            imageUri = expectedImagePathsModified[4],
            imageModified = true,
            imageSaved = true,
            containsIccProfile = false,
            containsExif = true,
            containsPhotoshopImageResources = false,
            containsXmp = false,
            containsExtendedXmp = false
        ),
        Summary(
            displayName = WEBP_WITHOUT_EXIF_WITH_MAKE + CONTENT_URI_SUFFIX,
            extension = EXTENSION_WEBP,
            mimeType = MIME_TYPE_WEBP,
            imageUri = expectedImagePathsModified[5],
            imageModified = true,
            imageSaved = true,
            containsIccProfile = false,
            containsExif = true,
            containsPhotoshopImageResources = false,
            containsXmp = false,
            containsExtendedXmp = false
        )
    )

    private lateinit var context: Context
    private lateinit var testDispatcher: CoroutineDispatcher
    private lateinit var testRepository: ImageRepository

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = UnconfinedTestDispatcher()
        testRepository = ImageRepository(context, testDispatcher)
        copyResourcesToPictures()
    }

    @After
    fun tearDown() {
        resources.forEach { (_, displayName, extension) ->
            getFileFromExternalDir(displayName, extension)
                .takeIf { file -> file.exists() }
                ?.delete()
        }
    }

    @Test
    fun test_getExternalPicturesFileProviderUriOrNull() {
        resources.forEachIndexed { index, (_, displayName, extension) ->
            runTest {
                val uri = getExternalPicturesFileProviderUri(displayName, extension)
                expectThat(uri) {
                    isNotNull()
                    isEqualTo(expectedImagePaths[index])
                }
            }
        }
    }

    @Test
    fun test_removeMetaDataSingle() {
        resources.forEachIndexed { index, (_, displayName, extension) ->
            runTest {
                val uri = getExternalPicturesFileProviderUri(displayName, extension)
                expectThat(uri).isNotNull()
                testRepository.removeMetadataSingle(
                    selection = AnyMessage.pack(
                        UserImageSelectionProto(
                            image_path = uri.toString(),
                            from_camera = false
                        )
                    ),
                    preserveOrientation = false
                ).test {
                    expectImageHandled(
                        summary = expectedSummaries[index],
                        progress = PROGRESS_MAX
                    )
                    expectThat(awaitItem()).isA<Result.HandledAll>()
                    awaitComplete()
                }
            }
        }
    }

    @Test
    fun test_removeMetaDataBulk() = runTest {
        val selection = mutableListOf<UserImageSelectionProto>()
        resources.forEach { (_, displayName, extension) ->
            val uri = getExternalPicturesFileProviderUri(displayName, extension)
            expectThat(uri).isNotNull()
            selection.add(
                UserImageSelectionProto(
                    image_path = uri.toString(),
                    from_camera = false
                )
            )
        }
        testRepository.removeMetadataBulk(
            selection = AnyMessage.pack(UserImagesSelectionProto(selection)),
            preserveOrientation = false
        ).test {
            for (index in 0 until selection.size) {
                expectImageHandled(
                    summary = expectedSummaries[index],
                    progress = (index + 1).toProgress(selection.size)
                )
            }
            expectThat(awaitItem()).isA<Result.HandledAll>()
            awaitComplete()
        }
    }

    private suspend fun ReceiveTurbine<Result>.expectImageHandled(
        summary: Summary,
        @IntRange(from = PROGRESS_MIN.toLong(), to = PROGRESS_MAX.toLong()) progress: Int
    ) {
        with(awaitItem()) {
            expectThat(this).isA<Result.Report>()
            expectThat((this as Result.Report).summary) {
                isEqualTo(summary)
            }
        }
        with(awaitItem()) {
            expectThat(this).isA<Result.Handled>()
            expectThat((this as Result.Handled).progress).isEqualTo(progress)
        }
    }

    private fun copyResourcesToPictures() {
        resources.forEach { (id, displayName, extension) ->
            copyResourceToPictures(id, displayName, extension)
        }
    }

    private fun copyResourceToPictures(
        @RawRes id: Int,
        displayName: String,
        extension: String
    ) {
        context.resources.openRawResource(id).use { inputStream ->
            FileOutputStream(getFileFromExternalDir(displayName, extension)).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    private suspend fun getExternalPicturesFileProviderUri(
        displayName: String,
        extension: String
    ) = testRepository.getExternalPicturesFileProviderUriOrNull(
        displayName = displayName,
        extension = extension
    )

    private fun getFileFromExternalDir(
        displayName: String,
        extension: String
    ) = File(
        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
        displayName.plus(extension)
    )
}
