package com.example.findduplicates.data

import android.net.Uri

data class DeviceFile(
    val id: Long,
    val displayName: String,
    val size: Long,
    val uri: Uri,
    val mimeType: String?,
    val dateModified: Long,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val relativePath: String = ""
) {
    val isVideo: Boolean get() = mimeType?.startsWith("video/", ignoreCase = true) == true
    val isImage: Boolean get() = mimeType?.startsWith("image/", ignoreCase = true) == true
    val resolution: String get() = if (width > 0 && height > 0) "${width}x${height}" else ""
}

