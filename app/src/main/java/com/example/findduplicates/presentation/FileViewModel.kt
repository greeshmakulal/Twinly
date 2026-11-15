
package com.example.findduplicates.presentation

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findduplicates.data.DeviceFile
import com.example.findduplicates.data.DuplicateCategory
import com.example.findduplicates.data.DuplicateGroup
import com.example.findduplicates.data.MediaCounts
import com.example.findduplicates.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException

class FileViewModel(val repository: FileRepository) : ViewModel() {

    companion object {
        private const val TAG = "FileViewModel"
    }

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _files = MutableStateFlow<List<DeviceFile>>(emptyList())
    val files: StateFlow<List<DeviceFile>> = _files.asStateFlow()

    private val stopScanRequested = AtomicBoolean(false)
    private var scanJob: Job? = null

    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress.asStateFlow()

    private val _scanTotal = MutableStateFlow(0)
    val scanTotal: StateFlow<Int> = _scanTotal.asStateFlow()

    private val _scanStage = MutableStateFlow("Ready to scan")
    val scanStage: StateFlow<String> = _scanStage.asStateFlow()

    // 4 Dedicated categories
    private val _exactPhotoGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val exactPhotoGroups: StateFlow<List<DuplicateGroup>> = _exactPhotoGroups.asStateFlow()

    private val _similarPhotoGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val similarPhotoGroups: StateFlow<List<DuplicateGroup>> = _similarPhotoGroups.asStateFlow()

    private val _exactVideoGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val exactVideoGroups: StateFlow<List<DuplicateGroup>> = _exactVideoGroups.asStateFlow()

    private val _similarVideoGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val similarVideoGroups: StateFlow<List<DuplicateGroup>> = _similarVideoGroups.asStateFlow()

    // Selected files for deletion across all categories
    private val _selectedFileIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFileIds: StateFlow<Set<Long>> = _selectedFileIds.asStateFlow()

    // Counts
    private val _mediaCounts = MutableStateFlow(MediaCounts())
    val mediaCounts: StateFlow<MediaCounts> = _mediaCounts.asStateFlow()

