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
         *
         * @param context Application or activity context used to build the database.
         * @return The `AppDatabase` singleton instance.
         */
        fun getInstance(context: Context): AppDatabase {
            // Return existing instance if available; use double-checked locking
            // to avoid creating multiple instances under concurrency.
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
