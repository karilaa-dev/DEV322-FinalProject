package com.bananaginger.noisedetector.data.location

import android.content.Context
import com.bananaginger.noisedetector.data.model.LocationSnapshot

enum class LocationSelectionSource(
    val label: String
) {
    PHONE("Current phone location"),
    MAP("Manual map location")
}

data class LocationSelection(
    val source: LocationSelectionSource,
    val location: LocationSnapshot
)

class LocationSelectionStore(
    context: Context
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun load(): LocationSelection? {
        val source = preferences.getString(KEY_SOURCE, null)
            ?.let { value ->
                runCatching {
                    LocationSelectionSource.valueOf(value)
                }.getOrNull()
            }
            ?: return null

        if (!preferences.contains(KEY_LATITUDE) ||
            !preferences.contains(KEY_LONGITUDE)
        ) {
            return null
        }

        return LocationSelection(
            source = source,
            location = LocationSnapshot(
                latitude = preferences.getFloat(KEY_LATITUDE, 0f).toDouble(),
                longitude = preferences.getFloat(KEY_LONGITUDE, 0f).toDouble()
            )
        )
    }

    fun save(
        source: LocationSelectionSource,
        location: LocationSnapshot
    ) {
        preferences.edit()
            .putString(KEY_SOURCE, source.name)
            .putFloat(KEY_LATITUDE, location.latitude.toFloat())
            .putFloat(KEY_LONGITUDE, location.longitude.toFloat())
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "earthquake_location_selection"
        const val KEY_SOURCE = "source"
        const val KEY_LATITUDE = "latitude"
        const val KEY_LONGITUDE = "longitude"
    }
}
