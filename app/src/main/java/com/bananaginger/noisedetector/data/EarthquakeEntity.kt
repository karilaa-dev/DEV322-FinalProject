package com.bananaginger.noisedetector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "earthquakes")
data class EarthquakeEntity(
    @PrimaryKey val id: String,
    val place: String,
    val magnitude: Double? = null,
    val latitude: Double,
    val longitude: Double,
    val depthKm: Double? = null,
    val timeMillis: Long? = null,
    val source: String = "USGS",
    val remoteUploadedAt: Long? = null
)
