package com.bananaginger.noisedetector.history

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AnomalyHistoryScreen — displays the in-memory list of detected anomaly events.
 *
 * The list is stored inside AnomalyViewModel, so it survives:
 *   - Screen rotation
 *   - App going to background
 * The list is cleared when the process is killed (app fully closed by the user).
 * Room-backed SOUND_AND_MOTION entries are restored into the list by the merged ViewModel.
 *
 * @param entries  List of recorded anomaly events, newest first.
 * @param modifier Compose modifier forwarded from the parent.
 */
@Composable
fun AnomalyHistoryScreen(
    entries: List<HistoryEntry>,
    onUploadHistory: () -> Unit,
    isUploadingHistory: Boolean,
    uploadStatusMessage: String,
    modifier: Modifier = Modifier
) {
    var selectedEntry by remember { mutableStateOf<HistoryEntry?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ---- Screen title ----
        Text(
            text = "Local History",
            style = MaterialTheme.typography.headlineSmall
        )

        // ---- Entry count subtitle ----
        Text(
            text = "${entries.size} saved event(s)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // ---- List OR empty placeholder ----
        if (entries.isEmpty()) {
            // Shown until the first anomaly is recorded.
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No history available yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Start monitoring to detect sound and motion anomalies.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            // Scrollable list — newest entry at the top.
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                historyEventSections(entries) { it.timestamp }.forEach { section ->
                    item(key = "local-history-section-${section.title}") {
                        HistorySectionDivider(section.title)
                    }
                    items(section.events, key = { it.id }) { entry ->
                        HistoryEntryCard(
                            entry = entry,
                            onClick = { selectedEntry = entry }
                        )
                    }
                }
            }
        }

        if (isUploadingHistory) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text("Uploading history...")
            }
        }

        if (uploadStatusMessage.isNotBlank()) {
            Text(
                text = uploadStatusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(
            onClick = onUploadHistory,
            enabled = !isUploadingHistory,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            Text("Upload")
        }
    }

    selectedEntry?.let { entry ->
        HistoryEntryDetailSheet(
            entry = entry,
            onDismiss = { selectedEntry = null }
        )
    }
}

/**
 * HistoryEntryCard — a single card in the history list.
 *
 * Shows: anomaly type, date + time, one-line description, severity stars,
 * day of week, and sensor readings (hidden for earthquake entries).
 *
 * @param entry The anomaly record to display.
 */
