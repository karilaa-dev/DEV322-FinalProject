package com.bananaginger.noisedetector.data.remote

import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.EarthquakeEntity

interface RemoteHistoryDataSource {
    val isConfigured: Boolean

    suspend fun upsertAnomaly(
        anomaly: AnomalyEntity,
        earthquake: EarthquakeEntity?,
        installId: String,
        uploadedAt: Long
    )

    suspend fun upsertEarthquake(
        earthquake: EarthquakeEntity,
        installId: String,
        uploadedAt: Long
    )

    suspend fun fetchAnomalies(
        filter: RemoteDataFilter,
        installId: String
    ): List<RemoteAnomalyDocument>

    suspend fun fetchEarthquakes(
        filter: RemoteDataFilter,
        installId: String
    ): List<RemoteEarthquakeDocument>
}
