package com.bananaginger.noisedetector.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import java.util.Locale

internal fun historyTypeDisplayLabel(type: String): String {
    return when (type.uppercase(Locale.US)) {
        HistoryEntry.TYPE_SOUND -> "Sound"
        HistoryEntry.TYPE_MOTION -> "Motion"
        HistoryEntry.TYPE_EARTHQUAKE -> "Earthquake"
        HistoryEntry.TYPE_SOUND_AND_MOTION -> "Sound & Motion"
        else -> type.toTitleCaseWords()
    }
}

@Composable
internal fun historyTypeColor(type: String) = when (type.uppercase(Locale.US)) {
    HistoryEntry.TYPE_SOUND -> MaterialTheme.colorScheme.tertiary
    HistoryEntry.TYPE_MOTION -> MaterialTheme.colorScheme.primary
    HistoryEntry.TYPE_EARTHQUAKE -> MaterialTheme.colorScheme.error
    HistoryEntry.TYPE_SOUND_AND_MOTION -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurface
}

private fun String.toTitleCaseWords(): String {
    val words = lowercase(Locale.US)
        .split("_")
        .filter { it.isNotBlank() }

    return words.joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase(Locale.US) }
    }.ifBlank { this }
}
