package com.gymlog.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6FEB),
    secondary = Color(0xFF3559A6),
    tertiary = Color(0xFF6F4DBF),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEFF1F4)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF7AA9F7),
    secondary = Color(0xFFA6B8DA),
    tertiary = Color(0xFFBFA7F2),
    background = Color(0xFF101216),
    surface = Color(0xFF161A20),
    surfaceVariant = Color(0xFF1F242C)
)

@Composable
fun GymLogTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = scheme, content = content)
}
