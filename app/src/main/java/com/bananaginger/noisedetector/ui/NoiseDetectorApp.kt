package com.bananaginger.noisedetector.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bananaginger.noisedetector.data.model.LocationSnapshot
import com.bananaginger.noisedetector.data.remote.RemoteDataFilter
import com.bananaginger.noisedetector.data.remote.RemoteDataKind
import com.bananaginger.noisedetector.history.AnomalyHistoryScreen
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoiseDetectorApp(
    viewModel: AnomalyViewModel,
    openLocalHistoryOnStart: Boolean,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val microphonePermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                viewModel.startMonitoring()
            } else {
                viewModel.microphonePermissionDenied()
            }
        }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            if (result.values.any { granted -> granted }) {
                viewModel.useRealLocation()
            } else {
                viewModel.locationPermissionDenied()
            }
        }

    NoiseDetectorAppContent(
        uiState = uiState,
        openLocalHistoryOnStart = openLocalHistoryOnStart,
        onTestEarthquakeApi = viewModel::testEarthquakeApi,
        onStartMonitoring = {
            val permissionGranted =
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

            if (permissionGranted) {
                viewModel.startMonitoring()
            } else {
                microphonePermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        },
        onStopMonitoring = viewModel::stopMonitoring,
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
                viewModel.useRealLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onSetManualLocation = viewModel::setManualLocation,
        onSoundThresholdChanged = viewModel::updateSoundThreshold,
        onMotionThresholdChanged = viewModel::updateMotionThreshold,
        onUploadHistory = viewModel::uploadHistory,
        onRemoteKindChanged = viewModel::updateRemoteKind,
        onRemoteFilterChanged = viewModel::updateRemoteFilter,
        onRefreshRemoteData = viewModel::loadRemoteData,
        onDismissAnomalyDialog = viewModel::dismissAnomalyDialog,
        modifier = modifier
    )
}

@Composable
fun NoiseDetectorAppContent(
    uiState: AnomalyUiState,
    openLocalHistoryOnStart: Boolean = false,
    onTestEarthquakeApi: () -> Unit = {},
    onStartMonitoring: () -> Unit = {},
    onStopMonitoring: () -> Unit = {},
    onUsePhoneLocation: () -> Unit = {},
    onSetManualLocation: (LocationSnapshot) -> Unit = {},
    onSoundThresholdChanged: (Double) -> Unit = {},
    onMotionThresholdChanged: (Float) -> Unit = {},
    onUploadHistory: () -> Unit = {},
    onRemoteKindChanged: (RemoteDataKind) -> Unit = {},
    onRemoteFilterChanged: (RemoteDataFilter) -> Unit = {},
    onRefreshRemoteData: () -> Unit = {},
    onDismissAnomalyDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TopLevelDestination.entries.any { it.route == currentRoute }
    var handledHistoryIntent by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(openLocalHistoryOnStart) {
        if (openLocalHistoryOnStart && !handledHistoryIntent) {
            handledHistoryIntent = true
            navController.navigateTopLevel(TopLevelDestination.LocalHistory.route)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NoiseBottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigateTopLevel(destination.route)
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Anomalies.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TopLevelDestination.Anomalies.route) {
                AnomalyScreen(
                    uiState = uiState,
                    onTestEarthquakeApi = onTestEarthquakeApi,
                    onStartMonitoring = onStartMonitoring,
                    onStopMonitoring = onStopMonitoring,
                    onOpenSettings = {
                        navController.navigate(AppDestination.Settings.route)
                    }
                )
            }

            composable(TopLevelDestination.LocalHistory.route) {
                AnomalyHistoryScreen(
                    entries = uiState.historyEntries,
                    onUploadHistory = onUploadHistory,
                    isUploadingHistory = uiState.isUploadingHistory,
                    uploadStatusMessage = uiState.uploadStatusMessage
                )
            }

            composable(TopLevelDestination.RemoteHistory.route) {
                LaunchedEffect(Unit) {
                    onRefreshRemoteData()
                }

                RemoteDataScreen(
                    uiState = uiState,
                    onKindChanged = onRemoteKindChanged,
                    onFilterChanged = onRemoteFilterChanged,
                    onRefresh = onRefreshRemoteData
                )
            }

            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    uiState = uiState,
                    onUsePhoneLocation = onUsePhoneLocation,
                    onPickOnMap = {
                        navController.navigate(AppDestination.MapPicker.route)
                    },
                    onSoundThresholdChanged = onSoundThresholdChanged,
                    onMotionThresholdChanged = onMotionThresholdChanged,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(AppDestination.MapPicker.route) {
                MapLocationPickerScreen(
                    initialLocation = uiState.selectedLocation,
                    onConfirm = { location ->
                        onSetManualLocation(location)
                        navController.popBackStack(
                            route = AppDestination.Settings.route,
                            inclusive = false
                        )
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    if (uiState.showAnomalyDialog) {
        AnomalyDetectedDialog(
            uiState = uiState,
            onDismiss = onDismissAnomalyDialog
        )
    }
}

@Composable
private fun NoiseBottomNavigationBar(
    currentRoute: String?,
    onNavigate: (TopLevelDestination) -> Unit
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onNavigate(destination) },
                modifier = Modifier.testTag(destination.testTag),
                icon = {
                    Icon(
                        imageVector = destination.icon(),
                        contentDescription = null
                    )
                },
                label = {
                    Text(destination.label)
                }
            )
        }
    }
}

@Composable
private fun AnomalyDetectedDialog(
    uiState: AnomalyUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                onClick = onDismiss
            ) {
                Text("OK")
            }
        }
    )
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private enum class TopLevelDestination(
    val route: String,
    val label: String,
    val testTag: String
) {
    Anomalies("anomalies", "Anomalies", "nav_anomalies"),
    LocalHistory("local_history", "Local History", "nav_local_history"),
    RemoteHistory("remote_history", "Remote History", "nav_remote_history");

    fun icon(): ImageVector {
        return when (this) {
            TopLevelDestination.Anomalies -> Icons.Filled.Warning
            TopLevelDestination.LocalHistory -> Icons.Filled.History
            TopLevelDestination.RemoteHistory -> Icons.Filled.CloudQueue
        }
    }
}

private enum class AppDestination(
    val route: String
) {
    Settings("settings"),
    MapPicker("map_picker")
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
