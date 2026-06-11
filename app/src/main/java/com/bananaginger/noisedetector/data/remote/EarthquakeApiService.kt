package com.bananaginger.noisedetector.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// BananaGinger/Kyryl: Retrofit service for the USGS Earthquake Catalog query endpoint.
interface EarthquakeApiService {
    @GET("query")
    suspend fun queryEarthquakes(
        @Query("format") format: String,
        @Query("eventtype") eventType: String,
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("maxradiuskm") maxRadiusKm: Double,
        @Query("starttime") startTime: String,
        @Query("endtime") endTime: String,
        @Query("orderby") orderBy: String,
        @Query("limit") limit: Int
    ): EarthquakeResponseDto
}
