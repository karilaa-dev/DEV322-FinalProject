package com.bananaginger.noisedetector.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId

internal const val HISTORY_SECTION_TODAY = "Today"
internal const val HISTORY_SECTION_YESTERDAY = "Yesterday"
internal const val HISTORY_SECTION_EARLIER = "Earlier"

internal data class HistoryEventSection<T>(
    val title: String,
    val events: List<T>
)

internal fun <T> historyEventSections(
    events: List<T>,
    nowMillis: Long = System.currentTimeMillis(),
    zoneId: ZoneId = ZoneId.systemDefault(),
    timestampMillis: (T) -> Long
): List<HistoryEventSection<T>> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    val yesterday = today.minusDays(1)
    val todayEvents = mutableListOf<T>()
    val yesterdayEvents = mutableListOf<T>()
    val earlierEvents = mutableListOf<T>()

    events.forEach { event ->
        val eventDate = Instant.ofEpochMilli(timestampMillis(event)).atZone(zoneId).toLocalDate()
        when (eventDate) {
            today -> todayEvents += event
            yesterday -> yesterdayEvents += event
            else -> earlierEvents += event
        }
    }

    return buildList {
        if (todayEvents.isNotEmpty()) {
            add(HistoryEventSection(HISTORY_SECTION_TODAY, todayEvents))
        }
        if (yesterdayEvents.isNotEmpty()) {
            add(HistoryEventSection(HISTORY_SECTION_YESTERDAY, yesterdayEvents))
        }
        if (earlierEvents.isNotEmpty()) {
            add(HistoryEventSection(HISTORY_SECTION_EARLIER, earlierEvents))
        }
    }
}

@Composable
internal fun HistorySectionDivider(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
