package com.bananaginger.noisedetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.bananaginger.noisedetector.data.local.AppDatabase
import com.bananaginger.noisedetector.data.remote.EarthquakeRemoteDataSource
import com.bananaginger.noisedetector.data.remote.RetrofitProvider
import com.bananaginger.noisedetector.data.repository.AnomalyRepository
import com.bananaginger.noisedetector.ui.AnomalyScreen
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import com.bananaginger.noisedetector.ui.AnomalyViewModelFactory
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme

class MainActivity : ComponentActivity() {
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "anomaly_database"
        ).build()
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
        enableEdgeToEdge()
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
