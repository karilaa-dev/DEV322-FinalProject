package com.bananaginger.noisedetector.history

import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryTypeLabelsTest {
    @Test
    fun historyTypeDisplayLabel_formatsKnownTypes() {
        assertEquals("Sound", historyTypeDisplayLabel(HistoryEntry.TYPE_SOUND))
        assertEquals("Motion", historyTypeDisplayLabel(HistoryEntry.TYPE_MOTION))
        assertEquals("Earthquake", historyTypeDisplayLabel(HistoryEntry.TYPE_EARTHQUAKE))
        assertEquals("Sound & Motion", historyTypeDisplayLabel(HistoryEntry.TYPE_SOUND_AND_MOTION))
    }

    @Test
    fun historyTypeDisplayLabel_formatsUnknownUnderscoreTypes() {
        assertEquals("Custom Event", historyTypeDisplayLabel("CUSTOM_EVENT"))
    }
}
