package com.dailyfit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

private val Ink = Color(0xFF332B25)
private val Muted = Color(0xFF88796D)
private val Canvas = Color(0xFFFFF8EF)
private val Paper = Color(0xFFFFFDF9)
private val SunOrange = Color(0xFFE88A3A)
private val SunOrangeSoft = Color(0xFFFFE8D0)
private val Honey = Color(0xFFF4C76B)
private val Sage = Color(0xFF789477)
private val Line = Color(0xFFF0E5D8)

private val LightColors = lightColorScheme(
    primary = SunOrange,
    onPrimary = Color.White,
    primaryContainer = SunOrangeSoft,
    onPrimaryContainer = Color(0xFF633512),
    secondary = Color(0xFFB87936),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF0D5),
    onSecondaryContainer = Color(0xFF563B18),
    tertiary = Sage,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6F0E2),
    onTertiaryContainer = Color(0xFF243D27),
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFFFF1E2),
    onSurfaceVariant = Muted,
    outline = Color(0xFFD8C8B8),
    outlineVariant = Line,
    error = Color(0xFFB94B3E),
    errorContainer = Color(0xFFFFE8E3),
    onErrorContainer = Color(0xFF641E17)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB66F),
    onPrimary = Color(0xFF482300),
    primaryContainer = Color(0xFF704018),
    onPrimaryContainer = Color(0xFFFFDCC0),
    secondary = Color(0xFFEBC27D),
    onSecondary = Color(0xFF422D0D),
    secondaryContainer = Color(0xFF5B431F),
    onSecondaryContainer = Color(0xFFFFE8BD),
    tertiary = Color(0xFFB4CBAE),
    onTertiary = Color(0xFF203820),
    tertiaryContainer = Color(0xFF354F34),
    onTertiaryContainer = Color(0xFFD0E8C9),
    background = Color(0xFF211C17),
    onBackground = Color(0xFFF4E9DE),
    surface = Color(0xFF2B241E),
    onSurface = Color(0xFFF4E9DE),
    surfaceVariant = Color(0xFF3A3028),
    onSurfaceVariant = Color(0xFFD1BFAE),
    outline = Color(0xFF9B8571),
    outlineVariant = Color(0xFF514338),
    error = Color(0xFFFFB4A8),
    errorContainer = Color(0xFF672B24),
    onErrorContainer = Color(0xFFFFDAD3)
)

private val DailyFitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val BaseTypography = Typography()
private val DailyFitTypography = BaseTypography.copy(
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.Medium),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
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
