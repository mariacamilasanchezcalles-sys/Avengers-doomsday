package com.cami.maratondoomsday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

val CineRed = Color(0xFFE53935)
val DeepRed = Color(0xFF5E1416)
val Gold = Color(0xFFF2C14E)
val DeepGold = Color(0xFF4A3B12)
val Background = Color(0xFF0F0D0E)
val Ink = Color(0xFFF7F2EC)
val Muted = Color(0xFFCFC4BE)

private val colors = darkColorScheme(
    primary = CineRed,
    onPrimary = Color.White,
    primaryContainer = DeepRed,
    onPrimaryContainer = Color.White,
    secondary = Gold,
    onSecondary = Color(0xFF1E1600),
    secondaryContainer = DeepGold,
    onSecondaryContainer = Gold,
    tertiary = Gold,
    onTertiary = Color(0xFF1E1600),
    background = Background,
    onBackground = Ink,
    surface = Background,
    onSurface = Ink,
    surfaceVariant = Color(0xFF2B2426),
    onSurfaceVariant = Muted,
    surfaceContainerLowest = Color(0xFF0B090A),
    surfaceContainerLow = Color(0xFF161214),
    surfaceContainer = Color(0xFF1A1618),
    surfaceContainerHigh = Color(0xFF221D1F),
    surfaceContainerHighest = Color(0xFF2A2427),
    outline = Color(0xFF6E6064),
    outlineVariant = Color(0xFF3F3538),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0000),
)

/** Todo un poco más grande de lo normal para que se lea fácil. */
private fun TextStyle.bigger(f: Float = 1.18f) = copy(fontSize = fontSize * f, lineHeight = lineHeight * f)

private val base = Typography()
private val typography = Typography(
    displayLarge = base.displayLarge.bigger(1.0f),
    displayMedium = base.displayMedium.bigger(1.0f),
    displaySmall = base.displaySmall.bigger(1.05f),
    headlineLarge = base.headlineLarge.bigger(1.1f),
    headlineMedium = base.headlineMedium.bigger(1.1f),
    headlineSmall = base.headlineSmall.bigger(1.1f),
    titleLarge = base.titleLarge.bigger(),
    titleMedium = base.titleMedium.bigger(),
    titleSmall = base.titleSmall.bigger(),
    bodyLarge = base.bodyLarge.bigger(),
    bodyMedium = base.bodyMedium.bigger(),
    bodySmall = base.bodySmall.bigger(),
    labelLarge = base.labelLarge.bigger(),
    labelMedium = base.labelMedium.bigger(),
    labelSmall = base.labelSmall.bigger(),
)

@Composable
fun MaratonTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
