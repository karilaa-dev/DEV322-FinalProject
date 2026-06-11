package com.bananaginger.noisedetector.data

/**
 * Room database for the application.
 *
 * Exposes DAOs and provides a singleton `getInstance` helper.
 */

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AnomalyEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    // Accessor for anomaly DAO.
    abstract fun anomalyDao(): AnomalyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Return a singleton AppDatabase instance.
         * Uses double-checked locking to ensure only one instance is created.
         */
        fun getInstance(context: Context): AppDatabase {
            // Return existing instance if available, otherwise create it safely.
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
