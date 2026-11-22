package com.dailyfit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val Ink = Color(0xFF3D2930)
private val Muted = Color(0xFF927781)
private val Canvas = Color(0xFFFFF5F7)
private val Paper = Color(0xFFFFFCFD)
private val Rose = Color(0xFFD87595)
private val RoseSoft = Color(0xFFFBE1E9)
private val Berry = Color(0xFFB95F7D)
private val BerrySoft = Color(0xFFF8EAF0)
private val WarmGold = Color(0xFFC28A55)
private val WarmGoldSoft = Color(0xFFFFF0DF)
private val Line = Color(0xFFF0DFE5)

private val LightColors = lightColorScheme(
    primary = Rose,
    onPrimary = Color.White,
    primaryContainer = RoseSoft,
    onPrimaryContainer = Color(0xFF5B263A),
    secondary = Berry,
    onSecondary = Color.White,
    secondaryContainer = BerrySoft,
    onSecondaryContainer = Color(0xFF54283A),
    tertiary = WarmGold,
    onTertiary = Color.White,
    tertiaryContainer = WarmGoldSoft,
    onTertiaryContainer = Color(0xFF573719),
    background = Canvas,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFFFEDF1),
    onSurfaceVariant = Muted,
    outline = Color(0xFFD9BBC5),
    outlineVariant = Line,
    error = Color(0xFFB94B55),
    errorContainer = Color(0xFFFFE7E8),
    onErrorContainer = Color(0xFF641E27)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFAFC5),
    onPrimary = Color(0xFF581D34),
    primaryContainer = Color(0xFF79384F),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFEBAAC0),
    onSecondary = Color(0xFF4D2436),
    secondaryContainer = Color(0xFF653849),
    onSecondaryContainer = Color(0xFFFFD9E5),
    tertiary = Color(0xFFE6B987),
    onTertiary = Color(0xFF432A12),
    tertiaryContainer = Color(0xFF5C4126),
    onTertiaryContainer = Color(0xFFFFDEB8),
    background = Color(0xFF241B20),
    onBackground = Color(0xFFF7EAF0),
    surface = Color(0xFF30242B),
    onSurface = Color(0xFFF7EAF0),
    surfaceVariant = Color(0xFF43323B),
    onSurfaceVariant = Color(0xFFD8BEC9),
    outline = Color(0xFF9E7C8A),
    outlineVariant = Color(0xFF58434D),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF672B30),
    onErrorContainer = Color(0xFFFFDAD6)
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
