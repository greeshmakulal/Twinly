package com.example.findduplicates.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query


@Dao
interface EmbeddingDao {

    @Query("SELECT * FROM image_embeddings WHERE imageUri = :uri")
    suspend fun getEmbedding(uri: String): ImageEmbeddingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmbedding(entity: ImageEmbeddingEntity)

    @Query("DELETE FROM image_embeddings WHERE imageUri NOT IN (:activeUris)")
    suspend fun deleteStaleEmbeddings(activeUris: List<String>)
}