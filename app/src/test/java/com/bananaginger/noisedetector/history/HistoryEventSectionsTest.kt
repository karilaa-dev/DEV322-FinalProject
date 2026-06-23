package com.bananaginger.noisedetector.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class HistoryEventSectionsTest {
    private val zoneId = ZoneId.of("America/Los_Angeles")
    private val nowMillis = millisFor(2026, 6, 22, 10, 0)

    @Test
    fun historyEventSections_groupsTodayYesterdayAndEarlierEvents() {
        val todayEvent = TestEvent(id = 1, timestamp = millisFor(2026, 6, 22, 9, 30))
        val yesterdayEvent = TestEvent(id = 2, timestamp = millisFor(2026, 6, 21, 23, 59))
        val olderEvent = TestEvent(id = 3, timestamp = millisFor(2026, 6, 20, 12, 0))

        val sections = historyEventSections(
            events = listOf(todayEvent, yesterdayEvent, olderEvent),
            nowMillis = nowMillis,
            zoneId = zoneId
        ) { it.timestamp }

        assertEquals(
            listOf(HISTORY_SECTION_TODAY, HISTORY_SECTION_YESTERDAY, HISTORY_SECTION_EARLIER),
            sections.map { it.title }
        )
        assertEquals(listOf(todayEvent), sections[0].events)
        assertEquals(listOf(yesterdayEvent), sections[1].events)
        assertEquals(listOf(olderEvent), sections[2].events)
    }

    @Test
    fun historyEventSections_preservesOrderWithinSections() {
        val firstTodayEvent = TestEvent(id = 1, timestamp = millisFor(2026, 6, 22, 9, 30))
        val secondTodayEvent = TestEvent(id = 2, timestamp = millisFor(2026, 6, 22, 8, 45))

        val sections = historyEventSections(
            events = listOf(firstTodayEvent, secondTodayEvent),
            nowMillis = nowMillis,
            zoneId = zoneId
        ) { it.timestamp }

        assertEquals(listOf(HISTORY_SECTION_TODAY), sections.map { it.title })
        assertEquals(listOf(firstTodayEvent, secondTodayEvent), sections.single().events)
    }

    @Test
    fun historyEventSections_omitsEmptySections() {
        val yesterdayEvent = TestEvent(id = 1, timestamp = millisFor(2026, 6, 21, 12, 0))

        val sections = historyEventSections(
            events = listOf(yesterdayEvent),
            nowMillis = nowMillis,
            zoneId = zoneId
        ) { it.timestamp }

        assertEquals(listOf(HISTORY_SECTION_YESTERDAY), sections.map { it.title })
        assertEquals(listOf(yesterdayEvent), sections.single().events)
    }

    private fun millisFor(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int
    ): Long {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private data class TestEvent(
        val id: Int,
        val timestamp: Long
    )
}
