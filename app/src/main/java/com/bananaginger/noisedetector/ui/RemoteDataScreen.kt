package com.bananaginger.noisedetector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.data.remote.RemoteAnomalyDocument
import com.bananaginger.noisedetector.data.remote.RemoteDataFilter
import com.bananaginger.noisedetector.data.remote.RemoteDataKind
import com.bananaginger.noisedetector.data.remote.RemoteEarthquakeDocument
import com.bananaginger.noisedetector.history.historyTypeDisplayLabel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RemoteDataScreen(
    uiState: AnomalyUiState,
    onKindChanged: (RemoteDataKind) -> Unit,
    onFilterChanged: (RemoteDataFilter) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Remote History",
            style = MaterialTheme.typography.headlineSmall
        )

        TabRow(
            selectedTabIndex = if (uiState.remoteDataKind == RemoteDataKind.ANOMALIES) 0 else 1
        ) {
            Tab(
                selected = uiState.remoteDataKind == RemoteDataKind.ANOMALIES,
                onClick = { onKindChanged(RemoteDataKind.ANOMALIES) },
                text = { Text("Uploaded Events") }
            )
            Tab(
                selected = uiState.remoteDataKind == RemoteDataKind.EARTHQUAKES,
                onClick = { onKindChanged(RemoteDataKind.EARTHQUAKES) },
                text = { Text("Earthquakes") }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RemoteDataFilter.entries.forEach { filter ->
                FilterChip(
                    selected = uiState.remoteDataFilter == filter,
                    onClick = { onFilterChanged(filter) },
                    label = { Text(filter.label()) }
                )
            }
        }

        uiState.remoteErrorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (uiState.isLoadingRemoteData) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text("Loading remote data...")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.remoteDataKind == RemoteDataKind.ANOMALIES) {
                if (!uiState.isLoadingRemoteData &&
                    uiState.remoteErrorMessage == null &&
                    uiState.remoteAnomalies.isEmpty()
                ) {
                    item {
                        EmptyRemoteState("No uploaded events found for this filter.")
                    }
                }

                items(uiState.remoteAnomalies) { anomaly ->
                    RemoteAnomalyCard(anomaly)
                }
            } else {
                if (!uiState.isLoadingRemoteData &&
                    uiState.remoteErrorMessage == null &&
                    uiState.remoteEarthquakes.isEmpty()
                ) {
                    item {
                        EmptyRemoteState("No remote earthquakes found for this filter.")
                    }
                }

                items(uiState.remoteEarthquakes) { earthquake ->
                    RemoteEarthquakeCard(earthquake)
                }
            }
        }

        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Refresh")
        }
    }
}

@Composable
private fun EmptyRemoteState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun RemoteAnomalyCard(anomaly: RemoteAnomalyDocument) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = historyTypeDisplayLabel(anomaly.type),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = anomaly.description.orEmpty(),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Sound: ${anomaly.soundLevelDb.formatNullable()} dB  Accel: ${
                    anomaly.accelerationMagnitude?.toDouble().formatNullable()
                } m/s²",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Uploaded: ${anomaly.uploadedAt.toDisplayTime()}",
                style = MaterialTheme.typography.bodySmall
            )
            anomaly.closestEarthquakeId?.let {
                Text(
                    text = "Closest earthquake: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RemoteEarthquakeCard(earthquake: RemoteEarthquakeDocument) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "M${earthquake.magnitude.formatNullable()} ${earthquake.place}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Depth: ${earthquake.depthKm.formatNullable()} km",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Epicenter: %.4f, %.4f".format(
                    Locale.US,
                    earthquake.latitude,
                    earthquake.longitude
                ),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Time: ${earthquake.timeMillis.toDisplayTime()}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun RemoteDataFilter.label(): String {
    return when (this) {
        RemoteDataFilter.ALL -> "All"
        RemoteDataFilter.MINE -> "Mine"
        RemoteDataFilter.OTHERS -> "Others"
    }
}

private fun Double?.formatNullable(): String {
    return this?.let { String.format(Locale.US, "%.1f", it) } ?: "unknown"
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
