package com.bananaginger.noisedetector.data

/**
 * Entity representing a detected anomaly stored in the local Room database.
 *
 * Fields:
 * - `id`: primary key, auto-generated
 * - `timestamp`: epoch milliseconds when anomaly was detected
 * - `date` / `day`: optional human-readable date info
 * - `type`: anomaly type (e.g. "noise", "motion", "earthquake")
 * - `magnitude`: measured strength (optional)
 * - `severity`: integer severity level (optional)
 * - `description`: optional free-text note
 * - `metadata`: optional JSON string for extra structured data
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
