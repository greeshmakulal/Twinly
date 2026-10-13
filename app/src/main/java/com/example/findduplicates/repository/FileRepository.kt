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
import com.example.findduplicates.data.local.EmbeddingDao
import com.example.findduplicates.data.local.ImageEmbeddingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.system.measureTimeMillis

class FileRepository(private val context: Context, val embeddingDao: EmbeddingDao) {


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


    suspend fun findSimilarImageGroups(
        files: List<DeviceFile>,
        threshold: Float = 0.90f,
        shouldStop: () -> Boolean = { false },
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onDuplicateCount: (Int) -> Unit = {}
    ): List<List<DeviceFile>> = withContext(Dispatchers.Default) {

        val imageFiles = files.filter {
            it.mimeType?.startsWith("image/", ignoreCase = true) == true
        }
        if (imageFiles.size < 2) return@withContext emptyList()

        val scanStart = SystemClock.elapsedRealtime()
        Log.d("ImageSimilarity", "AI scan started for ${imageFiles.size} images")

        // 1. Gather embeddings (Cache hit vs Cache miss)
        val embeddedResults = mutableListOf<EmbeddingResult>()
        ImageEmbeddingAnalyzer(context.applicationContext).use { analyzer ->
            var processedImages = 0
            val totalImages = imageFiles.size

            for (file in imageFiles) {
                if (shouldStop()) break

                val (embedding, isNew) = getOrComputeEmbeddingWithStatus(analyzer, file)
                if (embedding != null) {
                    embeddedResults.add(EmbeddingResult(file, embedding, isNew))
                }

                processedImages++
                onProgress(processedImages, totalImages)
            }
        }

        if (embeddedResults.size < 2) return@withContext emptyList()

        val size = embeddedResults.size
        val adjacency = Array(size) { mutableListOf<Int>() }

        // 2. PARALLELIZED COMPARISON LOOP (Multi-core acceleration)
        // We split the outer loop rows across chunks to utilize all CPU cores
        val numCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val chunkSize = (size + numCores - 1) / numCores

        val jobs = (0 until numCores).map { coreIndex ->
            async(Dispatchers.Default) {
                val startRow = coreIndex * chunkSize
                val endRow = minOf(startRow + chunkSize, size)

                for (i in startRow until endRow) {
                    if (shouldStop()) break

                    val itemI = embeddedResults[i]

                    for (j in (i + 1) until size) {
                        val itemJ = embeddedResults[j]

                        // Optimization: If BOTH items are old/cached and neither is newly added,
                        // their similarity hasn't changed. (Optional shortcut if you store prior edges,
                        // but parallel raw math is already blazing fast).

                        val similarity = ImageSimilarity.cosineSimilarity(
                            itemI.embedding,
                            itemJ.embedding
                        )

                        if (similarity >= threshold) {
                            synchronized(adjacency) {
                                adjacency[i].add(j)
                                adjacency[j].add(i)
                            }
                        }
                    }
                }
            }
        }

        jobs.awaitAll()// Wait for all CPU cores to finish comparing their chunks

        if (shouldStop()) return@withContext emptyList()

        // 3. Connected Components (BFS) to build final groups
        val visited = BooleanArray(size)
        val groups = mutableListOf<List<DeviceFile>>()
        var foundGroupCount = 0

        for (start in 0 until size) {
            if (visited[start] || adjacency[start].isEmpty()) continue

            val queue = ArrayDeque<Int>()
            val group = mutableListOf<DeviceFile>()

            queue.addLast(start)
            visited[start] = true

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                group.add(embeddedResults[current].file)

                for (neighbor in adjacency[current]) {
                    if (!visited[neighbor]) {
                        visited[neighbor] = true
                        queue.addLast(neighbor)
                    }
                }
            }

            if (group.size > 1) {
                groups.add(group)
                foundGroupCount++
            }
        }

        onDuplicateCount(foundGroupCount)
        Log.d("ImageSimilarity", "AI scan finished in ${SystemClock.elapsedRealtime() - scanStart} ms. Groups found: ${groups.size}")

        return@withContext groups
    }

    /**
     * Helper that returns the embedding AND a boolean flag indicating if it was newly computed.
     */
    /**
     * Helper that returns the embedding AND a boolean flag indicating if it was newly computed.
     */
    private suspend fun getOrComputeEmbeddingWithStatus(
        analyzer: ImageEmbeddingAnalyzer,
        file: DeviceFile
    ): Pair<FloatArray?, Boolean> {
        val uriString = file.uri.toString()

        val cached = embeddingDao.getEmbedding(uriString)
        if (cached != null && cached.dateModified == file.dateModified) {
            return cached.embedding to false // Cache hit (Not new)
        }

        try {
            val embedding = analyzer.embedImage(context, file.uri)
            embeddingDao.insertEmbedding(
                ImageEmbeddingEntity(
                    imageUri = uriString,
                    dateModified = file.dateModified,
                    embedding = embedding
                )
            )
            return embedding to true // Cache miss (Newly computed)
        } catch (e: IOException) {
            Log.e("FileRepository", "Failed to read image: ${file.displayName}", e)
            return null to false
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to embed image: ${file.displayName}", e)
            return null to false
        }
    }
    suspend fun getOrComputeEmbedding(
        analyzer: ImageEmbeddingAnalyzer,
        file: DeviceFile
    ): FloatArray? {
        val uriString = file.uri.toString()

        // 1. Check local cache first
        val cached = embeddingDao.getEmbedding(uriString)
        if (cached != null && cached.dateModified == file.dateModified) {
            return cached.embedding // Cache hit! Skip TFLite inference completely.
        }

        // 2. Cache miss: Run MobileNetV3 inference
        try {
            val embedding = analyzer.embedImage(context, file.uri)

            // 3. Save to Room database for future scans
            embeddingDao.insertEmbedding(
                ImageEmbeddingEntity(
                    imageUri = uriString,
                    dateModified = file.dateModified,
                    embedding = embedding
                )
            )
            return embedding
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to embed image: ${file.displayName}", e)
            return null
        }
    }
}