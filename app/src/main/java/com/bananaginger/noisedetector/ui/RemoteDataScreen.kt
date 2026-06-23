package com.bananaginger.noisedetector.ui

import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.data.remote.RemoteAnomalyDocument
import com.bananaginger.noisedetector.data.remote.RemoteDataFilter
import com.bananaginger.noisedetector.data.remote.RemoteDataKind
import com.bananaginger.noisedetector.data.remote.RemoteEarthquakeDocument
import com.bananaginger.noisedetector.data.remote.RemoteEarthquakeSnapshot
import com.bananaginger.noisedetector.history.HistorySectionDivider
import com.bananaginger.noisedetector.history.historyEventSections
import com.bananaginger.noisedetector.history.historyTypeColor
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
    var selectedRemoteAnomaly by remember { mutableStateOf<RemoteAnomalyDocument?>(null) }

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

                historyEventSections(uiState.remoteAnomalies) { it.timestamp }.forEach { section ->
                    item(key = "remote-anomaly-section-${section.title}") {
                        HistorySectionDivider(section.title)
                    }
                    items(
                        items = section.events,
                        key = { anomaly ->
                            "remote-anomaly-${anomaly.installId}-${anomaly.localAnomalyId}-${anomaly.timestamp}"
                        }
                    ) { anomaly ->
                        RemoteAnomalyCard(
                            anomaly = anomaly,
                            onClick = { selectedRemoteAnomaly = anomaly }
                        )
                    }
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

    selectedRemoteAnomaly?.let { anomaly ->
        RemoteAnomalyDetailSheet(
            anomaly = anomaly,
            onDismiss = { selectedRemoteAnomaly = null }
        )
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
private fun RemoteAnomalyCard(
    anomaly: RemoteAnomalyDocument,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(anomaly.remoteEventTestTag())
            .clickable(onClick = onClick),
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
                style = MaterialTheme.typography.titleMedium,
                color = historyTypeColor(anomaly.type),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            anomaly.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            anomaly.sensorSummary()?.let { summary ->
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            anomaly.uploadedAt?.let { uploadedAt ->
                Text(
                    text = "Uploaded: ${uploadedAt.toDisplayTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            anomaly.closestEarthquake?.place?.takeIf { it.isNotBlank() }?.let { place ->
                Text(
                    text = "Nearest earthquake: $place",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            anomaly.triggeredByLabel()?.let { triggeredBy ->
                Text(
                    text = "Triggered by: $triggeredBy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteAnomalyDetailSheet(
    anomaly: RemoteAnomalyDocument,
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
                .fillMaxHeight(0.82f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RemoteDetailHeader(
                anomaly = anomaly,
                onDismiss = onDismiss
            )

            CompositionLocalProvider(LocalOverscrollFactory provides null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val columns = if (maxWidth >= 560.dp) 3 else 2

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        anomaly.description
                            ?.takeIf { it.isNotBlank() }
                            ?.let { description ->
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                        RemoteDetailSection(title = "Event") {
                            RemoteMetricGrid(
                                metrics = anomaly.remoteEventMetrics(),
                                columns = columns
                            )
                        }

                        val sensorMetrics = anomaly.remoteSensorMetrics()
                        if (sensorMetrics.isNotEmpty()) {
                            RemoteDetailSection(title = "Readings") {
                                RemoteMetricGrid(
                                    metrics = sensorMetrics,
                                    columns = columns
                                )
                            }
                        }

                        anomaly.closestEarthquake?.let { earthquake ->
                            RemoteDetailSection(title = "Nearest Earthquake") {
                                earthquake.place
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let { place ->
                                        RemoteMetricTile(
                                            metric = RemoteDetailMetric(
                                                label = "Place",
                                                value = place
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            maxValueLines = 2
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }

                                RemoteMetricGrid(
                                    metrics = earthquake.remoteEarthquakeMetrics(),
                                    columns = columns
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
private fun RemoteDetailHeader(
    anomaly: RemoteAnomalyDocument,
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
                    text = historyTypeDisplayLabel(anomaly.type),
                    style = MaterialTheme.typography.titleLarge,
                    color = historyTypeColor(anomaly.type),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = anomaly.remoteSubtitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close remote event details"
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            anomaly.triggeredByLabel()?.let { triggeredBy ->
                RemoteInfoPill(text = "Triggered by: $triggeredBy")
            }
            anomaly.severity?.let { severity ->
                RemoteInfoPill(text = "Severity $severity/5")
            }
            anomaly.uploadedAt?.let {
                RemoteInfoPill(text = "Uploaded remotely")
            }
        }
    }
}

@Composable
private fun RemoteInfoPill(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer
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
private fun RemoteDetailSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
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
private fun RemoteMetricGrid(
    metrics: List<RemoteDetailMetric>,
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
                    RemoteMetricTile(
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
private fun RemoteMetricTile(
    metric: RemoteDetailMetric,
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
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = maxValueLines,
            overflow = TextOverflow.Ellipsis
        )
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

private data class RemoteDetailMetric(
    val label: String,
    val value: String
)

private fun RemoteAnomalyDocument.remoteEventMetrics(): List<RemoteDetailMetric> {
    return buildList {
        date?.takeIf { it.isNotBlank() }?.let { add(RemoteDetailMetric("Date", it)) }
        day?.takeIf { it.isNotBlank() }?.let { add(RemoteDetailMetric("Day", it)) }
        if (timestamp > 0L) {
            add(RemoteDetailMetric("Event time", timestamp.toDisplayTime()))
        }
        triggeredByLabel()?.let { add(RemoteDetailMetric("Triggered by", it)) }
        severity?.let { add(RemoteDetailMetric("Severity", "$it/5")) }
        installId.takeIf { it.isNotBlank() }?.let { add(RemoteDetailMetric("Install ID", it)) }
        add(RemoteDetailMetric("Local ID", localAnomalyId.toString()))
        uploadedAt?.let { add(RemoteDetailMetric("Upload time", it.toDisplayTime())) }
    }
}

private fun RemoteAnomalyDocument.remoteSensorMetrics(): List<RemoteDetailMetric> {
    return buildList {
        soundLevelDb?.let { add(RemoteDetailMetric("Sound", "${it.formatNullable()} dB")) }
        accelerationMagnitude?.let {
            add(RemoteDetailMetric("Acceleration", "${it.toDouble().formatNullable()} m/s²"))
        }
    }
}

private fun RemoteAnomalyDocument.sensorSummary(): String? {
    val readings = buildList {
        soundLevelDb?.let { add("Sound: ${it.formatNullable()} dB") }
        accelerationMagnitude?.let { add("Accel: ${it.toDouble().formatNullable()} m/s²") }
    }
    return readings.takeIf { it.isNotEmpty() }?.joinToString("  ")
}

private fun RemoteAnomalyDocument.triggeredByLabel(): String? {
    val triggers = buildList {
        if (soundThresholdExceeded == true) add("Sound")
        if (motionThresholdExceeded == true) add("Motion")
    }
    return triggers.takeIf { it.isNotEmpty() }?.joinToString(" & ")
}

private fun RemoteEarthquakeSnapshot.remoteEarthquakeMetrics(): List<RemoteDetailMetric> {
    return buildList {
        magnitude?.let { add(RemoteDetailMetric("Magnitude", "M${it.formatNullable()}")) }
        depthKm?.let { add(RemoteDetailMetric("Depth", "${it.formatNullable()} km")) }
        timeMillis?.let { add(RemoteDetailMetric("Earthquake time", it.toDisplayTime())) }
        source?.takeIf { it.isNotBlank() }?.let { add(RemoteDetailMetric("Source", it)) }
        if (latitude != null && longitude != null) {
            add(
                RemoteDetailMetric(
                    label = "Epicenter",
                    value = "${latitude.formatCoordinate()}, ${longitude.formatCoordinate()}"
                )
            )
        }
    }
}

private fun RemoteAnomalyDocument.remoteSubtitle(): String {
    return date
        ?.takeIf { it.isNotBlank() }
        ?: timestamp.takeIf { it > 0L }?.toDisplayTime()
        ?: "Remote event"
}

private fun RemoteAnomalyDocument.remoteEventTestTag(): String {
    return "remote-history-event-$installId-$localAnomalyId-$timestamp"
}

private fun Double?.formatNullable(): String {
    return this?.let { String.format(Locale.US, "%.1f", it) } ?: "unknown"
}

private fun Double.formatCoordinate(): String {
    return String.format(Locale.US, "%.4f", this)
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
