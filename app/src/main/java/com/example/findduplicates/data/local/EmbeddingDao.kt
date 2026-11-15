package com.example.findduplicates.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction

@Dao
interface EmbeddingDao {

    @Query("SELECT * FROM image_embeddings WHERE imageUri = :uri")
    suspend fun getEmbedding(uri: String): ImageEmbeddingEntity?

    @Query("SELECT * FROM image_embeddings WHERE imageUri IN (:uris)")
    suspend fun getEmbeddingsByUris(uris: List<String>): List<ImageEmbeddingEntity>

    @Query("SELECT * FROM image_embeddings")
    suspend fun getAllEmbeddings(): List<ImageEmbeddingEntity>

    @Query("SELECT imageUri, dateModified FROM image_embeddings")
    suspend fun getAllUriAndDateModified(): List<UriDateModifiedTuple>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(entity: ImageEmbeddingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbeddings(entities: List<ImageEmbeddingEntity>)

    @Query("DELETE FROM image_embeddings WHERE imageUri NOT IN (:activeUris)")
    suspend fun deleteStaleEmbeddings(activeUris: List<String>)

    @Query("DELETE FROM image_embeddings")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM image_embeddings")
    suspend fun getEmbeddingCount(): Long
}

data class UriDateModifiedTuple(
    val imageUri: String,
    val dateModified: Long
)