package com.bananaginger.noisedetector.data.repository

import com.bananaginger.noisedetector.data.AnomalyDao
import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.model.AnomalyEvent
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.remote.EarthquakeDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnomalyRepositoryTest {
    @Test
    fun recordAnomaly_savesEarthquakeSummaryWhenApiReturnsFeature() = runBlocking {
        val dao = FakeAnomalyDao()
        val earthquake = sampleEarthquake()
        val dataSource = FakeEarthquakeDataSource(result = earthquake)
        val repository = AnomalyRepository(dao, dataSource)

        val result = repository.recordAnomaly(sampleEvent(), sampleLocation())

        assertEquals(1, dataSource.calls)
        assertEquals(earthquake, result.earthquake)
        assertNull(result.warningMessage)

        val saved = dao.inserted.single()
        assertEquals(1L, result.anomalyId)
        assertEquals("uw123456", saved.earthquakeId)
        assertEquals("10 km NW of Seattle", saved.earthquakePlace)
        assertEquals(3.4, saved.earthquakeMagnitude ?: -1.0, 0.0)
        assertEquals(47.7, saved.earthquakeLatitude ?: -1.0, 0.0)
        assertEquals(-122.3, saved.earthquakeLongitude ?: -1.0, 0.0)
        assertEquals(12.5, saved.earthquakeDepthKm ?: -1.0, 0.0)
        assertEquals(1_765_000_000_000, saved.earthquakeTimeMillis)
    }

    @Test
    fun recordAnomaly_savesWithoutEarthquakeWhenApiReturnsNoFeature() = runBlocking {
        val dao = FakeAnomalyDao()
        val dataSource = FakeEarthquakeDataSource(result = null)
        val repository = AnomalyRepository(dao, dataSource)

        val result = repository.recordAnomaly(sampleEvent(), sampleLocation())

        assertEquals(1, dataSource.calls)
        assertNull(result.earthquake)
        assertNull(result.warningMessage)

        val saved = dao.inserted.single()
        assertEquals(1L, result.anomalyId)
        assertNull(saved.earthquakeId)
        assertNull(saved.earthquakePlace)
        assertNull(saved.earthquakeMagnitude)
        assertNull(saved.earthquakeLatitude)
        assertNull(saved.earthquakeLongitude)
        assertNull(saved.earthquakeDepthKm)
        assertNull(saved.earthquakeTimeMillis)
    }

    @Test
    fun recordAnomaly_stillSavesAndReturnsWarningWhenApiThrows() = runBlocking {
        val dao = FakeAnomalyDao()
        val dataSource = FakeEarthquakeDataSource(
            failure = IllegalStateException("network down")
        )
        val repository = AnomalyRepository(dao, dataSource)

        val result = repository.recordAnomaly(sampleEvent(), sampleLocation())

        assertEquals(1, dataSource.calls)
        assertNull(result.earthquake)
        assertNotNull(result.warningMessage)
        assertTrue(result.warningMessage!!.contains("network down"))

        val saved = dao.inserted.single()
        assertEquals(1L, result.anomalyId)
        assertNull(saved.earthquakeId)
        assertEquals(82.0, saved.soundLevelDb ?: -1.0, 0.0)
        assertEquals(true, saved.motionDetected)
        assertEquals("abnormal_movement", saved.type)
    }

    private fun sampleEvent(): AnomalyEvent {
        return AnomalyEvent(
            timestampMillis = 1_765_000_000_000,
            soundLevelDb = 82.0,
            motionDetected = true,
            thresholdDb = 75.0,
            eventClassification = "abnormal_movement"
        )
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
        val inserted = mutableListOf<AnomalyEntity>()
        private val anomalies = MutableStateFlow<List<AnomalyEntity>>(emptyList())
        private var nextId = 1L

        override fun getAll(): Flow<List<AnomalyEntity>> {
            return anomalies
        }

        override suspend fun insert(anomaly: AnomalyEntity): Long {
            val saved = anomaly.copy(id = nextId++)
            inserted.add(saved)
            anomalies.value = listOf(saved) + anomalies.value
            return saved.id
        }

        override suspend fun insertAll(anomalies: List<AnomalyEntity>) = Unit

        override suspend fun update(anomaly: AnomalyEntity) = Unit

        override suspend fun delete(anomaly: AnomalyEntity) = Unit

        override fun getById(id: Long): Flow<AnomalyEntity?> {
            return MutableStateFlow(inserted.firstOrNull { it.id == id })
        }

        override fun getSince(since: Long): Flow<List<AnomalyEntity>> {
            return MutableStateFlow(inserted.filter { it.timestamp >= since })
        }

        override suspend fun deleteOlderThan(cutoff: Long) = Unit
    }

    private class FakeEarthquakeDataSource(
        private val result: EarthquakeSummary? = null,
        private val failure: Exception? = null
    ) : EarthquakeDataSource {
        var calls = 0

        override suspend fun findNearbyEarthquake(
            latitude: Double,
            longitude: Double,
            eventTimeMillis: Long,
            maxRadiusKm: Double,
            lookbackDays: Long
        ): EarthquakeSummary? {
            calls += 1
            failure?.let { throw it }
            return result
        }
    }
}
