package com.example.findduplicates.data

enum class DuplicateCategory(val title: String) {
    EXACT_PHOTO("Exact Duplicate Photos"),
    SIMILAR_PHOTO("Similar Photos"),
    EXACT_VIDEO("Exact Duplicate Videos"),
    SIMILAR_VIDEO("Similar Videos")
}

data class DuplicateGroup(
    val id: String,
    val category: DuplicateCategory,
    val files: List<DeviceFile>,
    val recommendedKeepId: Long,
    val similarityScore: Float = 1.0f
) {
    val totalSize: Long get() = files.sumOf { it.size }
    val recoverableSize: Long
        get() {
            val keep = files.find { it.id == recommendedKeepId } ?: files.firstOrNull()
            return if (keep != null) totalSize - keep.size else 0L
        }
    val count: Int get() = files.size
}

/**
 * Legacy wrapper preserved for backward compatibility
 */
data class DuplicateFile(
    val id: Int,
    val files: List<DeviceFile>
)

