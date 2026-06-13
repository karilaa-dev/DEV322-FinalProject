package com.bananaginger.noisedetector.ui

import com.bananaginger.noisedetector.data.model.EarthquakeSummary

// BananaGinger/Kyryl: Screen state for testing one USGS earthquake API lookup.
data class AnomalyUiState(
    val isLoading: Boolean = false,
    val statusMessage: String = "Tap the button to test the nearby earthquake lookup.",
    val earthquake: EarthquakeSummary? = null,
    val errorMessage: String? = null,

    val isMonitoring: Boolean = false,
    val showHistory: Boolean = false,

    // BananaGinger/Dylan: level values for sensors

    val estimatedSoundLevelDb: Double = 0.0,
    val accelerationMagnitude: Float = 0.0f,
    val motionDetected: Boolean = false
)
