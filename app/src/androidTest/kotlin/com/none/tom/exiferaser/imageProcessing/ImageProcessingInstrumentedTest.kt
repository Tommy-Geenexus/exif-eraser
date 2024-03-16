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

package com.none.tom.exiferaser.imageProcessing

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.annotation.RawRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.none.tom.exiferaser.R
import com.none.tom.exiferaser.UserImageSelectionProto
import com.none.tom.exiferaser.imageProcessing.data.ImageMetadataSnapshot
import com.none.tom.exiferaser.imageProcessing.data.ImageProcessingProgress
import com.none.tom.exiferaser.imageProcessing.data.ImageProcessingRepository
import com.none.tom.exiferaser.imageProcessing.data.ImageProcessingStep
import com.none.tom.exiferaser.imageProcessing.data.ImageProcessingSummary
import com.none.tom.exiferaser.main.data.CameraFileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEqualTo

@RunWith(AndroidJUnit4::class)
class ImageProcessingInstrumentedTest {

    private companion object {
        const val URI_SCHEME = ContentResolver.SCHEME_CONTENT
        const val URI_AUTHORITY = CameraFileProvider.AUTHORITY
        const val URI_PATH = CameraFileProvider.NAME
        const val URI_SUFFIX = CameraFileProvider.SUFFIX
        const val DISPLAY_NAME_SUFFIX = "test_suffix"
        const val MIME_TYPE_JPEG = "image/jpeg"
        const val MIME_TYPE_PNG = "image/png"
        const val MIME_TYPE_WEBP = "image/webp"
        const val EXTENSION_JPEG = "jpg"
        const val EXTENSION_PNG = "png"
        const val EXTENSION_WEBP = "webp"
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
    private val expectedUris = listOf(
        buildSourceUri(name = JPEG_WITH_EXIF_WITH_XMP, extension = EXTENSION_JPEG),
        buildSourceUri(name = JPEG_WITHOUT_EXIF_WITHOUT_XMP, extension = EXTENSION_JPEG),
        buildSourceUri(name = PNG_WITH_EXIF, extension = EXTENSION_PNG),
        buildSourceUri(name = PNG_WITHOUT_EXIF, extension = EXTENSION_PNG),
        buildSourceUri(name = WEBP_WITH_EXIF, extension = EXTENSION_WEBP),
        buildSourceUri(name = WEBP_WITHOUT_EXIF_WITH_MAKE, extension = EXTENSION_WEBP)
    )
    private val expectedUrisModified = listOf(
        buildSinkUri(name = JPEG_WITH_EXIF_WITH_XMP, extension = EXTENSION_JPEG),
        buildSourceUri(name = JPEG_WITHOUT_EXIF_WITHOUT_XMP, extension = EXTENSION_JPEG),
        buildSinkUri(name = PNG_WITH_EXIF, extension = EXTENSION_PNG),
        buildSourceUri(name = PNG_WITHOUT_EXIF, extension = EXTENSION_PNG),
        buildSinkUri(name = WEBP_WITH_EXIF, extension = EXTENSION_WEBP),
        buildSinkUri(name = WEBP_WITHOUT_EXIF_WITH_MAKE, extension = EXTENSION_WEBP)
    )
    private val expectedUrisModifiedWithDisplayNameSuffix = listOf(
        buildSinkUri(
            name = JPEG_WITH_EXIF_WITH_XMP,
            displayNameSuffix = DISPLAY_NAME_SUFFIX,
            extension = EXTENSION_JPEG
        ),
        buildSourceUri(
            name = JPEG_WITHOUT_EXIF_WITHOUT_XMP,
            extension = EXTENSION_JPEG
        ),
        buildSinkUri(
            name = PNG_WITH_EXIF,
            displayNameSuffix = DISPLAY_NAME_SUFFIX,
            extension = EXTENSION_PNG
        ),
        buildSourceUri(
            name = PNG_WITHOUT_EXIF,
            extension = EXTENSION_PNG
        ),
        buildSinkUri(
            name = WEBP_WITH_EXIF,
            displayNameSuffix = DISPLAY_NAME_SUFFIX,
            extension = EXTENSION_WEBP
        ),
        buildSinkUri(
            name = WEBP_WITHOUT_EXIF_WITH_MAKE,
            displayNameSuffix = DISPLAY_NAME_SUFFIX,
            extension = EXTENSION_WEBP
        )
    )
    private val expectedDisplayNames = listOf(
        JPEG_WITH_EXIF_WITH_XMP.plus(URI_SUFFIX),
        JPEG_WITHOUT_EXIF_WITHOUT_XMP,
        PNG_WITH_EXIF.plus(URI_SUFFIX),
        PNG_WITHOUT_EXIF,
        WEBP_WITH_EXIF.plus(URI_SUFFIX),
        WEBP_WITHOUT_EXIF_WITH_MAKE.plus(URI_SUFFIX)
    )
    private val expectedDisplayNamesWithDisplayNameSuffix = listOf(
        JPEG_WITH_EXIF_WITH_XMP.plus(URI_SUFFIX).plus(DISPLAY_NAME_SUFFIX),
        JPEG_WITHOUT_EXIF_WITHOUT_XMP,
        PNG_WITH_EXIF.plus(URI_SUFFIX).plus(DISPLAY_NAME_SUFFIX),
        PNG_WITHOUT_EXIF,
        WEBP_WITH_EXIF.plus(URI_SUFFIX).plus(DISPLAY_NAME_SUFFIX),
        WEBP_WITHOUT_EXIF_WITH_MAKE.plus(URI_SUFFIX).plus(DISPLAY_NAME_SUFFIX)
    )
    private val expectedSummaries = listOf(
        ImageProcessingSummary(
            displayName = expectedDisplayNames[0],
            extension = EXTENSION_JPEG,
            mimeType = MIME_TYPE_JPEG,
            uri = expectedUrisModified[0],
            isImageSaved = true,
            imageMetadataSnapshot = ImageMetadataSnapshot(
                isIccProfileContained = false,
                isExifContained = true,
                isPhotoshopImageResourcesContained = false,
                isXmpContained = true,
                isExtendedXmpContained = false
            )
        ),
        ImageProcessingSummary(
            displayName = expectedDisplayNames[1],
            extension = EXTENSION_JPEG,
            mimeType = MIME_TYPE_JPEG,
            uri = expectedUrisModified[1]
        ),
        ImageProcessingSummary(
            displayName = expectedDisplayNames[2],
            extension = EXTENSION_PNG,
            mimeType = MIME_TYPE_PNG,
            uri = expectedUrisModified[2],
            isImageSaved = true,
            imageMetadataSnapshot = ImageMetadataSnapshot(
                isIccProfileContained = false,
                isExifContained = true,
                isPhotoshopImageResourcesContained = false,
                isXmpContained = true,
                isExtendedXmpContained = false
            )
        ),
        ImageProcessingSummary(
            displayName = expectedDisplayNames[3],
            extension = EXTENSION_PNG,
            mimeType = MIME_TYPE_PNG,
            uri = expectedUrisModified[3]
        ),
        ImageProcessingSummary(
            displayName = expectedDisplayNames[4],
            extension = EXTENSION_WEBP,
            mimeType = MIME_TYPE_WEBP,
            uri = expectedUrisModified[4],
            isImageSaved = true,
            imageMetadataSnapshot = ImageMetadataSnapshot(
                isIccProfileContained = false,
                isExifContained = true,
                isPhotoshopImageResourcesContained = false,
                isXmpContained = false,
                isExtendedXmpContained = false
            )
        ),
        ImageProcessingSummary(
            displayName = expectedDisplayNames[5],
            extension = EXTENSION_WEBP,
            mimeType = MIME_TYPE_WEBP,
            uri = expectedUrisModified[5],
            isImageSaved = true,
            imageMetadataSnapshot = ImageMetadataSnapshot(
                isIccProfileContained = false,
                isExifContained = true,
                isPhotoshopImageResourcesContained = false,
                isXmpContained = false,
                isExtendedXmpContained = false
            )
        )
    )

    private lateinit var context: Context
    private lateinit var testDispatcher: CoroutineDispatcher
    private lateinit var imageProcessingRepository: ImageProcessingRepository

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = UnconfinedTestDispatcher()
        imageProcessingRepository = ImageProcessingRepository(context, testDispatcher)
        copyResources()
    }

