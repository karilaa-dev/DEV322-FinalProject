package com.bananaginger.noisedetector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.repository.AnomalyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// BananaGinger/Kyryl: ViewModel owns coroutine scope and exposes StateFlow to Compose.
class AnomalyViewModel(
    private val repository: AnomalyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnomalyUiState())
    val uiState: StateFlow<AnomalyUiState> = _uiState.asStateFlow()

    fun testEarthquakeApi() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    statusMessage = "Checking nearby earthquakes...",
                    earthquake = null,
                    errorMessage = null
                )
            }

            try {
                val earthquake = repository.testNearbyEarthquakeLookup(
                    location = DEMO_LOCATION,
                    eventTimeMillis = System.currentTimeMillis()
                )

                val statusMessage = if (earthquake != null) {
                    "Earthquake API test complete. Nearby event found."
                } else {
                    "Earthquake API test complete. No nearby event found."
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        statusMessage = statusMessage,
                        earthquake = earthquake,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        statusMessage = "Earthquake API test failed.",
                        earthquake = null,
                        errorMessage = exception.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    companion object {
        val DEMO_LOCATION = LocationSnapshot(
            latitude = 47.6101,
            longitude = -122.2015
        )
    }
}
