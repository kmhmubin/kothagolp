package com.kmhmubin.kothagolp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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
import com.kmhmubin.kothagolp.domain.model.AppSettings
import com.kmhmubin.kothagolp.domain.model.CustomThemeColors
import com.kmhmubin.kothagolp.domain.model.ThemeMode

/**
 * Kothagolp brand dark theme — Ink Blue
 */
private val DarkColorScheme = darkColorScheme(
    primary = InkPrimary,
    onPrimary = InkOnPrimary,
    primaryContainer = InkPrimaryContainer,
    onPrimaryContainer = InkOnPrimaryContainer,
    secondary = InkSecondary,
    onSecondary = InkOnSecondary,
    secondaryContainer = InkSecondaryContainer,
    onSecondaryContainer = InkOnSecondaryContainer,
    tertiary = InkTertiary,
    onTertiary = InkOnTertiary,
    tertiaryContainer = InkTertiaryContainer,
    onTertiaryContainer = InkOnTertiaryContainer,
    background = InkDarkBackground,
    onBackground = InkDarkOnBackground,
    surface = InkDarkBackground,
    onSurface = InkDarkOnBackground,
    surfaceVariant = InkDarkSurfaceVariant,
    onSurfaceVariant = InkDarkOnSurfaceVariant,
    surfaceContainerLowest = InkDarkSurfaceLowest,
    surfaceContainerLow = InkDarkSurfaceLow,
    surfaceContainer = InkDarkSurface,
    surfaceContainerHigh = InkDarkSurfaceHigh,
    surfaceContainerHighest = InkDarkSurfaceHighest,
    inverseSurface = InkDarkOnBackground,
    inverseOnSurface = InkDarkBackground,
    inversePrimary = InkLightPrimary,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = InkDarkOutline,
    outlineVariant = InkDarkOutlineVariant,
    scrim = Color.Black
)

/**
 * AMOLED variant — pure black bg + Ink Blue accents
 */
private val AmoledDarkColorScheme = DarkColorScheme.copy(
    background = Color.Black,
    surface = Color(0xFF05060A),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = InkDarkSurfaceLowest,
    surfaceContainer = InkDarkSurfaceLow,
    surfaceContainerHigh = InkDarkSurface,
    surfaceContainerHighest = InkDarkSurfaceHigh
)

/**
 * Light theme — Ink Blue on neutral-blue-tinted light surfaces
 */
private val LightColorScheme = lightColorScheme(
    primary = InkLightPrimary,
    onPrimary = InkLightOnPrimary,
    primaryContainer = InkLightPrimaryContainer,
    onPrimaryContainer = InkLightOnPrimaryContainer,
    secondary = InkLightSecondary,
    onSecondary = InkLightOnSecondary,
    secondaryContainer = InkLightSecondaryContainer,
    onSecondaryContainer = InkLightOnSecondaryContainer,
    tertiary = InkLightTertiary,
    onTertiary = InkLightOnTertiary,
    tertiaryContainer = InkLightTertiaryContainer,
    onTertiaryContainer = InkLightOnTertiaryContainer,
    background = InkLightBackground,
    onBackground = InkLightOnBackground,
    surface = InkLightBackground,
    onSurface = InkLightOnBackground,
    surfaceVariant = InkLightSurfaceVariant,
    onSurfaceVariant = InkLightOnSurfaceVariant,
    surfaceContainerLowest = InkLightSurfaceLowest,
    surfaceContainerLow = InkLightSurfaceLow,
    surfaceContainer = InkLightSurface,
    surfaceContainerHigh = InkLightSurfaceHigh,
    surfaceContainerHighest = InkLightSurfaceHighest,
    inverseSurface = InkLightOnBackground,
    inverseOnSurface = Color(0xFFEFF0F7),
    inversePrimary = InkPrimary,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = InkLightOutline,
    outlineVariant = InkLightOutlineVariant
)

/**
 * Creates a custom dark color scheme based on user-selected colors
 */
private fun createCustomDarkColorScheme(colors: CustomThemeColors): ColorScheme {
    val primary = Color(colors.primaryColor)
    val secondary = Color(colors.secondaryColor)
    val background = Color(colors.backgroundColor)
    val surface = Color(colors.surfaceColor)

    // Generate derived colors
    val onPrimary = getContrastColor(primary)
    val onSecondary = getContrastColor(secondary)
    val onBackground = getContrastColor(background)
    val onSurface = getContrastColor(surface)

    // Create lighter/darker variants
    val primaryContainer = primary.copy(alpha = 0.3f).compositeOver(background)
    val secondaryContainer = secondary.copy(alpha = 0.2f).compositeOver(surface)
    val surfaceVariant = blendColors(surface, Color.White, 0.05f)
    val surfaceContainer = blendColors(surface, Color.White, 0.08f)
    val surfaceContainerHigh = blendColors(surface, Color.White, 0.12f)
    val surfaceContainerHighest = blendColors(surface, Color.White, 0.16f)
    val surfaceContainerLow = blendColors(surface, background, 0.5f)

    return darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondary,
        tertiary = secondary.copy(alpha = 0.8f),
        onTertiary = onSecondary,
        tertiaryContainer = secondaryContainer,
        onTertiaryContainer = secondary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurface.copy(alpha = 0.7f),
        surfaceContainerLowest = background,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        inverseSurface = onSurface,
        inverseOnSurface = surface,
        inversePrimary = primary.copy(alpha = 0.8f),
        error = Error,
        onError = Color.White,
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = surfaceContainerHigh,
        outlineVariant = surfaceContainer,
        scrim = Color.Black
    )
}

