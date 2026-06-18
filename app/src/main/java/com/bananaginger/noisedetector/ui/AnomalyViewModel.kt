package com.bananaginger.noisedetector.ui

import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bananaginger.noisedetector.data.AnomalyEntity
import com.bananaginger.noisedetector.data.model.EarthquakeSummary
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.repository.AnomalyRepository
import com.bananaginger.noisedetector.data.sensor.MotionSensorReader
import com.bananaginger.noisedetector.data.sensor.SoundSensorReader
import com.bananaginger.noisedetector.history.AnomalyServerSync
import com.bananaginger.noisedetector.history.HistoryEntry
import com.bananaginger.noisedetector.history.NoOpServerSync
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

/**
 * AnomalyViewModel — the central ViewModel for the app.
 *
 * Responsibilities:
 *   1. Start/stop sensor monitoring (microphone + accelerometer).
 *   2. Trigger earthquake API lookups via the repository.
 *   3. Record anomaly events to the in-memory history list.
 *      - History survives rotation and going to background.
 *      - History is cleared when the process is killed (app fully closed).
 *   4. Upload each new history entry to the remote server via [AnomalyServerSync].
 *      - Currently a no-op stub; replace when API endpoint is provided.
 *   5. Expose a single [AnomalyUiState] StateFlow observed by all Compose screens.
 *
 * BananaGinger/Kyryl: ViewModel owns coroutine scope and exposes StateFlow to Compose.
 */
