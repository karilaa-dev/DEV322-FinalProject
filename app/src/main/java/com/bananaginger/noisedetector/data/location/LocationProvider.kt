package com.bananaginger.noisedetector.data.location

import com.bananaginger.noisedetector.data.model.LocationSnapshot

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationSnapshot?
}
