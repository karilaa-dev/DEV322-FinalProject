package com.bananaginger.noisedetector.data.remote

import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.EarthquakeEntity

object AtlasPayloadFactory {
    fun anomalyDocument(
        anomaly: AnomalyEntity,
        installId: String,
        uploadedAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "installId" to installId,
            "localAnomalyId" to anomaly.id,
            "timestamp" to anomaly.timestamp,
            "date" to anomaly.date,
            "day" to anomaly.day,
            "type" to anomaly.type,
            "soundLevelDb" to anomaly.magnitude,
            "accelerationMagnitude" to anomaly.accelerationMagnitude,
            "severity" to anomaly.severity,
            "description" to anomaly.description,
            "closestEarthquakeId" to anomaly.closestEarthquakeId,
            "uploadedAt" to uploadedAt
        )
    }

    fun earthquakeSetDocument(
        earthquake: EarthquakeEntity,
        uploadedAt: Long
    ): Map<String, Any?> {
        return mapOf(
            "earthquakeId" to earthquake.id,
            "place" to earthquake.place,
            "magnitude" to earthquake.magnitude,
            "latitude" to earthquake.latitude,
            "longitude" to earthquake.longitude,
            "depthKm" to earthquake.depthKm,
            "timeMillis" to earthquake.timeMillis,
            "source" to earthquake.source,
            "uploadedAt" to uploadedAt
        )
    }

    fun anomalyUpdateRequest(
        config: AtlasConfig,
        anomaly: AnomalyEntity,
        installId: String,
        uploadedAt: Long
    ): AtlasUpdateOneRequest {
        val document = anomalyDocument(
            anomaly = anomaly,
            installId = installId,
            uploadedAt = uploadedAt
        )

        return AtlasUpdateOneRequest(
            dataSource = config.dataSource,
            database = config.database,
            collection = config.anomalyCollection,
            filter = mapOf(
                "installId" to installId,
                "localAnomalyId" to anomaly.id
            ),
            update = mapOf("\$set" to document)
        )
    }

    fun earthquakeUpdateRequest(
        config: AtlasConfig,
        earthquake: EarthquakeEntity,
        installId: String,
        uploadedAt: Long
    ): AtlasUpdateOneRequest {
        return AtlasUpdateOneRequest(
            dataSource = config.dataSource,
            database = config.database,
            collection = config.earthquakeCollection,
            filter = mapOf("earthquakeId" to earthquake.id),
            update = mapOf(
                "\$set" to earthquakeSetDocument(earthquake, uploadedAt),
                "\$addToSet" to mapOf("reportingInstallIds" to installId)
            )
        )
    }
}
