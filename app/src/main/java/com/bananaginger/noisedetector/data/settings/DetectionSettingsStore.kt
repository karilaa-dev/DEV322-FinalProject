package com.bananaginger.noisedetector.data.settings

import android.content.Context

class DetectionSettingsStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadTriggerMode(): DetectionTriggerMode {
        return DetectionTriggerMode.fromStoredValue(
            preferences.getString(KEY_TRIGGER_MODE, null)
        )
    }

    fun saveTriggerMode(mode: DetectionTriggerMode) {
        preferences.edit()
            .putString(KEY_TRIGGER_MODE, mode.storedValue)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "detection_settings"
        const val KEY_TRIGGER_MODE = "trigger_mode"
    }
}
