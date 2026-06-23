package com.bananaginger.noisedetector.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {
    private val client = LocationServices.getFusedLocationProviderClient(
        context.applicationContext
    )

    override suspend fun getCurrentLocation(): LocationSnapshot? {
        if (!hasLocationPermission()) return null

        val location = runCatching {
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
            ).await()
        }.getOrNull() ?: runCatching {
            client.lastLocation.await()
        }.getOrNull()

        return location?.let {
            LocationSnapshot(
                latitude = it.latitude,
                longitude = it.longitude
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }
}
