package com.dailyfit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val Ink = Color(0xFF202522)
private val Muted = Color(0xFF737B75)
private val Canvas = Color(0xFFF6F7F5)
private val Paper = Color(0xFFFFFFFF)
private val Sage = Color(0xFF526B5A)
private val SageSoft = Color(0xFFE8EEE9)
private val Line = Color(0xFFE6E9E5)

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = SageSoft,
    onPrimaryContainer = Color(0xFF1D3325),
    secondary = Color(0xFF66736A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF0EC),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF8B765D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2ECE3),
    onTertiaryContainer = Color(0xFF34291C),
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF0F2EF),
    onSurfaceVariant = Muted,
    outline = Color(0xFFB9C0BA),
    outlineVariant = Line,
    error = Color(0xFFB54747),
    errorContainer = Color(0xFFF8E9E8),
    onErrorContainer = Color(0xFF5B1C1C)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB2C9B7),
    onPrimary = Color(0xFF203629),
    primaryContainer = Color(0xFF354D3C),
    onPrimaryContainer = Color(0xFFD0E5D4),
    secondary = Color(0xFFC0C9C1),
    onSecondary = Color(0xFF2A332C),
    secondaryContainer = Color(0xFF414B43),
    onSecondaryContainer = Color(0xFFDCE5DC),
    tertiary = Color(0xFFD5C2A8),
    onTertiary = Color(0xFF392D1F),
    tertiaryContainer = Color(0xFF514331),
    onTertiaryContainer = Color(0xFFF0DFC7),
    background = Color(0xFF151916),
    onBackground = Color(0xFFE7EBE6),
    surface = Color(0xFF1D221E),
    onSurface = Color(0xFFE7EBE6),
    surfaceVariant = Color(0xFF2A312B),
    onSurfaceVariant = Color(0xFFB9C2BA),
    outline = Color(0xFF879188),
    outlineVariant = Color(0xFF3B443C),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF5B2626),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val DailyFitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val DailyFitTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold)
)

@Composable
fun DailyFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = DailyFitTypography,
        shapes = DailyFitShapes,
        content = content
    )
}
