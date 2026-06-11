package com.bananaginger.noisedetector.data.repository

import com.bananaginger.noisedetector.data.AnomalyDao
import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.remote.EarthquakeDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AnomalyRepositoryTest {
    @Test
    fun testNearbyEarthquakeLookup_returnsEarthquakeFromDataSource() = runBlocking {
        val earthquake = sampleEarthquake()
        val dataSource = FakeEarthquakeDataSource(result = earthquake)
        val repository = AnomalyRepository(FakeAnomalyDao(), dataSource)
        val location = sampleLocation()

        val result = repository.testNearbyEarthquakeLookup(
            location = location,
            eventTimeMillis = 1_765_000_000_000
        )

        assertSame(earthquake, result)
        assertEquals(1, dataSource.calls)
        assertEquals(location.latitude, dataSource.lastLatitude, 0.0)
        assertEquals(location.longitude, dataSource.lastLongitude, 0.0)
        assertEquals(1_765_000_000_000, dataSource.lastEventTimeMillis)
        assertEquals(500.0, dataSource.lastMaxRadiusKm, 0.0)
        assertEquals(30L, dataSource.lastLookbackDays)
    }

    @Test
    fun testNearbyEarthquakeLookup_returnsNullWhenDataSourceReturnsNull() = runBlocking {
        val dataSource = FakeEarthquakeDataSource(result = null)
        val repository = AnomalyRepository(FakeAnomalyDao(), dataSource)

        val result = repository.testNearbyEarthquakeLookup(
            location = sampleLocation(),
            eventTimeMillis = 1_765_000_000_000
        )

        assertNull(result)
        assertEquals(1, dataSource.calls)
    }

    private fun sampleLocation(): LocationSnapshot {
        return LocationSnapshot(
            latitude = 47.6101,
            longitude = -122.2015
        )
    }

    private fun sampleEarthquake(): EarthquakeSummary {
        return EarthquakeSummary(
            id = "uw123456",
            place = "10 km NW of Seattle",
            magnitude = 3.4,
            latitude = 47.7,
            longitude = -122.3,
            depthKm = 12.5,
            timeMillis = 1_765_000_000_000
        )
    }

    private class FakeAnomalyDao : AnomalyDao {
        private val anomalies = MutableStateFlow<List<AnomalyEntity>>(emptyList())

        override fun getAll(): Flow<List<AnomalyEntity>> {
            return anomalies
        }

        override suspend fun insert(anomaly: AnomalyEntity): Long {
            return 1L
        }

        override suspend fun insertAll(anomalies: List<AnomalyEntity>) = Unit

        override suspend fun update(anomaly: AnomalyEntity) = Unit

        override suspend fun delete(anomaly: AnomalyEntity) = Unit

        override fun getById(id: Long): Flow<AnomalyEntity?> {
            return MutableStateFlow(null)
        }

        override fun getSince(since: Long): Flow<List<AnomalyEntity>> {
            return anomalies
        }

        override suspend fun deleteOlderThan(cutoff: Long) = Unit
    }

    private class FakeEarthquakeDataSource(
        private val result: EarthquakeSummary?
    ) : EarthquakeDataSource {
        var calls = 0
        var lastLatitude = 0.0
        var lastLongitude = 0.0
        var lastEventTimeMillis = 0L
        var lastMaxRadiusKm = 0.0
        var lastLookbackDays = 0L

        override suspend fun findNearbyEarthquake(
            latitude: Double,
            longitude: Double,
            eventTimeMillis: Long,
            maxRadiusKm: Double,
            lookbackDays: Long
        ): EarthquakeSummary? {
            calls += 1
            lastLatitude = latitude
            lastLongitude = longitude
            lastEventTimeMillis = eventTimeMillis
            lastMaxRadiusKm = maxRadiusKm
            lastLookbackDays = lookbackDays
            return result
        }
    }
}
