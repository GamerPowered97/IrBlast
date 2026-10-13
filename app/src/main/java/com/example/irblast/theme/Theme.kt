package com.example.irblast.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.irblast.data.settings.ThemeMode

val IrBlastDarkColorScheme = darkColorScheme(
    primary = IrPrimary,
    onPrimary = IrOnPrimary,
    primaryContainer = IrPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = IrSecondary,
    secondaryContainer = IrSecondaryContainer,
    tertiary = IrTertiary,
    background = IrBackground,
    surface = IrSurface,
    surfaceVariant = IrSurfaceVariant,
    outline = IrOutline,
    error = IrError,
    onBackground = IrOnSurface,
    onSurface = IrOnSurface,
    onSurfaceVariant = IrOnSurfaceVariant
)

val IrBlastLightColorScheme = lightColorScheme(
    primary = IrLightPrimary,
    background = IrLightBackground,
    surface = IrLightSurface,
    surfaceVariant = IrLightSurfaceVariant,
    onBackground = IrLightOnSurface,
    onSurface = IrLightOnSurface,
    error = IrError
)

@Composable
fun IrBlastTheme(
    themeMode: ThemeMode = ThemeMode.IRBLAST_DARK,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
