package com.bananaginger.noisedetector

// ---------------------------------------------------------------------------
// MainActivity.kt
//
// Entry point of the application.
//
// Responsibilities (kept intentionally minimal):
//   1. Create the Room database and AnomalyRepository (data layer).
//   2. Create the sensor reader instances (hardware access).
//   3. Create AnomalyViewModel via the factory (business logic).
//   4. Hand everything to AppNavHost which owns all screen navigation.
//
// The old manual if/else navigation and the AlertDialog have been moved:
//   • Screen routing  → AppNavigation.kt  (NavHost + bottom bar)
//   • Anomaly dialog  → HomeScreen.kt     (shown on top of the monitor screen)
// ---------------------------------------------------------------------------

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bananaginger.noisedetector.data.AppDatabase
import com.bananaginger.noisedetector.data.remote.EarthquakeRemoteDataSource
import com.bananaginger.noisedetector.data.remote.RetrofitProvider
import com.bananaginger.noisedetector.data.repository.AnomalyRepository
import com.bananaginger.noisedetector.data.sensor.AndroidMotionSensorReader
import com.bananaginger.noisedetector.data.sensor.AndroidSoundSensorReader
import com.bananaginger.noisedetector.data.sensor.MotionSensorReader
import com.bananaginger.noisedetector.data.sensor.SoundSensorReader
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import com.bananaginger.noisedetector.ui.AnomalyViewModelFactory
import com.bananaginger.noisedetector.ui.navigation.AppNavHost
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    // ---- Data layer --------------------------------------------------------
    // AppDatabase is a Room singleton.  We use `by lazy` so it is only
    // created the first time it is accessed, not at Activity creation time.
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    private val repository: AnomalyRepository by lazy {
        AnomalyRepository(
            database.anomalyDao(),
            EarthquakeRemoteDataSource(RetrofitProvider.earthquakeApi)
        )
    }

    // ---- Sensor readers ----------------------------------------------------
    // These wrap the Android hardware APIs so the ViewModel never touches
    // Android sensor classes directly (easier to test with fakes).
    private val motionSensorReader: MotionSensorReader by lazy {
        AndroidMotionSensorReader(applicationContext)
    }

    private val soundSensorReader: SoundSensorReader by lazy {
        AndroidSoundSensorReader(applicationContext)
    }

    // ---- ViewModel ---------------------------------------------------------
    // `by viewModels` keeps the ViewModel alive across screen rotations.
    // AnomalyViewModelFactory injects the dependencies listed above.
    private val anomalyViewModel: AnomalyViewModel by viewModels {
        AnomalyViewModelFactory(
            repository = repository,
            motionSensorReader = motionSensorReader,
            soundSensorReader = soundSensorReader
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw content edge-to-edge so the app looks correct on modern devices.
        enableEdgeToEdge()

        setContent {
            NoiseAndMotionAnomalyDetectorTheme {

                // AppNavHost owns the Scaffold, the bottom navigation bar, and
                // all four screen composables.  This Activity only needs to
                // provide the shared ViewModel.
                AppNavHost(viewModel = anomalyViewModel)

                // ---- Anomaly alert dialog ----------------------------------
                // Shown on top of whatever screen is currently visible whenever
                // the ViewModel detects both thresholds are crossed at once.
                val uiState by anomalyViewModel.uiState.collectAsState()
                if (uiState.showAnomalyDialog) {
                    AlertDialog(
                        onDismissRequest = anomalyViewModel::dismissAnomalyDialog,
                        title = { Text("Anomaly Detected") },
                        text = {
                            Text(
                                text = "Sound: ${
                                    String.format(Locale.US, "%.1f", uiState.estimatedSoundLevelDb)
                                } dB  ·  Accel: ${
                                    String.format(Locale.US, "%.1f", uiState.accelerationMagnitude)
                                } m/s²\n\nBoth thresholds exceeded at the same time."
                            )
                        },
                        confirmButton = {
                            Button(onClick = anomalyViewModel::dismissAnomalyDialog) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}
