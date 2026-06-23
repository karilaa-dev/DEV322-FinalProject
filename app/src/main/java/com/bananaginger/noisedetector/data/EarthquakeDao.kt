package com.bananaginger.noisedetector.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EarthquakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(earthquake: EarthquakeEntity)

    @Query("SELECT * FROM earthquakes ORDER BY timeMillis DESC")
    fun observeAll(): Flow<List<EarthquakeEntity>>

    @Query("SELECT * FROM earthquakes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EarthquakeEntity?
}
