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
        location: LocationSnapshot,
        eventTimeMillis: Long
    ): EarthquakeSummary? = withContext(Dispatchers.IO) {
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
                type = "SOUND_AND_MOTION",
                magnitude = soundLevelDb,
                accelerationMagnitude = accelerationMagnitude,
                severity = maxOf(
                    soundSeverity(soundLevelDb),
                    motionSeverity(accelerationMagnitude)
                ),
                description =
                    "Loud sound ${String.format(Locale.US, "%.1f", soundLevelDb)} dB " +
                            "and movement ${String.format(Locale.US, "%.1f", accelerationMagnitude)} m/s² detected",
                metadata = null,
                closestEarthquakeId = earthquake?.id
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
        )?.also { earthquake ->
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