// BananaGinger/Kyryl: ViewModel owns coroutine scope and exposes StateFlow to Compose.
class AnomalyViewModel(
    private val repository: AnomalyRepository,
    private val motionSensorReader: MotionSensorReader,
    private val soundSensorReader: SoundSensorReader,
    // Server sync is injected so it can be swapped for a real implementation later.
    // Default = NoOpServerSync (does nothing) until an API endpoint is provided.
    private val serverSync: AnomalyServerSync = NoOpServerSync
) : ViewModel() {
    private val _uiState = MutableStateFlow(AnomalyUiState())
    val uiState: StateFlow<AnomalyUiState> = _uiState.asStateFlow()

    private var motionJob: Job? = null
    private var soundJob: Job? = null

    /**
     * Tracks the last time (epoch ms) each anomaly type was recorded.
     * Used for debounce: we skip recording if MIN_INTERVAL_MS has not elapsed.
     * Key = one of the HistoryEntry.TYPE_* constants.
     */
    private val lastRecordedMs = mutableMapOf<String, Long>()

    // Date/time formatters for building human-readable HistoryEntry fields.
    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFmt = SimpleDateFormat("HH:mm:ss",   Locale.US)
    private val dayFmt  = SimpleDateFormat("EEEE",       Locale.US)

    // dylan anomaly history
    init {
        observeAnomalyHistory()
    }

    /**
     * Sends a test earthquake lookup for the hard-coded demo location.
     * If a nearby earthquake is found it is also saved to history and uploaded.
     */
    fun testEarthquakeApi() {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                statusMessage = "Checking nearby earthquakes...",
                earthquake = null,
                errorMessage = null
            )}

            try {
                val earthquake = repository.testNearbyEarthquakeLookup(
                    location = DEMO_LOCATION,
                    eventTimeMillis = System.currentTimeMillis()
                )

                // If an earthquake was found, record it in the history.
                earthquake?.let { eq ->
                    maybeRecord(
                        type = HistoryEntry.TYPE_EARTHQUAKE,
                        soundLevelDb = _uiState.value.estimatedSoundLevelDb,
                        accelerationMagnitude = _uiState.value.accelerationMagnitude,
                        motionDetected = _uiState.value.motionDetected,
                        earthquakeSummary = eq
                    )
                }

                val statusMessage = if (earthquake != null)
                    "Earthquake API test complete. Nearby event found."
                else
                    "Earthquake API test complete. No nearby event found."

                _uiState.update { it.copy(
                    isLoading = false,
                    statusMessage = statusMessage,
                    earthquake = earthquake,
                    errorMessage = null
                )}
            } catch (exception: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    statusMessage = "Earthquake API test failed.",
                    earthquake = null,
                    errorMessage = exception.message ?: "Unknown error"
                )}
            }
        }
    }
    /**
     * Starts continuous monitoring of the microphone and accelerometer.
     *
     * For every sensor reading:
     *   - The UI is updated immediately (StateFlow emission).
     *   - If the reading exceeds the recording threshold AND the debounce
     *     interval has passed, a new HistoryEntry is created and uploaded.
     *
     * Recording thresholds (deliberately low so history fills up quickly):
     *   Sound  : > 30.0 dB  (HistoryEntry.SOUND_THRESHOLD_DB)
     *   Motion : deviation from gravity > 0.5 m/s²  (HistoryEntry.MOTION_THRESHOLD)
     *
     * The merged WITH POPUP detector uses its original combined rule instead:
     * sound > 50.0 dB AND motion deviation > 1.5 m/s².
     */
    fun startMonitoring() {
        // Guard: do not start a second pair of jobs if already running.
        if (motionJob?.isActive == true || soundJob?.isActive == true) return

        if (!motionSensorReader.isAvailable) { showError("Accelerometer is unavailable."); return }
        if (!soundSensorReader.isAvailable)  { showError("Microphone is unavailable.");    return }

        _uiState.update { it.copy(
            isMonitoring = true,
            showHistory = false,
            statusMessage = "Monitoring started. Listening for sound and motion.",
            errorMessage = null
        )}

        // --- Motion sensor coroutine ---
        motionJob = viewModelScope.launch {
            motionSensorReader.readings()
                .catch { exception -> handleSensorFailure(exception) }
                .collect { reading ->
                    // How much the device accelerates beyond resting gravity.
                    val deviationFromGravity = abs(
                        reading.accelerationMagnitude - SensorManager.GRAVITY_EARTH
                    )
                    val motionDetected = deviationFromGravity > MOTION_DISPLAY_THRESHOLD

                    // Update UI with latest sensor values.
                    _uiState.update { it.copy(
                        accelerationMagnitude = reading.accelerationMagnitude,
                        motionDetected = motionDetected
                    )}

                    // Record to history when the (low) threshold is exceeded.

                    evaluateAndRecordAnomaly()
                }
        }

        // --- Sound sensor coroutine ---
        soundJob = viewModelScope.launch {
            soundSensorReader.readings()
                .catch { exception -> handleSensorFailure(exception) }
                .collect { reading ->
                    // Update UI with latest sound level.
                    _uiState.update { it.copy(
                        estimatedSoundLevelDb = reading.estimatedSoundLevelDb
                    )}

                    // Record to history when the (low) threshold is exceeded.

                    evaluateAndRecordAnomaly()
                }
        }
    }

    /** Cancels both sensor jobs and resets all sensor readings to zero. */
    fun stopMonitoring() {
        motionJob?.cancel()
        soundJob?.cancel()
        motionJob = null
        soundJob = null

        _uiState.update { it.copy(
            isMonitoring = false,
            estimatedSoundLevelDb = 0.0,
            accelerationMagnitude = 0.0f,
            motionDetected = false,
            anomalyDetected = false,
            statusMessage = "Monitoring stopped.",
            errorMessage = null
        )}
    }

    /** Called by the UI when the user denies the microphone permission. */
    fun microphonePermissionDenied() {
        showError("Microphone permission is required to start monitoring.")
    }

    /** Shows the history screen by setting the showHistory flag to true. */
    fun viewHistory() {
        _uiState.update { it.copy(
            showHistory = true,
            statusMessage = "Showing anomaly history.",
            errorMessage = null
        )}
    }

    /** Returns to the main screen by clearing the showHistory flag. */
    fun hideHistory() {
        _uiState.update { it.copy(
            showHistory = false,
            statusMessage = "",
            errorMessage = null
        )}
    }

    private fun observeAnomalyHistory() {
        viewModelScope.launch {
            repository.observeAnomalies()
                .catch { exception ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            errorMessage = exception.message
                                ?: "Unable to load anomaly history."
                        )
                    }
                }
                .collect { anomalies ->
                    val savedEntries = anomalies.map { anomaly ->
                        anomaly.toHistoryEntry()
                    }

                    _uiState.update { currentState ->
                        val entriesNotYetInRoom =
                            currentState.historyEntries.filter { currentEntry ->
                                savedEntries.none { savedEntry ->
                                    savedEntry.id == currentEntry.id
                                }
                            }

                        currentState.copy(
                            historyEntries =
                                (savedEntries + entriesNotYetInRoom)
                                    .distinctBy { entry -> entry.id }
                                    .sortedByDescending { entry -> entry.timestamp }
                        )
                    }
                }
        }
    }

    private fun AnomalyEntity.toHistoryEntry(): HistoryEntry {
        val detectedAt = Date(timestamp)
        val normalizedType = when (type.uppercase(Locale.US)) {
            "SOUND_AND_MOTION" -> HistoryEntry.TYPE_SOUND_AND_MOTION
            "SOUND" -> HistoryEntry.TYPE_SOUND
            "NOISE" -> HistoryEntry.TYPE_SOUND
            "MOTION" -> HistoryEntry.TYPE_MOTION
            "EARTHQUAKE" -> HistoryEntry.TYPE_EARTHQUAKE
            else -> type.uppercase(Locale.US)
        }

        val savedAcceleration = metadata
            ?.split(';')
            ?.firstOrNull { value ->
                value.startsWith("accelerationMagnitude=")
            }
            ?.substringAfter("accelerationMagnitude=")
            ?.toFloatOrNull()

        return HistoryEntry(
            id = timestamp,
            timestamp = timestamp,
            date = date ?: dateFmt.format(detectedAt),
            time = timeFmt.format(detectedAt),
            day = day ?: dayFmt.format(detectedAt),
            type = normalizedType,
            soundLevelDb = when (normalizedType) {
                HistoryEntry.TYPE_SOUND,
                HistoryEntry.TYPE_SOUND_AND_MOTION -> magnitude
                else -> null
            },
            accelerationMagnitude = when (normalizedType) {
                HistoryEntry.TYPE_MOTION,
                HistoryEntry.TYPE_SOUND_AND_MOTION -> savedAcceleration
                else -> null
            },
            motionDetected = normalizedType == HistoryEntry.TYPE_MOTION ||
                    normalizedType == HistoryEntry.TYPE_SOUND_AND_MOTION,
            severity = severity ?: 1,
            description = description ?: "$normalizedType anomaly detected"
        )
    }

    fun dismissAnomalyDialog() {
        _uiState.update { currentState ->
            currentState.copy(
                showAnomalyDialog = false
            )
        }
    }

    // dylan calculate whether something is an anomaly
    private fun evaluateAndRecordAnomaly() {
        val currentState = _uiState.value

        val thresholdMet =
            currentState.estimatedSoundLevelDb > SOUND_THRESHOLD_DB &&
                    currentState.motionDetected

        _uiState.update { state ->
            state.copy(
                anomalyDetected = thresholdMet
            )
        }

        if (!thresholdMet) {
            return
        }

        val recordedEntry = maybeRecord(
            type = HistoryEntry.TYPE_SOUND_AND_MOTION,
            soundLevelDb = currentState.estimatedSoundLevelDb,
            accelerationMagnitude = currentState.accelerationMagnitude,
            motionDetected = currentState.motionDetected
        ) ?: return

        _uiState.update { state ->
            state.copy(
                showAnomalyDialog = true,
                statusMessage = "Anomaly detected."
            )
        }

        viewModelScope.launch {
            try {
                val earthquake = repository.recordAnomaly(
                    soundLevelDb = recordedEntry.soundLevelDb ?: 0.0,
                    accelerationMagnitude =
                        recordedEntry.accelerationMagnitude ?: 0.0f,
                    location = DEMO_LOCATION,
                    eventTimeMillis = recordedEntry.timestamp
                )

                _uiState.update { state ->
                    state.copy(
                        statusMessage = "Anomaly detected and saved.",
                        earthquake = earthquake,
                        errorMessage = null
                    )
                }
            } catch (exception: Exception) {
                _uiState.update { state ->
                    state.copy(
                        errorMessage = exception.message
                            ?: "Unable to save anomaly."
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Tries to record a new [HistoryEntry] for the given [type].
     *
     * Debounce: if the same type was recorded less than [HistoryEntry.MIN_INTERVAL_MS]
     * milliseconds ago, the call is ignored.
     *
     * After recording, the entry is uploaded to the server via [serverSync]
     * inside a fire-and-forget coroutine. Upload errors are silently swallowed
     * until a real API endpoint is provided.
     *
     * @param type                  One of HistoryEntry.TYPE_* constants.
     * @param soundLevelDb          Current sound level in dB.
     * @param accelerationMagnitude Current accelerometer magnitude in m/s².
     * @param motionDetected        Whether the display threshold was exceeded.
     * @param earthquakeSummary     Populated only for TYPE_EARTHQUAKE entries.
     */
    private fun maybeRecord(
        type: String,
        soundLevelDb: Double,
        accelerationMagnitude: Float,
        motionDetected: Boolean,
        earthquakeSummary: EarthquakeSummary? = null
    ): HistoryEntry? {
        val now  = System.currentTimeMillis()
        val last = lastRecordedMs[type] ?: 0L

        // Debounce: skip if we recorded this type too recently.
        if (now - last < HistoryEntry.MIN_INTERVAL_MS) return null
        lastRecordedMs[type] = now

        val date = Date(now)
        val entry = HistoryEntry(
            id        = now,
            timestamp = now,
            date      = dateFmt.format(date),
            time      = timeFmt.format(date),
            day       = dayFmt.format(date),
            type      = type,
            // Hide sensor fields for earthquake entries to avoid misleading data.
            soundLevelDb          = if (type == HistoryEntry.TYPE_EARTHQUAKE) null else soundLevelDb,
            accelerationMagnitude = if (type == HistoryEntry.TYPE_EARTHQUAKE) null else accelerationMagnitude,
            motionDetected        = motionDetected,
            severity    = computeSeverity(type, soundLevelDb, accelerationMagnitude, earthquakeSummary?.magnitude),
            description = buildDescription(type, soundLevelDb, accelerationMagnitude, earthquakeSummary),
            // Earthquake-specific fields (null for SOUND / MOTION entries).
            earthquakeMagnitude = earthquakeSummary?.magnitude,
            earthquakePlace     = earthquakeSummary?.place,
            latitude            = earthquakeSummary?.latitude,
            longitude           = earthquakeSummary?.longitude,
            depthKm             = earthquakeSummary?.depthKm
        )

        // Prepend so the newest entry appears at the top of the history list.
        _uiState.update { current ->
            current.copy(historyEntries = listOf(entry) + current.historyEntries)
        }

        // Fire-and-forget upload — does not block or delay the UI.
        viewModelScope.launch {
            try {
                serverSync.uploadEntry(entry)
            } catch (e: Exception) {
                // Upload failed silently. History remains saved in memory.
                // TODO: add a retry queue when a real server endpoint is provided.
            }
        }

        return entry
    }

    /**
     * Computes a severity score from 1 (low) to 5 (extreme).
     *
     * Thresholds by type:
     *   SOUND      : 1=30–50 dB    2=50–65    3=65–80    4=80–100   5=>100
     *   MOTION     : 1=0.5–1 m/s²  2=1–2      3=2–4      4=4–8      5=>8
     *   EARTHQUAKE : 1=<M2         2=M2–3     3=M3–4.5   4=M4.5–6   5=M≥6
     */
    private fun computeSeverity(
        type: String,
        soundDb: Double,
        accel: Float,
        magnitude: Double?
    ): Int = when (type) {
        HistoryEntry.TYPE_SOUND -> when {
            soundDb > 100 -> 5
            soundDb > 80  -> 4
            soundDb > 65  -> 3
            soundDb > 50  -> 2
            else          -> 1
        }
        HistoryEntry.TYPE_MOTION -> {
            val dev = abs(accel - SensorManager.GRAVITY_EARTH)
            when {
                dev > 8 -> 5
                dev > 4 -> 4
                dev > 2 -> 3
                dev > 1 -> 2
                else    -> 1
            }
        }
        HistoryEntry.TYPE_SOUND_AND_MOTION -> {
            val soundSeverity = when {
                soundDb > 100 -> 5
                soundDb > 80  -> 4
                soundDb > 65  -> 3
                soundDb > 50  -> 2
                else          -> 1
            }
            val dev = abs(accel - SensorManager.GRAVITY_EARTH)
            val motionSeverity = when {
                dev > 8 -> 5
                dev > 4 -> 4
                dev > 2 -> 3
                dev > 1 -> 2
                else    -> 1
            }
            maxOf(soundSeverity, motionSeverity)
        }
        HistoryEntry.TYPE_EARTHQUAKE -> when {
            (magnitude ?: 0.0) >= 6.0 -> 5
            (magnitude ?: 0.0) >= 4.5 -> 4
            (magnitude ?: 0.0) >= 3.0 -> 3
            (magnitude ?: 0.0) >= 2.0 -> 2
            else                      -> 1
        }
        else -> 1
    }

    /**
     * Builds a short one-line description shown on each history card.
     * Examples:
     *   "Sound spike 72.3 dB"
     *   "Motion detected 4.2 m/s²"
     *   "M3.1 — 10 km NW of Seattle"
     */
    private fun buildDescription(
        type: String,
        soundDb: Double,
        accel: Float,
        earthquake: EarthquakeSummary?
    ): String = when (type) {
        HistoryEntry.TYPE_SOUND      -> "Sound spike %.1f dB".format(soundDb)
        HistoryEntry.TYPE_MOTION     -> "Motion detected %.1f m/s\u00B2".format(accel)
        HistoryEntry.TYPE_SOUND_AND_MOTION ->
            "Loud sound %.1f dB and movement %.1f m/s\u00B2 detected".format(
                soundDb,
                accel
            )
        HistoryEntry.TYPE_EARTHQUAKE ->
            if (earthquake != null)
                "M%.1f \u2014 %s".format(earthquake.magnitude ?: 0.0, earthquake.place)
            else "Earthquake detected"
        else -> type
    }

    /** Sets an error message and stops monitoring. */
    private fun showError(message: String) {
        _uiState.update { it.copy(
            isMonitoring = false,
            statusMessage = "Unable to start monitoring.",
            errorMessage = message
        )}
    }

    /** Handles unexpected sensor stream failures and cancels both jobs. */
    private fun handleSensorFailure(exception: Throwable) {
        if (exception is CancellationException) return
        motionJob?.cancel()
        soundJob?.cancel()
        motionJob = null
        soundJob = null
        _uiState.update { it.copy(
            isMonitoring = false,
            statusMessage = "Sensor monitoring stopped.",
            errorMessage = exception.message ?: "Unable to read sensor data."
        )}
    }

    companion object {
        /**
         * Threshold for the UI "Motion detected" label.
         * This is higher than HistoryEntry.MOTION_THRESHOLD which controls recording.
         */
        private const val MOTION_DISPLAY_THRESHOLD = 1.5f

        // dylan anomaly threshholds
        // THIS IS WHAT IS SHOULD BE
        private const val SOUND_THRESHOLD_DB = 50.0

        // ZERO FOR TESTING PURPOSE
        //private const val SOUND_THRESHOLD_DB = 00.0

        /** Demo location used for earthquake API tests (Bellevue, WA). */
        val DEMO_LOCATION = LocationSnapshot(
            latitude  = 47.6101,
            longitude = -122.2015
        )
    }
}
