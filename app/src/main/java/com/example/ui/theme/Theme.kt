package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MinaDarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = NeonBlue,
    tertiary = Purple80,
    background = BgDark,
    surface = CardDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextMain,
    onSurface = TextMain,
    surfaceVariant = NavBg,
    onSurfaceVariant = TextMuted
)

private val MinaAmoledColorScheme = darkColorScheme(
    primary = NeonGreen,
    secondary = NeonBlue,
    tertiary = Purple80,
    background = BgAmoled,
    surface = BgAmoled,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = TextMain,
    onSurface = TextMain,
    surfaceVariant = NavBg,
    onSurfaceVariant = TextMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme to match Mina Player identity
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (isAmoled) MinaAmoledColorScheme else MinaDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
