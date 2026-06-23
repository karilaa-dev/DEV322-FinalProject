package com.bananaginger.noisedetector.data.remote

import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.EarthquakeEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AtlasPayloadFactoryTest {
    @Test
    fun anomalyDocument_excludesUserLocationAndThresholdValues() {
        val document = AtlasPayloadFactory.anomalyDocument(
            anomaly = sampleAnomaly(),
            installId = "install-1",
            uploadedAt = 1_765_000_010_000
        )

        assertFalse(document.containsKey("latitude"))
        assertFalse(document.containsKey("longitude"))
        assertFalse(document.containsKey("location"))
        assertFalse(document.containsKey("soundThresholdDb"))
        assertFalse(document.containsKey("motionThreshold"))
        assertFalse(document.containsKey("soundThresholdExceeded"))
        assertFalse(document.containsKey("motionThresholdExceeded"))
        assertFalse(document.containsKey("detectionTriggerMode"))
        assertEquals("install-1", document["installId"])
        assertEquals(44L, document["localAnomalyId"])
        assertEquals("uw123456", document["closestEarthquakeId"])
    }

    @Test
    fun earthquakeDocument_containsOnlyPublicEarthquakeCoordinates() {
        val document = AtlasPayloadFactory.earthquakeSetDocument(
            earthquake = sampleEarthquake(),
            uploadedAt = 1_765_000_010_000
        )

        assertEquals("uw123456", document["earthquakeId"])
        assertEquals(47.7, document["latitude"])
        assertEquals(-122.3, document["longitude"])
        assertFalse(document.containsKey("userLatitude"))
        assertFalse(document.containsKey("userLongitude"))
    }

    @Test
    fun earthquakeUpdateRequest_usesUpsertAndAddToSetForInstallIds() {
        val request = AtlasPayloadFactory.earthquakeUpdateRequest(
            config = sampleConfig(),
            earthquake = sampleEarthquake(),
            installId = "install-1",
            uploadedAt = 1_765_000_010_000
        )

        assertTrue(request.upsert)
        assertEquals(mapOf("earthquakeId" to "uw123456"), request.filter)
        assertTrue(request.update.containsKey("\$addToSet"))
    }

    @Test
    fun remoteFilters_buildAllMineAndOthersQueries() {
        assertEquals(
            emptyMap<String, Any?>(),
            RemoteDataFilter.ALL.toAtlasAnomalyFilter("install-1")
        )
        assertEquals(
            mapOf("installId" to "install-1"),
            RemoteDataFilter.MINE.toAtlasAnomalyFilter("install-1")
        )
        assertEquals(
            mapOf("installId" to mapOf("\$ne" to "install-1")),
            RemoteDataFilter.OTHERS.toAtlasAnomalyFilter("install-1")
        )
        assertEquals(
            mapOf("reportingInstallIds" to mapOf("\$ne" to "install-1")),
            RemoteDataFilter.OTHERS.toAtlasEarthquakeFilter("install-1")
        )
    }

    private fun sampleAnomaly(): AnomalyEntity {
        return AnomalyEntity(
            id = 44L,
            timestamp = 1_765_000_000_000,
            date = "2026-06-21",
            day = "Sunday",
            type = "SOUND_AND_MOTION",
            magnitude = 82.0,
            accelerationMagnitude = 13.0f,
            soundThresholdDb = 50.0,
            motionThreshold = 1.5f,
            soundThresholdExceeded = true,
            motionThresholdExceeded = true,
            severity = 4,
            description = "Loud sound and movement detected",
            closestEarthquakeId = "uw123456"
        )
    }

    private fun sampleEarthquake(): EarthquakeEntity {
        return EarthquakeEntity(
            id = "uw123456",
            place = "10 km NW of Seattle",
            magnitude = 3.4,
            latitude = 47.7,
            longitude = -122.3,
            depthKm = 12.5,
            timeMillis = 1_765_000_000_000
        )
    }

    private fun sampleConfig(): AtlasConfig {
        return AtlasConfig(
            database = "bananaginger",
            anomalyCollection = "anomalies",
            earthquakeCollection = "earthquakes",
            dataSource = "mongodb-atlas"
        )
    }
}
