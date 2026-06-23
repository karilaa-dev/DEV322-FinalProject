package com.bananaginger.noisedetector.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anomalies")
data class AnomalyEntity(
    @PrimaryKey val id: Long = 0,
    val timestamp: Long,
    val date: String? = null,
    val day: String? = null,
    val type: String,
    val magnitude: Double? = null,
    val accelerationMagnitude: Float? = null,
    val soundThresholdDb: Double? = null,
    val motionThreshold: Float? = null,
    val soundThresholdExceeded: Boolean? = null,
    val motionThresholdExceeded: Boolean? = null,
    val severity: Int? = null,
    val description: String? = null,
    val metadata: String? = null,
    val closestEarthquakeId: String? = null,
    val remoteUploadedAt: Long? = null,
    val remoteSyncStatus: String = SYNC_PENDING,
    val remoteError: String? = null
) {
    companion object {
        const val SYNC_PENDING = "PENDING"
        const val SYNC_UPLOADED = "UPLOADED"
        const val SYNC_FAILED = "FAILED"
    }
}