    val exactDuplicateCount: StateFlow<Int> = combine(
        _exactPhotoGroups,
        _exactVideoGroups
    ) { photos, videos -> photos.size + videos.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val similarDuplicateCount: StateFlow<Int> = combine(
        _similarPhotoGroups,
        _similarVideoGroups
    ) { photos, videos -> photos.size + videos.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val liveDuplicateCount: StateFlow<Int> = combine(
        exactDuplicateCount,
        similarDuplicateCount
    ) { exact, similar -> exact + similar }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Legacy backward compatibility: flattened list of duplicate groups
    val duplicateGroups: StateFlow<List<List<DeviceFile>>> = combine(
        _exactPhotoGroups,
        _similarPhotoGroups,
        _exactVideoGroups,
        _similarVideoGroups
    ) { ep, sp, ev, sv ->
        (ep + sp + ev + sv).map { it.files }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recoverable storage metrics
    val totalRecoverableBytes: StateFlow<Long> = combine(
        _exactPhotoGroups,
        _similarPhotoGroups,
        _exactVideoGroups,
        _similarVideoGroups
    ) { ep, sp, ev, sv ->
        (ep + sp + ev + sv).sumOf { it.recoverableSize }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    // Selected recoverable size
    val selectedRecoverableBytes: StateFlow<Long> = combine(
        _selectedFileIds,
        _exactPhotoGroups,
        _similarPhotoGroups,
        _exactVideoGroups,
        _similarVideoGroups
    ) { selectedIds, ep, sp, ev, sv ->
        val allFiles = (ep + sp + ev + sv).flatMap { it.files }.associateBy { it.id }
        selectedIds.sumOf { allFiles[it]?.size ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init {
        loadMediaCounts()
    }

    fun stopScan() {
        stopScanRequested.set(true)
        _scanStage.value = "Stopping scan..."
    }

    fun loadMediaCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            _mediaCounts.value = repository.getMediaCounts()
        }
    }

    /**
     * Scans MediaStore for all photo and video records.
     */
    fun getMediaFiles(): List<DeviceFile> {
        val files = mutableListOf<DeviceFile>()
        val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val videoUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        files += repository.getMediaFiles(imageUri)
        files += repository.getMediaFiles(videoUri)
        return files
    }

    /**
     * Unified scanning method handling Photos, Videos, or All Media.
     */
    fun scanMedia(
        scanType: ScanTarget,
        maxLimit: Int? = null,
        similarityThreshold: Float = 0.90f
    ) {
        if (scanJob?.isActive == true) return
        stopScanRequested.set(false)

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _scanProgress.value = 0
            _scanTotal.value = 0
            _selectedFileIds.value = emptySet()
            _scanStage.value = "Indexing media files..."

            try {
                currentCoroutineContext().ensureActive()
                val allFiles = getMediaFiles()

                // Filter according to target
                val targetFiles = when (scanType) {
                    ScanTarget.PHOTOS -> allFiles.filter { it.isImage }
                    ScanTarget.VIDEOS -> allFiles.filter { it.isVideo }
                    ScanTarget.ALL -> allFiles
                }

                val filesToScan = if (maxLimit != null && maxLimit > 0) {
                    targetFiles.take(maxLimit)
                } else {
                    targetFiles
                }

                _scanTotal.value = filesToScan.size

                val imageFiles = filesToScan.filter { it.isImage }
                val videoFiles = filesToScan.filter { it.isVideo }

                // 1. Photos Exact Duplicates
                if (imageFiles.size >= 2 && (scanType == ScanTarget.PHOTOS || scanType == ScanTarget.ALL)) {
                    _scanStage.value = "Checking exact duplicate photos..."
                    val exactPhotos = repository.findExactDuplicates(imageFiles, DuplicateCategory.EXACT_PHOTO)
                    _exactPhotoGroups.value = exactPhotos
                }

                if (stopScanRequested.get()) return@launch

                // 2. Photos Similar Detection (MobileNetV3 + LiteRT)
                if (imageFiles.size >= 2 && (scanType == ScanTarget.PHOTOS || scanType == ScanTarget.ALL)) {
                    _scanStage.value = "Analyzing visual photo similarity..."
                    val similarPhotos = repository.findSimilarPhotos(
                        files = imageFiles,
                        threshold = similarityThreshold,
                        shouldStop = { stopScanRequested.get() },
                        onProgress = { processed, total ->
                            _scanProgress.value = processed
                            _scanTotal.value = total
                        }
                    )
                    _similarPhotoGroups.value = similarPhotos
                }

                if (stopScanRequested.get()) return@launch

                // 3. Videos Exact Duplicates (Two-tier SHA-256)
                if (videoFiles.size >= 2 && (scanType == ScanTarget.VIDEOS || scanType == ScanTarget.ALL)) {
                    _scanStage.value = "Checking exact duplicate videos..."
                    val exactVideos = repository.findExactDuplicates(videoFiles, DuplicateCategory.EXACT_VIDEO)
                    _exactVideoGroups.value = exactVideos
                }

                if (stopScanRequested.get()) return@launch

                // 4. Videos Similar Detection (Keyframe sampling)
                if (videoFiles.size >= 2 && (scanType == ScanTarget.VIDEOS || scanType == ScanTarget.ALL)) {
                    _scanStage.value = "Analyzing video similarities..."
                    val similarVideos = repository.findSimilarVideos(
                        files = videoFiles,
                        threshold = (similarityThreshold - 0.04f).coerceAtLeast(0.85f),
                        shouldStop = { stopScanRequested.get() },
                        onProgress = { processed, total ->
                            _scanProgress.value = processed
                            _scanTotal.value = total
                        }
                    )
                    _similarVideoGroups.value = similarVideos
                }

                _scanStage.value = "Scan complete"
            } catch (e: CancellationException) {
                _scanStage.value = "Scan stopped"
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Scan failed", e)
                _scanStage.value = "Scan encountered an error"
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * Legacy method backward-compatibility wrapper
     */
    fun scanForDuplicates(mediaType: String, maxLimit: Int? = null) {
        val target = when {
            mediaType.startsWith("video", ignoreCase = true) -> ScanTarget.VIDEOS
            mediaType.startsWith("all", ignoreCase = true) -> ScanTarget.ALL
            else -> ScanTarget.PHOTOS
        }
        scanMedia(target, maxLimit)
    }

    // Selection controls
    fun toggleFileSelection(fileId: Long) {
        val current = _selectedFileIds.value.toMutableSet()
        if (current.contains(fileId)) {
            current.remove(fileId)
        } else {
            current.add(fileId)
        }
        _selectedFileIds.value = current
    }

    fun selectAllDuplicatesInGroup(group: DuplicateGroup) {
        val current = _selectedFileIds.value.toMutableSet()
        // Select all except the recommended one
        for (file in group.files) {
            if (file.id != group.recommendedKeepId) {
                current.add(file.id)
            }
        }
        _selectedFileIds.value = current
    }

    fun deselectGroup(group: DuplicateGroup) {
        val current = _selectedFileIds.value.toMutableSet()
        for (file in group.files) {
            current.remove(file.id)
        }
        _selectedFileIds.value = current
    }

    fun selectAllRecommendedDuplicates(category: DuplicateCategory? = null) {
        val current = _selectedFileIds.value.toMutableSet()
        val groupsToProcess = when (category) {
            DuplicateCategory.EXACT_PHOTO -> _exactPhotoGroups.value
            DuplicateCategory.SIMILAR_PHOTO -> _similarPhotoGroups.value
            DuplicateCategory.EXACT_VIDEO -> _exactVideoGroups.value
            DuplicateCategory.SIMILAR_VIDEO -> _similarVideoGroups.value
            null -> _exactPhotoGroups.value + _similarPhotoGroups.value + _exactVideoGroups.value + _similarVideoGroups.value
        }

        for (group in groupsToProcess) {
            for (file in group.files) {
                if (file.id != group.recommendedKeepId) {
                    current.add(file.id)
                }
            }
        }
        _selectedFileIds.value = current
    }

    fun deselectAll() {
        _selectedFileIds.value = emptySet()
    }

    /**
     * Creates a delete PendingIntent via MediaStore.createDeleteRequest for Scoped Storage (Android 11+).
     */
    fun createDeleteIntent(context: Context, fileIds: Set<Long>): PendingIntent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val allFiles = (_exactPhotoGroups.value + _similarPhotoGroups.value +
                    _exactVideoGroups.value + _similarVideoGroups.value)
                .flatMap { it.files }
                .associateBy { it.id }

            val uris = fileIds.mapNotNull { allFiles[it]?.uri }
            if (uris.isEmpty()) return null

            return MediaStore.createDeleteRequest(context.contentResolver, uris)
        }
        return null
    }

    /**
     * Updates internal group states after files are deleted by user.
     */
    fun updateGroupsAfterDeletion(deletedIds: Set<Long>) {
        if (deletedIds.isEmpty()) return

        fun filterGroups(groups: List<DuplicateGroup>): List<DuplicateGroup> {
            return groups.mapNotNull { group ->
                val remaining = group.files.filterNot { it.id in deletedIds }
                if (remaining.size >= 2) {
                    val newKeep = if (remaining.any { it.id == group.recommendedKeepId }) {
                        group.recommendedKeepId
                    } else {
                        repository.selectRecommendedFile(remaining).id
                    }
                    group.copy(files = remaining, recommendedKeepId = newKeep)
                } else null
            }
        }

        _exactPhotoGroups.value = filterGroups(_exactPhotoGroups.value)
        _similarPhotoGroups.value = filterGroups(_similarPhotoGroups.value)
        _exactVideoGroups.value = filterGroups(_exactVideoGroups.value)
        _similarVideoGroups.value = filterGroups(_similarVideoGroups.value)

        val updatedSelection = _selectedFileIds.value.toMutableSet()
        updatedSelection.removeAll(deletedIds)
        _selectedFileIds.value = updatedSelection

        loadMediaCounts()
    }

    fun removeDeletedFiles(deletedIds: Set<Long>) {
        updateGroupsAfterDeletion(deletedIds)
    }

    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        if (size < 1024) return "$size B"
        val kb = size / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }
}

enum class ScanTarget {
    PHOTOS,
    VIDEOS,
    ALL
}
