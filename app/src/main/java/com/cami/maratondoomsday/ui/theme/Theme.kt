package com.cami.maratondoomsday.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.cami.maratondoomsday.R

// ---------- Paleta HUD ----------
/** Fondo: negro azulado de laboratorio. */
val Void = Color(0xFF03070C)
val Panel = Color(0xFF0A1420)
val PanelHigh = Color(0xFF0F1C2B)
/** Holograma: líneas, rejilla y datos. */
val Holo = Color(0xFF5CE1FF)
val HoloDim = Color(0xFF1B4A5C)
/** Dorado: títulos y logros. */
val Gold = Color(0xFFFFC24A)
val GoldDeep = Color(0xFFE09A12)
/** Rojo: acciones principales y alertas. */
val StarkRed = Color(0xFFE4252F)
val RedDeep = Color(0xFF8E0F17)
val TextHi = Color(0xFFEAF6FF)
val TextLo = Color(0xFF8FA9BC)
val Online = Color(0xFF3DFFB0)

// ---------- Tipografía ----------
val Rajdhani = FontFamily(
    Font(R.font.rajdhani_medium, FontWeight.Medium),
    Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
    Font(R.font.rajdhani_bold, FontWeight.Bold),
)
val Orbitron = FontFamily(
    Font(R.font.orbitron_bold, FontWeight.Bold),
    Font(R.font.orbitron_black, FontWeight.Black),
)
val Mono = FontFamily(Font(R.font.sharetech_mono, FontWeight.Normal))

private fun raj(size: Int, weight: FontWeight = FontWeight.SemiBold, line: Int = (size * 1.25f).toInt()) =
    TextStyle(fontFamily = Rajdhani, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp)

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 56.sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 50.sp),
    displaySmall = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp),
    headlineLarge = raj(36, FontWeight.Bold, 40),
    headlineMedium = raj(30, FontWeight.Bold, 34),
    headlineSmall = raj(26, FontWeight.Bold, 30),
    titleLarge = raj(24, FontWeight.Bold, 28),
    titleMedium = raj(21, FontWeight.Bold, 25),
    titleSmall = raj(18, FontWeight.Bold, 22),
    bodyLarge = raj(21, FontWeight.Medium, 27),
    bodyMedium = raj(19, FontWeight.Medium, 24),
    bodySmall = raj(17, FontWeight.Medium, 21),
    labelLarge = TextStyle(fontFamily = Mono, fontSize = 16.sp, lineHeight = 20.sp, letterSpacing = 1.5.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 1.5.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.5.sp),
)

private val colors = darkColorScheme(
    primary = StarkRed,
    onPrimary = Color.White,
    primaryContainer = RedDeep,
    onPrimaryContainer = Color.White,
    secondary = Gold,
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = Color(0xFF3A2A06),
    onSecondaryContainer = Gold,
    tertiary = Holo,
    onTertiary = Void,
    background = Void,
    onBackground = TextHi,
    surface = Panel,
    onSurface = TextHi,
    surfaceVariant = PanelHigh,
    onSurfaceVariant = TextLo,
    surfaceContainerLowest = Void,
    surfaceContainerLow = Panel,
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelHigh,
    surfaceContainerHighest = PanelHigh,
    outline = HoloDim,
    outlineVariant = HoloDim,
    error = Color(0xFFFF6B6B),
    onError = Color.Black,
)

@Composable
fun MaratonTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
