package com.bananaginger.noisedetector.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                NoiseDetectorAppContent(
                    uiState = AnomalyUiState()
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

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Sensor monitoring").assertIsDisplayed()
    }
}
