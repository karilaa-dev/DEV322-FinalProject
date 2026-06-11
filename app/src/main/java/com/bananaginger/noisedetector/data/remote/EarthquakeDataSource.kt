package com.bananaginger.noisedetector.data.remote

import com.bananaginger.noisedetector.data.model.EarthquakeSummary

// BananaGinger/Kyryl: Interface keeps repository tests independent from Retrofit.
interface EarthquakeDataSource {
    suspend fun findNearbyEarthquake(
        latitude: Double,
        longitude: Double,
        eventTimeMillis: Long,
        maxRadiusKm: Double,
        lookbackDays: Long
    ): EarthquakeSummary?
}
