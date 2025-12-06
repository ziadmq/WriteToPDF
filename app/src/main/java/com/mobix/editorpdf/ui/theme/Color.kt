package com.mobix.editorpdf.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFFFFFFF)
val PurpleGrey80 = Color(0xFFFFFFFF)
val Pink80 = Color(0xFFFFFFFF)

val Purple40 = Color(0xFFFFFFFF)
val PurpleGrey40 = Color(0xFFFFFFFF)
val Pink40 = Color(0xFFFFFFFF)

// --- Galaxy / Canva Dark Theme Colors ---
val GalaxyBackground = Color(0xFF0F172A) // Deep Space Blue
val GalaxySurface = Color(0xFF1E293B)    // Lighter Space Blue
val GalaxyTextPrimary = Color(0xFFFFFFFF)
val GalaxyTextSecondary = Color(0xFF94A3B8)
val GalaxyAccentPurple = Color(0xFF7D2AE8)
val GalaxyAccentTeal = Color(0xFF00E5FF)

// Galaxy Gradient Brush
val GalaxyGradient = Brush.linearGradient(
    colors = listOf(GalaxyAccentPurple, GalaxyAccentTeal)
)

val GalaxyCardGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
)