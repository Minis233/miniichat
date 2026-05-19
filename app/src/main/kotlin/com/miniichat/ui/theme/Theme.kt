package com.miniichat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * Liquid-glass palette.
 * Iridescent pastels in light mode, deep neon-tinted navy in dark.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF6E5CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E0FF),
    onPrimaryContainer = Color(0xFF170B5C),
    secondary = Color(0xFF00B59C),
    onSecondary = Color.White,
    background = Color(0xFFF6F4FF),
    onBackground = Color(0xFF0E0D14),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E0D14),
    surfaceVariant = Color(0xFFEDEAF7),
    onSurfaceVariant = Color(0xFF504C66),
    outline = Color(0xFFCDC8DE),
    outlineVariant = Color(0xFFE6E2F2),
    error = Color(0xFFE34864),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB1A4FF),
    onPrimary = Color(0xFF15093D),
    primaryContainer = Color(0xFF2C1F70),
    onPrimaryContainer = Color(0xFFE6E0FF),
    secondary = Color(0xFF55E6CB),
    onSecondary = Color(0xFF00342B),
    background = Color(0xFF0B0B12),
    onBackground = Color(0xFFEEEAFB),
    surface = Color(0xFF13131C),
    onSurface = Color(0xFFEEEAFB),
    surfaceVariant = Color(0xFF1E1E2C),
    onSurfaceVariant = Color(0xFFB6B0CB),
    outline = Color(0xFF3A3A4F),
    outlineVariant = Color(0xFF272736),
    error = Color(0xFFFF8FA0),
    onError = Color(0xFF3D0011)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

@Composable
fun MiniiChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
    )
}

/** Iridescent gradient + blurred color blobs covering the whole window. */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val baseBrush = if (dark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF11061F),
                Color(0xFF09102C),
                Color(0xFF180A26)
            ),
            start = Offset(0f, 0f),
            end = Offset(1500f, 3000f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFEDE6FF),
                Color(0xFFE0F4FF),
                Color(0xFFFFE9F2),
                Color(0xFFFFF6E0)
            ),
            start = Offset(0f, 0f),
            end = Offset(1500f, 3000f)
        )
    }

    Box(modifier = modifier.fillMaxSize().background(baseBrush)) {
        DecorBlobs(dark)
        content()
    }
}

@Composable
private fun DecorBlobs(dark: Boolean) {
    val (a, b, c) = if (dark) Triple(
        Color(0xFF7C5CFF).copy(alpha = 0.55f),
        Color(0xFF00B59C).copy(alpha = 0.40f),
        Color(0xFFFF6B9D).copy(alpha = 0.40f)
    ) else Triple(
        Color(0xFFB59FFF).copy(alpha = 0.70f),
        Color(0xFF8BD8FF).copy(alpha = 0.65f),
        Color(0xFFFFB6A0).copy(alpha = 0.65f)
    )

    val blurDp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 100.dp else 0.dp

    Box(
        modifier = Modifier
            .size(420.dp)
            .offset(x = (-120).dp, y = (-80).dp)
            .clip(CircleShape)
            .blur(blurDp)
            .background(a)
    )
    Box(
        modifier = Modifier
            .size(360.dp)
            .offset(x = 240.dp, y = 200.dp)
            .clip(CircleShape)
            .blur(blurDp)
            .background(b)
    )
    Box(
        modifier = Modifier
            .size(320.dp)
            .offset(x = (-80).dp, y = 540.dp)
            .clip(CircleShape)
            .blur(blurDp)
            .background(c)
    )
    Box(
        modifier = Modifier
            .size(380.dp)
            .offset(x = 200.dp, y = 880.dp)
            .clip(CircleShape)
            .blur(blurDp)
            .background(a.copy(alpha = (a.alpha * 0.7f)))
    )
}
