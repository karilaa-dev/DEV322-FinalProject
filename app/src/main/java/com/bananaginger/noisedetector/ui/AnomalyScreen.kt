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

@Composable
fun AnomalyScreen(
    viewModel: AnomalyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    AnomalyScreenContent(
        uiState = uiState,
        onTestEarthquakeApi = viewModel::testEarthquakeApi,
        modifier = modifier
    )
}

@Composable
private fun AnomalyScreenContent(
    uiState: AnomalyUiState,
    onTestEarthquakeApi: () -> Unit,
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
            onClick = onTestEarthquakeApi,
            enabled = !uiState.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (uiState.isLoading) "Testing..." else "Test Earthquake API")
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
private fun EarthquakeResult(earthquake: EarthquakeSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Nearby Earthquake",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "M${earthquake.magnitude.formatNullable()} ${earthquake.place}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Depth ${earthquake.depthKm.formatNullable()} km",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Coordinates ${earthquake.latitude.formatOneDecimal()}, ${earthquake.longitude.formatOneDecimal()}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Time ${earthquake.timeMillis?.toDisplayTime() ?: "unknown"}",
            style = MaterialTheme.typography.bodyMedium
        )
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
            onTestEarthquakeApi = {}
        )
    }
}
