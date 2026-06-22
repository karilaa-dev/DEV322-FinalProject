package com.bananaginger.noisedetector.ui.screens

// ---------------------------------------------------------------------------
// SettingsScreen.kt  —  Screen 2: "Settings"
//
// Lets the user adjust the two detection thresholds:
//   • Sound threshold (dB)  — how loud a sound must be to count
//   • Motion threshold (m/s²) — how hard the device must shake to count
//
// An anomaly is only recorded when BOTH thresholds are exceeded at the same
// time.  That rule is explained in a hint at the bottom of this screen.
//
// The sliders call ViewModel methods directly so changes are reflected
// immediately in the live sensor card on HomeScreen (shared UiState).
// ---------------------------------------------------------------------------

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: AnomalyViewModel,
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
            text = "Detection Settings",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Drag the sliders to change how sensitive the detector is.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // ---- Sound threshold section ----
        Text(
            text = "Sound threshold",
            style = MaterialTheme.typography.titleMedium
        )

        // Shows the current value next to the label so the user sees it
        // update in real time as they drag.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Trigger level")
            Text(
                text = "${String.format(Locale.US, "%.1f", uiState.soundThresholdDb)} dB",
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Sound slider: range 0–120 dB (typical room noise = ~40 dB).
        Slider(
            value = uiState.soundThresholdDb.toFloat(),
            onValueChange = { viewModel.updateSoundThreshold(it.toDouble()) },
            valueRange = 0f..120f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Lower value → detects quieter sounds.\nHigher value → only very loud sounds trigger.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // ---- Motion threshold section ----
        Text(
            text = "Motion threshold",
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Trigger level")
            Text(
                text = "${String.format(Locale.US, "%.1f", uiState.motionThreshold)} m/s²",
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Motion slider: range 0–8 m/s² (gravity = 9.8 m/s² for reference).
        Slider(
            value = uiState.motionThreshold,
            onValueChange = viewModel::updateMotionThreshold,
            valueRange = 0f..8f,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Lower value → detects gentle movement.\nHigher value → only strong shaking triggers.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // ---- Combined rule reminder ----
        Text(
            text = "An anomaly is recorded only when BOTH sound AND motion " +
                    "exceed their thresholds at the same time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
