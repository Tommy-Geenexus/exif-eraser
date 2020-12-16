/*
 * Copyright (c) 2018-2020, Tom Geiselmann (tomgapplicationsdevelopment@gmail.com)
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

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.none.tom.exiferaser.selection.PROGRESS_MAX
import com.none.tom.exiferaser.selection.data.ImageRepository
import com.none.tom.exiferaser.selection.data.Result
import com.none.tom.exiferaser.selection.data.Summary
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
class ImageRepositoryInstrumentedTest {

    private companion object {
        const val CONTENT_URI_PREFIX = "content://com.none.tom.exiferaser.fileprovider/my_images/"
        const val EXTENSION_JPEG = ".jpg"
        const val EXTENSION_PNG = ".png"
        const val EXTENSION_WEBP = ".webp"
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
    private val expectedSummaries = listOf(
        Summary(
            displayName = JPEG_WITH_EXIF_WITH_XMP,
            imageModified = true,
            imageSaved = false, // FIXME: Missing uri write permissions
            imageUri = expectedImagePaths[0]
        ),
        Summary(
            displayName = JPEG_WITHOUT_EXIF_WITHOUT_XMP,
            imageModified = false,
            imageSaved = false,
            imageUri = expectedImagePaths[1]
        ),
        Summary(
            displayName = PNG_WITH_EXIF,
            imageModified = true,
            imageSaved = false,
            imageUri = expectedImagePaths[2]
        ),
        Summary(
            displayName = PNG_WITHOUT_EXIF,
            imageModified = false,
            imageSaved = false,
            imageUri = expectedImagePaths[3]
        ),
        Summary(
            displayName = WEBP_WITH_EXIF,
            imageModified = true,
            imageSaved = false,
            imageUri = expectedImagePaths[4]
        ),
        Summary(
            displayName = WEBP_WITHOUT_EXIF_WITH_MAKE,
            imageModified = true, // FIXME: Contains maker notes
            imageSaved = false,
            imageUri = expectedImagePaths[5]
        )
    )

    private lateinit var context: Context
    private lateinit var testDispatcher: CoroutineDispatcher
    private lateinit var testRepository: ImageRepository

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = TestCoroutineDispatcher()
        testRepository = ImageRepository(context, context.contentResolver, testDispatcher)
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

    @ExperimentalCoroutinesApi
    @Test
    fun test_getExternalPicturesFileProviderUriOrNull() {
        resources.forEachIndexed { index, (_, displayName, extension) ->
            runBlockingTest {
                val uri = getExternalPicturesFileProviderUriOrThrow(displayName, extension)
                expectThat(uri).isEqualTo(expectedImagePaths[index])
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun test_removeMetaDataSingle() {
        resources.forEachIndexed { index, (_, displayName, extension) ->
            runBlockingTest {
                testRepository.removeMetadataSingle(
                    selection = UserImageSelectionProto(
                        image_path = getExternalPicturesFileProviderUriOrThrow(
                            displayName,
                            extension
                        ).toString(),
                        from_camera = false
                    ),
                    preserveOrientation = false
                ).collectIndexed { i: Int, result: Result ->
                    when (i) {
                        0 -> {
                            expectThat(result).isA<Result.Report>()
                            expectThat((result as Result.Report).summary) {
                                isEqualTo(expectedSummaries[index])
                            }
                        }
                        1 -> {
                            expectThat(result).isA<Result.Handled>()
                            expectThat((result as Result.Handled).progress).isEqualTo(PROGRESS_MAX)
                        }
                        2 -> {
                            expectThat(result).isA<Result.HandledAll>()
                        }
                        else -> {
                            throw IllegalStateException()
                        }
                    }
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    @Test
    fun test_removeMetaDataBulk() = runBlockingTest {
        val selection = mutableListOf<UserImageSelectionProto>()
        resources.forEach { (_, displayName, extension) ->
            selection.add(
                UserImageSelectionProto(
                    image_path = getExternalPicturesFileProviderUriOrThrow(
                        displayName,
                        extension
                    ).toString(),
                    from_camera = false
                )
            )
        }
        val lastIndex = expectedSummaries.size * 2
        testRepository.removeMetadataBulk(
            selection = selection,
            preserveOrientation = false
        ).collectIndexed { index: Int, result: Result ->
            if (index > lastIndex) {
                throw IllegalStateException()
            }
            when {
                index == lastIndex -> {
                    expectThat(result).isA<Result.HandledAll>()
                }
                index % 2 == 0 -> {
                    expectThat(result).isA<Result.Report>()
                    expectThat((result as Result.Report).summary) {
                        isEqualTo(expectedSummaries[if (index > 0) index / 2 else index])
                    }
                }
                else -> {
                    expectThat(result).isA<Result.Handled>()
                    expectThat((result as Result.Handled).progress) {
                        isEqualTo(((((index - 1) / 2) + 1) * 100) / selection.size)
                    }
                }
            }
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

    private fun getExternalPicturesFileProviderUriOrThrow(
        displayName: String,
        extension: String
    ): Uri {
        return testRepository.getExternalPicturesFileProviderUriOrNull(
            displayName = displayName,
            extension = extension
        ) ?: throw IllegalStateException()
    }

    private fun getFileFromExternalDir(
        displayName: String,
        extension: String
    ): File {
        return File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            displayName.plus(extension)
        )
    }
}
