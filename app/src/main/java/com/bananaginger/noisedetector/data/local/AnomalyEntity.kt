package com.bananaginger.noisedetector.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// BananaGinger/Kyryl: Room record for saved anomaly metadata and optional nearby earthquake details.
@Entity(tableName = "anomalies")
data class AnomalyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMillis: Long,
    val soundLevelDb: Double,
    val motionDetected: Boolean,
    val thresholdDb: Double,
    val eventClassification: String,
    val latitude: Double?,
    val longitude: Double?,
    val earthquakeId: String?,
    val earthquakePlace: String?,
    val earthquakeMagnitude: Double?,
    val earthquakeLatitude: Double?,
    val earthquakeLongitude: Double?,
    val earthquakeDepthKm: Double?,
    val earthquakeTimeMillis: Long?
)
