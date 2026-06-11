package com.bananaginger.noisedetector.data.model

// BananaGinger/Kyryl: Small domain model with only the earthquake fields saved to Room.
data class EarthquakeSummary(
    val id: String,
    val place: String,
    val magnitude: Double?,
    val latitude: Double,
    val longitude: Double,
    val depthKm: Double?,
    val timeMillis: Long?
)
