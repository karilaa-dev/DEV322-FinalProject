package com.bananaginger.noisedetector.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EarthquakeDtoMapperTest {
    @Test
    fun toEarthquakeSummary_mapsGeoJsonCoordinatesInCorrectOrder() {
        val feature = EarthquakeFeatureDto(
            id = "uw123456",
            properties = EarthquakePropertiesDto(
                magnitude = 3.4,
                place = "10 km NW of Seattle",
                time = 1_765_000_000_000
            ),
            geometry = EarthquakeGeometryDto(
                coordinates = listOf(-122.3, 47.7, 12.5)
            )
        )

        val summary = feature.toEarthquakeSummary()

        requireNotNull(summary)
        assertEquals("uw123456", summary.id)
        assertEquals("10 km NW of Seattle", summary.place)
        assertEquals(3.4, summary.magnitude ?: -1.0, 0.0)
        assertEquals(47.7, summary.latitude, 0.0)
        assertEquals(-122.3, summary.longitude, 0.0)
        assertEquals(12.5, summary.depthKm ?: -1.0, 0.0)
        assertEquals(1_765_000_000_000, summary.timeMillis)
    }

    @Test
    fun toEarthquakeSummary_returnsNullWhenCoordinatesAreMissing() {
        val feature = EarthquakeFeatureDto(
            id = "uw123456",
            properties = EarthquakePropertiesDto(
                magnitude = 3.4,
                place = "10 km NW of Seattle",
                time = 1_765_000_000_000
            ),
            geometry = EarthquakeGeometryDto(
                coordinates = emptyList()
            )
        )

        assertNull(feature.toEarthquakeSummary())
    }
}
