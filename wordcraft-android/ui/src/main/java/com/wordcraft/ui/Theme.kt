package com.wordcraft.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Restrained, document-first palette: paper surfaces, quiet blue accent.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2E5AAC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6FA),
    onPrimaryContainer = Color(0xFF122A57),
    secondary = Color(0xFF5A6472),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE3EA),
    onSecondaryContainer = Color(0xFF171C24),
    background = Color(0xFFF7F7F9),
    onBackground = Color(0xFF1A1C1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFE8E9EE),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFFC4C6CE),
    surfaceContainerLow = Color(0xFFF1F2F5),
    surfaceContainer = Color(0xFFEBECF0),
    surfaceContainerHigh = Color(0xFFE5E6EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DBAFF),
    onPrimary = Color(0xFF0C2A63),
    primaryContainer = Color(0xFF213F7C),
    onPrimaryContainer = Color(0xFFDCE6FA),
    secondary = Color(0xFFC3C7D0),
    onSecondary = Color(0xFF2A2F38),
    secondaryContainer = Color(0xFF404650),
    onSecondaryContainer = Color(0xFFDFE3EA),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1F),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6CE),
    outline = Color(0xFF8E9099),
    surfaceContainerLow = Color(0xFF16181B),
    surfaceContainer = Color(0xFF1E2023),
    surfaceContainerHigh = Color(0xFF282A2E),
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
    ),
)

/** The WordCraft Android theme: light/dark following the system, neutral surfaces. */
@Composable
fun WordCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
