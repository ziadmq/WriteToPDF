package com.mobix.editorpdf.ui.view

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mobix.editorpdf.R
import com.mobix.editorpdf.ui.theme.GalaxyAccentPurple
import com.mobix.editorpdf.ui.theme.GalaxyAccentTeal
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember { Animatable(0f) }

    // Typing text state
    val fullText = "WriteToPDF"
    var displayedText by remember { mutableStateOf("") }

    // Animation Logic
    LaunchedEffect(key1 = true) {
        // 1. Logo Pop Animation (Bouncy)
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = { OvershootInterpolator(2f).getInterpolation(it) }
            )
        )

        // 2. Typing Effect for Text
        fullText.forEachIndexed { index, _ ->
            displayedText = fullText.substring(0, index + 1)
            delay(100) // Speed of typing
        }

        // 3. Wait a bit, then navigate to Home
        delay(1000L)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true } // Removes splash from back stack
        }
    }

    // Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        GalaxyAccentPurple, // Purple Top
                        GalaxyAccentTeal    // Teal Bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Animated Logo
            Icon(
                // Make sure you created this file in the previous step!
                // If not, use Icons.Default.Edit
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Typing Text
            Text(
                text = displayedText,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}