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
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AnomalyEntity::class, EarthquakeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // Accessor for anomaly DAO.
    abstract fun anomalyDao(): AnomalyDao
    abstract fun earthquakeDao(): EarthquakeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `earthquakes` (" +
                            "`id` TEXT NOT NULL, " +
                            "`place` TEXT NOT NULL, " +
                            "`magnitude` REAL, " +
                            "`latitude` REAL NOT NULL, " +
                            "`longitude` REAL NOT NULL, " +
                            "`depthKm` REAL, " +
                            "`timeMillis` INTEGER, " +
                            "`source` TEXT NOT NULL DEFAULT 'USGS', " +
                            "`remoteUploadedAt` INTEGER, " +
                            "PRIMARY KEY(`id`)" +
                            ")"
                )
                db.execSQL(
                    "ALTER TABLE `anomalies` " +
                            "ADD COLUMN `accelerationMagnitude` REAL"
                )
                db.execSQL(
                    "ALTER TABLE `anomalies` " +
                            "ADD COLUMN `closestEarthquakeId` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `anomalies` " +
                            "ADD COLUMN `remoteUploadedAt` INTEGER"
                )
                db.execSQL(
                    "ALTER TABLE `anomalies` " +
                            "ADD COLUMN `remoteSyncStatus` TEXT NOT NULL DEFAULT 'PENDING'"
                )
                db.execSQL(
                    "ALTER TABLE `anomalies` " +
                            "ADD COLUMN `remoteError` TEXT"
                )
            }
        }

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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
