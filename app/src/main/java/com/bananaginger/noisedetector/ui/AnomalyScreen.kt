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
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog

@Composable
fun AnomalyScreen(
    viewModel: AnomalyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.startMonitoring()
            } else {
                viewModel.microphonePermissionDenied()
            }
        }

    AnomalyScreenContent(
        uiState = uiState,
        onTestEarthquakeApi = viewModel::testEarthquakeApi,
        onStartMonitoringApi = {
            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

            if (permissionGranted) {
                viewModel.startMonitoring()
            } else {
                microphonePermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        },
        onStopMonitoringApi = viewModel::stopMonitoring,
        onViewHistoryApi = viewModel::viewHistory,
        onDismissAnomalyDialog = viewModel::dismissAnomalyDialog,
        modifier = modifier
        )
}

@Composable
private fun AnomalyScreenContent(
    uiState: AnomalyUiState,
    onTestEarthquakeApi: () -> Unit,
    onStartMonitoringApi: () -> Unit,
    onStopMonitoringApi: () -> Unit,
    onViewHistoryApi: () -> Unit,
    onDismissAnomalyDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    // dylan shows a dialoge box if an anomaly happens
    if (uiState.showAnomalyDialog) {
        AlertDialog(
            onDismissRequest = onDismissAnomalyDialog,
            title = {
                Text("Anomaly Detected")
            },
            text = {
                Column {
                    Text(
                        text = "Loud sound and movement were detected."
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "Sound level: ${
                            uiState.estimatedSoundLevelDb.formatOneDecimal()
                        } dB"
                    )

                    Text(
                        text = "Acceleration: ${
                            uiState.accelerationMagnitude.formatOneDecimal()
                        } m/s²"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissAnomalyDialog
                ) {
                    Text("OK")
                }
            }
        )
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Noise and Motion Anomaly Detector",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Sensor monitoring",
            style = MaterialTheme.typography.titleMedium
        )

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

        Button(
            onClick = onStartMonitoringApi,
            enabled = !uiState.isMonitoring,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Monitoring")
        }

        Button(
            onClick = onStopMonitoringApi,
            enabled = uiState.isMonitoring,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop Monitoring")
        }

        HorizontalDivider()

        Text(
            text = "Earthquake API integration",
            style = MaterialTheme.typography.titleMedium
        )

        Button(
            onClick = onTestEarthquakeApi,
            enabled = !uiState.isLoading,
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

        Button(
            onClick = onViewHistoryApi,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View History")
        }

        // dylan show history
        if (uiState.showHistory) {
            HorizontalDivider()

            Text(
                text = "Anomaly History",
                style = MaterialTheme.typography.titleMedium
            )

            if (uiState.anomalyHistory.isEmpty()) {
                Text("No anomalies have been recorded.")
            } else {
                uiState.anomalyHistory.take(10).forEach { anomaly ->
                    Text(
                        text = buildString {
                            append(anomaly.type)
                            append(" — ")
                            append(
                                anomaly.magnitude?.let {
                                    String.format(
                                        Locale.US,
                                        "%.1f dB",
                                        it
                                    )
                                } ?: "Unknown magnitude"
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = DateFormat.getDateTimeInstance()
                            .format(Date(anomaly.timestamp)),
                        style = MaterialTheme.typography.bodySmall
                    )

                    anomaly.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
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

        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium
        )

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
        AnomalyScreenContent(
            uiState = AnomalyUiState(
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
            onStartMonitoringApi = {},
            onStopMonitoringApi = {},
            onViewHistoryApi = {},
            onDismissAnomalyDialog = {}
        )
    }
}