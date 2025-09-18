package com.feuerwehr.checklist.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme

// Fire department theme colors (red-based)
private val FeuerwehrRed = Color(0xFFD32F2F)
private val FeuerwehrRedVariant = Color(0xFFB71C1C)
private val FeuerwehrGold = Color(0xFFFFC107)

private val LightColorScheme = lightColorScheme(
    primary = FeuerwehrRed,
    primaryContainer = FeuerwehrRedVariant,
    secondary = FeuerwehrGold,
    secondaryContainer = Color(0xFFFFF3C4),
    tertiary = Color(0xFF6750A4),
    background = Color(0xFFFFFBFF),
    surface = Color(0xFFFFFBFF),
    error = Color(0xFFBA1A1A),
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    primaryContainer = FeuerwehrRedVariant,
    secondary = Color(0xFFE0C2A5),
    secondaryContainer = Color(0xFF5D4E37),
    tertiary = Color(0xFFD0BCFF),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    error = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    onSecondary = Color(0xFF3C2F23),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
)

@Composable
fun ChecklistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}