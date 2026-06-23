package com.bananaginger.noisedetector.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.data.location.LocationSelectionSource
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.settings.DetectionTriggerMode
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme
import java.util.Locale

@Composable
fun SettingsScreen(
    uiState: AnomalyUiState,
    onUsePhoneLocation: () -> Unit,
    onPickOnMap: () -> Unit,
    onSoundThresholdChanged: (Double) -> Unit,
    onMotionThresholdChanged: (Float) -> Unit,
    onDetectionTriggerModeChanged: (DetectionTriggerMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Detection Settings",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Text(
            text = "Lookup location",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = null
            )
            Column {
                Text(
                    text = uiState.locationSourceLabel ?: "No lookup location set",
                    color = if (uiState.selectedLocation == null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                uiState.selectedLocation?.let { location ->
                    Text(
                        text = "%.4f, %.4f".format(
                            Locale.US,
                            location.latitude,
                            location.longitude
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.isMonitoring) {
            Text(
                text = "Stop monitoring before changing the lookup location.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onUsePhoneLocation,
                enabled = !uiState.isResolvingLocation && !uiState.isMonitoring,
                modifier = Modifier.weight(1f)
            ) {
                Text("Use Phone")
            }

            OutlinedButton(
                onClick = onPickOnMap,
                enabled = !uiState.isResolvingLocation && !uiState.isMonitoring,
                modifier = Modifier.weight(1f)
            ) {
                Text("Pick Map")
            }
        }

        if (uiState.isResolvingLocation) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text("Resolving current location...")
            }
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        HorizontalDivider()

        Text(
            text = "Detection thresholds",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = "Sound threshold: ${
                uiState.soundThresholdDb.formatOneDecimal()
            } dB"
        )

        Slider(
            value = uiState.soundThresholdDb.toFloat(),
            onValueChange = { value ->
                onSoundThresholdChanged(value.toDouble())
            },
            valueRange = 0f..120f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Motion threshold: ${
                uiState.motionThreshold.formatOneDecimal()
            } m/s²"
        )

        Slider(
            value = uiState.motionThreshold,
            onValueChange = onMotionThresholdChanged,
            valueRange = 0f..8f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Trigger mode",
            style = MaterialTheme.typography.titleSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DetectionTriggerMode.entries.forEach { mode ->
                FilterChip(
                    selected = uiState.detectionTriggerMode == mode,
                    onClick = { onDetectionTriggerModeChanged(mode) },
                    label = { Text(mode.displayLabel) }
                )
            }
        }

        Text(
            text = uiState.detectionTriggerMode.helperText(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    NoiseAndMotionAnomalyDetectorTheme {
        SettingsScreen(
            uiState = AnomalyUiState(
                selectedLocation = LocationSnapshot(47.6101, -122.2015),
                locationSource = LocationSelectionSource.MAP,
                locationSourceLabel = LocationSelectionSource.MAP.label
            ),
            onUsePhoneLocation = {},
            onPickOnMap = {},
            onSoundThresholdChanged = {},
            onMotionThresholdChanged = {},
            onDetectionTriggerModeChanged = {},
            onBack = {}
        )
    }
}

private fun DetectionTriggerMode.helperText(): String {
    return when (this) {
        DetectionTriggerMode.BOTH ->
            "An anomaly is saved only when sound and motion are both above their thresholds."
        DetectionTriggerMode.EITHER ->
            "An anomaly is saved when sound or motion is above its threshold."
    }
}