@Composable
private fun HistoryEntryCard(
    entry: HistoryEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("local-history-event-${entry.id}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Type badge + date/time on the same row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = historyTypeDisplayLabel(entry.type),
                    style = MaterialTheme.typography.labelMedium,
                    color = typeColor(entry.type)
                )
                Text(
                    text = "${entry.date}  ${entry.time}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // One-line description of what was detected
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium
            )

            // Severity stars + day of week
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Severity: " + "\u2605".repeat(entry.severity) + "\u2606".repeat(5 - entry.severity),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = entry.day,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (entry.type != HistoryEntry.TYPE_EARTHQUAKE) {
                Text(
                    text = "Exceeded: ${entry.exceededLabel()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Sensor readings row — only shown for SOUND and MOTION entries
            if (entry.type != HistoryEntry.TYPE_EARTHQUAKE) {
                Text(
                    text = "Sound: %.1f dB  Accel: %.1f m/s\u00B2".format(
                        entry.soundLevelDb ?: 0.0,
                        entry.accelerationMagnitude ?: 0.0
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Earthquake-specific details — only shown for EARTHQUAKE entries
            if (entry.type == HistoryEntry.TYPE_EARTHQUAKE) {
                entry.earthquakePlace?.let { place ->
                    Text(
                        text = "Location: $place",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.depthKm != null) {
                    Text(
                        text = "Depth: %.1f km".format(entry.depthKm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Remote: ${entry.remoteSyncStatus}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            entry.remoteError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryEntryDetailSheet(
    entry: HistoryEntry,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DetailSheetHeader(
                entry = entry,
                onDismiss = onDismiss
            )

            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val wideLayout = maxWidth >= 560.dp
                    val standardMetricColumns = if (maxWidth >= 700.dp) 4 else 2
                    val sideSectionColumns = if (wideLayout) 2 else 1

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (entry.type != HistoryEntry.TYPE_EARTHQUAKE) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                DetailSection(
                                    title = "Event",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MetricGrid(
                                        metrics = entry.eventMetrics(),
                                        columns = sideSectionColumns
                                    )
                                }

                                DetailSection(
                                    title = "Sound",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MetricGrid(
                                        metrics = entry.soundThresholdMetrics(),
                                        columns = sideSectionColumns
                                    )
                                }

                                DetailSection(
                                    title = "Motion",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MetricGrid(
                                        metrics = entry.motionThresholdMetrics(),
                                        columns = sideSectionColumns
                                    )
                                }
                            }
                        } else {
                            DetailSection(title = "Event") {
                                MetricGrid(
                                    metrics = entry.eventMetrics(),
                                    columns = standardMetricColumns
                                )
                            }
                        }

                        DetailSection(title = "Nearest Earthquake") {
                            if (entry.closestEarthquakeId == null) {
                                Text(
                                    text = "No linked earthquake was saved for this event.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                MetricTile(
                                    metric = DetailMetric(
                                        label = "Place",
                                        value = entry.earthquakePlace ?: "unknown"
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxValueLines = 2
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                MetricGrid(
                                    metrics = entry.earthquakeMetrics(),
                                    columns = standardMetricColumns
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSheetHeader(
    entry: HistoryEntry,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = historyTypeDisplayLabel(entry.type),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${entry.date} ${entry.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close anomaly details"
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoPill(text = "Exceeded: ${entry.exceededLabel()}")
            InfoPill(text = "Severity ${entry.severity}/5")
        }
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun DetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun MetricGrid(
    metrics: List<DetailMetric>,
    columns: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        metrics.chunked(columns.coerceAtLeast(1)).forEach { rowMetrics ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowMetrics.forEach { metric ->
                    MetricTile(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowMetrics.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    metric: DetailMetric,
    modifier: Modifier = Modifier,
    maxValueLines: Int = 1
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = metric.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metric.value,
            style = MaterialTheme.typography.bodyMedium,
            color = if (metric.isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = maxValueLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Returns a Material3 color for the type badge based on the anomaly type.
 *   SOUND      = tertiary (amber-ish)
 *   MOTION     = primary  (blue-ish)
 *   EARTHQUAKE = error    (red)
 */
@Composable
private fun typeColor(type: String) = when (type) {
    HistoryEntry.TYPE_SOUND            -> MaterialTheme.colorScheme.tertiary
    HistoryEntry.TYPE_MOTION           -> MaterialTheme.colorScheme.primary
    HistoryEntry.TYPE_EARTHQUAKE       -> MaterialTheme.colorScheme.error
    HistoryEntry.TYPE_SOUND_AND_MOTION -> MaterialTheme.colorScheme.error   // combined sound and motion
    else                               -> MaterialTheme.colorScheme.onSurface
}

private fun HistoryEntry.exceededLabel(): String {
    val exceeded = buildList {
        if (soundThresholdExceeded == true) add("Sound")
        if (motionThresholdExceeded == true) add("Motion")
    }
    return if (exceeded.isEmpty()) {
        "Unknown"
    } else {
        exceeded.joinToString(" & ")
    }
}

private fun HistoryEntry.eventMetrics(): List<DetailMetric> {
    return buildList {
        add(DetailMetric("Day", day))
        add(DetailMetric("Remote", remoteSyncStatus))
        remoteUploadedAt?.let { uploadedAt ->
            add(DetailMetric("Upload date", uploadedAt.toDisplayUploadDate()))
            add(DetailMetric("Upload time", uploadedAt.toDisplayUploadTime()))
        }
        remoteError?.let { error ->
            add(DetailMetric("Remote error", error, isError = true))
        }
    }
}

private fun HistoryEntry.soundThresholdMetrics(): List<DetailMetric> {
    return listOf(
        DetailMetric("Reading", "${soundLevelDb.formatNullable()} dB"),
        DetailMetric("Threshold", "${soundThresholdDb.formatNullable()} dB"),
        DetailMetric("Sound exceeded", soundThresholdExceeded.yesNoUnknown())
    )
}

private fun HistoryEntry.motionThresholdMetrics(): List<DetailMetric> {
    return listOf(
        DetailMetric("Acceleration", "${accelerationMagnitude.formatNullable()} m/s²"),
        DetailMetric("Threshold", "${motionThreshold.formatNullable()} m/s²"),
        DetailMetric("Motion exceeded", motionThresholdExceeded.yesNoUnknown())
    )
}

private fun HistoryEntry.earthquakeMetrics(): List<DetailMetric> {
    return buildList {
        add(DetailMetric("ID", closestEarthquakeId ?: "unknown"))
        add(DetailMetric("Source", earthquakeSource ?: "unknown"))
        add(DetailMetric("Magnitude", "M${earthquakeMagnitude.formatNullable()}"))
        add(DetailMetric("Depth", "${depthKm.formatNullable()} km"))
        add(DetailMetric("Earthquake time", earthquakeTimeMillis.toDisplayTime()))
        add(DetailMetric("Latitude", latitude.formatCoordinate()))
        add(DetailMetric("Longitude", longitude.formatCoordinate()))
        earthquakeRemoteUploadedAt?.let { uploadedAt ->
            add(DetailMetric("Upload date", uploadedAt.toDisplayUploadDate()))
            add(DetailMetric("Upload time", uploadedAt.toDisplayUploadTime()))
        }
    }
}

private data class DetailMetric(
    val label: String,
    val value: String,
    val isError: Boolean = false
)

private fun Boolean?.yesNoUnknown(): String {
    return when (this) {
        true -> "Yes"
        false -> "No"
        null -> "Unknown"
    }
}

private fun Double?.formatNullable(): String {
    return this?.let { String.format(Locale.US, "%.1f", it) } ?: "unknown"
}

private fun Float?.formatNullable(): String {
    return this?.let { String.format(Locale.US, "%.1f", it) } ?: "unknown"
}

private fun Double?.formatCoordinate(): String {
    return this?.let { String.format(Locale.US, "%.4f", it) } ?: "unknown"
}

private fun Long?.toDisplayTime(): String {
    return this?.let {
        DateFormat.getDateTimeInstance(
            DateFormat.SHORT,
            DateFormat.SHORT,
            Locale.US
        ).format(Date(it))
    } ?: "unknown"
}

private fun Long.toDisplayUploadDate(): String {
    return SimpleDateFormat("MMM d", Locale.US).format(Date(this))
}

private fun Long.toDisplayUploadTime(): String {
    return SimpleDateFormat("h:mm a", Locale.US).format(Date(this))
}

@Preview(showBackground = true)
@Composable
fun AnomalyHistoryScreenPreview() {
    MaterialTheme {
        AnomalyHistoryScreen(
            entries = listOf(
                HistoryEntry(
                    timestamp = System.currentTimeMillis(),
                    date = "2026-06-16", time = "14:35:22", day = "Monday",
                    type = HistoryEntry.TYPE_SOUND,
                    soundLevelDb = 72.3, accelerationMagnitude = 9.8f,
                    motionDetected = false, severity = 3,
                    description = "Sound spike 72.3 dB"
                ),
                HistoryEntry(
                    timestamp = System.currentTimeMillis() - 10_000,
                    date = "2026-06-16", time = "14:35:10", day = "Monday",
                    type = HistoryEntry.TYPE_MOTION,
                    soundLevelDb = 30.0, accelerationMagnitude = 12.5f,
                    motionDetected = true, severity = 4,
                    description = "Motion detected 12.5 m/s\u00B2"
                )
            ),
            onUploadHistory = {},
            isUploadingHistory = false,
            uploadStatusMessage = ""
        )
    }
}
