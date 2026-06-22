package com.bananaginger.noisedetector.data.remote

import com.bananaginger.noisedetector.BuildConfig

data class AtlasConfig(
    val connectionString: String = "",
    val database: String,
    val anomalyCollection: String,
    val earthquakeCollection: String,
    val dataSource: String = "mongodb-atlas"
) {
    val isConfigured: Boolean
        get() = connectionString.isNotBlank()

    companion object {
        fun fromBuildConfig(): AtlasConfig {
            return AtlasConfig(
                connectionString = BuildConfig.MONGODB_CONNECTION_STRING.trim(),
                database = BuildConfig.ATLAS_DATABASE.trim()
                    .ifBlank { "bananaginger" },
                anomalyCollection = BuildConfig.ATLAS_ANOMALY_COLLECTION.trim()
                    .ifBlank { "anomalies" },
                earthquakeCollection = BuildConfig.ATLAS_EARTHQUAKE_COLLECTION.trim()
                    .ifBlank { "earthquakes" }
            )
        }
    }
}
