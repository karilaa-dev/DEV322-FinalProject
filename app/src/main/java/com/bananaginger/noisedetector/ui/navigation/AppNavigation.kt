package com.bananaginger.noisedetector.ui.navigation

// ---------------------------------------------------------------------------
// AppNavigation.kt
//
// This file defines:
//   1. Screen — a sealed class that lists every destination in the app.
//      Each object has a `route` String used by NavHost to identify screens,
//      a `label` shown in the bottom bar, and an `icon` drawn in the tab.
//
//   2. AppBottomNavBar — the reusable bottom navigation bar composable.
//      It reads the current back-stack entry so the correct tab stays
//      highlighted as the user navigates.
//
//   3. AppNavHost — wires everything together: NavController + NavHost +
//      Scaffold with the bottom bar.  MainActivity simply calls AppNavHost
//      and the rest is handled here.
// ---------------------------------------------------------------------------

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bananaginger.noisedetector.ui.AnomalyViewModel
import com.bananaginger.noisedetector.ui.screens.EarthquakeApiScreen
import com.bananaginger.noisedetector.ui.screens.HistoryScreen
import com.bananaginger.noisedetector.ui.screens.HomeScreen
import com.bananaginger.noisedetector.ui.screens.SettingsScreen

// ---------------------------------------------------------------------------
// 1. SCREEN DESTINATIONS
//    Each object = one tab in the bottom bar.
//    `route` must be unique — NavHost uses it like a URL path.
// ---------------------------------------------------------------------------
sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : Screen(
        route = "home",
        label = "Monitor",
        icon = Icons.Filled.Home
    )

    object Settings : Screen(
        route = "settings",
        label = "Settings",
        icon = Icons.Filled.Settings
    )

    object History : Screen(
        route = "history",
        label = "History",
        icon = Icons.Filled.History
    )

    object EarthquakeApi : Screen(
        route = "earthquake_api",
        label = "API",
        icon = Icons.Filled.Api
    )

    companion object {
        // Ordered list used to build the bottom bar tabs left-to-right.
        val all = listOf(Home, Settings, History, EarthquakeApi)
    }
}

// ---------------------------------------------------------------------------
// 2. BOTTOM NAVIGATION BAR
//    Composable that draws the four tabs.
//    `currentRoute` comes from NavController so the active tab is highlighted.
// ---------------------------------------------------------------------------
@Composable
fun AppBottomNavBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit
) {
    NavigationBar {
        Screen.all.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 3. APP NAV HOST
//    Entry point called from MainActivity.
//    Creates the NavController, wraps everything in a Scaffold so the bottom
//    bar is always visible, and maps each route to its Screen composable.
// ---------------------------------------------------------------------------
@Composable
fun AppNavHost(viewModel: AnomalyViewModel) {
    val navController = rememberNavController()

    // currentBackStackEntryAsState() re-composes the bar whenever the back
    // stack changes, keeping the selected tab in sync automatically.
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Helper: navigate to a bottom-bar destination while keeping only one
    // copy of each destination in the back stack (popUpTo + launchSingleTop).
    fun navigateTo(screen: Screen) {
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Permission launcher is hoisted here so HomeScreen can request microphone
    // access without knowing about Android permission APIs.
    val context = LocalContext.current
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startMonitoring()
        else viewModel.microphonePermissionDenied()
    }

    Scaffold(
        bottomBar = {
            AppBottomNavBar(
                currentRoute = currentRoute,
                onNavigate = ::navigateTo
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route
        ) {
            // --- Screen 1: Monitor (start / stop + live readings) -----------
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onStartMonitoring = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED

                        if (granted) viewModel.startMonitoring()
                        else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    contentPadding = innerPadding
                )
            }

            // --- Screen 2: Sensor settings (thresholds) --------------------
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding
                )
            }

            // --- Screen 3: Anomaly history list ----------------------------
            composable(Screen.History.route) {
                HistoryScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding
                )
            }

            // --- Screen 4: Earthquake API test -----------------------------
            composable(Screen.EarthquakeApi.route) {
                EarthquakeApiScreen(
                    viewModel = viewModel,
                    contentPadding = innerPadding
                )
            }
        }
    }
}
