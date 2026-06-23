package com.bananaginger.noisedetector.history

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

private fun String.toTitleCaseWords(): String {
    val words = lowercase(Locale.US)
        .split("_")
        .filter { it.isNotBlank() }

    return words.joinToString(" ") { word ->
        word.replaceFirstChar { it.titlecase(Locale.US) }
    }.ifBlank { this }
}
