package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.AppTheme

private val ObsidianColorScheme = darkColorScheme(
    primary = ObsidianPrimary,
    secondary = ObsidianAccent,
    background = ObsidianBg,
    surface = ObsidianSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val SilverLightColorScheme = lightColorScheme(
    primary = SilverPrimary,
    secondary = Color(0xFF5856D6), // iOS Purple
    background = SilverBg,
    surface = SilverSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = SilverTextDark,
    onSurface = SilverTextDark
)

private val SilverDarkColorScheme = darkColorScheme(
    primary = SilverPrimary,
    secondary = Color(0xFF5856D6),
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF2C2C2E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val EmeraldColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    secondary = EmeraldAccent,
    background = EmeraldBg,
    surface = EmeraldSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White
)

private val SolarColorScheme = darkColorScheme(
    primary = SolarPrimary,
    secondary = SolarAccent,
    background = SolarBg,
    surface = SolarSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    secondary = MidnightAccent,
    background = MidnightBg,
    surface = MidnightSurface,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun ScorpioTheme(
    appTheme: AppTheme = AppTheme.IOS_SILVER,
    isDark: Boolean = isSystemInDarkTheme(),
    dynamicColorScheme: androidx.compose.material3.ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = dynamicColorScheme ?: when (appTheme) {
        AppTheme.OBSIDIAN_BLACK -> ObsidianColorScheme
        AppTheme.IOS_SILVER -> if (isDark) SilverDarkColorScheme else SilverLightColorScheme
        AppTheme.CYBER_EMERALD -> EmeraldColorScheme
        AppTheme.SOLAR_GOLD -> SolarColorScheme
        AppTheme.MIDNIGHT_BLUE -> MidnightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
