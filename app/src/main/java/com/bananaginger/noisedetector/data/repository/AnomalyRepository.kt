package com.bananaginger.noisedetector.data.repository

import com.bananaginger.noisedetector.data.AnomalyDao
import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.remote.EarthquakeDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// BananaGinger/Kyryl: Repository keeps Room access separate and owns earthquake API calls.
class AnomalyRepository(
    private val dao: AnomalyDao,
    private val earthquakeDataSource: EarthquakeDataSource
) {
    fun observeAnomalies(): Flow<List<AnomalyEntity>> {
        return dao.getAll()
    }

    suspend fun testNearbyEarthquakeLookup(
        location: LocationSnapshot,
        eventTimeMillis: Long
    ): EarthquakeSummary? = withContext(Dispatchers.IO) {
        earthquakeDataSource.findNearbyEarthquake(
            latitude = location.latitude,
            longitude = location.longitude,
            eventTimeMillis = eventTimeMillis,
            maxRadiusKm = EARTHQUAKE_SEARCH_RADIUS_KM,
            lookbackDays = EARTHQUAKE_LOOKBACK_DAYS
        )
    }

    companion object {
        const val EARTHQUAKE_SEARCH_RADIUS_KM = 500.0
        const val EARTHQUAKE_LOOKBACK_DAYS = 30L
    }
}
