package com.example.writetopdf.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

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