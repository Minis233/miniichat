package com.miniichat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A translucent glass surface with hairline border and a subtle inner highlight on top.
 * No real backdrop blur (Android <31 lacks it) — relies on layered semi-transparent fills,
 * which read as "frosted" against the gradient backdrop.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    cornerRadius: Dp = 20.dp,
    tintAlpha: Float = 0.55f,
    borderAlpha: Float = 0.28f,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val tint = if (isDark) Color.White.copy(alpha = tintAlpha * 0.18f)
    else Color.White.copy(alpha = tintAlpha)
    val highlight = if (isDark)
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.10f),
            0.4f to Color.White.copy(alpha = 0.02f),
            1f to Color.Transparent
        )
    else
        Brush.verticalGradient(
            0f to Color.White.copy(alpha = 0.55f),
            0.4f to Color.White.copy(alpha = 0.10f),
            1f to Color.Transparent
        )
    val borderBrush = if (isDark)
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                Color.White.copy(alpha = borderAlpha * 0.3f)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 800f)
        )
    else
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha + 0.20f),
                Color.White.copy(alpha = borderAlpha * 0.5f)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 800f)
        )

    Box(
        modifier = modifier
            .clip(shape)
            .background(tint, shape)
            .background(highlight, shape)
            .border(BorderStroke(1.dp, borderBrush), shape)
    ) {
        content()
    }
}

private fun Color.luminance(): Float =
    0.2126f * red + 0.7152f * green + 0.0722f * blue
