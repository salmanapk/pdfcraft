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

private val LightColorScheme = lightColorScheme(
    primary = VermilionCrimson,
    onPrimary = Color.White,
    primaryContainer = VermilionSoftContainer,
    onPrimaryContainer = VermilionOnContainer,
    secondary = Color(0xFF2F3440),
    onSecondary = Color.White,
    secondaryContainer = WarmStoneContainer,
    onSecondaryContainer = InkCharcoal,
    tertiary = ToolAccentOptimize,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD2F4EE),
    onTertiaryContainer = Color(0xFF063A35),
    background = AlabasterCanvas,
    onBackground = InkCharcoal,
    surface = PaperWhite,
    onSurface = InkCharcoal,
    surfaceVariant = WarmStoneSurface,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = PaperWhite,
    surfaceContainerLow = Color(0xFFF7F4EE),
    surfaceContainer = WarmStoneSurface,
    surfaceContainerHigh = WarmStoneContainer,
    surfaceContainerHighest = Color(0xFFE0DBD1),
    outline = InkBorder,
    outlineVariant = Color(0xFFEAE5DC),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = VermilionLight,
    onPrimary = Color(0xFF470804),
    primaryContainer = VermilionDarkContainer,
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFBFC4D0),
    onSecondary = Color(0xFF1F222A),
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF043731),
    tertiaryContainer = Color(0xFF0B4640),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = DarkCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainerLowest = DarkCanvas,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkSurfaceHighest,
    surfaceContainerHighest = Color(0xFF383C47),
    outline = DarkOutline,
    outlineVariant = Color(0xFF282B34),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val PDFCraftShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun PDFCraftTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = PDFCraftTypography,
        shapes = PDFCraftShapes,
        content = content
    )
}
