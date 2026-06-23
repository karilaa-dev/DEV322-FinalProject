package com.bananaginger.noisedetector.data.settings

enum class DetectionTriggerMode(
    val storedValue: String,
    val displayLabel: String
) {
    BOTH("BOTH", "Both sound + motion"),
    EITHER("EITHER", "Sound or motion");

    companion object {
        val DEFAULT = EITHER

        fun fromStoredValue(value: String?): DetectionTriggerMode {
            return entries.firstOrNull { it.storedValue == value } ?: DEFAULT
        }
    }
}
