package com.bananaginger.noisedetector.ui

import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.AnomalyWithEarthquake
import com.bananaginger.noisedetector.data.location.LocationProvider
import com.bananaginger.noisedetector.data.location.LocationSelectionSource
import com.bananaginger.noisedetector.data.location.LocationSelectionStore
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.remote.RemoteDataFilter
import com.bananaginger.noisedetector.data.remote.RemoteDataKind
import com.bananaginger.noisedetector.data.repository.AnomalyRepository
import com.bananaginger.noisedetector.data.settings.DetectionSettingsStore
import com.bananaginger.noisedetector.data.settings.DetectionTriggerEvaluator
import com.bananaginger.noisedetector.data.settings.DetectionTriggerMode
import com.bananaginger.noisedetector.data.sensor.MotionSensorReader
import com.bananaginger.noisedetector.data.sensor.SoundSensorReader
import com.bananaginger.noisedetector.history.HistoryEntry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class AnomalyViewModel(
    private val repository: AnomalyRepository,
    private val motionSensorReader: MotionSensorReader,
    private val soundSensorReader: SoundSensorReader,
    private val locationProvider: LocationProvider,
    private val locationSelectionStore: LocationSelectionStore,
    private val detectionSettingsStore: DetectionSettingsStore
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnomalyUiState())
    val uiState: StateFlow<AnomalyUiState> = _uiState.asStateFlow()

    private var motionJob: Job? = null
    private var soundJob: Job? = null
    private val lastRecordedMs = mutableMapOf<String, Long>()

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    private val dayFmt = SimpleDateFormat("EEEE", Locale.US)

    init {
        restoreDetectionSettings()
        restoreSavedLookupLocation()
        observeAnomalyHistory()
    }

    fun useRealLocation() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isResolvingLocation = true,
                    errorMessage = null,
                    statusMessage = "Resolving current location..."
                )
            }

            val location = locationProvider.getCurrentLocation()
            if (location == null) {
                _uiState.update {
                    it.copy(
                        isResolvingLocation = false,
                        statusMessage = "Choose a lookup location before monitoring.",
                        errorMessage = "Location permission or current location is unavailable."
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    selectedLocation = location,
                    locationSource = LocationSelectionSource.PHONE,
                    locationSourceLabel = LocationSelectionSource.PHONE.label,
                    isResolvingLocation = false,
                    statusMessage = "Earthquake lookup location is set.",
                    errorMessage = null
                )
            }
            locationSelectionStore.save(LocationSelectionSource.PHONE, location)
        }
    }

    fun locationPermissionDenied() {
        _uiState.update {
            it.copy(
                isResolvingLocation = false,
                statusMessage = "Choose a lookup location before monitoring.",
                errorMessage = "Location permission was denied. Pick a location on the map or allow location access."
            )
        }
    }

    fun changeLookupLocation() {
        stopMonitoring()
        _uiState.update {
            it.copy(
                statusMessage = "Choose a lookup location."
            )
        }
    }

    fun setManualLocation(location: LocationSnapshot) {
        _uiState.update {
            it.copy(
                selectedLocation = location,
                locationSource = LocationSelectionSource.MAP,
                locationSourceLabel = LocationSelectionSource.MAP.label,
                statusMessage = "Manual earthquake lookup location is set.",
                errorMessage = null
            )
        }
        locationSelectionStore.save(LocationSelectionSource.MAP, location)
    }

    fun testEarthquakeApi() {
        if (_uiState.value.selectedLocation == null) {
            showError("Choose a lookup location before testing earthquakes.")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    statusMessage = "Checking nearby earthquakes...",
                    earthquake = null,
                    errorMessage = null
                )
            }

            try {
                val location = resolveLookupLocation()
                if (location == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            statusMessage = "Choose a lookup location before testing earthquakes.",
                            errorMessage = null
                        )
                    }
                    return@launch
                }

                val earthquake = repository.testNearbyEarthquakeLookup(
                    location = location,
                    eventTimeMillis = System.currentTimeMillis()
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = if (earthquake != null) {
                            "Nearby earthquake found and saved locally."
                        } else {
                            "No nearby earthquake found."
                        },
                        earthquake = earthquake,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
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
        if (_uiState.value.selectedLocation == null) {
            showError("Choose a lookup location before monitoring.")
            return
        }
        if (motionJob?.isActive == true || soundJob?.isActive == true) return
        if (!motionSensorReader.isAvailable) {
            showError("Accelerometer is unavailable.")
            return
        }
        if (!soundSensorReader.isAvailable) {
            showError("Microphone is unavailable.")
            return
        }

        _uiState.update {
            it.copy(
                isMonitoring = true,
                statusMessage = "Monitoring started.",
                errorMessage = null
            )
        }

        motionJob = viewModelScope.launch {
            motionSensorReader.readings()
                .catch { exception -> handleSensorFailure(exception) }
                .collect { reading ->
                    val deviationFromGravity = abs(
                        reading.accelerationMagnitude - SensorManager.GRAVITY_EARTH
                    )
                    val motionDetected =
                        deviationFromGravity > _uiState.value.motionThreshold

                    _uiState.update {
                        it.copy(
                            accelerationMagnitude = reading.accelerationMagnitude,
                            motionDetected = motionDetected
                        )
                    }
                    evaluateAndRecordAnomaly()
                }
        }

        soundJob = viewModelScope.launch {
            soundSensorReader.readings()
                .catch { exception -> handleSensorFailure(exception) }
                .collect { reading ->
                    _uiState.update {
                        it.copy(estimatedSoundLevelDb = reading.estimatedSoundLevelDb)
                    }
                    evaluateAndRecordAnomaly()
                }
        }
    }

    fun stopMonitoring() {
        motionJob?.cancel()
        soundJob?.cancel()
        motionJob = null
        soundJob = null

        _uiState.update {
            it.copy(
                isMonitoring = false,
                estimatedSoundLevelDb = 0.0,
                accelerationMagnitude = 0.0f,
                motionDetected = false,
                anomalyDetected = false,
                statusMessage = "Monitoring stopped.",
                errorMessage = null
            )
        }
    }

    fun microphonePermissionDenied() {
        showError("Microphone permission is required to start monitoring.")
    }

    fun updateSoundThreshold(value: Double) {
        val clampedValue = value.coerceIn(0.0, 120.0)
        _uiState.update {
            it.copy(
                soundThresholdDb = clampedValue,
                statusMessage = "Sound threshold set to ${
                    String.format(Locale.US, "%.1f", clampedValue)
                } dB.",
                errorMessage = null
            )
        }
    }

    fun updateMotionThreshold(value: Float) {
        val clampedValue = value.coerceIn(0.0f, 8.0f)
        val motionDetected = abs(
            _uiState.value.accelerationMagnitude - SensorManager.GRAVITY_EARTH
        ) > clampedValue

        _uiState.update {
            it.copy(
                motionThreshold = clampedValue,
                motionDetected = motionDetected,
                statusMessage = "Motion threshold set to ${
                    String.format(Locale.US, "%.1f", clampedValue)
                } m/s² above resting gravity.",
                errorMessage = null
            )
        }
    }

    fun updateDetectionTriggerMode(mode: DetectionTriggerMode) {
        _uiState.update {
            it.copy(
                detectionTriggerMode = mode,
                statusMessage = when (mode) {
                    DetectionTriggerMode.BOTH ->
                        "Anomalies require both sound and motion thresholds."
                    DetectionTriggerMode.EITHER ->
                        "Anomalies can be saved by sound or motion threshold."
                },
                errorMessage = null
            )
        }
        detectionSettingsStore.saveTriggerMode(mode)
    }

    fun uploadHistory() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUploadingHistory = true,
                    uploadStatusMessage = "Uploading pending history...",
                    errorMessage = null
                )
            }

            val result = repository.uploadPendingHistory()
            _uiState.update {
                it.copy(
                    isUploadingHistory = false,
                    uploadStatusMessage = result.message,
                    errorMessage = null
                )
            }
        }
    }

    fun updateRemoteKind(kind: RemoteDataKind) {
        _uiState.update { it.copy(remoteDataKind = kind) }
        loadRemoteData()
    }

    fun updateRemoteFilter(filter: RemoteDataFilter) {
        _uiState.update { it.copy(remoteDataFilter = filter) }
        loadRemoteData()
    }

    fun loadRemoteData() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update {
                it.copy(isLoadingRemoteData = true, remoteErrorMessage = null)
            }

            try {
                val result = repository.fetchRemoteHistory(
                    kind = state.remoteDataKind,
                    filter = state.remoteDataFilter
                )
                _uiState.update {
                    it.copy(
                        isLoadingRemoteData = false,
                        remoteAnomalies = result.anomalies,
                        remoteEarthquakes = result.earthquakes,
                        remoteErrorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingRemoteData = false,
                        remoteErrorMessage = exception.message
                            ?: "Unable to load remote data."
                    )
                }
            }
        }
    }

    fun dismissAnomalyDialog() {
        _uiState.update { it.copy(showAnomalyDialog = false) }
    }

    private fun restoreSavedLookupLocation() {
        val selection = locationSelectionStore.load() ?: return
        _uiState.update {
            it.copy(
                selectedLocation = selection.location,
                locationSource = selection.source,
                locationSourceLabel = selection.source.label,
                statusMessage = "Using saved earthquake lookup location.",
                errorMessage = null
            )
        }
    }

    private fun restoreDetectionSettings() {
        _uiState.update {
            it.copy(
                detectionTriggerMode = detectionSettingsStore.loadTriggerMode()
            )
        }
    }

    private suspend fun resolveLookupLocation(): LocationSnapshot? {
        val state = _uiState.value
        val selectedLocation = state.selectedLocation ?: return null
        if (state.locationSource != LocationSelectionSource.PHONE) {
            return selectedLocation
        }

        val currentLocation = locationProvider.getCurrentLocation()
            ?: return selectedLocation

        locationSelectionStore.save(LocationSelectionSource.PHONE, currentLocation)
        _uiState.update {
            it.copy(
                selectedLocation = currentLocation,
                locationSource = LocationSelectionSource.PHONE,
                locationSourceLabel = LocationSelectionSource.PHONE.label,
                isResolvingLocation = false
            )
        }
        return currentLocation
    }

    private fun observeAnomalyHistory() {
        viewModelScope.launch {
            repository.observeAnomalyHistory()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            errorMessage = exception.message
                                ?: "Unable to load anomaly history."
                        )
                    }
                }
                .collect { anomalies ->
                    _uiState.update {
                        it.copy(
                            historyEntries = anomalies
                                .map { anomaly -> anomaly.toHistoryEntry() }
                                .sortedByDescending { entry -> entry.timestamp }
                        )
                    }
                }
        }
    }

    private fun AnomalyWithEarthquake.toHistoryEntry(): HistoryEntry {
        val detectedAt = Date(anomaly.timestamp)
        val normalizedType = when (anomaly.type.uppercase(Locale.US)) {
            "SOUND_AND_MOTION" -> HistoryEntry.TYPE_SOUND_AND_MOTION
            "SOUND" -> HistoryEntry.TYPE_SOUND
            "NOISE" -> HistoryEntry.TYPE_SOUND
            "MOTION" -> HistoryEntry.TYPE_MOTION
            "EARTHQUAKE" -> HistoryEntry.TYPE_EARTHQUAKE
            else -> anomaly.type.uppercase(Locale.US)
        }

        val savedAcceleration = anomaly.accelerationMagnitude
            ?: anomaly.metadata
                ?.split(';')
                ?.firstOrNull { value ->
                    value.startsWith("accelerationMagnitude=")
                }
                ?.substringAfter("accelerationMagnitude=")
                ?.toFloatOrNull()

        return HistoryEntry(
            id = anomaly.id,
            timestamp = anomaly.timestamp,
            date = anomaly.date ?: dateFmt.format(detectedAt),
            time = timeFmt.format(detectedAt),
            day = anomaly.day ?: dayFmt.format(detectedAt),
            type = normalizedType,
            soundLevelDb = anomaly.magnitude,
            accelerationMagnitude = savedAcceleration,
            soundThresholdDb = anomaly.soundThresholdDb,
            motionThreshold = anomaly.motionThreshold,
            soundThresholdExceeded = anomaly.soundThresholdExceeded
                ?: (normalizedType == HistoryEntry.TYPE_SOUND ||
                        normalizedType == HistoryEntry.TYPE_SOUND_AND_MOTION),
            motionThresholdExceeded = anomaly.motionThresholdExceeded
                ?: (normalizedType == HistoryEntry.TYPE_MOTION ||
                        normalizedType == HistoryEntry.TYPE_SOUND_AND_MOTION),
            motionDetected = anomaly.motionThresholdExceeded
                ?: (normalizedType == HistoryEntry.TYPE_MOTION ||
                        normalizedType == HistoryEntry.TYPE_SOUND_AND_MOTION),
            severity = anomaly.severity ?: 1,
            description = anomaly.description ?: "$normalizedType anomaly detected",
            closestEarthquakeId = anomaly.closestEarthquakeId,
            earthquakeMagnitude = earthquake?.magnitude,
            earthquakePlace = earthquake?.place,
            latitude = earthquake?.latitude,
            longitude = earthquake?.longitude,
            depthKm = earthquake?.depthKm,
            earthquakeTimeMillis = earthquake?.timeMillis,
            earthquakeSource = earthquake?.source,
            earthquakeRemoteUploadedAt = earthquake?.remoteUploadedAt,
            remoteSyncStatus = anomaly.remoteSyncStatus,
            remoteUploadedAt = anomaly.remoteUploadedAt,
            remoteError = anomaly.remoteError
        )
    }

    private fun evaluateAndRecordAnomaly() {
        val currentState = _uiState.value
        val lookupLocation = currentState.selectedLocation ?: return
        val soundLevelSnapshot = currentState.estimatedSoundLevelDb
        val accelerationSnapshot = currentState.accelerationMagnitude
        val soundThresholdSnapshot = currentState.soundThresholdDb
        val motionThresholdSnapshot = currentState.motionThreshold
        val motionDeviation = abs(
            accelerationSnapshot - SensorManager.GRAVITY_EARTH
        )
        val soundThresholdMet = soundLevelSnapshot > soundThresholdSnapshot
        val motionThresholdMet = motionDeviation > motionThresholdSnapshot
        val thresholdMet = DetectionTriggerEvaluator.shouldRecord(
            mode = currentState.detectionTriggerMode,
            soundThresholdExceeded = soundThresholdMet,
            motionThresholdExceeded = motionThresholdMet
        )

        _uiState.update {
            it.copy(
                motionDetected = motionThresholdMet,
                anomalyDetected = thresholdMet
            )
        }

        if (!thresholdMet) return

        val eventType = DetectionTriggerEvaluator.eventTypeFor(
            soundThresholdExceeded = soundThresholdMet,
            motionThresholdExceeded = motionThresholdMet
        ) ?: return
        val now = System.currentTimeMillis()
        val last = lastRecordedMs[eventType] ?: 0L
        if (now - last < HistoryEntry.MIN_INTERVAL_MS) return
        lastRecordedMs[eventType] = now

        _uiState.update {
            it.copy(
                showAnomalyDialog = true,
                isLoading = true,
                earthquake = null,
                detectedAnomalyType = eventType,
                detectedSoundLevelDb = soundLevelSnapshot,
                detectedAccelerationMagnitude = accelerationSnapshot,
                detectedSoundThresholdDb = soundThresholdSnapshot,
                detectedMotionThreshold = motionThresholdSnapshot,
                detectedSoundThresholdExceeded = soundThresholdMet,
                detectedMotionThresholdExceeded = motionThresholdMet,
                statusMessage = "${
                    exceededThresholdLabel(soundThresholdMet, motionThresholdMet)
                } threshold exceeded. Checking nearby earthquakes...",
                errorMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val currentLocation = resolveLookupLocation() ?: lookupLocation
                val earthquake = repository.recordAnomaly(
                    soundLevelDb = soundLevelSnapshot,
                    accelerationMagnitude = accelerationSnapshot,
                    soundThresholdDb = soundThresholdSnapshot,
                    motionThreshold = motionThresholdSnapshot,
                    soundThresholdExceeded = soundThresholdMet,
                    motionThresholdExceeded = motionThresholdMet,
                    location = currentLocation,
                    eventTimeMillis = now
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        statusMessage = if (earthquake != null) {
                            "Anomaly detected and saved locally. Nearby earthquake found."
                        } else {
                            "Anomaly detected and saved locally. No nearby earthquake found."
                        },
                        earthquake = earthquake,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Unable to save anomaly."
                    )
                }
            }
        }
    }

    private fun exceededThresholdLabel(
        soundThresholdMet: Boolean,
        motionThresholdMet: Boolean
    ): String {
        return when {
            soundThresholdMet && motionThresholdMet -> "Sound and motion"
            soundThresholdMet -> "Sound"
            motionThresholdMet -> "Motion"
            else -> "No"
        }
    }

    private fun showError(message: String) {
        _uiState.update {
            it.copy(
                isMonitoring = false,
                statusMessage = "Action unavailable.",
                errorMessage = message
            )
        }
    }

    private fun handleSensorFailure(exception: Throwable) {
        if (exception is CancellationException) return
        motionJob?.cancel()
        soundJob?.cancel()
        motionJob = null
        soundJob = null
        _uiState.update {
            it.copy(
                isMonitoring = false,
                statusMessage = "Sensor monitoring stopped.",
                errorMessage = exception.message ?: "Unable to read sensor data."
            )
        }
    }
}
