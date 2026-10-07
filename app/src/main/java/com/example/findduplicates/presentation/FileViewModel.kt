package com.example.findduplicates.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.findduplicates.data.DeviceFile
import com.example.findduplicates.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileViewModel(val repository: FileRepository): ViewModel() {

    private val _files = MutableStateFlow<List<DeviceFile>>(emptyList())
    val files: StateFlow<List<DeviceFile>> = _files

    fun getMediaFiles(){
        viewModelScope.launch {
            _files.value = withContext(Dispatchers.IO) {
                repository.getMediaFiles()
            }
        }
    }
}