package com.example.findduplicates.repository

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.example.findduplicates.ai.ImageEmbeddingAnalyzer
import com.example.findduplicates.ai.ImageSimilarity
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
        files: List<DeviceFile>,
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
    fun findSimilarImageGroups(
        files: List<DeviceFile>,
        threshold: Float = 0.90f,

        shouldStop: () -> Boolean = { false },
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onDuplicateCount: (Int) -> Unit = {}

    ): List<List<DeviceFile>> {

        val imageFiles = files.filter {
            it.mimeType?.startsWith("image/", ignoreCase = true) == true
        }
        val scanStart = SystemClock.elapsedRealtime()
        Log.d("ImageSimilarity", "AI scan started")
        Log.d(
            "ImageSimilarity",
            "Images to analyze: ${imageFiles.size}"
        )
        if (imageFiles.size < 2) return emptyList()

        val embeddedFiles = mutableListOf<Pair<DeviceFile, FloatArray>>()

        ImageEmbeddingAnalyzer(context.applicationContext).use { analyzer ->
            var processedImages = 0
            val totalImages = imageFiles.size
            for (file in imageFiles) {
                if (shouldStop()) break

                try {
                    val embedding = analyzer.embedImage(
                        context.applicationContext,
                        file.uri
                    )

                    embeddedFiles.add(file to embedding)

                } catch (e: Exception) {
                    Log.e(
                        "ImageSimilarity",
                        "Failed to analyze ${file.displayName}",
                        e
                    )
                } finally {
                    processedImages++
                    onProgress(processedImages, totalImages)
                }
            }
        }

        if (embeddedFiles.size < 2) return emptyList()

        val adjacency = Array(embeddedFiles.size) {
            mutableListOf<Int>()
        }


        for (i in embeddedFiles.indices) {
            if (shouldStop()) break

            for (j in (i + 1) until embeddedFiles.size) {
                if (shouldStop()) break

                val similarity = ImageSimilarity.cosineSimilarity(
                    embeddedFiles[i].second,
                    embeddedFiles[j].second
                )

                if (similarity >= threshold) {
                    adjacency[i].add(j)
                    adjacency[j].add(i)
                }
            }

            // Report the number of groups discovered so far.
            val visitedDuringCount = BooleanArray(embeddedFiles.size)
            var currentGroupCount = 0

            for (start in embeddedFiles.indices) {
                if (visitedDuringCount[start] ||
                    adjacency[start].isEmpty()
                ) {
                    continue
                }

                val queue = ArrayDeque<Int>()
                queue.addLast(start)
                visitedDuringCount[start] = true
                var groupSize = 0

                while (queue.isNotEmpty()) {
                    val current = queue.removeFirst()
                    groupSize++

                    for (neighbor in adjacency[current]) {
                        if (!visitedDuringCount[neighbor]) {
                            visitedDuringCount[neighbor] = true
                            queue.addLast(neighbor)
                        }
                    }
                }

                if (groupSize > 1) {
                    currentGroupCount++
                }
            }

            onDuplicateCount(currentGroupCount)
        }


        val visited = BooleanArray(embeddedFiles.size)
        val groups = mutableListOf<List<DeviceFile>>()

        for (start in embeddedFiles.indices) {
            if (visited[start] || adjacency[start].isEmpty()) continue

            val queue = ArrayDeque<Int>()
            val group = mutableListOf<DeviceFile>()

            queue.addLast(start)
            visited[start] = true

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()

                group.add(embeddedFiles[current].first)

                for (neighbor in adjacency[current]) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true
                        queue.addLast(neighbor)
                    }
                }
            }

            if (group.size > 1) {
                groups.add(group)
            }
        }
        Log.d(
            "ImageSimilarity",
            "AI scan finished in ${
                SystemClock.elapsedRealtime() - scanStart
            } ms. Groups found: ${groups.size}"
        )
        return groups
    }
}