    @After
    fun tearDown() {
        CameraFileProvider.getRootDirectory(context).deleteRecursively()
    }

    @Test
    fun test_getExternalPicturesFileProviderUriOrNull() {
        resources.forEachIndexed { index, (_, displayName, extension) ->
            runTest {
                val uri = imageProcessingRepository
                    .getFileProviderUri(displayName, extension)
                    .getOrThrow()
                expectThat(uri).isEqualTo(expectedUris[index])
            }
        }
    }

    @Test
    fun test_removeMetaDataSingle() {
        resources.forEachIndexed { index, (_, displayName, extension) ->
            runTest {
                imageProcessingRepository.removeMetadata(
                    protos = listOf(
                        UserImageSelectionProto(
                            image_path = imageProcessingRepository
                                .getFileProviderUri(displayName, extension)
                                .getOrThrow()
                                .toString(),
                            from_camera = false
                        )
                    ),
                    treeUri = Uri.EMPTY,
                    displayNameSuffix = "",
                    isAutoDeleteEnabled = false,
                    isPreserveOrientationEnabled = false,
                    isRandomizeFileNamesEnabled = false
                ).test {
                    val item = awaitItem()
                    expectThat(item).isA<ImageProcessingStep.FinishedSingle>()
                    (item as ImageProcessingStep.FinishedSingle)
                    expectThat(item.imageProcessingSummary).isEqualTo(expectedSummaries[index])
                    expectThat(item.progress).isEqualTo(ImageProcessingProgress.calculate(0, 1))
                    expectThat(awaitItem()).isA<ImageProcessingStep.FinishedBulk>()
                    awaitComplete()
                }
            }
        }
    }

