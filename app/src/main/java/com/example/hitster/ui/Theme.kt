package com.example.hitster.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Hitster runs on its own dark palette instead of the wallpaper colors: the song cards are bright
 * and randomly colored, so the app around them has to stay dark and predictable.
 */
private val HitsterColorScheme = darkColorScheme(
    primary = Color(0xFFFF2E63),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF4A0F22),
    onPrimaryContainer = Color(0xFFFFD6E0),

    secondary = Color(0xFFFFC53D),
    onSecondary = Color(0xFF2A1E00),
    secondaryContainer = Color(0xFF4A3608),
    onSecondaryContainer = Color(0xFFFFE9B0),

    // Tokens live on the tertiary colors, which is why they read as gold coins.
    tertiary = Color(0xFFFFC53D),
    onTertiary = Color(0xFF2A1E00),
    tertiaryContainer = Color(0xFF6B4C0A),
    onTertiaryContainer = Color(0xFFFFEFC4),

    background = Color(0xFF0D0B12),
    onBackground = Color(0xFFF2EEF8),
    surface = Color(0xFF17141F),
    onSurface = Color(0xFFF2EEF8),
    surfaceVariant = Color(0xFF241F30),
    onSurfaceVariant = Color(0xFFB7AECA),
    surfaceContainerHighest = Color(0xFF2C2639),

    outline = Color(0xFF7A6F91),
    outlineVariant = Color(0xFF3A3348),

    error = Color(0xFFFF5A5A),
    onError = Color(0xFF3A0000)
)

@Composable
fun HitsterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HitsterColorScheme,
        typography = HitsterTypography,
        content = content
    )
}
