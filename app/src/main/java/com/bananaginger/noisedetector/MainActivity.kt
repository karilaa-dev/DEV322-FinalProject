package com.bananaginger.noisedetector

/**
 * MainActivity — entry point Activity for the app.
 *
 * Responsibilities:
 * - Apply the app theme
 * - Set the root Compose content
 * - Provide a simple demo `Greeting` composable
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bananaginger.noisedetector.ui.theme.NoiseAndMotionAnomalyDetectorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge rendering so content may draw behind system bars.
        enableEdgeToEdge()
        // Set the Compose UI content and apply the app theme.
        setContent {
            NoiseAndMotionAnomalyDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Simple composable that displays a greeting.
 *
 * @param name The name to display in the greeting text.
 * @param modifier Modifier applied to the text element.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    // Display the greeting text.
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

/**
 * Preview for the `Greeting` composable used in Android Studio.
 */
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NoiseAndMotionAnomalyDetectorTheme {
        Greeting("Android")
    }
}