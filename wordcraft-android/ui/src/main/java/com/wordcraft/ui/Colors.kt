package com.wordcraft.ui

import androidx.compose.ui.graphics.Color

/** Parse `RRGGBB` / `#RRGGBB` (WordCraft uses unprefixed RRGGBB) into a Compose color. */
fun parseHexColor(hex: String): Color {
    val h = hex.removePrefix("#")
    return try {
        when (h.length) {
            6 -> Color((0xFF shl 24) or h.toLong(16).toInt())
            8 -> Color(h.toLong(16).toInt())
            else -> Color.Unspecified
        }
    } catch (_: NumberFormatException) {
        Color.Unspecified
    }
}
