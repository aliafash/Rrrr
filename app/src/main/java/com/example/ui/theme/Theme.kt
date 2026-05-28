package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DaliliTheme(
    primaryColor: Color = Color(0xFF000000),
    secondaryColor: Color = Color(0xFFFFD700),
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = primaryColor,
        secondary = secondaryColor,
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFF000000),
        onBackground = Color(0xFF212121),
        onSurface = Color(0xFF212121),
        surfaceVariant = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFF757575)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
