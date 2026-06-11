package com.bananaginger.noisedetector.data.model

// BananaGinger/Kyryl: Domain input created by the ViewModel before repository persistence.
data class AnomalyEvent(
    val timestampMillis: Long,
    val soundLevelDb: Double,
    val motionDetected: Boolean,
    val thresholdDb: Double,
    val eventClassification: String
)
