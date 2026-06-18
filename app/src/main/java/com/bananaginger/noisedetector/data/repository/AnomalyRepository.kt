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

    // dylan record anomaly
    suspend fun recordAnomaly(
        soundLevelDb: Double,
        accelerationMagnitude: Float,
        location: LocationSnapshot,
        eventTimeMillis: Long
    ): EarthquakeSummary? = withContext(Dispatchers.IO) {

        val earthquake = runCatching {
            earthquakeDataSource.findNearbyEarthquake(
                latitude = location.latitude,
                longitude = location.longitude,
                eventTimeMillis = eventTimeMillis,
                maxRadiusKm = EARTHQUAKE_SEARCH_RADIUS_KM,
                lookbackDays = EARTHQUAKE_LOOKBACK_DAYS
            )
        }.getOrNull()

        val earthquakeMetadata = earthquake?.let {
            "earthquakeId=${it.id};" +
                    "place=${it.place};" +
                    "magnitude=${it.magnitude};" +
                    "depthKm=${it.depthKm};" +
                    "time=${it.timeMillis}"
        }

        dao.insert(
            AnomalyEntity(
                timestamp = eventTimeMillis,
                type = "sound_and_motion",
                magnitude = soundLevelDb,
                severity = when {
                    soundLevelDb >= 90.0 -> 3
                    soundLevelDb >= 82.0 -> 2
                    else -> 1
                },
                description =
                    "Sound exceeded 50 dB while movement was detected. " +
                            "Acceleration: $accelerationMagnitude m/s²",
                metadata = earthquakeMetadata
            )
        )

        earthquake
    }

    companion object {
        const val EARTHQUAKE_SEARCH_RADIUS_KM = 500.0
        const val EARTHQUAKE_LOOKBACK_DAYS = 30L
    }


}
