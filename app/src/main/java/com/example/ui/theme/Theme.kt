package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CookPrimaryDark,
    onPrimary = CookOnPrimaryDark,
    primaryContainer = CookPrimaryContainerDark,
    onPrimaryContainer = CookOnPrimaryContainerDark,
    secondary = CookSecondaryDark,
    onSecondary = CookOnSecondaryDark,
    secondaryContainer = CookSecondaryContainerDark,
    onSecondaryContainer = CookOnSecondaryContainerDark,
    tertiary = CookTertiaryDark,
    onTertiary = CookOnTertiaryDark,
    tertiaryContainer = CookTertiaryContainerDark,
    onTertiaryContainer = CookOnTertiaryContainerDark,
    background = CookBackgroundDark,
    onBackground = CookOnBackgroundDark,
    surface = CookSurfaceDark,
    onSurface = CookOnSurfaceDark,
    surfaceVariant = CookSurfaceVariantDark,
    onSurfaceVariant = CookOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = CookPrimaryLight,
    onPrimary = CookOnPrimaryLight,
    primaryContainer = CookPrimaryContainerLight,
    onPrimaryContainer = CookOnPrimaryContainerLight,
    secondary = CookSecondaryLight,
    onSecondary = CookOnSecondaryLight,
    secondaryContainer = CookSecondaryContainerLight,
    onSecondaryContainer = CookOnSecondaryContainerLight,
    tertiary = CookTertiaryLight,
    onTertiary = CookOnTertiaryLight,
    tertiaryContainer = CookTertiaryContainerLight,
    onTertiaryContainer = CookOnTertiaryContainerLight,
    background = CookBackgroundLight,
    onBackground = CookOnBackgroundLight,
    surface = CookSurfaceLight,
    onSurface = CookOnSurfaceLight,
    surfaceVariant = CookSurfaceVariantLight,
    onSurfaceVariant = CookOnSurfaceVariantLight
)

@Composable
fun CookTonightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our culinary palette by default for strong branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
