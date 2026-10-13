package com.example.findduplicates.repository

import com.example.findduplicates.data.DeviceFile

data class EmbeddingResult(
    val file: DeviceFile,
    val embedding: FloatArray,
    val isNewOrModified: Boolean // True if TFLite ran, false if loaded from Room
)