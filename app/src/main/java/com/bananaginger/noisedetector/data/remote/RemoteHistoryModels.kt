package com.bananaginger.noisedetector.data.remote

import com.google.gson.JsonObject
import org.bson.Document

enum class RemoteDataKind {
    ANOMALIES,
    EARTHQUAKES
}

enum class RemoteDataFilter {
    ALL,
    MINE,
    OTHERS
}

data class RemoteAnomalyDocument(
    val installId: String,
    val localAnomalyId: Long,
    val timestamp: Long,
    val date: String?,
    val day: String?,
    val type: String,
    val soundLevelDb: Double?,
    val accelerationMagnitude: Float?,
    val severity: Int?,
    val description: String?,
    val closestEarthquakeId: String?,
    val uploadedAt: Long?
)

data class RemoteEarthquakeDocument(
    val earthquakeId: String,
    val place: String,
    val magnitude: Double?,
    val latitude: Double,
    val longitude: Double,
    val depthKm: Double?,
    val timeMillis: Long?,
    val source: String?,
    val uploadedAt: Long?
)

data class RemoteHistoryResult(
    val anomalies: List<RemoteAnomalyDocument> = emptyList(),
    val earthquakes: List<RemoteEarthquakeDocument> = emptyList()
)

fun RemoteDataFilter.toAtlasAnomalyFilter(installId: String): Map<String, Any?> {
    return when (this) {
        RemoteDataFilter.ALL -> emptyMap()
        RemoteDataFilter.MINE -> mapOf("installId" to installId)
        RemoteDataFilter.OTHERS -> mapOf("installId" to mapOf("\$ne" to installId))
    }
}

fun RemoteDataFilter.toAtlasEarthquakeFilter(installId: String): Map<String, Any?> {
    return when (this) {
        RemoteDataFilter.ALL -> emptyMap()
        RemoteDataFilter.MINE -> mapOf("reportingInstallIds" to installId)
        RemoteDataFilter.OTHERS -> mapOf(
            "reportingInstallIds" to mapOf("\$ne" to installId)
        )
    }
}

fun JsonObject.toRemoteAnomalyDocument(): RemoteAnomalyDocument {
    return RemoteAnomalyDocument(
        installId = string("installId"),
        localAnomalyId = long("localAnomalyId"),
        timestamp = long("timestamp"),
        date = nullableString("date"),
        day = nullableString("day"),
        type = string("type"),
        soundLevelDb = nullableDouble("soundLevelDb"),
        accelerationMagnitude = nullableDouble("accelerationMagnitude")?.toFloat(),
        severity = nullableInt("severity"),
        description = nullableString("description"),
        closestEarthquakeId = nullableString("closestEarthquakeId"),
        uploadedAt = nullableLong("uploadedAt")
    )
}

fun JsonObject.toRemoteEarthquakeDocument(): RemoteEarthquakeDocument {
    return RemoteEarthquakeDocument(
        earthquakeId = string("earthquakeId"),
        place = string("place"),
        magnitude = nullableDouble("magnitude"),
        latitude = double("latitude"),
        longitude = double("longitude"),
        depthKm = nullableDouble("depthKm"),
        timeMillis = nullableLong("timeMillis"),
        source = nullableString("source"),
        uploadedAt = nullableLong("uploadedAt")
    )
}

fun Document.toRemoteAnomalyDocument(): RemoteAnomalyDocument {
    return RemoteAnomalyDocument(
        installId = string("installId"),
        localAnomalyId = long("localAnomalyId"),
        timestamp = long("timestamp"),
        date = nullableString("date"),
        day = nullableString("day"),
        type = string("type"),
        soundLevelDb = nullableDouble("soundLevelDb"),
        accelerationMagnitude = nullableDouble("accelerationMagnitude")?.toFloat(),
        severity = nullableInt("severity"),
        description = nullableString("description"),
        closestEarthquakeId = nullableString("closestEarthquakeId"),
        uploadedAt = nullableLong("uploadedAt")
    )
}

fun Document.toRemoteEarthquakeDocument(): RemoteEarthquakeDocument {
    return RemoteEarthquakeDocument(
        earthquakeId = string("earthquakeId"),
        place = string("place"),
        magnitude = nullableDouble("magnitude"),
        latitude = double("latitude"),
        longitude = double("longitude"),
        depthKm = nullableDouble("depthKm"),
        timeMillis = nullableLong("timeMillis"),
        source = nullableString("source"),
        uploadedAt = nullableLong("uploadedAt")
    )
}

private fun JsonObject.string(name: String): String {
    return nullableString(name).orEmpty()
}

private fun JsonObject.long(name: String): Long {
    return nullableLong(name) ?: 0L
}

private fun JsonObject.double(name: String): Double {
    return nullableDouble(name) ?: 0.0
}

private fun JsonObject.nullableString(name: String): String? {
    val element = get(name) ?: return null
    return if (element.isJsonNull) null else element.asString
}

private fun JsonObject.nullableLong(name: String): Long? {
    val element = get(name) ?: return null
    return if (element.isJsonNull) null else element.asLong
}

private fun JsonObject.nullableInt(name: String): Int? {
    val element = get(name) ?: return null
    return if (element.isJsonNull) null else element.asInt
}

private fun JsonObject.nullableDouble(name: String): Double? {
    val element = get(name) ?: return null
    return if (element.isJsonNull) null else element.asDouble
}

private fun Document.string(name: String): String {
    return nullableString(name).orEmpty()
}

private fun Document.long(name: String): Long {
    return nullableLong(name) ?: 0L
}

private fun Document.double(name: String): Double {
    return nullableDouble(name) ?: 0.0
}

private fun Document.nullableString(name: String): String? {
    return get(name)?.toString()
}

private fun Document.nullableLong(name: String): Long? {
    return (get(name) as? Number)?.toLong()
}

private fun Document.nullableInt(name: String): Int? {
    return (get(name) as? Number)?.toInt()
}

private fun Document.nullableDouble(name: String): Double? {
    return (get(name) as? Number)?.toDouble()
}
