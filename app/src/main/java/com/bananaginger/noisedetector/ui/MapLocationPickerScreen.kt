package com.bananaginger.noisedetector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapLocationPickerScreen(
    initialLocation: LocationSnapshot?,
    onConfirm: (LocationSnapshot) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultLocation = initialLocation?.toLatLng()
        ?: LatLng(47.6101, -122.2015)
    var selectedLocation by remember {
        mutableStateOf(defaultLocation)
    }
    val markerState = remember {
        MarkerState(position = selectedLocation)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 8f)
    }

    LaunchedEffect(selectedLocation) {
        markerState.position = selectedLocation
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Pick Lookup Location",
            style = MaterialTheme.typography.headlineSmall
        )

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                selectedLocation = latLng
            }
        ) {
            Marker(
                state = markerState,
                title = "Lookup location"
            )
        }

        Text(
            text = "Selected: %.4f, %.4f".format(
                selectedLocation.latitude,
                selectedLocation.longitude
            ),
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    onConfirm(
                        LocationSnapshot(
                            latitude = selectedLocation.latitude,
                            longitude = selectedLocation.longitude
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Use Location")
            }
        }
    }
}

private fun LocationSnapshot.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}
