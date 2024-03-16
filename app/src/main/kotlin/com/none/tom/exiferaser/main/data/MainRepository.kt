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

package com.none.tom.exiferaser.main.data

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import com.none.tom.exiferaser.di.DispatcherIo
import com.none.tom.exiferaser.supportedImageUrisToList
import com.none.tom.exiferaser.suspendRunCatching
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class MainRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @DispatcherIo private val dispatcherIo: CoroutineDispatcher
) {

    suspend fun deleteCameraImages(): Result<Unit> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                check(CameraFileProvider.getRootDirectory(context).deleteRecursively())
                Result.success(Unit)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun getChildDocuments(treeUri: Uri?): Result<List<Uri>> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val childDocumentsUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                )
                checkNotNull(
                    context.contentResolver.query(
                        childDocumentsUri,
                        null,
                        null,
                        null,
                        null
                    )
                ).use { cursor ->
                    val uris = mutableListOf<Uri>()
                    while (cursor.moveToNext()) {
                        val columnName = DocumentsContract.Document.COLUMN_DOCUMENT_ID
                        val id = cursor.getString(cursor.getColumnIndexOrThrow(columnName))
                        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                        val mimeType = context.contentResolver.getType(uri).orEmpty()
                        if (supportedImageFormats.map { f -> f.mimeType }.contains(mimeType)) {
                            uris.add(uri)
                        }
                    }
                    check(uris.isNotEmpty())
                    Result.success(uris)
                }
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun getPrimaryClipImageUris(): Result<List<Uri>> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val primaryClip = cm.primaryClip
                check(cm.hasPrimaryClip())
                checkNotNull(primaryClip)
                val uris = primaryClip.supportedImageUrisToList()
                check(uris.isNotEmpty())
                Result.success(uris)
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }

    suspend fun getFileProviderUri(): Result<Uri> {
        return withContext(dispatcherIo) {
            coroutineContext.suspendRunCatching {
                Result.success(
                    value = CameraFileProvider.getUriForFile(
                        context = context,
                        displayName = UUID.randomUUID().toString(),
                        extension = checkNotNull(
                            MimeTypeMap
                                .getSingleton()
                                .getExtensionFromMimeType(
                                    supportedImageFormats.first().mimeType
                                )
                        )
                    )
                )
            }.getOrElse { exception ->
                Timber.e(exception)
                Result.failure(exception)
            }
        }
    }
}
