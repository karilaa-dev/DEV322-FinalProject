package com.bananaginger.noisedetector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.bananaginger.noisedetector.data.local.AnomalyEntity
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnomalyScreen(
    viewModel: AnomalyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    AnomalyScreenContent(
        uiState = uiState,
        onRecordDemoAnomaly = viewModel::recordDemoAnomaly,
        modifier = modifier
    )
}

@Composable
private fun AnomalyScreenContent(
    uiState: AnomalyUiState,
    onRecordDemoAnomaly: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Noise and Motion Anomaly Detector",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Earthquake API integration",
            style = MaterialTheme.typography.titleMedium
        )
        Button(
            onClick = onRecordDemoAnomaly,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isLoading) "Recording..." else "Record Demo Anomaly")
        }

        if (uiState.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text("Checking nearby earthquakes...")
            }
        }

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium
        )
        uiState.warningMessage?.let { warning ->
            Text(
                text = "Warning: $warning",
                color = MaterialTheme.colorScheme.tertiary,
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

        HorizontalDivider()
        Text(
            text = "Anomaly History",
            style = MaterialTheme.typography.titleLarge
        )

        if (uiState.anomalies.isEmpty()) {
            Text(
                text = "No anomaly records yet.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.anomalies,
                    key = { it.id }
                ) { anomaly ->
                    AnomalyRow(anomaly)
                }
            }
        }
    }
}

@Composable
private fun AnomalyRow(anomaly: AnomalyEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = anomaly.timestampMillis.toDisplayTime(),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Sound ${anomaly.soundLevelDb.formatOneDecimal()} dB, threshold ${anomaly.thresholdDb.formatOneDecimal()} dB",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Motion detected: ${if (anomaly.motionDetected) "yes" else "no"}",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (anomaly.earthquakeId == null) {
            Text(
                text = "Nearby earthquake: none found",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Text(
                text = "Nearby earthquake: M${anomaly.earthquakeMagnitude.formatNullable()} ${anomaly.earthquakePlace ?: "Unknown location"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Depth ${anomaly.earthquakeDepthKm.formatNullable()} km, time ${anomaly.earthquakeTimeMillis?.toDisplayTime() ?: "unknown"}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

private fun Long.toDisplayTime(): String {
    return DateFormat.getDateTimeInstance(
        DateFormat.SHORT,
        DateFormat.SHORT,
        Locale.US
    ).format(Date(this))
}

private fun Double.formatOneDecimal(): String {
    return String.format(Locale.US, "%.1f", this)
}

private fun Double?.formatNullable(): String {
    return this?.formatOneDecimal() ?: "unknown"
}

@Preview(showBackground = true)
@Composable
private fun AnomalyScreenPreview() {
    NoiseAndMotionAnomalyDetectorTheme {
        AnomalyScreenContent(
            uiState = AnomalyUiState(
                anomalies = listOf(
                    AnomalyEntity(
                        id = 1,
                        timestampMillis = 1_765_000_000_000,
                        soundLevelDb = 82.0,
                        motionDetected = true,
                        thresholdDb = 75.0,
                        eventClassification = "abnormal_movement",
                        latitude = 47.6101,
                        longitude = -122.2015,
                        earthquakeId = "uw123456",
                        earthquakePlace = "10 km NW of Seattle",
                        earthquakeMagnitude = 3.2,
                        earthquakeLatitude = 47.7,
                        earthquakeLongitude = -122.3,
                        earthquakeDepthKm = 12.5,
                        earthquakeTimeMillis = 1_765_000_000_000
                    )
                )
            ),
            onRecordDemoAnomaly = {}
        )
    }
}
