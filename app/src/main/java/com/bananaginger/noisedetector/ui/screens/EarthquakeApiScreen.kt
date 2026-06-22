package com.bananaginger.noisedetector.ui.screens

// ---------------------------------------------------------------------------
// EarthquakeApiScreen.kt  —  Screen 4: "API"
//
// Lets the user manually test the USGS Earthquake API integration.
// Pressing the button triggers a network call that looks up the nearest
// recent earthquake to a fixed reference point.
//
// What this screen shows:
//   • A "Test Earthquake API" button (disabled while a request is in flight)
//   • A loading spinner + status text while the call is running
//   • The result card with magnitude, location, depth, coordinates, and time
//   • An error message if the network call fails
//
// The actual network logic lives in AnomalyViewModel + AnomalyRepository,
// so this file only handles the UI layer.
// ---------------------------------------------------------------------------

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EarthquakeApiScreen(
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
            text = "Earthquake API",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "Tap the button to query the USGS earthquake feed for " +
                    "the nearest recent seismic event.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()

        // ---- Test button ----
        // Disabled while isLoading = true so the user cannot fire
        // multiple concurrent requests.
        Button(
            onClick = viewModel::testEarthquakeApi,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = if (uiState.isLoading) "Fetching…" else "Test Earthquake API",
                style = MaterialTheme.typography.titleMedium
            )
        }

        // ---- Loading spinner — visible only during the API call ----
        if (uiState.isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Text(
                    text = "Checking nearby earthquakes…",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // ---- Status / informational message ----
        Text(
            text = uiState.statusMessage,
            style = MaterialTheme.typography.bodyMedium
        )

        // ---- Error message (shown in red when the call fails) ----
        uiState.errorMessage?.let { error ->
            Text(
                text = "Error: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // ---- Result card — only rendered when we have a successful response ----
        uiState.earthquake?.let { quake ->
            HorizontalDivider()
            EarthquakeResultCard(earthquake = quake)
        }
    }
}

// ---------------------------------------------------------------------------
// EarthquakeResultCard
//
// Displays the earthquake data returned by the USGS API inside a Card.
// Fields: magnitude, location name, depth, lat/lon coordinates, event time.
// ---------------------------------------------------------------------------
@Composable
private fun EarthquakeResultCard(earthquake: EarthquakeSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Nearby Earthquake",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            LabelValue(
                label = "Magnitude",
                value = earthquake.magnitude?.let {
                    String.format(Locale.US, "M%.1f", it)
                } ?: "Unknown"
            )

            LabelValue(label = "Location", value = earthquake.place)

            LabelValue(
                label = "Depth",
                value = earthquake.depthKm?.let {
                    String.format(Locale.US, "%.1f km", it)
                } ?: "Unknown"
            )

            LabelValue(
                label = "Coordinates",
                value = String.format(
                    Locale.US,
                    "%.2f, %.2f",
                    earthquake.latitude,
                    earthquake.longitude
                )
            )

            LabelValue(
                label = "Time",
                value = earthquake.timeMillis?.toDisplayTime() ?: "Unknown"
            )
        }
    }
}

// Small helper composable: left-aligned label, right-aligned value.
@Composable
private fun LabelValue(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun Long.toDisplayTime(): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US)
        .format(Date(this))
