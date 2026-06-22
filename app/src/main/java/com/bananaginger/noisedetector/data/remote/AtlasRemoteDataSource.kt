package com.bananaginger.noisedetector.data.remote

import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.EarthquakeEntity
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.model.UpdateOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document

class AtlasRemoteDataSource(
    private val config: AtlasConfig
) : RemoteHistoryDataSource {
    private val client: MongoClient by lazy {
        MongoClients.create(config.connectionString)
    }

    override val isConfigured: Boolean
        get() = config.isConfigured

    override suspend fun upsertAnomaly(
        anomaly: AnomalyEntity,
        installId: String,
        uploadedAt: Long
    ) {
        withContext(Dispatchers.IO) {
            ensureConfigured()
            val request = AtlasPayloadFactory.anomalyUpdateRequest(
                config = config,
                anomaly = anomaly,
                installId = installId,
                uploadedAt = uploadedAt
            )

            collection(config.anomalyCollection).updateOne(
                request.filter.toBsonDocument(),
                request.update.toBsonDocument(),
                UpdateOptions().upsert(request.upsert)
            )
        }
    }

    override suspend fun upsertEarthquake(
        earthquake: EarthquakeEntity,
        installId: String,
        uploadedAt: Long
    ) {
        withContext(Dispatchers.IO) {
            ensureConfigured()
            val request = AtlasPayloadFactory.earthquakeUpdateRequest(
                config = config,
                earthquake = earthquake,
                installId = installId,
                uploadedAt = uploadedAt
            )

            collection(config.earthquakeCollection).updateOne(
                request.filter.toBsonDocument(),
                request.update.toBsonDocument(),
                UpdateOptions().upsert(request.upsert)
            )
        }
    }

    override suspend fun fetchAnomalies(
        filter: RemoteDataFilter,
        installId: String
    ): List<RemoteAnomalyDocument> = withContext(Dispatchers.IO) {
        ensureConfigured()
        collection(config.anomalyCollection)
            .find(filter.toAtlasAnomalyFilter(installId).toBsonDocument())
            .sort(Document("timestamp", -1))
            .limit(100)
            .map { it.toRemoteAnomalyDocument() }
            .toList()
    }

    override suspend fun fetchEarthquakes(
        filter: RemoteDataFilter,
        installId: String
    ): List<RemoteEarthquakeDocument> = withContext(Dispatchers.IO) {
        ensureConfigured()
        collection(config.earthquakeCollection)
            .find(filter.toAtlasEarthquakeFilter(installId).toBsonDocument())
            .sort(Document("timeMillis", -1))
            .limit(100)
            .map { it.toRemoteEarthquakeDocument() }
            .toList()
    }

    private fun ensureConfigured() {
        check(config.isConfigured) {
            "Atlas upload is not configured. Add MONGODB_CONNECTION_STRING to local.properties."
        }
    }

    private fun collection(name: String): MongoCollection<Document> {
        return client
            .getDatabase(config.database)
            .getCollection(name)
    }
}

private fun Map<String, Any?>.toBsonDocument(): Document {
    return Document().also { document ->
        for ((key, value) in this) {
            document[key] = value.toBsonValue()
        }
    }
}

private fun Any?.toBsonValue(): Any? {
    return when (this) {
        is Map<*, *> -> Document().also { document ->
            for ((key, value) in this) {
                if (key is String) {
                    document[key] = value.toBsonValue()
                }
            }
        }

        is Iterable<*> -> map { item -> item.toBsonValue() }
        else -> this
    }
}
