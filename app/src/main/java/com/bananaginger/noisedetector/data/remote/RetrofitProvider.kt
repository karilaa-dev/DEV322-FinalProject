package com.bananaginger.noisedetector.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// BananaGinger/Kyryl: Shared Retrofit setup for USGS Earthquake Catalog API.
object RetrofitProvider {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://earthquake.usgs.gov/fdsnws/event/1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val earthquakeApi: EarthquakeApiService =
        retrofit.create(EarthquakeApiService::class.java)
}
