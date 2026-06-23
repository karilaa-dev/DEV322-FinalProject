package com.bananaginger.noisedetector.data.settings

import com.bananaginger.noisedetector.history.HistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionTriggerEvaluatorTest {
    @Test
    fun bothMode_recordsOnlyWhenBothThresholdsExceeded() {
        assertFalse(
            DetectionTriggerEvaluator.shouldRecord(
                mode = DetectionTriggerMode.BOTH,
                soundThresholdExceeded = true,
                motionThresholdExceeded = false
            )
        )
        assertFalse(
            DetectionTriggerEvaluator.shouldRecord(
                mode = DetectionTriggerMode.BOTH,
                soundThresholdExceeded = false,
                motionThresholdExceeded = true
            )
        )
        assertTrue(
            DetectionTriggerEvaluator.shouldRecord(
                mode = DetectionTriggerMode.BOTH,
                soundThresholdExceeded = true,
                motionThresholdExceeded = true
            )
        )
    }

    @Test
    fun eitherMode_recordsWhenEitherThresholdIsExceeded() {
        assertTrue(
            DetectionTriggerEvaluator.shouldRecord(
                mode = DetectionTriggerMode.EITHER,
                soundThresholdExceeded = true,
                motionThresholdExceeded = false
            )
        )
        assertTrue(
            DetectionTriggerEvaluator.shouldRecord(
                mode = DetectionTriggerMode.EITHER,
                soundThresholdExceeded = false,
                motionThresholdExceeded = true
            )
        )
        assertFalse(
            DetectionTriggerEvaluator.shouldRecord(
                mode = DetectionTriggerMode.EITHER,
                soundThresholdExceeded = false,
                motionThresholdExceeded = false
            )
        )
    }

    @Test
    fun eventTypeFor_matchesExceededThresholds() {
        assertEquals(
            HistoryEntry.TYPE_SOUND,
            DetectionTriggerEvaluator.eventTypeFor(
                soundThresholdExceeded = true,
                motionThresholdExceeded = false
            )
        )
        assertEquals(
            HistoryEntry.TYPE_MOTION,
            DetectionTriggerEvaluator.eventTypeFor(
                soundThresholdExceeded = false,
                motionThresholdExceeded = true
            )
        )
        assertEquals(
            HistoryEntry.TYPE_SOUND_AND_MOTION,
            DetectionTriggerEvaluator.eventTypeFor(
                soundThresholdExceeded = true,
                motionThresholdExceeded = true
            )
        )
        assertNull(
            DetectionTriggerEvaluator.eventTypeFor(
                soundThresholdExceeded = false,
                motionThresholdExceeded = false
            )
        )
    }
}
