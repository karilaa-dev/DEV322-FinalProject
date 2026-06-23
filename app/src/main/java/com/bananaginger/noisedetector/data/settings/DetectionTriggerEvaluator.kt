package com.bananaginger.noisedetector.data.settings

import com.bananaginger.noisedetector.history.HistoryEntry

object DetectionTriggerEvaluator {
    fun shouldRecord(
        mode: DetectionTriggerMode,
        soundThresholdExceeded: Boolean,
        motionThresholdExceeded: Boolean
    ): Boolean {
        return when (mode) {
            DetectionTriggerMode.BOTH ->
                soundThresholdExceeded && motionThresholdExceeded
            DetectionTriggerMode.EITHER ->
                soundThresholdExceeded || motionThresholdExceeded
        }
    }

    fun eventTypeFor(
        soundThresholdExceeded: Boolean,
        motionThresholdExceeded: Boolean
    ): String? {
        return when {
            soundThresholdExceeded && motionThresholdExceeded ->
                HistoryEntry.TYPE_SOUND_AND_MOTION
            soundThresholdExceeded -> HistoryEntry.TYPE_SOUND
            motionThresholdExceeded -> HistoryEntry.TYPE_MOTION
            else -> null
        }
    }
}
