package dev.boop.android.ui

import androidx.compose.ui.graphics.Color
import dev.boop.android.data.model.Level
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun levelColor(level: Level): Color = when (level) {
    Level.INFO -> Color(0xFF3B82F6)
    Level.SUCCESS -> Color(0xFF22C55E)
    Level.WARNING -> Color(0xFFF59E0B)
    Level.ERROR -> Color(0xFFEF4444)
    Level.CRITICAL -> Color(0xFFB91C1C)
}

private val timeOnly: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateTime: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM, HH:mm")

/** Absolute timestamp: time if today, otherwise date + time. */
fun formatTimestamp(iso: String): String {
    val t = parseInstant(iso) ?: return iso
    val today = OffsetDateTime.now().toLocalDate()
    return if (t.toLocalDate() == today) t.format(timeOnly) else t.format(dateTime)
}

/** Compact relative time for list rows ("now", "5m", "3h", "2d"). */
fun relativeTime(iso: String): String {
    val t = parseInstant(iso) ?: return ""
    val seconds = Duration.between(t, OffsetDateTime.now()).seconds
    return when {
        seconds < 60 -> "now"
        seconds < 3_600 -> "${seconds / 60}m"
        seconds < 86_400 -> "${seconds / 3_600}h"
        else -> "${seconds / 86_400}d"
    }
}

private fun parseInstant(iso: String): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(iso) }.getOrNull()
