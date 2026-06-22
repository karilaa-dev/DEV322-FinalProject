package com.bananaginger.noisedetector.ui

import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.remote.RemoteAnomalyDocument
import com.bananaginger.noisedetector.data.remote.RemoteDataFilter
import com.bananaginger.noisedetector.data.remote.RemoteDataKind
import com.bananaginger.noisedetector.data.remote.RemoteEarthquakeDocument
import com.bananaginger.noisedetector.history.HistoryEntry

// BananaGinger/Kyryl: Screen state for testing one USGS earthquake API lookup.
data class AnomalyUiState(
    val isLoading: Boolean = false,
    val statusMessage: String = "Tap the button to test the nearby earthquake lookup.",
    val earthquake: EarthquakeSummary? = null,
    val errorMessage: String? = null,

    val isMonitoring: Boolean = false,
    val showHistory: Boolean = false,
    val showRemoteData: Boolean = false,
    val showMapPicker: Boolean = false,

    val estimatedSoundLevelDb: Double = 0.0,
    val accelerationMagnitude: Float = 0.0f,
    val motionDetected: Boolean = false,

    val anomalyDetected: Boolean = false,
    val showAnomalyDialog: Boolean = false,

    // threshholds
    val soundThresholdDb: Double = HistoryEntry.SOUND_THRESHOLD_DB,
    val motionThreshold: Float = HistoryEntry.MOTION_THRESHOLD,

    val selectedLocation: LocationSnapshot? = null,
    val locationSourceLabel: String? = null,
    val locationChoiceRequired: Boolean = true,
    val isResolvingLocation: Boolean = false,

    val isUploadingHistory: Boolean = false,
    val uploadStatusMessage: String = "",
    val remoteDataKind: RemoteDataKind = RemoteDataKind.ANOMALIES,
    val remoteDataFilter: RemoteDataFilter = RemoteDataFilter.ALL,
    val isLoadingRemoteData: Boolean = false,
    val remoteAnomalies: List<RemoteAnomalyDocument> = emptyList(),
    val remoteEarthquakes: List<RemoteEarthquakeDocument> = emptyList(),
    val remoteErrorMessage: String? = null,

    // anomaly history list
    /**
     * In-memory detection history for the current session.
     * Survives rotation and going to background; cleared when the process is killed.
     * Entries are appended by AnomalyViewModel when sensor thresholds are crossed.
     * Room-backed popup detections are also restored into this list when available.
     */
    val historyEntries: List<HistoryEntry> = emptyList()
)
