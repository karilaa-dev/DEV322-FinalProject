package com.bananaginger.noisedetector.data.repository

import com.bananaginger.noisedetector.data.AnomalyDao
import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.AnomalyWithEarthquake
import com.bananaginger.noisedetector.data.EarthquakeDao
import com.bananaginger.noisedetector.data.EarthquakeEntity
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.remote.RemoteDataFilter
import com.bananaginger.noisedetector.data.remote.RemoteDataKind
import com.bananaginger.noisedetector.data.remote.RemoteHistoryDataSource
import com.bananaginger.noisedetector.data.remote.RemoteHistoryResult
import com.bananaginger.noisedetector.data.remote.EarthquakeDataSource
import com.bananaginger.noisedetector.data.settings.DetectionTriggerEvaluator
import com.bananaginger.noisedetector.history.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AnomalyRepository(
    private val anomalyDao: AnomalyDao,
    private val earthquakeDao: EarthquakeDao,
    private val earthquakeDataSource: EarthquakeDataSource,
    private val remoteHistoryDataSource: RemoteHistoryDataSource? = null,
    private val installId: String = "local-install"
) {
    val isRemoteConfigured: Boolean
        get() = remoteHistoryDataSource?.isConfigured == true

    fun observeAnomalies(): Flow<List<AnomalyEntity>> {
        return anomalyDao.getAll()
    }

    fun observeAnomalyHistory(): Flow<List<AnomalyWithEarthquake>> {
        return anomalyDao.getAllWithEarthquakes()
    }

    suspend fun testNearbyEarthquakeLookup(
        location: LocationSnapshot,
        eventTimeMillis: Long
    ): EarthquakeSummary? = withContext(Dispatchers.IO) {
        findAndSaveNearbyEarthquake(location, eventTimeMillis)
    }

    suspend fun recordAnomaly(
        soundLevelDb: Double,
        accelerationMagnitude: Float,
        soundThresholdDb: Double,
        motionThreshold: Float,
        soundThresholdExceeded: Boolean,
        motionThresholdExceeded: Boolean,
        location: LocationSnapshot,
        eventTimeMillis: Long
    ): EarthquakeSummary? = withContext(Dispatchers.IO) {
        val type = DetectionTriggerEvaluator.eventTypeFor(
            soundThresholdExceeded = soundThresholdExceeded,
            motionThresholdExceeded = motionThresholdExceeded
        ) ?: HistoryEntry.TYPE_SOUND_AND_MOTION
        val earthquake = runCatching {
            findAndSaveNearbyEarthquake(location, eventTimeMillis)
        }.getOrNull()

        val detectedAt = Date(eventTimeMillis)
        anomalyDao.insert(
            AnomalyEntity(
                id = eventTimeMillis,
                timestamp = eventTimeMillis,
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(detectedAt),
                day = SimpleDateFormat("EEEE", Locale.US).format(detectedAt),
                type = type,
                magnitude = soundLevelDb,
                accelerationMagnitude = accelerationMagnitude,
                soundThresholdDb = soundThresholdDb,
                motionThreshold = motionThreshold,
                soundThresholdExceeded = soundThresholdExceeded,
                motionThresholdExceeded = motionThresholdExceeded,
                severity = anomalySeverity(
                    type = type,
                    soundLevelDb = soundLevelDb,
                    accelerationMagnitude = accelerationMagnitude
                ),
                description = anomalyDescription(
                    type = type,
                    soundLevelDb = soundLevelDb,
                    accelerationMagnitude = accelerationMagnitude
                ),
                metadata = null,
                closestEarthquakeId = earthquake?.id?.takeIf { it.isNotBlank() }
            )
        )

        earthquake
    }

    private suspend fun findAndSaveNearbyEarthquake(
        location: LocationSnapshot,
        eventTimeMillis: Long
    ): EarthquakeSummary? {
        return earthquakeDataSource.findNearbyEarthquake(
            latitude = location.latitude,
            longitude = location.longitude,
            eventTimeMillis = eventTimeMillis,
            maxRadiusKm = EARTHQUAKE_SEARCH_RADIUS_KM,
            lookbackDays = EARTHQUAKE_LOOKBACK_DAYS
        )?.takeIf { earthquake ->
            earthquake.id.isNotBlank()
        }?.also { earthquake ->
            earthquakeDao.upsert(earthquake.toEntity())
        }
    }

    suspend fun uploadPendingHistory(): UploadHistoryResult = withContext(Dispatchers.IO) {
        val remote = remoteHistoryDataSource
        if (remote?.isConfigured != true) {
            return@withContext UploadHistoryResult(
                uploadedCount = 0,
                failedCount = 0,
                message = "Atlas upload is not configured."
            )
        }

        val pending = anomalyDao.getPendingUpload()
        if (pending.isEmpty()) {
            return@withContext UploadHistoryResult(
                uploadedCount = 0,
                failedCount = 0,
                message = "No pending history to upload."
            )
        }

        var uploaded = 0
        var failed = 0

        pending.forEach { item ->
            val uploadedAt = System.currentTimeMillis()
            try {
                item.earthquake?.let { earthquake ->
                    remote.upsertEarthquake(
                        earthquake = earthquake,
                        installId = installId,
                        uploadedAt = uploadedAt
                    )
                }
                remote.upsertAnomaly(
                    anomaly = item.anomaly,
                    earthquake = item.earthquake,
                    installId = installId,
                    uploadedAt = uploadedAt
                )
                anomalyDao.markUploaded(item.anomaly.id, uploadedAt)
                uploaded += 1
            } catch (exception: Exception) {
                failed += 1
                anomalyDao.markUploadFailed(
                    id = item.anomaly.id,
                    error = exception.message ?: "Upload failed."
                )
            }
        }

        UploadHistoryResult(
            uploadedCount = uploaded,
            failedCount = failed,
            message = "Uploaded $uploaded event(s). Failed $failed."
        )
    }

    suspend fun fetchRemoteHistory(
        kind: RemoteDataKind,
        filter: RemoteDataFilter
    ): RemoteHistoryResult = withContext(Dispatchers.IO) {
        val remote = remoteHistoryDataSource
        check(remote?.isConfigured == true) {
            "Atlas remote data is not configured."
        }

        when (kind) {
            RemoteDataKind.ANOMALIES -> RemoteHistoryResult(
                anomalies = remote.fetchAnomalies(filter, installId)
            )
            RemoteDataKind.EARTHQUAKES -> RemoteHistoryResult(
                earthquakes = remote.fetchEarthquakes(filter, installId)
            )
        }
    }

    private fun soundSeverity(soundLevelDb: Double): Int {
        return when {
            soundLevelDb > 100.0 -> 5
            soundLevelDb > 80.0 -> 4
            soundLevelDb > 65.0 -> 3
            soundLevelDb > 50.0 -> 2
            else -> 1
        }
    }

    private fun motionSeverity(accelerationMagnitude: Float): Int {
        val motionDeviation = abs(accelerationMagnitude - 9.80665f)
        return when {
            motionDeviation > 8.0f -> 5
            motionDeviation > 4.0f -> 4
            motionDeviation > 2.0f -> 3
            motionDeviation > 1.0f -> 2
            else -> 1
        }
    }

    private fun anomalySeverity(
        type: String,
        soundLevelDb: Double,
        accelerationMagnitude: Float
    ): Int {
        return when (type) {
            HistoryEntry.TYPE_SOUND -> soundSeverity(soundLevelDb)
            HistoryEntry.TYPE_MOTION -> motionSeverity(accelerationMagnitude)
            else -> maxOf(
                soundSeverity(soundLevelDb),
                motionSeverity(accelerationMagnitude)
            )
        }
    }

    private fun anomalyDescription(
        type: String,
        soundLevelDb: Double,
        accelerationMagnitude: Float
    ): String {
        val soundText = String.format(Locale.US, "%.1f", soundLevelDb)
        val motionText = String.format(Locale.US, "%.1f", accelerationMagnitude)
        return when (type) {
            HistoryEntry.TYPE_SOUND -> "Loud sound $soundText dB detected"
            HistoryEntry.TYPE_MOTION -> "Movement $motionText m/s² detected"
            else -> "Loud sound $soundText dB and movement $motionText m/s² detected"
        }
    }

    private fun EarthquakeSummary.toEntity(): EarthquakeEntity {
        return EarthquakeEntity(
            id = id,
            place = place,
            magnitude = magnitude,
            latitude = latitude,
            longitude = longitude,
            depthKm = depthKm,
            timeMillis = timeMillis
        )
    }

    companion object {
        const val EARTHQUAKE_SEARCH_RADIUS_KM = 500.0
        const val EARTHQUAKE_LOOKBACK_DAYS = 30L
    }
}

data class UploadHistoryResult(
    val uploadedCount: Int,
    val failedCount: Int,
    val message: String
)
