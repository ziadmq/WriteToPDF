package com.example.writetopdf

import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@SuppressLint("UnnecessaryComposedModifier")
fun Modifier.dashedBorder(
    width: Dp = 1.dp,
    color: Color = Color.Black,
    cornerShape: Dp = 0.dp,
    dashLength: Dp = 4.dp,
    dashWidth: Dp = 4.dp
) = composed{
    drawBehind {
        val pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), dashWidth.toPx()),
            0f
        )
        val halfWidth = width.toPx()/2
        val cornerRadius = cornerShape.toPx()

        drawRoundRect(
            color = color,
            style = Stroke(
                width = halfWidth,
                pathEffect = pathEffect
            ),
            cornerRadius = CornerRadius(cornerRadius)
        )
    }
}