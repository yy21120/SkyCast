package com.yy21120.skycast.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkyCastColors = lightColorScheme(
    primary = Color(0xFF9D3D12),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF351000),
    secondary = Color(0xFF705B52),
    background = Color(0xFFFFF8F4),
    surface = Color(0xFFFFF8F4),
    onSurface = Color(0xFF241A17),
    surfaceVariant = Color(0xFFF5DED5),
    onSurfaceVariant = Color(0xFF53433D),
)

@Composable
fun SkyCastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkyCastColors,
        content = content,
    )
}
