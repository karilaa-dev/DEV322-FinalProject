package com.bananaginger.noisedetector.ui.screens

// ---------------------------------------------------------------------------
// HistoryScreen.kt  —  Screen 3: "History"
//
// Shows a scrollable list of every anomaly event recorded during the current
// session.  The list is stored inside AnomalyViewModel, so it survives:
//   • Screen rotation
//   • App going to background
// It is cleared when the process is killed (user fully closes the app).
//
// Each item in the list is rendered by HistoryEntryCard (imported from the
// existing history package so we reuse the same card design).
//
// Key differences from the old AnomalyHistoryScreen:
//   • No "Back" button — the bottom nav bar handles navigation.
//   • Accepts PaddingValues from the Scaffold so it doesn't hide behind
//     the bottom navigation bar.
// ---------------------------------------------------------------------------

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.history.HistoryEntry
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: AnomalyViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsState()
    val entries = uiState.historyEntries

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // ---- Page title ----
        Text(
            text = "Anomaly History",
            style = MaterialTheme.typography.headlineSmall
        )

        // ---- Entry count subtitle ----
        Text(
            text = "${entries.size} event(s) recorded this session",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        if (entries.isEmpty()) {
            // ---- Empty state — nothing recorded yet ----
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "No anomalies recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Go to the Monitor tab, start monitoring, and trigger a loud sound + movement.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            // ---- Scrollable list of recorded anomaly cards ----
            // LazyColumn only renders visible items, keeping memory usage low
            // even if there are hundreds of entries in a long session.
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.id }) { entry ->
                    HistoryEntryCard(entry = entry)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// HistoryEntryCard
//
// Displays a single anomaly record as a Material 3 Card.
// Shows: anomaly type, timestamp, sound level, acceleration, severity.
// ---------------------------------------------------------------------------
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
            // Type badge + timestamp on the same row
            Text(
                text = "${entry.type}  •  ${entry.timestamp.toDisplayTime()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium
            )

            // Sensor readings — only meaningful for SOUND_AND_MOTION entries
            if (entry.soundLevelDb != null && entry.accelerationMagnitude != null) {
                Text(
                    text = "Sound: ${String.format(Locale.US, "%.1f", entry.soundLevelDb)} dB  " +
                            "· Accel: ${String.format(Locale.US, "%.1f", entry.accelerationMagnitude)} m/s²",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Formats a Long epoch-millisecond timestamp to a short readable date+time string.
private fun Long.toDisplayTime(): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US)
        .format(Date(this))
