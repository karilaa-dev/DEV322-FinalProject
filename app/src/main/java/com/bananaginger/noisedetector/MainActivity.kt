package com.bananaginger.noisedetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import com.bananaginger.noisedetector.history.AnomalyHistoryScreen
import com.bananaginger.noisedetector.ui.AnomalyScreen
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import com.bananaginger.noisedetector.ui.AnomalyViewModelFactory
import com.bananaginger.noisedetector.ui.LocationChoiceScreen
import com.bananaginger.noisedetector.ui.MapLocationPickerScreen
import com.bananaginger.noisedetector.ui.RemoteDataScreen
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme
import java.text.DateFormat
import java.util.Date
import java.util.Locale

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

        if (intent?.getBooleanExtra("showHistory", false) == true) {
            anomalyViewModel.viewHistory()
        }

        setContent {
            NoiseAndMotionAnomalyDetectorTheme {
                val uiState by anomalyViewModel.uiState.collectAsState()
                val context = LocalContext.current
                val locationPermissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestMultiplePermissions()
                    ) { result ->
                        if (result.values.any { granted -> granted }) {
                            anomalyViewModel.useRealLocation()
                        } else {
                            anomalyViewModel.locationPermissionDenied()
                        }
                    }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when {
                        uiState.showMapPicker -> {
                            MapLocationPickerScreen(
                                initialLocation = uiState.selectedLocation,
                                onConfirm = anomalyViewModel::setManualLocation,
                                onCancel = anomalyViewModel::cancelMapLocationChoice,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        uiState.locationChoiceRequired -> {
                            LocationChoiceScreen(
                                uiState = uiState,
                                onUsePhoneLocation = {
                                    val fineGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED
                                    val coarseGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (fineGranted || coarseGranted) {
                                        anomalyViewModel.useRealLocation()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                onPickOnMap = anomalyViewModel::chooseLocationOnMap,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        uiState.showRemoteData -> {
                            RemoteDataScreen(
                                uiState = uiState,
                                onKindChanged = anomalyViewModel::updateRemoteKind,
                                onFilterChanged = anomalyViewModel::updateRemoteFilter,
                                onRefresh = anomalyViewModel::loadRemoteData,
                                onBack = anomalyViewModel::hideRemoteData,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        uiState.showHistory -> {
                            AnomalyHistoryScreen(
                                entries = uiState.historyEntries,
                                onBack = anomalyViewModel::hideHistory,
                                onUploadHistory = anomalyViewModel::uploadHistory,
                                onViewRemoteData = anomalyViewModel::viewRemoteData,
                                isUploadingHistory = uiState.isUploadingHistory,
                                uploadStatusMessage = uiState.uploadStatusMessage,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }

                        else -> {
                            AnomalyScreen(
                                viewModel = anomalyViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }

                if (uiState.showAnomalyDialog) {
                    AlertDialog(
                        onDismissRequest = anomalyViewModel::dismissAnomalyDialog,
                        title = { Text("Anomaly Detected") },
                        text = {
                            Column {
                                Text("Loud sound and movement were detected.")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Sound level: ${
                                        String.format(
                                            Locale.US,
                                            "%.1f",
                                            uiState.estimatedSoundLevelDb
                                        )
                                    } dB"
                                )
                                Text(
                                    text = "Acceleration: ${
                                        String.format(
                                            Locale.US,
                                            "%.1f",
                                            uiState.accelerationMagnitude
                                        )
                                    } m/s²"
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text(uiState.statusMessage)

                                if (uiState.isLoading) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Checking nearby earthquakes...")
                                }

                                uiState.earthquake?.let { earthquake ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Closest earthquake:")
                                    Text(
                                        text = "M${earthquake.magnitude.formatNullable()} " +
                                                earthquake.place
                                    )
                                    Text(
                                        text = "Depth ${
                                            earthquake.depthKm.formatNullable()
                                        } km"
                                    )
                                    Text(
                                        text = "Time ${
                                            earthquake.timeMillis.toDisplayTime()
                                        }"
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = anomalyViewModel::dismissAnomalyDialog
                            ) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun Double?.formatNullable(): String {
    return this?.let {
        String.format(Locale.US, "%.1f", it)
    } ?: "unknown"
}

private fun Long?.toDisplayTime(): String {
    if (this == null) return "unknown"
    return DateFormat.getDateTimeInstance(
        DateFormat.SHORT,
        DateFormat.SHORT,
        Locale.US
    ).format(Date(this))
}
