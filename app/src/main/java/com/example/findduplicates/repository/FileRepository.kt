package com.example.findduplicates.repository

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.example.findduplicates.ai.ImageEmbeddingAnalyzer
import com.example.findduplicates.ai.ImageSimilarity
import com.example.findduplicates.ai.VideoEmbeddingAnalyzer
import com.example.findduplicates.data.DeviceFile
import com.example.findduplicates.data.DuplicateCategory
import com.example.findduplicates.data.DuplicateGroup
import com.example.findduplicates.data.MediaCounts
import com.example.findduplicates.data.local.EmbeddingDao
import com.example.findduplicates.data.local.ImageEmbeddingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

class FileRepository(
    private val context: Context,
    val embeddingDao: EmbeddingDao
) {

    companion object {
        private const val TAG = "FileRepository"
        private const val PARTIAL_HASH_CHUNK_SIZE = 16384 // 16 KB header + tail
    }

    /**
     * Queries MediaStore for files, populating full dimensions and video duration metadata.
     */
    @SuppressLint("Recycle")
    fun getMediaFiles(collection: Uri, minSizeBytes: Long = 1024L): List<DeviceFile> {
        val files = mutableListOf<DeviceFile>()

        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.WIDTH)
                add(MediaStore.MediaColumns.HEIGHT)
                add(MediaStore.MediaColumns.DURATION)
            }
        }.toTypedArray()

        val selection = "${MediaStore.MediaColumns.SIZE} >= ?"
        val selectionArgs = arrayOf(minSizeBytes.toString())
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                val widthCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                } else -1

                val heightCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                } else -1

                val durationCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)

                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val duration = if (durationCol != -1) cursor.getLong(durationCol) else 0L

                    files.add(
                        DeviceFile(
                            id = id,
                            displayName = cursor.getString(nameCol) ?: "file_$id",
                            size = cursor.getLong(sizeCol),
                            uri = uri,
                            mimeType = cursor.getString(mimeCol),
                            dateModified = cursor.getLong(dateCol),
                            width = width,
                            height = height,
                            durationMs = duration
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore collection $collection", e)
        }

        return files
    }

    /**
     * High-performance exact duplicate detection using Two-Tier Hashing:
     * 1. Group files by size.
     * 2. For same-size files, compute partial hash (first 16KB + last 16KB).
     * 3. Compute full SHA-256 only on files with identical partial hashes.
     */
    fun findExactDuplicates(
        files: List<DeviceFile>,
        category: DuplicateCategory = DuplicateCategory.EXACT_PHOTO
    ): List<DuplicateGroup> {
        val sameSizeGroups = files
            .groupBy { it.size }
            .filterValues { it.size > 1 }

        if (sameSizeGroups.isEmpty()) return emptyList()

        val results = mutableListOf<DuplicateGroup>()

        for ((_, candidateList) in sameSizeGroups) {
            // Stage 1: Fast partial hash
            val partialHashGroups = candidateList.groupBy { file ->
                calculatePartialHash(file.uri, file.size)
            }.filterValues { it.size > 1 }

            // Stage 2: Full SHA-256 on partial-hash collisions
            for ((_, partialCandidates) in partialHashGroups) {
                val fullHashGroups = partialCandidates.groupBy { file ->
                    calculateFullHash(file.uri)
                }.filterValues { it.size > 1 }

                for ((_, exactDuplicates) in fullHashGroups) {
                    val recommendedKeep = selectRecommendedFile(exactDuplicates)
                    results.add(
                        DuplicateGroup(
                            id = UUID.randomUUID().toString(),
                            category = category,
                            files = exactDuplicates,
                            recommendedKeepId = recommendedKeep.id,
                            similarityScore = 1.0f
                        )
                    )
                }
            }
        }

        return results
    }

    /**
     * Legacy method for backward compatibility
     */
    fun findDuplicates(files: List<DeviceFile>): List<List<DeviceFile>> {
        return findExactDuplicates(files).map { it.files }
    }

    /**
     * Two-tier fast partial hash (header + tail).
     */
    private fun calculatePartialHash(uri: Uri, fileSize: Long): String {
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(PARTIAL_HASH_CHUNK_SIZE)
                val readHeader = stream.read(buffer)
                if (readHeader > 0) {
                    digest.update(buffer, 0, readHeader)
                }

                // If file is larger than 2x chunk size, skip to tail
                if (fileSize > PARTIAL_HASH_CHUNK_SIZE * 2) {
                    val toSkip = fileSize - (PARTIAL_HASH_CHUNK_SIZE * 2)
                    try {
                        stream.skip(toSkip)
                        val readTail = stream.read(buffer)
                        if (readTail > 0) {
                            digest.update(buffer, 0, readTail)
                        }
                    } catch (ignored: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed partial hash for $uri: ${e.message}")
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Full cryptographic SHA-256 checksum.
     */
    private fun calculateFullHash(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(65536) // 64KB read buffer
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed full hash for $uri: ${e.message}")
        }

        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Optimized Similar Image Detection:
     * - Batch loads cache records from Room to eliminate N+1 queries.
     * - Uses MobileNetV3 + LiteRT with downsampled decoding to generate unit embeddings.
     * - Batch inserts newly computed embeddings.
     * - Uses pure dot product cosine similarity.
     * - Uses Star/Centroid clustering to eliminate transitive chaining.
     */
    suspend fun findSimilarPhotos(
        files: List<DeviceFile>,
        threshold: Float = 0.90f,
        shouldStop: () -> Boolean = { false },
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onGroupFound: (Int) -> Unit = {}
    ): List<DuplicateGroup> = withContext(Dispatchers.Default) {
        val imageFiles = files.filter { it.isImage }
        if (imageFiles.size < 2) return@withContext emptyList()

        val scanStart = SystemClock.elapsedRealtime()

        // 1. Bulk check Room database cache
        val cachedTuples = withContext(Dispatchers.IO) {
            try {
                embeddingDao.getAllUriAndDateModified().associateBy({ it.imageUri }, { it.dateModified })
            } catch (e: Exception) {
                emptyMap()
            }
        }

        val embeddedItems = mutableListOf<EmbeddingResult>()
        val newEntitiesToInsert = mutableListOf<ImageEmbeddingEntity>()

        ImageEmbeddingAnalyzer(context.applicationContext).use { analyzer ->
            var processed = 0
            val total = imageFiles.size

            for (file in imageFiles) {
                if (shouldStop()) break

                val uriStr = file.uri.toString()
                val cachedDate = cachedTuples[uriStr]

                if (cachedDate != null && cachedDate == file.dateModified) {
                    // Cache hit: load embedding from Room
                    val cachedEntity = withContext(Dispatchers.IO) {
                        embeddingDao.getEmbedding(uriStr)
                    }
                    if (cachedEntity != null) {
                        embeddedItems.add(EmbeddingResult(file, cachedEntity.embedding, false))
                    }
                } else {
                    // Cache miss: execute MobileNetV3 LiteRT inference
                    try {
                        val embedding = analyzer.embedImage(context, file.uri)
                        embeddedItems.add(EmbeddingResult(file, embedding, true))
                        newEntitiesToInsert.add(
                            ImageEmbeddingEntity(
                                imageUri = uriStr,
                                dateModified = file.dateModified,
                                embedding = embedding
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Embedding extraction failed for ${file.displayName}: ${e.message}")
                    }
                }

                processed++
                onProgress(processed, total)

                // Periodic batch insert to prevent unbounded memory growth
                if (newEntitiesToInsert.size >= 50) {
                    Log.e("debug","inserting embeddings: ${newEntitiesToInsert.size}")
                    val batch = newEntitiesToInsert.toList()
                    newEntitiesToInsert.clear()
                    withContext(Dispatchers.IO) {
                        try {
                            embeddingDao.insertEmbeddings(batch)
                        } catch (e: Exception) {
                            Log.e(TAG, "Batch insert failed", e)
                        }
                    }
                }
            }
        }

        // Final batch insert
        if (newEntitiesToInsert.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    embeddingDao.insertEmbeddings(newEntitiesToInsert)
                } catch (e: Exception) {
                    Log.e(TAG, "Final batch insert failed", e)
                }
            }
        }

        if (shouldStop() || embeddedItems.size < 2) return@withContext emptyList()

        // 2. Intelligent Centroid / Star Clustering (prevents transitive chaining)
        val assigned = BooleanArray(embeddedItems.size)
        val groups = mutableListOf<DuplicateGroup>()

        for (i in embeddedItems.indices) {
            if (assigned[i] || shouldStop()) continue

            val clusterMembers = mutableListOf(embeddedItems[i].file)
            var totalSimilarity = 0f
            var pairCount = 0

            for (j in (i + 1) until embeddedItems.size) {
                if (assigned[j]) continue

                val similarity = ImageSimilarity.dotProductSimilarity(
                    embeddedItems[i].embedding,
                    embeddedItems[j].embedding
                )

                if (similarity >= threshold) {
                    clusterMembers.add(embeddedItems[j].file)
                    assigned[j] = true
                    totalSimilarity += similarity
                    pairCount++
                }
            }

            if (clusterMembers.size > 1) {
                assigned[i] = true
                val avgScore = if (pairCount > 0) totalSimilarity / pairCount else threshold
                val recommended = selectRecommendedFile(clusterMembers)

                groups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        category = DuplicateCategory.SIMILAR_PHOTO,
                        files = clusterMembers,
                        recommendedKeepId = recommended.id,
                        similarityScore = avgScore
                    )
                )
                onGroupFound(groups.size)
            }
        }

        Log.d(TAG, "Similar photo scan finished in ${SystemClock.elapsedRealtime() - scanStart} ms. Found ${groups.size} groups.")
        return@withContext groups
    }

    /**
     * Legacy similar image groups method for backward compatibility
     */
    suspend fun findSimilarImageGroups(
        files: List<DeviceFile>,
        mimetype: String,
        threshold: Float = 0.90f,
        shouldStop: () -> Boolean = { false },
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onDuplicateCount: (Int) -> Unit = {}
    ): List<List<DeviceFile>> {
        val groups = findSimilarPhotos(
            files = files,
            threshold = threshold,
            shouldStop = shouldStop,
            onProgress = onProgress,
            onGroupFound = onDuplicateCount
        )
        return groups.map { it.files }
    }

    /**
     * Similar Video Detection:
     * Uses VideoEmbeddingAnalyzer to extract keyframes and composite embeddings,
     * then clusters similar video sequences.
     */
    suspend fun findSimilarVideos(
        files: List<DeviceFile>,
        threshold: Float = 0.88f,
        shouldStop: () -> Boolean = { false },
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onGroupFound: (Int) -> Unit = {}
    ): List<DuplicateGroup> = withContext(Dispatchers.Default) {
        val videoFiles = files.filter { it.isVideo }
        if (videoFiles.size < 2) return@withContext emptyList()

        val embeddedVideos = mutableListOf<Pair<DeviceFile, FloatArray>>()

        ImageEmbeddingAnalyzer(context.applicationContext).use { imageAnalyzer ->
            val videoAnalyzer = VideoEmbeddingAnalyzer(imageAnalyzer)
            var processed = 0
            val total = videoFiles.size

            for (file in videoFiles) {
                if (shouldStop()) break

                val embedding = videoAnalyzer.extractVideoEmbedding(context, file)
                if (embedding != null) {
                    embeddedVideos.add(file to embedding)
                }

                processed++
                onProgress(processed, total)
            }
        }

        if (shouldStop() || embeddedVideos.size < 2) return@withContext emptyList()

        // Cluster similar videos
        val assigned = BooleanArray(embeddedVideos.size)
        val groups = mutableListOf<DuplicateGroup>()

        for (i in embeddedVideos.indices) {
            if (assigned[i] || shouldStop()) continue

            val clusterMembers = mutableListOf(embeddedVideos[i].first)
            var totalSimilarity = 0f
            var pairCount = 0

            for (j in (i + 1) until embeddedVideos.size) {
                if (assigned[j]) continue

                val similarity = ImageSimilarity.dotProductSimilarity(
                    embeddedVideos[i].second,
                    embeddedVideos[j].second
                )

                if (similarity >= threshold) {
                    clusterMembers.add(embeddedVideos[j].first)
                    assigned[j] = true
                    totalSimilarity += similarity
                    pairCount++
                }
            }

            if (clusterMembers.size > 1) {
                assigned[i] = true
                val avgScore = if (pairCount > 0) totalSimilarity / pairCount else threshold
                val recommended = selectRecommendedFile(clusterMembers)

                groups.add(
                    DuplicateGroup(
                        id = UUID.randomUUID().toString(),
                        category = DuplicateCategory.SIMILAR_VIDEO,
                        files = clusterMembers,
                        recommendedKeepId = recommended.id,
                        similarityScore = avgScore
                    )
                )
                onGroupFound(groups.size)
            }
        }

        return@withContext groups
    }

    /**
     * Intelligent recommendation engine to choose the "best item to keep".
     * Prioritizes:
     * 1. Higher pixel resolution (width * height).
     * 2. Larger file size (less compressed).
     * 3. Earliest modification timestamp (original capture).
     */
    fun selectRecommendedFile(files: List<DeviceFile>): DeviceFile {
        return files.maxWithOrNull(
            compareBy<DeviceFile> { it.width * it.height }
                .thenBy { it.size }
                .thenByDescending { it.dateModified } // Older is often original
        ) ?: files.first()
    }

    fun getMediaCounts(): MediaCounts {
        val resolver = context.contentResolver
        fun count(uri: Uri): Int {
            return try {
                resolver.query(uri, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use {
                    it.count
                } ?: 0
            } catch (e: Exception) {
                0
            }
        }

        return MediaCounts(
            totalImages = count(MediaStore.Images.Media.EXTERNAL_CONTENT_URI),
            totalVideos = count(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        )
    }
}