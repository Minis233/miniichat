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

/**
 * Clean iOS-style flat theme.
 * Pure white / near-black backgrounds, hairline dividers, accent purple for primary.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF6E5CFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FF),
    onPrimaryContainer = Color(0xFF170B5C),
    secondary = Color(0xFF6E5CFF),
    onSecondary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF111114),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFF3F2F7),
    onSurfaceVariant = Color(0xFF6F6E78),
    outline = Color(0xFFE3E1EA),
    outlineVariant = Color(0xFFEEEDF2),
    error = Color(0xFFE34864),
    onError = Color.White
)

// Tightened dark scheme: lighter onSurfaceVariant for legibility, slightly bolder
// surface contrast so subtle UI (chips, dividers, hint text) reads cleanly.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB1A4FF),
    onPrimary = Color(0xFF15093D),
    primaryContainer = Color(0xFF3826A8),
    onPrimaryContainer = Color(0xFFEDE7FF),
    secondary = Color(0xFFB1A4FF),
    onSecondary = Color(0xFF15093D),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F1F7),
    surface = Color(0xFF101013),
    onSurface = Color(0xFFF2F1F7),
    surfaceVariant = Color(0xFF24242C),
    onSurfaceVariant = Color(0xFFC4C2D0),
    outline = Color(0xFF3A3A45),
    outlineVariant = Color(0xFF26262E),
    error = Color(0xFFFF8FA0),
    onError = Color(0xFF3D0011)
)

private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp)
)

@Composable
fun MiniiChatTheme(
    themeMode: String = "system", // system | light | dark
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val ctx = LocalContext.current
            val dyn = if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            // Material You's generated dark scheme can produce very low-contrast
            // onSurfaceVariant against its surface, especially under cool-toned
            // wallpapers. Override the most legibility-critical roles to keep
            // chip labels, hints, and divider tints readable while still using
            // the wallpaper-derived hue for primary / containers.
            if (darkTheme) {
                dyn.copy(
                    background = Color(0xFF000000),
                    onBackground = Color(0xFFF2F1F7),
                    surface = Color(0xFF101013),
                    onSurface = Color(0xFFF2F1F7),
                    surfaceVariant = Color(0xFF24242C),
                    onSurfaceVariant = Color(0xFFC4C2D0),
                    outline = Color(0xFF3A3A45),
                    outlineVariant = Color(0xFF26262E)
                )
            } else dyn
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
