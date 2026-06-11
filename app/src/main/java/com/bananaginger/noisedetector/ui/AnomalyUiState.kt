package com.bananaginger.noisedetector.ui

import com.bananaginger.noisedetector.data.local.AnomalyEntity

// BananaGinger/Kyryl: Screen state for anomaly history, loading, status, and recoverable errors.
data class AnomalyUiState(
    val anomalies: List<AnomalyEntity> = emptyList(),
    val isLoading: Boolean = false,
    val statusMessage: String = "No demo anomaly recorded yet.",
    val warningMessage: String? = null,
    val errorMessage: String? = null
)
