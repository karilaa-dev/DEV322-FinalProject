package com.bananaginger.noisedetector.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bananaginger.noisedetector.data.remote.RemoteAnomalyDocument
import com.bananaginger.noisedetector.data.remote.RemoteDataKind
import com.bananaginger.noisedetector.data.remote.RemoteEarthquakeSnapshot
import com.bananaginger.noisedetector.data.settings.DetectionTriggerMode
import com.bananaginger.noisedetector.history.HistoryEntry
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme
import org.junit.Rule
import org.junit.Test

class NoiseDetectorNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun bottomTabsSwitchAndSettingsBackWorks() {
        composeRule.setContent {
            NoiseAndMotionAnomalyDetectorTheme {
                var uiState by remember { mutableStateOf(AnomalyUiState()) }
                NoiseDetectorAppContent(
                    uiState = uiState,
                    onDetectionTriggerModeChanged = { mode ->
                        uiState = uiState.copy(detectionTriggerMode = mode)
                    }
                )
            }
        }

        composeRule.onNodeWithText("Sensor monitoring").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_local_history").performClick()
        composeRule.onNodeWithText("0 saved event(s)").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sensor monitoring").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_remote_history").performClick()
        composeRule.onNodeWithText("Uploaded Events").assertIsDisplayed()
        composeRule.onNodeWithText("Refresh").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_anomalies").performClick()
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Detection Settings").assertIsDisplayed()
        composeRule.onNodeWithText("Sound or motion").assertIsDisplayed()
        composeRule.onNodeWithText("Both sound + motion").performClick()
        composeRule.onNodeWithText(
            "An anomaly is saved only when sound and motion are both above their thresholds."
        ).assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sensor monitoring").assertIsDisplayed()
    }

    @Test
    fun localHistoryEventClickShowsDetails() {
        composeRule.setContent {
            NoiseAndMotionAnomalyDetectorTheme {
                NoiseDetectorAppContent(
                    uiState = AnomalyUiState(
                        detectionTriggerMode = DetectionTriggerMode.EITHER,
                        historyEntries = listOf(
                            HistoryEntry(
                                id = 44L,
                                timestamp = 1_765_000_000_000,
                                date = "2026-06-21",
                                time = "14:35:22",
                                day = "Sunday",
                                type = HistoryEntry.TYPE_SOUND_AND_MOTION,
                                soundLevelDb = 82.0,
                                accelerationMagnitude = 13.0f,
                                soundThresholdDb = 50.0,
                                motionThreshold = 1.5f,
                                soundThresholdExceeded = true,
                                motionThresholdExceeded = true,
                                motionDetected = true,
                                severity = 4,
                                description = "Loud sound and movement detected",
                                closestEarthquakeId = "uw123456",
                                earthquakeMagnitude = 3.4,
                                earthquakePlace = "10 km NW of Seattle",
                                latitude = 47.7,
                                longitude = -122.3,
                                depthKm = 12.5,
                                earthquakeTimeMillis = 1_765_000_000_000,
                                earthquakeSource = "USGS"
                            )
                        )
                    )
                )
            }
        }

        composeRule.onNodeWithTag("nav_local_history").performClick()
        composeRule.onNodeWithText("Exceeded: Sound & Motion").assertIsDisplayed()
        composeRule.onNodeWithTag("local-history-event-44").performClick()
        composeRule.onNodeWithContentDescription("Close anomaly details").assertIsDisplayed()
        composeRule.onNodeWithText("Event").assertIsDisplayed()
        composeRule.onNodeWithText("Sound").assertIsDisplayed()
        composeRule.onNodeWithText("Motion").assertIsDisplayed()
        composeRule.onNodeWithText("Nearest Earthquake").assertIsDisplayed()
        composeRule.onNodeWithText("Sound exceeded").assertIsDisplayed()
        composeRule.onNodeWithText("Motion exceeded").assertIsDisplayed()
        composeRule.onNodeWithText("Magnitude").assertIsDisplayed()
        composeRule.onNodeWithText("Latitude").assertIsDisplayed()
        composeRule.onNodeWithText("Longitude").assertIsDisplayed()
        composeRule.onNodeWithText("uw123456").assertIsDisplayed()
        composeRule.onNodeWithText("USGS").assertIsDisplayed()
        composeRule.onNodeWithText("M3.4").assertIsDisplayed()
    }

    @Test
    fun remoteUploadedEventClickShowsRemoteDetailsOnly() {
        composeRule.setContent {
            NoiseAndMotionAnomalyDetectorTheme {
                NoiseDetectorAppContent(
                    uiState = AnomalyUiState(
                        remoteDataKind = RemoteDataKind.ANOMALIES,
                        remoteAnomalies = listOf(
                            RemoteAnomalyDocument(
                                installId = "install-test",
                                localAnomalyId = 99L,
                                timestamp = 1_765_000_000_000,
                                date = "2026-06-21",
                                day = "Sunday",
                                type = HistoryEntry.TYPE_SOUND_AND_MOTION,
                                soundLevelDb = 82.0,
                                accelerationMagnitude = 13.0f,
                                soundThresholdExceeded = true,
                                motionThresholdExceeded = true,
                                severity = 4,
                                description = "Uploaded loud sound and movement",
                                closestEarthquakeId = "us7000abcd",
                                closestEarthquake = RemoteEarthquakeSnapshot(
                                    place = "10 km NW of Seattle",
                                    magnitude = 3.4,
                                    latitude = 47.7,
                                    longitude = -122.3,
                                    depthKm = 12.5,
                                    timeMillis = 1_765_000_000_000,
                                    source = "USGS"
                                ),
                                uploadedAt = 1_765_000_060_000
                            )
                        )
                    )
                )
            }
        }

        composeRule.onNodeWithTag("nav_remote_history").performClick()
        composeRule.onNodeWithTag(
            "remote-history-event-install-test-99-1765000000000"
        ).performClick()

        composeRule.onNodeWithContentDescription("Close remote event details").assertIsDisplayed()
        composeRule.onNodeWithText("Event").assertIsDisplayed()
        composeRule.onNodeWithText("Readings").assertIsDisplayed()
        composeRule.onNodeWithText("Nearest Earthquake").assertIsDisplayed()
        composeRule.onAllNodesWithText("Triggered by: Sound & Motion").assertCountEquals(2)
        composeRule.onNodeWithText("Triggered by").assertIsDisplayed()
        composeRule.onNodeWithText("Severity").assertIsDisplayed()
        composeRule.onNodeWithText("4/5").assertIsDisplayed()
        composeRule.onNodeWithText("Sound").assertIsDisplayed()
        composeRule.onNodeWithText("82.0 dB").assertIsDisplayed()
        composeRule.onNodeWithText("Acceleration").assertIsDisplayed()
        composeRule.onNodeWithText("13.0 m/s²").assertIsDisplayed()
        composeRule.onNodeWithText("Install ID").assertIsDisplayed()
        composeRule.onNodeWithText("install-test").assertIsDisplayed()
        composeRule.onNodeWithText("Local ID").assertIsDisplayed()
        composeRule.onNodeWithText("99").assertIsDisplayed()
        composeRule.onNodeWithText("Uploaded remotely").assertIsDisplayed()
        composeRule.onNodeWithText("Upload time").assertIsDisplayed()
        composeRule.onNodeWithText("Place").assertIsDisplayed()
        composeRule.onNodeWithText("10 km NW of Seattle").assertIsDisplayed()
        composeRule.onNodeWithText("Magnitude").assertIsDisplayed()
        composeRule.onNodeWithText("M3.4").assertIsDisplayed()
        composeRule.onNodeWithText("Depth").assertIsDisplayed()
        composeRule.onNodeWithText("12.5 km").assertIsDisplayed()
        composeRule.onNodeWithText("Earthquake time").assertIsDisplayed()
        composeRule.onNodeWithText("Source").assertIsDisplayed()
        composeRule.onNodeWithText("USGS").assertIsDisplayed()
        composeRule.onNodeWithText("Epicenter").assertIsDisplayed()
        composeRule.onNodeWithText("47.7000, -122.3000").assertIsDisplayed()

        composeRule.onAllNodesWithText("Threshold").assertCountEquals(0)
        composeRule.onAllNodesWithText("Sound exceeded").assertCountEquals(0)
        composeRule.onAllNodesWithText("Motion exceeded").assertCountEquals(0)
        composeRule.onAllNodesWithText("Closest ID").assertCountEquals(0)
        composeRule.onAllNodesWithText("us7000abcd").assertCountEquals(0)
    }
}
