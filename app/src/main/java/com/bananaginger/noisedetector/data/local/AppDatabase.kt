package com.bananaginger.noisedetector.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

// BananaGinger/Kyryl: Room database for anomaly history used by the GP-5 API integration.
@Database(
    entities = [AnomalyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun anomalyDao(): AnomalyDao
}
