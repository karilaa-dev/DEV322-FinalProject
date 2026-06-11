package com.bananaginger.noisedetector.data.remote

import com.google.gson.annotations.SerializedName

data class EarthquakeResponseDto(
    val features: List<EarthquakeFeatureDto> = emptyList()
)

data class EarthquakeFeatureDto(
    val id: String?,
    val properties: EarthquakePropertiesDto?,
    val geometry: EarthquakeGeometryDto?
)

data class EarthquakePropertiesDto(
    @SerializedName("mag")
    val magnitude: Double?,
    val place: String?,
    val time: Long?
)

data class EarthquakeGeometryDto(
    val coordinates: List<Double>?
)
