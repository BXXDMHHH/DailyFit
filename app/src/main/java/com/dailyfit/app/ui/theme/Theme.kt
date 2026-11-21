package com.dailyfit.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

private val LightColors = lightColorScheme(
    primary = Color(0xFF356B62),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E9E2),
    onPrimaryContainer = Color(0xFF08201B),
    secondary = Color(0xFF526A64),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD5E6E0),
    onSecondaryContainer = Color(0xFF10201C),
    tertiary = Color(0xFF9A6548),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBC8),
    onTertiaryContainer = Color(0xFF351707),
    background = Color(0xFFF6F8F6),
    onBackground = Color(0xFF191D1B),
    surface = Color(0xFFF6F8F6),
    onSurface = Color(0xFF191D1B),
    surfaceVariant = Color(0xFFE1E8E4),
    onSurfaceVariant = Color(0xFF414A46),
    outline = Color(0xFF717B76),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9BCFC3),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF1D5147),
    onPrimaryContainer = Color(0xFFB7EADD),
    secondary = Color(0xFFB9CCC5),
    onSecondary = Color(0xFF24332E),
    secondaryContainer = Color(0xFF3A4B45),
    onSecondaryContainer = Color(0xFFD5E6DF),
    tertiary = Color(0xFFE7B99D),
    onTertiary = Color(0xFF512D18),
    tertiaryContainer = Color(0xFF6B432D),
    onTertiaryContainer = Color(0xFFFFDBC8),
    background = Color(0xFF111714),
    onBackground = Color(0xFFE0E5E1),
    surface = Color(0xFF111714),
    onSurface = Color(0xFFE0E5E1),
    surfaceVariant = Color(0xFF414A46),
    onSurfaceVariant = Color(0xFFC1CAC4),
    outline = Color(0xFF8B938E),
    error = Color(0xFFFFB4AB)
)

@Composable
fun DailyFitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
