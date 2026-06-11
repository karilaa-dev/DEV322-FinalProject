package com.bananaginger.noisedetector.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bananaginger.noisedetector.data.model.AnomalyEvent
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

    init {
        observeAnomalies()
    }

    private fun observeAnomalies() {
        viewModelScope.launch {
            repository.observeAnomalies().collect { anomalies ->
                _uiState.update { currentState ->
                    currentState.copy(anomalies = anomalies)
                }
            }
        }
    }

    fun recordDemoAnomaly() {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    statusMessage = "Recording demo anomaly...",
                    warningMessage = null,
                    errorMessage = null
                )
            }

            try {
                val result = repository.recordAnomaly(
                    event = createDemoAnomaly(),
                    location = DEMO_LOCATION
                )

                val statusMessage = if (result.earthquake != null) {
                    "Demo anomaly saved with nearby earthquake data."
                } else {
                    "Demo anomaly saved. No nearby earthquake found."
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        statusMessage = statusMessage,
                        warningMessage = result.warningMessage,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        statusMessage = "Could not record demo anomaly.",
                        warningMessage = null,
                        errorMessage = exception.message ?: "Unknown error"
                    )
                }
            }
        }
    }

    private fun createDemoAnomaly(): AnomalyEvent {
        return AnomalyEvent(
            timestampMillis = System.currentTimeMillis(),
            soundLevelDb = 82.0,
            motionDetected = true,
            thresholdDb = 75.0,
            eventClassification = "abnormal_movement"
        )
    }

    companion object {
        val DEMO_LOCATION = LocationSnapshot(
            latitude = 47.6101,
            longitude = -122.2015
        )
    }
}
