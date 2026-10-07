package com.example.findduplicates.data

import android.net.Uri

data class DeviceFile (
    val id: Long,
    val displayName: String,
    val size: Long,
    val uri: Uri,
    val mimeType: String?,
    val dateModified: Long,
)
