package com.bananaginger.noisedetector.data.repository

import com.bananaginger.noisedetector.data.AnomalyDao
import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.AnomalyWithEarthquake
import com.bananaginger.noisedetector.data.EarthquakeDao
import com.bananaginger.noisedetector.data.EarthquakeEntity
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.remote.EarthquakeDataSource
import com.bananaginger.noisedetector.data.remote.RemoteAnomalyDocument
import com.bananaginger.noisedetector.data.remote.RemoteDataFilter
import com.bananaginger.noisedetector.data.remote.RemoteEarthquakeDocument
import com.bananaginger.noisedetector.data.remote.RemoteHistoryDataSource
import com.bananaginger.noisedetector.history.HistoryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class AnomalyRepositoryTest {
    @Test
    fun testNearbyEarthquakeLookup_returnsEarthquakeFromDataSource() = runBlocking {
        val earthquake = sampleEarthquake()
        val dataSource = FakeEarthquakeDataSource(result = earthquake)
        val earthquakeDao = FakeEarthquakeDao()
        val repository = AnomalyRepository(
            FakeAnomalyDao(),
            earthquakeDao,
            dataSource
        )
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
        assertEquals("uw123456", earthquakeDao.saved.single().id)
    }

    @Test
    fun testNearbyEarthquakeLookup_returnsNullWhenDataSourceReturnsNull() = runBlocking {
        val dataSource = FakeEarthquakeDataSource(result = null)
        val repository = AnomalyRepository(
            FakeAnomalyDao(),
            FakeEarthquakeDao(),
            dataSource
        )

        val result = repository.testNearbyEarthquakeLookup(
            location = sampleLocation(),
            eventTimeMillis = 1_765_000_000_000
        )

        assertNull(result)
        assertEquals(1, dataSource.calls)
    }

    @Test
    fun recordAnomaly_persistsLocalThresholdDetailsAndNearestEarthquake() = runBlocking {
        val anomalyDao = FakeAnomalyDao()
        val earthquakeDao = FakeEarthquakeDao()
        val repository = AnomalyRepository(
            anomalyDao,
            earthquakeDao,
            FakeEarthquakeDataSource(result = sampleEarthquake())
        )

        repository.recordAnomaly(
            soundLevelDb = 83.2,
            accelerationMagnitude = 13.1f,
            soundThresholdDb = 50.0,
            motionThreshold = 1.5f,
            soundThresholdExceeded = true,
            motionThresholdExceeded = true,
            location = sampleLocation(),
            eventTimeMillis = 1_765_000_000_000
        )

        val saved = anomalyDao.inserted.single()
        assertEquals(HistoryEntry.TYPE_SOUND_AND_MOTION, saved.type)
        assertEquals(83.2, saved.magnitude ?: -1.0, 0.0)
        assertEquals(13.1f, saved.accelerationMagnitude ?: -1f, 0.0f)
        assertEquals(50.0, saved.soundThresholdDb ?: -1.0, 0.0)
        assertEquals(1.5f, saved.motionThreshold ?: -1f, 0.0f)
        assertEquals(true, saved.soundThresholdExceeded)
        assertEquals(true, saved.motionThresholdExceeded)
        assertEquals("uw123456", saved.closestEarthquakeId)
        assertNull(saved.metadata)
        assertFalse(saved.description.orEmpty().contains("threshold", ignoreCase = true))
        assertFalse(saved.description.orEmpty().contains("47.6101"))
        assertEquals("uw123456", earthquakeDao.saved.single().id)
    }

    @Test
    fun recordAnomaly_persistsSoundOnlyEventType() = runBlocking {
        val anomalyDao = FakeAnomalyDao()
        val repository = AnomalyRepository(
            anomalyDao,
            FakeEarthquakeDao(),
            FakeEarthquakeDataSource(result = null)
        )

        repository.recordAnomaly(
            soundLevelDb = 83.2,
            accelerationMagnitude = 9.9f,
            soundThresholdDb = 50.0,
            motionThreshold = 1.5f,
            soundThresholdExceeded = true,
            motionThresholdExceeded = false,
            location = sampleLocation(),
            eventTimeMillis = 1_765_000_000_000
        )

        val saved = anomalyDao.inserted.single()
        assertEquals(HistoryEntry.TYPE_SOUND, saved.type)
        assertEquals(true, saved.soundThresholdExceeded)
        assertEquals(false, saved.motionThresholdExceeded)
        assertEquals("Loud sound 83.2 dB detected", saved.description)
    }

    @Test
    fun recordAnomaly_persistsMotionOnlyEventType() = runBlocking {
        val anomalyDao = FakeAnomalyDao()
        val repository = AnomalyRepository(
            anomalyDao,
            FakeEarthquakeDao(),
            FakeEarthquakeDataSource(result = null)
        )

        repository.recordAnomaly(
            soundLevelDb = 42.0,
            accelerationMagnitude = 13.1f,
            soundThresholdDb = 50.0,
            motionThreshold = 1.5f,
            soundThresholdExceeded = false,
            motionThresholdExceeded = true,
            location = sampleLocation(),
            eventTimeMillis = 1_765_000_000_000
        )

        val saved = anomalyDao.inserted.single()
        assertEquals(HistoryEntry.TYPE_MOTION, saved.type)
        assertEquals(false, saved.soundThresholdExceeded)
        assertEquals(true, saved.motionThresholdExceeded)
        assertEquals("Movement 13.1 m/s² detected", saved.description)
    }

    @Test
    fun recordAnomaly_doesNotPersistBlankEarthquakeId() = runBlocking {
        val anomalyDao = FakeAnomalyDao()
        val earthquakeDao = FakeEarthquakeDao()
        val repository = AnomalyRepository(
            anomalyDao,
            earthquakeDao,
            FakeEarthquakeDataSource(result = sampleEarthquake(id = " "))
        )

        repository.recordAnomaly(
            soundLevelDb = 83.2,
            accelerationMagnitude = 13.1f,
            soundThresholdDb = 50.0,
            motionThreshold = 1.5f,
            soundThresholdExceeded = true,
            motionThresholdExceeded = true,
            location = sampleLocation(),
            eventTimeMillis = 1_765_000_000_000
        )

        val saved = anomalyDao.inserted.single()
        assertNull(saved.closestEarthquakeId)
        assertEquals(emptyList<EarthquakeEntity>(), earthquakeDao.saved)
    }

    @Test
    fun uploadPendingHistory_uploadsOnceAndMarksUploaded() = runBlocking {
        val anomaly = AnomalyEntity(
            id = 10L,
            timestamp = 10L,
            date = "2026-06-21",
            day = "Sunday",
            type = "SOUND_AND_MOTION",
            magnitude = 80.0,
            accelerationMagnitude = 12.0f,
            severity = 4,
            description = "Saved anomaly",
            closestEarthquakeId = "uw123456"
        )
        val anomalyDao = FakeAnomalyDao(
            pending = listOf(
                AnomalyWithEarthquake(
                    anomaly = anomaly,
                    earthquake = EarthquakeEntity(
                        id = "uw123456",
                        place = "10 km NW of Seattle",
                        magnitude = 3.4,
                        latitude = 47.7,
                        longitude = -122.3,
                        depthKm = 12.5,
                        timeMillis = 1_765_000_000_000
                    )
                )
            )
        )
        val remote = FakeRemoteHistoryDataSource()
        val repository = AnomalyRepository(
            anomalyDao,
            FakeEarthquakeDao(),
            FakeEarthquakeDataSource(result = null),
            remote,
            installId = "install-1"
        )

        val result = repository.uploadPendingHistory()

        assertEquals(1, result.uploadedCount)
        assertEquals(0, result.failedCount)
        assertEquals(listOf(10L), anomalyDao.uploadedIds)
        assertEquals(1, remote.uploadedAnomalies.size)
        assertEquals(1, remote.uploadedEarthquakes.size)
    }

    private fun sampleLocation(): LocationSnapshot {
        return LocationSnapshot(
            latitude = 47.6101,
            longitude = -122.2015
        )
    }

    private fun sampleEarthquake(
        id: String = "uw123456"
    ): EarthquakeSummary {
        return EarthquakeSummary(
            id = id,
            place = "10 km NW of Seattle",
            magnitude = 3.4,
            latitude = 47.7,
            longitude = -122.3,
            depthKm = 12.5,
            timeMillis = 1_765_000_000_000
        )
    }

    private class FakeAnomalyDao(
        private val pending: List<AnomalyWithEarthquake> = emptyList()
    ) : AnomalyDao {
        val inserted = mutableListOf<AnomalyEntity>()
        val uploadedIds = mutableListOf<Long>()
        private val anomalies = MutableStateFlow<List<AnomalyEntity>>(emptyList())

        override fun getAll(): Flow<List<AnomalyEntity>> = anomalies

        override fun getAllWithEarthquakes(): Flow<List<AnomalyWithEarthquake>> {
            return MutableStateFlow(emptyList())
        }

        override suspend fun getPendingUpload(): List<AnomalyWithEarthquake> {
            return pending
        }

        override suspend fun markUploaded(
            id: Long,
            uploadedAt: Long,
            status: String
        ) {
            uploadedIds += id
        }

        override suspend fun markUploadFailed(
            id: Long,
            error: String,
            status: String
        ) = Unit

        override suspend fun insert(anomaly: AnomalyEntity): Long {
            inserted += anomaly
            return anomaly.id
        }

        override suspend fun insertAll(anomalies: List<AnomalyEntity>) = Unit

        override suspend fun update(anomaly: AnomalyEntity) = Unit

        override suspend fun delete(anomaly: AnomalyEntity) = Unit

        override fun getById(id: Long): Flow<AnomalyEntity?> {
            return MutableStateFlow(null)
        }

        override fun getSince(since: Long): Flow<List<AnomalyEntity>> = anomalies

        override suspend fun deleteOlderThan(cutoff: Long) = Unit
    }

    private class FakeEarthquakeDao : EarthquakeDao {
        val saved = mutableListOf<EarthquakeEntity>()

        override suspend fun upsert(earthquake: EarthquakeEntity) {
            saved.removeAll { it.id == earthquake.id }
            saved += earthquake
        }

        override fun observeAll(): Flow<List<EarthquakeEntity>> {
            return MutableStateFlow(saved)
        }

        override suspend fun getById(id: String): EarthquakeEntity? {
            return saved.firstOrNull { it.id == id }
        }
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

    private class FakeRemoteHistoryDataSource : RemoteHistoryDataSource {
        override val isConfigured: Boolean = true
        val uploadedAnomalies = mutableListOf<AnomalyEntity>()
        val uploadedEarthquakes = mutableListOf<EarthquakeEntity>()

        override suspend fun upsertAnomaly(
            anomaly: AnomalyEntity,
            installId: String,
            uploadedAt: Long
        ) {
            uploadedAnomalies += anomaly
        }

        override suspend fun upsertEarthquake(
            earthquake: EarthquakeEntity,
            installId: String,
            uploadedAt: Long
        ) {
            uploadedEarthquakes.removeAll { it.id == earthquake.id }
            uploadedEarthquakes += earthquake
        }

        override suspend fun fetchAnomalies(
            filter: RemoteDataFilter,
            installId: String
        ): List<RemoteAnomalyDocument> = emptyList()

        override suspend fun fetchEarthquakes(
            filter: RemoteDataFilter,
            installId: String
        ): List<RemoteEarthquakeDocument> = emptyList()
    }
}
