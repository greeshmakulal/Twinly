package com.example.findduplicates.repository

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.findduplicates.data.DeviceFile
import java.io.File
import java.security.MessageDigest

class FileRepository(private val context: Context) {


    @SuppressLint("Recycle")
    fun getMediaFiles(
            collection: Uri
        ): List<DeviceFile> {

            val files = mutableListOf<DeviceFile>()

            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_MODIFIED
            )

            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                null
            )?.use { cursor ->

                val idColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)

                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)

                val sizeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

                val mimeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                val dateColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {

                    val id = cursor.getLong(idColumn)

                    // IMPORTANT
                    val uri = ContentUris.withAppendedId(
                        collection,
                        id
                    )

                    files.add(
                        DeviceFile(
                            id = id,
                            displayName = cursor.getString(nameColumn),
                            size = cursor.getLong(sizeColumn),
                            uri = uri,
                            mimeType = cursor.getString(mimeColumn),
                            dateModified = cursor.getLong(dateColumn)
                        )
                    )
                }
            }

            return files
        }
    fun findDuplicates(
        files: List<DeviceFile>
    ): List<List<DeviceFile>> {

        val possibleDuplicates = files
            .groupBy { it.size }
            .filterValues { it.size > 1 }

        val duplicateGroups = mutableListOf<List<DeviceFile>>()

        possibleDuplicates.values.forEach { sameSizeFiles ->

            val hashGroups = sameSizeFiles.groupBy { file ->
                calculateHash(file.uri)
            }

            hashGroups.values
                .filter { it.size > 1 }
                .forEach { duplicates ->
                    duplicateGroups.add(duplicates)
                }
        }

        return duplicateGroups
    }
    private fun calculateHash(uri: Uri): String {

        val digest = MessageDigest.getInstance("SHA-256")

        context.contentResolver
            .openInputStream(uri)
            ?.use { inputStream ->

                val buffer = ByteArray(8192)

                var read: Int

                while (
                    inputStream.read(buffer).also { read = it } != -1
                ) {
                    digest.update(
                        buffer,
                        0,
                        read
                    )
                }
            }

        return digest.digest()
            .joinToString("") {
                "%02x".format(it)
            }
    }
}