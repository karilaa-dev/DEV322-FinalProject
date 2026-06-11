package com.bananaginger.noisedetector

/**
 * MainActivity — entry point Activity for the app.
 *
 * Responsibilities:
 * - Apply the app theme
 * - Set the root Compose content
 * - Wire the anomaly Repository/ViewModel for the earthquake API integration
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.bananaginger.noisedetector.data.AppDatabase
import com.bananaginger.noisedetector.data.remote.EarthquakeRemoteDataSource
import com.bananaginger.noisedetector.data.remote.RetrofitProvider
import com.bananaginger.noisedetector.data.repository.AnomalyRepository
import com.bananaginger.noisedetector.ui.AnomalyScreen
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import com.bananaginger.noisedetector.ui.AnomalyViewModelFactory
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme

class MainActivity : ComponentActivity() {
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    private val repository: AnomalyRepository by lazy {
        val remoteDataSource = EarthquakeRemoteDataSource(RetrofitProvider.earthquakeApi)
        AnomalyRepository(database.anomalyDao(), remoteDataSource)
    }

    private val anomalyViewModel: AnomalyViewModel by viewModels {
        AnomalyViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge rendering so content may draw behind system bars.
        enableEdgeToEdge()
        // Set the Compose UI content and apply the app theme.
        setContent {
            NoiseAndMotionAnomalyDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AnomalyScreen(
                        viewModel = anomalyViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
