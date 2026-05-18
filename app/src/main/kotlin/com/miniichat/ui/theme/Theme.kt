package com.miniichat.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF7C5CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E1FF),
    onPrimaryContainer = Color(0xFF1B1147),
    secondary = Color(0xFF6E5CB6),
    onSecondary = Color.White,
    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFF1F1F5),
    onSurfaceVariant = Color(0xFF4A4A52),
    outline = Color(0xFFD8D8DE),
    outlineVariant = Color(0xFFE6E6EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB7A6FF),
    onPrimary = Color(0xFF1B1147),
    primaryContainer = Color(0xFF3A2A8A),
    onPrimaryContainer = Color(0xFFE8E1FF),
    secondary = Color(0xFFB1A1E5),
    onSecondary = Color(0xFF1B1147),
    background = Color(0xFF101013),
    onBackground = Color(0xFFEDEDF2),
    surface = Color(0xFF17171B),
    onSurface = Color(0xFFEDEDF2),
    surfaceVariant = Color(0xFF24242A),
    onSurfaceVariant = Color(0xFFB7B7BF),
    outline = Color(0xFF35353C),
    outlineVariant = Color(0xFF2A2A30),
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun MiniiChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
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