/**
 * Creates a custom light color scheme based on user-selected colors
 */
private fun createCustomLightColorScheme(colors: CustomThemeColors): ColorScheme {
    val primary = Color(colors.primaryColor)
    val secondary = Color(colors.secondaryColor)
    val bgColor = Color(colors.backgroundColor)
    val surfColor = Color(colors.surfaceColor)

    // Use preset bg/surface if they look light, otherwise fall back to defaults
    val bgLuminance = 0.299 * bgColor.red + 0.587 * bgColor.green + 0.114 * bgColor.blue
    val background = if (bgLuminance > 0.5) bgColor else Color(0xFFFAFAFA)
    val surface = if (bgLuminance > 0.5) surfColor else Color.White

    val onBackground = getContrastColor(background)
    val onSurface = getContrastColor(surface)
    val onPrimary = getContrastColor(primary)
    val onSecondary = getContrastColor(secondary)

    val primaryContainer = primary.copy(alpha = 0.12f).compositeOver(surface)
    val secondaryContainer = secondary.copy(alpha = 0.12f).compositeOver(surface)
    val surfaceVariant = blendColors(surface, primary, 0.05f)

    return lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = primary.darken(0.3f),
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondary.darken(0.3f),
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurface.copy(alpha = 0.7f),
        error = Error,
        onError = Color.White,
        outline = blendColors(surface, onSurface, 0.3f),
        outlineVariant = blendColors(surface, onSurface, 0.15f)
    )
}

/**
 * Utility function to get contrasting color (black or white)
 */
private fun getContrastColor(color: Color): Color {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return if (luminance > 0.5) Color.Black else Color.White
}

/**
 * Blend two colors together
 */
private fun blendColors(color1: Color, color2: Color, ratio: Float): Color {
    val inverseRatio = 1f - ratio
    return Color(
        red = (color1.red * inverseRatio + color2.red * ratio).coerceIn(0f, 1f),
        green = (color1.green * inverseRatio + color2.green * ratio).coerceIn(0f, 1f),
        blue = (color1.blue * inverseRatio + color2.blue * ratio).coerceIn(0f, 1f),
        alpha = 1f
    )
}

/**
 * Darken a color by a given factor
 */
private fun Color.darken(factor: Float): Color {
    return Color(
        red = (red * (1 - factor)).coerceIn(0f, 1f),
        green = (green * (1 - factor)).coerceIn(0f, 1f),
        blue = (blue * (1 - factor)).coerceIn(0f, 1f),
        alpha = alpha
    )
}

/**
 * Composite one color over another
 */
private fun Color.compositeOver(background: Color): Color {
    val fgAlpha = this.alpha
    val bgAlpha = background.alpha
    val outAlpha = fgAlpha + bgAlpha * (1f - fgAlpha)

    return if (outAlpha == 0f) {
        Color.Transparent
    } else {
        Color(
            red = (red * fgAlpha + background.red * bgAlpha * (1f - fgAlpha)) / outAlpha,
            green = (green * fgAlpha + background.green * bgAlpha * (1f - fgAlpha)) / outAlpha,
            blue = (blue * fgAlpha + background.blue * bgAlpha * (1f - fgAlpha)) / outAlpha,
            alpha = outAlpha
        )
    }
}

/**
 * Main theme composable for Kothagolp with settings support
 */
@Composable
fun KothagolpTheme(
    appSettings: AppSettings = AppSettings(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    // Determine if dark theme based on settings
    val useDarkTheme = when (appSettings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Select color scheme
    val colorScheme = when {
        // Custom theme takes priority (but not over dynamic color if that's enabled)
        appSettings.useCustomTheme && !appSettings.useDynamicColor -> {
            if (useDarkTheme) {
                val customScheme = createCustomDarkColorScheme(appSettings.customThemeColors)
                if (appSettings.amoledBlack) {
                    customScheme.copy(
                        background = Color.Black,
                        surface = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color(0xFF0A0A0A),
                        surfaceContainer = Color(0xFF121212),
                        surfaceContainerHigh = Color(0xFF1A1A1A),
                        surfaceContainerHighest = Color(0xFF222222)
                    )
                } else {
                    customScheme
                }
            } else {
                createCustomLightColorScheme(appSettings.customThemeColors)
            }
        }
        // Dynamic color (Material You)
        appSettings.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        // AMOLED black (only in dark mode)
        useDarkTheme && appSettings.amoledBlack -> AmoledDarkColorScheme
        // Standard dark
        useDarkTheme -> DarkColorScheme
        // Light
        else -> LightColorScheme
    }

    // Update status/nav bar icon appearance for current theme
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    // Provide dimensions based on density
    ProvideDimensions(density = appSettings.uiDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KothagolpTypography,
            content = content
        )
    }
}

/**
 * Legacy theme for backward compatibility (always dark)
 */
@Composable
fun KothagolpTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    KothagolpTheme(
        appSettings = AppSettings(
            themeMode = if (darkTheme) ThemeMode.DARK else ThemeMode.LIGHT
        ),
        content = content
    )
}