    @Test
    fun test_removeMetaDataSingleWithDisplayNameSuffix() {
        resources.forEachIndexed { index, (_, displayName, extension) ->
            runTest {
                imageProcessingRepository.removeMetadata(
                    protos = listOf(
                        UserImageSelectionProto(
                            image_path = imageProcessingRepository
                                .getFileProviderUri(displayName, extension)
                                .getOrThrow()
                                .toString(),
                            from_camera = false
                        )
                    ),
                    treeUri = Uri.EMPTY,
                    displayNameSuffix = DISPLAY_NAME_SUFFIX,
                    isAutoDeleteEnabled = false,
                    isPreserveOrientationEnabled = false,
                    isRandomizeFileNamesEnabled = false
                ).test {
                    val item = awaitItem()
                    expectThat(item).isA<ImageProcessingStep.FinishedSingle>()
                    (item as ImageProcessingStep.FinishedSingle)
                    val summary = item.imageProcessingSummary
                    expectThat(summary).isEqualTo(
                        expectedSummaries[index].copy(
                            displayName = expectedDisplayNamesWithDisplayNameSuffix[index],
                            uri = expectedUrisModifiedWithDisplayNameSuffix[index]
                        )
                    )
                    expectThat(item.progress).isEqualTo(ImageProcessingProgress.calculate(0, 1))
                    expectThat(awaitItem()).isA<ImageProcessingStep.FinishedBulk>()
                    awaitComplete()
                }
            }
        }
    }

