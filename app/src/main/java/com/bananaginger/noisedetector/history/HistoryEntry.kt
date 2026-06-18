package com.bananaginger.noisedetector.history

/**
 * HistoryEntry — one recorded detection event kept in-memory for the session.
 *
 * Lifecycle:
 *  - Stored in ViewModel memory: survives rotation and going to background.
 *  - Cleared when the process is killed (app fully closed).
 *
 * Each entry is also pushed to the remote server via AnomalyServerSync.
 *
 * Recording thresholds (deliberately low so history is never empty):
 *  - SOUND  : estimatedSoundLevelDb  > SOUND_THRESHOLD_DB  (30.0 dB)
 *  - MOTION : deviationFromGravity   > MOTION_THRESHOLD    (0.5 m/s²)
 *  - EARTHQUAKE : always when the API returns a result
 *
 * Debounce: at most one entry per type every MIN_INTERVAL_MS (5 000 ms).
 */
data class HistoryEntry(
    /** Unique id — epoch-ms of recording, unique enough for a session list. */
    val id: Long = System.currentTimeMillis(),

    /** Unix epoch ms when the anomaly was detected. */
    val timestamp: Long,

    /** Human-readable date, e.g. "2026-06-16". */
    val date: String,

    /** Human-readable time, e.g. "14:35:22". */
    val time: String,

    /** Day of week, e.g. "Monday". */
    val day: String,

    /** Detection type: "SOUND", "MOTION", or "EARTHQUAKE". */
    val type: String,

    /** Sound level in dB at the moment of recording (null for EARTHQUAKE entries). */
    val soundLevelDb: Double?,

    /** Raw accelerometer magnitude in m/s² (null for EARTHQUAKE entries). */
    val accelerationMagnitude: Float?,

    /** Whether motion threshold was exceeded at the moment of recording. */
    val motionDetected: Boolean,

    /**
     * Severity score 1–5 computed from the raw sensor values:
     *  SOUND   : 1 (30–50 dB) … 5 (>100 dB)
     *  MOTION  : 1 (0.5–1 m/s²) … 5 (>10 m/s²)
     *  EARTHQUAKE : 1 (M<2) … 5 (M≥6)
     */
    val severity: Int,

    /** Short human-readable summary, e.g. "Sound spike 72.3 dB". */
    val description: String,

    /* ---- Earthquake-only fields (null for SOUND / MOTION) ---- */

    /** Richter magnitude of a nearby earthquake, or null. */
    val earthquakeMagnitude: Double? = null,

    /** Descriptive place string from USGS API, or null. */
    val earthquakePlace: String? = null,

    /** Earthquake latitude, or null. */
    val latitude: Double? = null,

    /** Earthquake longitude, or null. */
    val longitude: Double? = null,

    /** Earthquake depth in km, or null. */
    val depthKm: Double? = null
) {
    companion object {
        const val TYPE_SOUND      = "SOUND"
        const val TYPE_MOTION     = "MOTION"
        const val TYPE_EARTHQUAKE = "EARTHQUAKE"

        // dylan combined sound and motion
        const val TYPE_SOUND_AND_MOTION = "SOUND_AND_MOTION"

        /** Recording threshold for sound (deliberately low). */
        const val SOUND_THRESHOLD_DB = 30.0

        /** Recording threshold for motion deviation from gravity (deliberately low). */
        const val MOTION_THRESHOLD = 0.5f

        /** Minimum ms between two consecutive records of the same type (debounce). */
        const val MIN_INTERVAL_MS = 5_000L
    }
}
