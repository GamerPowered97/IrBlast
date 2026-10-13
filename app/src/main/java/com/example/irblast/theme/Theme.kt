package com.example.irblast.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.irblast.data.settings.ThemeMode

val IrBlastDarkColorScheme = darkColorScheme(
    primary = IrPrimary,
    onPrimary = IrOnPrimary,
    primaryContainer = IrPrimaryContainer,
    onPrimaryContainer = IrTertiary,
    secondary = IrSecondary,
    onSecondary = Color(0xFF002022),
    secondaryContainer = IrSecondaryContainer,
    onSecondaryContainer = IrSecondary,
    tertiary = IrTertiary,
    onTertiary = Color(0xFF1F1247),
    background = IrBackground,
    surface = IrSurface,
    surfaceVariant = IrSurfaceVariant,
    outline = IrOutline,
    outlineVariant = IrOutlineVariant,
    error = IrError,
    onError = Color.White,
    onBackground = IrOnSurface,
    onSurface = IrOnSurface,
    onSurfaceVariant = IrOnSurfaceVariant
)

val IrBlastLightColorScheme = lightColorScheme(
    primary = IrLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E0FF),
    onPrimaryContainer = Color(0xFF201265),
    secondary = Color(0xFF008985),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0F8F6),
    onSecondaryContainer = Color(0xFF003836),
    tertiary = Color(0xFF6B58A6),
    onTertiary = Color.White,
    background = IrLightBackground,
    surface = IrLightSurface,
    surfaceVariant = IrLightSurfaceVariant,
    outline = IrLightOutline,
    outlineVariant = Color(0xFFE2E6F2),
    onBackground = IrLightOnSurface,
    onSurface = IrLightOnSurface,
    onSurfaceVariant = IrLightOnSurfaceVariant,
    error = IrError,
    onError = Color.White
)

@Composable
fun IrBlastTheme(
    themeMode: ThemeMode = ThemeMode.IRBLAST_DARK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.DYNAMIC -> systemInDark
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.IRBLAST_DARK -> true
    }

    val colorScheme = when (themeMode) {
        ThemeMode.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemInDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemInDark) IrBlastDarkColorScheme else IrBlastLightColorScheme
            }
        }
        ThemeMode.SYSTEM -> {
            if (systemInDark) IrBlastDarkColorScheme else IrBlastLightColorScheme
        }
        ThemeMode.IRBLAST_DARK -> {
            IrBlastDarkColorScheme
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
