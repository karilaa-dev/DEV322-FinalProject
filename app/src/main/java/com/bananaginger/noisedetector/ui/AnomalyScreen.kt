package com.bananaginger.noisedetector.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.location.LocationSelectionSource
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnomalyScreen(
    uiState: AnomalyUiState,
    onTestEarthquakeApi: () -> Unit,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasLookupLocation = uiState.selectedLocation != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Anomalies",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Sensor monitoring",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Lookup location: ${uiState.locationSourceLabel ?: "not set"}",
            color = if (hasLookupLocation) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.error
            }
        )

        if (!hasLookupLocation) {
            Text(
                text = "Set a lookup location in Settings before monitoring or testing earthquakes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Settings")
        }

        HorizontalDivider()

        Text(
            text = "Monitoring: ${
                if (uiState.isMonitoring) "ACTIVE" else "STOPPED"
            }"
        )

        Text(
            text = "Estimated sound level: ${
                uiState.estimatedSoundLevelDb.formatOneDecimal()
            } dB"
        )

        Text(
            text = "Acceleration magnitude: ${
                uiState.accelerationMagnitude.formatOneDecimal()
            } m/s²"
        )

        Text(
            text = "Motion detected: ${uiState.motionDetected}"
        )

        Text(
            text = "Anomaly detected: ${uiState.anomalyDetected}"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartMonitoring,
                enabled = !uiState.isMonitoring && hasLookupLocation,
                modifier = Modifier.weight(1f)
            ) {
                Text("Start")
            }

            Button(
                onClick = onStopMonitoring,
                enabled = uiState.isMonitoring,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }
        }

        HorizontalDivider()

        Text(
            text = "Earthquake API integration",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = onTestEarthquakeApi,
            enabled = !uiState.isLoading && hasLookupLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.isLoading) {
                    "Testing..."
                } else {
                    "Test Earthquake API"
                }
            )
        }

        if (uiState.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp)
                )

                Text("Checking nearby earthquakes...")
            }
        }

        if (uiState.statusMessage.isNotBlank()) {
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        uiState.earthquake?.let { earthquake ->
            HorizontalDivider()
            EarthquakeResult(earthquake)
        }
    }
}

@Composable
private fun EarthquakeResult(
    earthquake: EarthquakeSummary
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Nearby Earthquake",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = "M${earthquake.magnitude.formatNullable()} " +
                    earthquake.place,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Depth ${earthquake.depthKm.formatNullable()} km",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Coordinates ${
                earthquake.latitude.formatOneDecimal()
            }, ${earthquake.longitude.formatOneDecimal()}",
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = "Time ${
                earthquake.timeMillis?.toDisplayTime() ?: "unknown"
            }",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun Long.toDisplayTime(): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.SHORT,
        DateFormat.SHORT,
        Locale.US
    ).format(
        Date(this)
    )
}

private fun Float.formatOneDecimal(): String {
    return String.format(
        Locale.US,
        "%.1f",
        this
    )
}

private fun Double.formatOneDecimal(): String {
    return String.format(
        Locale.US,
        "%.1f",
        this
    )
}

private fun Double?.formatNullable(): String {
    return this?.formatOneDecimal() ?: "unknown"
}

@Preview(showBackground = true)
@Composable
private fun AnomalyScreenPreview() {
    NoiseAndMotionAnomalyDetectorTheme {
        AnomalyScreen(
            uiState = AnomalyUiState(
                selectedLocation = LocationSnapshot(47.6101, -122.2015),
                locationSource = LocationSelectionSource.MAP,
                locationSourceLabel = LocationSelectionSource.MAP.label,
                earthquake = EarthquakeSummary(
                    id = "uw123456",
                    place = "10 km NW of Seattle",
                    magnitude = 3.2,
                    latitude = 47.7,
                    longitude = -122.3,
                    depthKm = 12.5,
                    timeMillis = 1_765_000_000_000
                )
            ),
            onTestEarthquakeApi = {},
            onStartMonitoring = {},
            onStopMonitoring = {},
            onOpenSettings = {}
        )
    }
}