    @Test
    fun test_removeMetaDataSingleWithRandomizeFileNamesEnabled() {
        resources.forEachIndexed { index, (_, displayName, extension) ->
            runTest {
                imageProcessingRepository.removeMetadata(
                    protos = listOf(
                        UserImageSelectionProto(
                            image_path = imageProcessingRepository
                                .getFileProviderUri(displayName, extension)
                                .getOrThrow()
                                .toString(),
                            from_camera = false
                        )
                    ),
                    treeUri = Uri.EMPTY,
                    displayNameSuffix = "",
                    isAutoDeleteEnabled = false,
                    isPreserveOrientationEnabled = false,
                    isRandomizeFileNamesEnabled = true
                ).test {
                    val item = awaitItem()
                    expectThat(item).isA<ImageProcessingStep.FinishedSingle>()
                    (item as ImageProcessingStep.FinishedSingle)
                    val summary = item.imageProcessingSummary
                    expectThat(summary) {
                        if (summary.isImageSaved) {
                            isNotEqualTo(expectedSummaries[index])
                        } else {
                            isEqualTo(expectedSummaries[index])
                        }
                    }
                    expectThat(item.progress).isEqualTo(ImageProcessingProgress.calculate(0, 1))
                    expectThat(awaitItem()).isA<ImageProcessingStep.FinishedBulk>()
                    awaitComplete()
                }
            }
        }
    }

    @Test
    fun test_removeMetaDataBulk() = runTest {
        val protos = mutableListOf<UserImageSelectionProto>()
        resources.forEach { (_, displayName, extension) ->
            protos.add(
                UserImageSelectionProto(
                    image_path = imageProcessingRepository
                        .getFileProviderUri(displayName, extension)
                        .getOrThrow()
                        .toString(),
                    from_camera = false
                )
            )
        }
        imageProcessingRepository.removeMetadata(
            protos = protos,
            treeUri = Uri.EMPTY,
            displayNameSuffix = "",
            isAutoDeleteEnabled = false,
            isPreserveOrientationEnabled = false,
            isRandomizeFileNamesEnabled = false
        ).test {
            for (index in 0 until protos.size) {
                val item = awaitItem()
                expectThat(item).isA<ImageProcessingStep.FinishedSingle>()
                (item as ImageProcessingStep.FinishedSingle)
                expectThat(item.imageProcessingSummary).isEqualTo(expectedSummaries[index])
                expectThat(item.progress).isEqualTo(
                    ImageProcessingProgress.calculate(index, protos.size)
                )
            }
            expectThat(awaitItem()).isA<ImageProcessingStep.FinishedBulk>()
            awaitComplete()
        }
    }

    private fun copyResource(@RawRes id: Int, displayName: String, extension: String) {
        context.resources.openRawResource(id).use { source ->
            FileOutputStream(
                File(
                    CameraFileProvider.getRootDirectory(context),
                    displayName.plus('.').plus(extension)
                )
            ).use { sink ->
                source.copyTo(sink)
            }
        }
    }

    private fun copyResources() {
        resources.forEach { (id, displayName, extension) ->
            copyResource(id, displayName, extension)
        }
    }

    private fun buildSourceUri(name: String, extension: String) = Uri.parse(
        URI_SCHEME
            .plus(":")
            .plus(File.separatorChar)
            .plus(File.separatorChar)
            .plus(URI_AUTHORITY)
            .plus(File.separatorChar)
            .plus(URI_PATH)
            .plus(File.separatorChar)
            .plus(name)
            .plus('.')
            .plus(extension)
    )

    private fun buildSinkUri(name: String, displayNameSuffix: String = "", extension: String) =
        Uri.parse(
            URI_SCHEME
                .plus(":")
                .plus(File.separatorChar)
                .plus(File.separatorChar)
                .plus(URI_AUTHORITY)
                .plus(File.separatorChar)
                .plus(URI_PATH)
                .plus(File.separatorChar)
                .plus(name)
                .plus(URI_SUFFIX)
                .plus(displayNameSuffix)
                .plus('.')
                .plus(extension)
        )
}
