package com.bananaginger.noisedetector.data.remote

import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import java.time.Instant
import java.time.temporal.ChronoUnit

// BananaGinger/Kyryl: Remote data source owns USGS request parameters and DTO mapping.
class EarthquakeRemoteDataSource(
    private val api: EarthquakeApiService
) : EarthquakeDataSource {
    override suspend fun findNearbyEarthquake(
        latitude: Double,
        longitude: Double,
        eventTimeMillis: Long,
        maxRadiusKm: Double,
        lookbackDays: Long
    ): EarthquakeSummary? {
        val endTime = Instant.ofEpochMilli(eventTimeMillis)
        val startTime = endTime.minus(lookbackDays, ChronoUnit.DAYS)

        val response = api.queryEarthquakes(
            format = "geojson",
            eventType = "earthquake",
            latitude = latitude,
            longitude = longitude,
            maxRadiusKm = maxRadiusKm,
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            orderBy = "time",
            limit = 1
        )

        return response.features.firstNotNullOfOrNull { it.toEarthquakeSummary() }
    }
}
