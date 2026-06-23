package com.bananaginger.noisedetector.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithText("Local History").assertIsDisplayed()
        composeRule.onNodeWithText("0 saved event(s)").assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sensor monitoring").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_remote_history").performClick()
        composeRule.onNodeWithText("Remote History").assertIsDisplayed()
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
}
