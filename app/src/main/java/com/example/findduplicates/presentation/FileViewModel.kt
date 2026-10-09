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

class FileViewModel(val repository: FileRepository): ViewModel() {

    private val _files = MutableStateFlow<List<DeviceFile>>(emptyList())
    val files: StateFlow<List<DeviceFile>> = _files
    private var _isScanning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    var isScanning: StateFlow<Boolean> = _isScanning

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

            _isScanning.value = true

            try {

                val duplicates =
                    repository.findDuplicates(getMediaFiles())

                _duplicateGroups.value =
                    duplicates

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
}