package com.bananaginger.noisedetector.data.remote

import com.bananaginger.noisedetector.data.model.EarthquakeSummary

// BananaGinger/Kyryl: USGS GeoJSON coordinates are longitude, latitude, depth.
fun EarthquakeFeatureDto.toEarthquakeSummary(): EarthquakeSummary? {
    val eventId = id?.takeIf { it.isNotBlank() } ?: return null
    val coordinates = geometry?.coordinates ?: return null
    val longitude = coordinates.getOrNull(0) ?: return null
    val latitude = coordinates.getOrNull(1) ?: return null

    return EarthquakeSummary(
        id = eventId,
        place = properties?.place ?: "Unknown location",
        magnitude = properties?.magnitude,
        latitude = latitude,
        longitude = longitude,
        depthKm = coordinates.getOrNull(2),
        timeMillis = properties?.time
    )
}
