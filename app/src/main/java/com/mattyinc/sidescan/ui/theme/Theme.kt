package com.mattyinc.sidescan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4F9CF9),
    onPrimary = Color(0xFF07121F),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE6EAF0),
    surface = Color(0xFF161B22),
    onSurface = Color(0xFFE6EAF0),
    surfaceVariant = Color(0xFF1F2630),
    onSurfaceVariant = Color(0xFFAEB8C4)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1C64D8),
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF)
)

@Composable
fun SideScanTheme(
    useDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        content = content
    )
}

object RiskColors {
    val High = Color(0xFFF26D6D)
    val Medium = Color(0xFFF2B705)
    val Low = Color(0xFF7FB3FF)
    val Safe = Color(0xFF59C28A)
}
