package app.abaco.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Paper = Color(0xFFF7F4EE)
val Surface = Color(0xFFFFFDF9)
val Ink = Color(0xFF1A1814)
val Muted = Color(0xFF655E55)
val Line = Color(0xFFE4DACE)
val Pine = Color(0xFF1B6B4A)
val PineSoft = Color(0xFFE6F2EB)
val Clay = Color(0xFFA33B2B)
val ClaySoft = Color(0xFFF8EBE7)
val Gold = Color(0xFF6E5316)
val GoldSoft = Color(0xFFF4EDDC)

private val scheme = lightColorScheme(
    primary = Pine,
    onPrimary = Color.White,
    primaryContainer = PineSoft,
    onPrimaryContainer = Pine,
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = Gold,
    tertiary = Clay,
    onTertiary = Color.White,
    tertiaryContainer = ClaySoft,
    onTertiaryContainer = Clay,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = Muted,
    outline = Line,
    outlineVariant = Line,
    error = Clay,
    onError = Color.White,
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
fun AbacoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content,
    )
}
