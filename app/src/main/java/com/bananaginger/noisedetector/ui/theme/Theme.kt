package com.bananaginger.noisedetector.ui.theme

/**
 * Theme configuration for the app using Material3.
 *
 * Responsibilities:
 * - Define dark and light color schemes
 * - Choose dynamic colors on Android 12+ when enabled
 * - Provide a `MaterialTheme` wrapper for app composables
 */

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Color scheme used when the app is in dark mode.
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

// Color scheme used when the app is in light mode.
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

/**
 * Compose wrapper that applies the chosen color scheme and typography.
 *
 * @param darkTheme If true, force dark theme; otherwise follow system setting.
 * @param dynamicColor When true and Android 12+, use dynamic color palettes.
 * @param content Composable content to style.
 */
@Composable
fun NoiseAndMotionAnomalyDetectorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Select a color scheme depending on dynamic color availability and system dark theme.
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Apply MaterialTheme for color and typography to the provided content.
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}