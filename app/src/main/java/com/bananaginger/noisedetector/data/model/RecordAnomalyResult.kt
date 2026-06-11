package com.bananaginger.noisedetector.data.model

// BananaGinger/Kyryl: Result lets the ViewModel show saved status and recoverable API warnings.
data class RecordAnomalyResult(
    val anomalyId: Long,
    val earthquake: EarthquakeSummary?,
    val warningMessage: String? = null
)
