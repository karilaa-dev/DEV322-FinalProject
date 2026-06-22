package com.bananaginger.noisedetector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LocationChoiceScreen(
    uiState: AnomalyUiState,
    onUsePhoneLocation: () -> Unit,
    onPickOnMap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Choose Earthquake Lookup Location",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = "This location is used only on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Button(
            onClick = onUsePhoneLocation,
            enabled = !uiState.isResolvingLocation,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Use Phone Location")
        }

        OutlinedButton(
            onClick = onPickOnMap,
            enabled = !uiState.isResolvingLocation,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Text("Pick on Map")
        }

        if (uiState.isResolvingLocation) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
        }

        uiState.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}
