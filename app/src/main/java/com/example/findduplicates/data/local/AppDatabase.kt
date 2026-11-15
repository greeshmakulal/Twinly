package com.example.findduplicates.data.local

import android.content.Context
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import com.example.findduplicates.data.local.converters.EmbeddingTypeConverter


@Database(entities = [ImageEmbeddingEntity::class], version = 3, exportSchema = false)
@ColumnTypeConverters(EmbeddingTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun embeddingDao(): EmbeddingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "duplicate_finder_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}