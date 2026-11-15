package com.example.findduplicates.data.local


import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "image_embeddings",
    indices = [Index(value = ["dateModified"])]
)
data class ImageEmbeddingEntity(
    @PrimaryKey val imageUri: String,
    val dateModified: Long, // Used to check if the image has changed on disk
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ImageEmbeddingEntity
        if (imageUri != other.imageUri) return false
        if (dateModified != other.dateModified) return false
        return embedding.contentEquals(other.embedding)
    }

    override fun hashCode(): Int {
        var result = imageUri.hashCode()
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + embedding.contentHashCode()
        return result
    }
}