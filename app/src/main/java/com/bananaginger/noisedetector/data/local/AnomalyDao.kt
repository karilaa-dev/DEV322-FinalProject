package com.bananaginger.noisedetector.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// BananaGinger/Kyryl: DAO exposes anomaly history as Flow so UI observes Room through the repository.
@Dao
interface AnomalyDao {
    @Query("SELECT * FROM anomalies ORDER BY timestampMillis DESC")
    fun observeAnomalies(): Flow<List<AnomalyEntity>>

    @Insert
    suspend fun insertAnomaly(anomaly: AnomalyEntity): Long
}
