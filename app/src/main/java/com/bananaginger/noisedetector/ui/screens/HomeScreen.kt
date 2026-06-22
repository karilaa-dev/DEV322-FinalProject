package com.bananaginger.noisedetector.ui.screens

// ---------------------------------------------------------------------------
// HomeScreen.kt  —  Screen 1: "Monitor"
//
// This is the ROOT screen — the first thing the user sees when the app opens.
//
// Behaviour:
//   - Shows two large buttons: "Start Monitoring" and "Stop Monitoring".
//   - When monitoring is STOPPED: only the Start button is active; no sensor
//     readings are shown (clean, minimal view).
//   - When monitoring is ACTIVE: a live sensor card slides in at the top,
//     showing real-time sound level, acceleration, and anomaly status.
//     The Stop button becomes active so the user can halt monitoring.
//
// This screen intentionally has NO threshold sliders and NO history list —
// those live on their own dedicated screens (Settings and History).
// ---------------------------------------------------------------------------

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: AnomalyViewModel,
    onStartMonitoring: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // ---- Page title ----
        Text(
            text = "Noise & Motion Monitor",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Press Start to begin detecting sound and motion anomalies.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ---- Live sensor card — only shown while monitoring is ACTIVE ----
        // AnimatedVisibility adds a slide-down + fade-in when the card appears,
        // and slide-up + fade-out when it disappears.  This gives the user
        // clear visual feedback that monitoring started or stopped.
        AnimatedVisibility(
            visible = uiState.isMonitoring,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            LiveSensorCard(
                soundDb = uiState.estimatedSoundLevelDb,
                accelMs2 = uiState.accelerationMagnitude,
                motionDetected = uiState.motionDetected,
                anomalyDetected = uiState.anomalyDetected
            )
        }

        HorizontalDivider()

        // ---- Start button ----
        // Disabled while monitoring is already active so the user cannot
        // accidentally start a second monitoring session.
        Button(
            onClick = onStartMonitoring,
            enabled = !uiState.isMonitoring,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (uiState.isMonitoring) "Monitoring Active…" else "Start Monitoring",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // ---- Stop button ----
        // Uses OutlinedButton (secondary style) to signal it is a "stop" action.
        OutlinedButton(
            onClick = viewModel::stopMonitoring,
            enabled = uiState.isMonitoring,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "Stop Monitoring",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // ---- Hint text ----
        if (!uiState.isMonitoring) {
            Text(
                text = "Tip: use the Settings tab to adjust detection thresholds\n" +
                        "and the History tab to review past anomalies.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// LiveSensorCard
//
// Small card shown at the top of HomeScreen while monitoring is running.
// Displays the three key real-time values coming from the ViewModel:
//   • Estimated sound level in dB
//   • Acceleration magnitude in m/s²
//   • Whether an anomaly (both thresholds crossed) is currently active
// ---------------------------------------------------------------------------
@Composable
private fun LiveSensorCard(
    soundDb: Double,
    accelMs2: Float,
    motionDetected: Boolean,
    anomalyDetected: Boolean
) {
    // Highlight the card red when an anomaly is detected.
    val cardColor = if (anomalyDetected)
        MaterialTheme.colorScheme.errorContainer
    else
        MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (anomalyDetected) "⚠ Anomaly Detected!" else "Live Readings",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Sound level")
                Text(text = "${String.format(Locale.US, "%.1f", soundDb)} dB")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Acceleration")
                Text(text = "${String.format(Locale.US, "%.1f", accelMs2)} m/s²")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Motion detected")
                Text(text = if (motionDetected) "Yes" else "No")
            }
        }
    }
}
