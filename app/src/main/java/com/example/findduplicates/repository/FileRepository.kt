package com.example.findduplicates.repository

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.findduplicates.data.DeviceFile
import java.io.File

class FileRepository(private val context: Context) {


    @SuppressLint("Recycle")
    fun getMediaFiles():List<DeviceFile>{

        val files = mutableListOf<DeviceFile>()
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED)
        val collection = MediaStore.Files.getContentUri(
            MediaStore.VOLUME_EXTERNAL)
        context.contentResolver
            .query(collection,projection,null,null,null)
            ?.use{cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val displayNAmeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                while(cursor.moveToNext())
                {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection,id)

                    files.add(
                        DeviceFile(
                        id = id,
                        displayName =cursor.getString(displayNAmeColumn),
                        size = cursor.getLong(sizeColumn),
                        uri = uri,
                        mimeType = cursor.getString(mimeColumn),
                        dateModified = cursor.getLong(dateColumn)))

                }

            }
        Log.e("Debug","fileList :${files}")
        return files
    }
}