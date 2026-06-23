package com.bananaginger.noisedetector.data.remote

import org.bson.Document
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteHistoryModelsTest {
    @Test
    fun remoteAnomalyDocument_parsesTriggerAndClosestEarthquakeSnapshot() {
        val document = Document(
            mapOf(
                "installId" to "install-test",
                "localAnomalyId" to 99L,
                "timestamp" to 1_765_000_000_000,
                "date" to "2026-06-21",
                "day" to "Sunday",
                "type" to "SOUND_AND_MOTION",
                "soundLevelDb" to 82.0,
                "accelerationMagnitude" to 13.0,
                "soundThresholdExceeded" to true,
                "motionThresholdExceeded" to true,
                "severity" to 4,
                "description" to "Uploaded loud sound and movement",
                "closestEarthquakeId" to "us7000abcd",
                "closestEarthquake" to Document(
                    mapOf(
                        "place" to "10 km NW of Seattle",
                        "magnitude" to 3.4,
                        "latitude" to 47.7,
                        "longitude" to -122.3,
                        "depthKm" to 12.5,
                        "timeMillis" to 1_765_000_000_000,
                        "source" to "USGS"
                    )
                ),
                "uploadedAt" to 1_765_000_060_000
            )
        )

        val anomaly = document.toRemoteAnomalyDocument()

        assertEquals(true, anomaly.soundThresholdExceeded)
        assertEquals(true, anomaly.motionThresholdExceeded)
        assertEquals("us7000abcd", anomaly.closestEarthquakeId)
        assertEquals("10 km NW of Seattle", anomaly.closestEarthquake?.place)
        assertEquals(3.4, anomaly.closestEarthquake?.magnitude)
        assertEquals(47.7, anomaly.closestEarthquake?.latitude)
        assertEquals(-122.3, anomaly.closestEarthquake?.longitude)
        assertEquals(12.5, anomaly.closestEarthquake?.depthKm)
        assertEquals(1_765_000_000_000, anomaly.closestEarthquake?.timeMillis)
        assertEquals("USGS", anomaly.closestEarthquake?.source)
    }
}
