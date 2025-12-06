package com.mobix.editorpdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CreativePurpleLight = lightColorScheme(
    primary = Color(0xFF6C63FF),
    onPrimary = Color.White,

    secondary = Color(0xFF4FD0E3),
    onSecondary = Color.Black,

    background = Color(0xFFF6F6FE),
    onBackground = Color(0xFF202124),

    surface = Color.White,
    onSurface = Color(0xFF202124),

    outline = Color(0xFFCBC7F3)
)

@Composable
fun DocumentEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CreativePurpleLight,
        typography = Typography,
        shapes = Shapes(),
        content = content
    )
}
