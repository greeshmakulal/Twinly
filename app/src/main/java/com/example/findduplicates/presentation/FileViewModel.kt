package com.example.findduplicates.presentation

import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findduplicates.data.DeviceFile
import com.example.findduplicates.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

class FileViewModel(val repository: FileRepository): ViewModel() {

    private val _files = MutableStateFlow<List<DeviceFile>>(emptyList())
    val files: StateFlow<List<DeviceFile>> = _files
    private var _isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var isScanning: StateFlow<Boolean> = _isScanning
    private val stopScanRequested = AtomicBoolean(false)
    private val _scanProgress = MutableStateFlow(0)
    val scanProgress: StateFlow<Int> = _scanProgress.asStateFlow()

    private val _scanTotal = MutableStateFlow(0)
    val scanTotal: StateFlow<Int> = _scanTotal.asStateFlow()

    private val _scanStage = MutableStateFlow("Ready to scan")
    val scanStage: StateFlow<String> = _scanStage.asStateFlow()

    private val _liveDuplicateCount = MutableStateFlow(0)
    val liveDuplicateCount: StateFlow<Int> =
        _liveDuplicateCount.asStateFlow()

    fun stopScan() {
        stopScanRequested.set(true)
        _scanStage.value = "Stopping scan..."
    }
    fun getMediaFiles(): List<DeviceFile> {

        val files = mutableListOf<DeviceFile>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            files += repository.getMediaFiles(
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            )

            files += repository.getMediaFiles(
                MediaStore.Video.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            )

            files += repository.getMediaFiles(
                MediaStore.Audio.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
                )
            )

        } else {

            files += repository.getMediaFiles(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )

            files += repository.getMediaFiles(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )

            files += repository.getMediaFiles(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            )
        }

        Log.d(
            "MEDIA_DEBUG",
            "Total media files = ${files.size}"
        )

        return files
    }
    private val _duplicateGroups =
        MutableStateFlow<List<List<DeviceFile>>>(emptyList())

    val duplicateGroups =
        _duplicateGroups.asStateFlow()

    fun scanForDuplicates() {
        viewModelScope.launch(Dispatchers.IO) {
            _liveDuplicateCount.value = 0
            stopScanRequested.set(false)
            _isScanning.value = true
            _scanProgress.value = 0
            _scanTotal.value = 0
            _scanStage.value = "Loading files..."

            try {
                val scannedFiles = getMediaFiles()

                val imageCount = scannedFiles.count {
                    it.mimeType?.startsWith("image/", ignoreCase = true) == true
                }

                _scanTotal.value = imageCount
                _scanStage.value = "Checking exact duplicates..."

                val exactDuplicates = repository.findDuplicates(scannedFiles)

                _liveDuplicateCount.value = exactDuplicates.size
                _scanStage.value = "Analyzing similar images..."

                _scanStage.value = "Analyzing similar images..."
                val similarImages = repository.findSimilarImageGroups(
                    files = scannedFiles,
                    shouldStop = { stopScanRequested.get() },
                    onProgress = { processed, total ->
                        _scanProgress.value = processed
                        _scanTotal.value = total
                    },
                    onDuplicateCount = { count ->
                        _liveDuplicateCount.value = exactDuplicates.size + count
                    }
                )

                _duplicateGroups.value = mergeGroups(
                    exactDuplicates + similarImages
                )

                _liveDuplicateCount.value = _duplicateGroups.value.size
                _scanStage.value = if (stopScanRequested.get()) {
                    "Scan stopped"
                } else {
                    "Scan complete"
                }

            } catch (e: Exception) {
                Log.e("FileViewModel", "Media scan failed", e)
                _scanStage.value = "Scan failed"
            } finally {
                _isScanning.value = false
            }
        }
    }
    fun removeDeletedFiles(deletedIds: Set<Long>) {

        _duplicateGroups.value =
            _duplicateGroups.value
                .map { group ->

                    group.filterNot {
                        it.id in deletedIds
                    }
                }
                .filter {
                    it.size > 1
                }
    }
    private fun mergeGroups(
        groups: List<List<DeviceFile>>
    ): List<List<DeviceFile>> {

        val pending = groups
            .filter { it.size > 1 }
            .map { it.distinctBy { file -> file.uri } }
            .toMutableList()

        val result = mutableListOf<List<DeviceFile>>()

        while (pending.isNotEmpty()) {
            val current = pending.removeAt(0).toMutableList()
            var changed: Boolean

            do {
                changed = false
                val iterator = pending.iterator()

                while (iterator.hasNext()) {
                    val candidate = iterator.next()

                    val overlaps = candidate.any { candidateFile ->
                        current.any { currentFile ->
                            currentFile.uri == candidateFile.uri
                        }
                    }

                    if (overlaps) {
                        current.addAll(candidate)
                        iterator.remove()
                        changed = true
                    }
                }
            } while (changed)

            result.add(current.distinctBy { it.uri })
        }

        return result
    }
}