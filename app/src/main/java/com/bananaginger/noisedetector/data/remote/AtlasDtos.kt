package com.bananaginger.noisedetector.data.remote

import com.google.gson.JsonObject

data class AtlasUpdateOneRequest(
    val dataSource: String,
    val database: String,
    val collection: String,
    val filter: Map<String, Any?>,
    val update: Map<String, Any?>,
    val upsert: Boolean = true
)

data class AtlasFindRequest(
    val dataSource: String,
    val database: String,
    val collection: String,
    val filter: Map<String, Any?> = emptyMap(),
    val sort: Map<String, Int> = emptyMap(),
    val limit: Int = 100
)

data class AtlasUpdateResponse(
    val matchedCount: Int? = null,
    val modifiedCount: Int? = null,
    val upsertedId: String? = null
)

data class AtlasFindResponse(
    val documents: List<JsonObject> = emptyList()
)
