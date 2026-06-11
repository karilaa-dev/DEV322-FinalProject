package com.bananaginger.noisedetector.data.repository

import com.bananaginger.noisedetector.data.local.AnomalyDao
import com.bananaginger.noisedetector.data.local.AnomalyEntity
import com.bananaginger.noisedetector.data.model.AnomalyEvent
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.model.RecordAnomalyResult
import com.bananaginger.noisedetector.data.remote.EarthquakeDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

// BananaGinger/Kyryl: Repository decides when to call USGS and keeps Room as the history source.
class AnomalyRepository(
    private val dao: AnomalyDao,
    private val earthquakeDataSource: EarthquakeDataSource
) {
    fun observeAnomalies(): Flow<List<AnomalyEntity>> {
        return dao.observeAnomalies()
    }

    suspend fun recordAnomaly(
        event: AnomalyEvent,
        location: LocationSnapshot?
    ): RecordAnomalyResult = withContext(Dispatchers.IO) {
        var earthquake: EarthquakeSummary? = null
        var warningMessage: String? = null

        if (event.motionDetected && location != null) {
            try {
                earthquake = earthquakeDataSource.findNearbyEarthquake(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    eventTimeMillis = event.timestampMillis,
                    maxRadiusKm = EARTHQUAKE_SEARCH_RADIUS_KM,
                    lookbackDays = EARTHQUAKE_LOOKBACK_DAYS
                )
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }
                warningMessage = "Earthquake lookup failed: ${exception.message ?: "unknown error"}"
            }
        }

        val anomalyId = dao.insertAnomaly(event.toEntity(location, earthquake))
        RecordAnomalyResult(
            anomalyId = anomalyId,
            earthquake = earthquake,
            warningMessage = warningMessage
        )
    }

    private fun AnomalyEvent.toEntity(
        location: LocationSnapshot?,
        earthquake: EarthquakeSummary?
    ): AnomalyEntity {
        return AnomalyEntity(
            timestampMillis = timestampMillis,
            soundLevelDb = soundLevelDb,
            motionDetected = motionDetected,
            thresholdDb = thresholdDb,
            eventClassification = eventClassification,
            latitude = location?.latitude,
            longitude = location?.longitude,
            earthquakeId = earthquake?.id,
            earthquakePlace = earthquake?.place,
            earthquakeMagnitude = earthquake?.magnitude,
            earthquakeLatitude = earthquake?.latitude,
            earthquakeLongitude = earthquake?.longitude,
            earthquakeDepthKm = earthquake?.depthKm,
            earthquakeTimeMillis = earthquake?.timeMillis
        )
    }

    companion object {
        const val EARTHQUAKE_SEARCH_RADIUS_KM = 500.0
        const val EARTHQUAKE_LOOKBACK_DAYS = 30L
    }
}
