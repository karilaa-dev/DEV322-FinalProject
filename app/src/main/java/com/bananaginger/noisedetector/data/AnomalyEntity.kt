package com.bananaginger.noisedetector.data

/**
 * Entity representing a detected anomaly stored in the local Room database.
 *
 * @property id Primary key, auto-generated.
 * @property timestamp Epoch milliseconds when the anomaly was detected.
 * @property date Human-readable date (e.g. "2026-06-10").
 * @property day Day of week (e.g. "Thursday").
 * @property type Anomaly type (e.g. "noise", "motion", "earthquake").
 * @property magnitude Measured strength (e.g. dB, g-force). Nullable when not available.
 * @property severity Integer severity level for quick filtering/sorting. Nullable if unknown.
 * @property description Optional free-text note describing the anomaly.
 * @property metadata Optional JSON string containing extra structured data.
 */

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "anomalies")
data class AnomalyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val date: String? = null, // human-readable date, e.g. "2026-06-10"
    val day: String? = null, // day of week, e.g. "Thursday"
    val type: String, // e.g. "noise", "motion", "earthquake"
    val magnitude: Double? = null, // measured strength (e.g. dB, g-force)
    val severity: Int? = null, // integer severity level for quick filtering/sorting
    val description: String? = null, // optional user/automated description
    val metadata: String? = null // optional JSON or extra info
)
