package com.androidide.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF4FC3F7),
    onPrimary        = Color(0xFF003549),
    primaryContainer = Color(0xFF004D6A),
    secondary        = Color(0xFF569CD6),
    background       = Color(0xFF1E1E1E),
    surface          = Color(0xFF252526),
    surfaceVariant   = Color(0xFF2D2D2D),
    onBackground     = Color(0xFFD4D4D4),
    onSurface        = Color(0xFFCCCCCC),
    outline          = Color(0xFF3C3C3C),
    error            = Color(0xFFF48771),
)

private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF007ACC),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0EEFF),
    secondary        = Color(0xFF0000FF),
    background       = Color(0xFFFFFFFF),
    surface          = Color(0xFFF3F3F3),
    surfaceVariant   = Color(0xFFEEEEEE),
    onBackground     = Color(0xFF000000),
    onSurface        = Color(0xFF333333),
    outline          = Color(0xFFCECECE),
)

@Composable
fun AndroidIDETheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

object EditorColors {
    val background       = Color(0xFF1E1E1E)
    val lineNumber       = Color(0xFF858585)
    val activeLine       = Color(0xFF2A2D2E)
    val selection        = Color(0xFF264F78)
    val keyword          = Color(0xFF569CD6)
    val string           = Color(0xFFCE9178)
    val comment          = Color(0xFF6A9955)
    val function         = Color(0xFFDCDCAA)
    val number           = Color(0xFFB5CEA8)
    val type             = Color(0xFF4EC9B0)
    val variable         = Color(0xFF9CDCFE)
    val errorUnderline   = Color(0xFFF44747)
    val warningUnderline = Color(0xFFCCA700)
}
