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
import android.hardware.SensorManager
import com.bananaginger.noisedetector.data.sensor.MotionSensorReader
import com.bananaginger.noisedetector.data.sensor.SoundSensorReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

// BananaGinger/Kyryl: ViewModel owns coroutine scope and exposes StateFlow to Compose.
class AnomalyViewModel(
    private val repository: AnomalyRepository,
    private val motionSensorReader: MotionSensorReader,
    private val soundSensorReader: SoundSensorReader
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnomalyUiState())
    val uiState: StateFlow<AnomalyUiState> = _uiState.asStateFlow()

    private var motionJob: Job? = null
    private var soundJob: Job? = null

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
    fun startMonitoring() {
        if (motionJob?.isActive == true || soundJob?.isActive == true) {
            return
        }

        if (!motionSensorReader.isAvailable) {
            showError("Accelerometer is unavailable.")
            return
        }

        if (!soundSensorReader.isAvailable) {
            showError("Microphone is unavailable.")
            return
        }

        _uiState.update { currentState ->
            currentState.copy(
                isMonitoring = true,
                showHistory = false,
                statusMessage = "Monitoring started. Listening for sound and motion.",
                errorMessage = null
            )
        }

        motionJob = viewModelScope.launch {
            motionSensorReader.readings()
                .catch { exception ->
                    handleSensorFailure(exception)
                }
                .collect { reading ->
                    val deviationFromGravity = abs(
                        reading.accelerationMagnitude -
                                SensorManager.GRAVITY_EARTH
                    )

                    _uiState.update { currentState ->
                        currentState.copy(
                            accelerationMagnitude =
                                reading.accelerationMagnitude,
                            motionDetected =
                                deviationFromGravity > MOTION_THRESHOLD
                        )
                    }
                }
        }

        soundJob = viewModelScope.launch {
            soundSensorReader.readings()
                .catch { exception ->
                    handleSensorFailure(exception)
                }
                .collect { reading ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            estimatedSoundLevelDb =
                                reading.estimatedSoundLevelDb
                        )
                    }
                }
        }
    }

    fun stopMonitoring() {
        motionJob?.cancel()
        soundJob?.cancel()

        motionJob = null
        soundJob = null

        _uiState.update { currentState ->
            currentState.copy(
                isMonitoring = false,
                estimatedSoundLevelDb = 0.0,
                accelerationMagnitude = 0.0f,
                motionDetected = false,
                statusMessage = "Monitoring stopped.",
                errorMessage = null
            )
        }
    }

    fun microphonePermissionDenied() {
        showError(
            "Microphone permission is required to start monitoring."
        )
    }

    fun viewHistory() {
        _uiState.update { currentState ->
            currentState.copy(
                showHistory = true,
                statusMessage = "Showing anomaly history.",
                errorMessage = null
            )
        }
    }

    private fun handleSensorFailure(exception: Throwable) {
        if (exception is CancellationException) {
            return
        }

        motionJob?.cancel()
        soundJob?.cancel()

        motionJob = null
        soundJob = null

        _uiState.update { currentState ->
            currentState.copy(
                isMonitoring = false,
                statusMessage = "Sensor monitoring stopped.",
                errorMessage =
                    exception.message ?: "Unable to read sensor data."
            )
        }
    }

    private fun showError(message: String) {
        _uiState.update { currentState ->
            currentState.copy(
                isMonitoring = false,
                statusMessage = "Unable to start monitoring.",
                errorMessage = message
            )
        }
    }

    companion object {
        private const val MOTION_THRESHOLD = 1.5f

        val DEMO_LOCATION = LocationSnapshot(
            latitude = 47.6101,
            longitude = -122.2015
        )
    }
}
