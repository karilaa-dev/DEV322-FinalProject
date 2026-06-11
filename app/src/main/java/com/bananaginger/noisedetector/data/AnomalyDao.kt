package com.bananaginger.noisedetector.data

/**
 * Data Access Object for the `anomalies` table.
 *
 * Write operations are `suspend` functions. Read queries return cold `Flow` streams
 * that emit updates when the underlying table changes.
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
    /**
     * Insert a single anomaly.
     *
     * @param anomaly An `AnomalyEntity` to insert or replace.
     * @return The generated row id for the inserted entity.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(anomaly: AnomalyEntity): Long

    // Insert multiple anomalies in a single batch.
    /**
     * Insert multiple anomalies in a single batch.
     *
     * @param anomalies List of `AnomalyEntity` to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(anomalies: List<AnomalyEntity>)

    // Update an existing anomaly (matched by primary key).
    /**
     * Update an existing anomaly (matched by primary key).
     *
     * @param anomaly `AnomalyEntity` containing updated fields. The primary key
     * determines which row is updated.
     */
    @Update
    suspend fun update(anomaly: AnomalyEntity)

    // Delete a specific anomaly record.
    /**
     * Delete a specific anomaly record.
     *
     * @param anomaly `AnomalyEntity` to delete.
     */
    @Delete
    suspend fun delete(anomaly: AnomalyEntity)

    // Observe all anomalies sorted by newest first.
    /**
     * Observe all anomalies, ordered by `timestamp` descending (newest first).
     *
     * @return A `Flow` that emits the full list of anomalies.
     */
    @Query("SELECT * FROM anomalies ORDER BY timestamp DESC")
    fun getAll(): Flow<List<AnomalyEntity>>

    // Observe a single anomaly by id. Emits null if not found.
    /**
     * Observe a single anomaly by its primary key id.
     *
     * @param id Primary key of the anomaly.
     * @return A `Flow` that emits the `AnomalyEntity` or `null` if not found.
     */
    @Query("SELECT * FROM anomalies WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<AnomalyEntity?>

    // Observe anomalies detected since the given epoch millis.
    /**
     * Observe anomalies detected since the given epoch milliseconds.
     *
     * @param since Epoch milliseconds cutoff (inclusive).
     * @return A `Flow` that emits a list of anomalies with `timestamp >= since`.
     */
    @Query("SELECT * FROM anomalies WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getSince(since: Long): Flow<List<AnomalyEntity>>

    // Remove records older than supplied epoch millis (cleanup operation).
    /**
     * Remove records older than the supplied epoch milliseconds.
     *
     * @param cutoff Epoch milliseconds. Rows with `timestamp < cutoff` will be deleted.
     */
    @Query("DELETE FROM anomalies WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
