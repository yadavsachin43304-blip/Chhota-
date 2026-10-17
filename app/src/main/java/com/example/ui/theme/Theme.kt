package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ChhotuDarkColorScheme = darkColorScheme(
    primary = SaffronPrimary,
    onPrimary = SaffronOnPrimary,
    primaryContainer = SaffronContainer,
    onPrimaryContainer = OnSaffronContainer,
    secondary = ElectricTealSecondary,
    onSecondary = OnElectricTealSecondary,
    secondaryContainer = ElectricTealContainer,
    onSecondaryContainer = Color(0xFFB2F5EA),
    tertiary = CyberBlueTertiary,
    onTertiary = Color(0xFF001E2C),
    background = ObsidianBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = ObsidianSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = AlertCrimsonError,
    onError = Color.White
)

private val ChhotuLightColorScheme = lightColorScheme(
    primary = SaffronLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = TealLightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = IndigoLightTertiary,
    onTertiary = Color.White,
    background = DaylightBackground,
    onBackground = Color(0xFF0F172A),
    surface = DaylightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = DaylightSurfaceVariant,
    onSurfaceVariant = Color(0xFF334155),
    error = Color(0xFFE11D48),
    onError = Color.White
)

val ChhotuShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ChhotuAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ChhotuDarkColorScheme else ChhotuLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ChhotuShapes,
        content = content
    )
}
