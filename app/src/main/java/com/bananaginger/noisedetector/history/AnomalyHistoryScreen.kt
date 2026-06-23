package com.bananaginger.noisedetector.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

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
                        HistoryEntryCard(entry = entry)
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
private fun HistoryEntryCard(entry: HistoryEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
