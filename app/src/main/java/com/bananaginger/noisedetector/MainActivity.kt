package com.bananaginger.noisedetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.bananaginger.noisedetector.data.AppDatabase
import com.bananaginger.noisedetector.data.InstallIdProvider
import com.bananaginger.noisedetector.data.location.AndroidLocationProvider
import com.bananaginger.noisedetector.data.location.LocationProvider
import com.bananaginger.noisedetector.data.location.LocationSelectionStore
import com.bananaginger.noisedetector.data.remote.AtlasConfig
import com.bananaginger.noisedetector.data.remote.AtlasRemoteDataSource
import com.bananaginger.noisedetector.data.remote.EarthquakeRemoteDataSource
import com.bananaginger.noisedetector.data.remote.RetrofitProvider
import com.bananaginger.noisedetector.data.repository.AnomalyRepository
import com.bananaginger.noisedetector.data.sensor.AndroidMotionSensorReader
import com.bananaginger.noisedetector.data.sensor.AndroidSoundSensorReader
import com.bananaginger.noisedetector.data.sensor.MotionSensorReader
import com.bananaginger.noisedetector.data.sensor.SoundSensorReader
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import com.bananaginger.noisedetector.ui.AnomalyViewModelFactory
import com.bananaginger.noisedetector.ui.NoiseDetectorApp
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme

class MainActivity : ComponentActivity() {
    private val database: AppDatabase by lazy {
        AppDatabase.getInstance(applicationContext)
    }

    private val installId: String by lazy {
        InstallIdProvider(applicationContext).getInstallId()
    }

    private val repository: AnomalyRepository by lazy {
        val atlasConfig = AtlasConfig.fromBuildConfig()
        val remoteHistoryDataSource = AtlasRemoteDataSource(
            config = atlasConfig
        )

        AnomalyRepository(
            anomalyDao = database.anomalyDao(),
            earthquakeDao = database.earthquakeDao(),
            earthquakeDataSource = EarthquakeRemoteDataSource(
                RetrofitProvider.earthquakeApi
            ),
            remoteHistoryDataSource = remoteHistoryDataSource,
            installId = installId
        )
    }

    private val motionSensorReader: MotionSensorReader by lazy {
        AndroidMotionSensorReader(applicationContext)
    }

    private val soundSensorReader: SoundSensorReader by lazy {
        AndroidSoundSensorReader(applicationContext)
    }

    private val locationProvider: LocationProvider by lazy {
        AndroidLocationProvider(applicationContext)
    }

    private val locationSelectionStore: LocationSelectionStore by lazy {
        LocationSelectionStore(applicationContext)
    }

    private val anomalyViewModel: AnomalyViewModel by viewModels {
        AnomalyViewModelFactory(
            repository = repository,
            motionSensorReader = motionSensorReader,
            soundSensorReader = soundSensorReader,
            locationProvider = locationProvider,
            locationSelectionStore = locationSelectionStore
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val openLocalHistoryOnStart =
            intent?.getBooleanExtra("showHistory", false) == true

        setContent {
            NoiseAndMotionAnomalyDetectorTheme {
                NoiseDetectorApp(
                    viewModel = anomalyViewModel,
                    openLocalHistoryOnStart = openLocalHistoryOnStart
                )
            }
        }
    }
}
