package com.bananaginger.noisedetector.data

/**
 * Data Access Object for the `anomalies` table.
 *
 * Methods return `Flow` for observable queries and `suspend` for writes.
 */

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AnomalyDao {
    // Insert a single anomaly. Returns generated row id.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anomaly: AnomalyEntity): Long

    // Insert multiple anomalies in a single batch.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(anomalies: List<AnomalyEntity>)

    // Update an existing anomaly (matched by primary key).
    @Update
    suspend fun update(anomaly: AnomalyEntity)

    // Delete a specific anomaly record.
    @Delete
    suspend fun delete(anomaly: AnomalyEntity)

    // Observe all anomalies sorted by newest first.
    @Query("SELECT * FROM anomalies ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AnomalyEntity>>

    // Observe a single anomaly by id. Emits null if not found.
    @Query("SELECT * FROM anomalies WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<AnomalyEntity?>

    // Observe anomalies detected since the given epoch millis.
    @Query("SELECT * FROM anomalies WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSince(since: Long): Flow<List<AnomalyEntity>>

    // Remove records older than supplied epoch millis (cleanup operation).
    @Query("DELETE FROM anomalies WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
