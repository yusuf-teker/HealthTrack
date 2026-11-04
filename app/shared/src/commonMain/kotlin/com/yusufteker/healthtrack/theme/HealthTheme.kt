package com.yusufteker.healthtrack.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalIsDarkTheme = compositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = HealthColors.Primary,
    onPrimary = HealthColors.OnPrimary,
    primaryContainer = HealthColors.PrimaryContainer,
    onPrimaryContainer = HealthColors.OnPrimaryContainer,
    secondary = HealthColors.Secondary,
    onSecondary = HealthColors.OnSecondary,
    secondaryContainer = HealthColors.SecondaryContainer,
    onSecondaryContainer = HealthColors.OnSecondaryContainer,
    tertiary = HealthColors.Tertiary,
    onTertiary = HealthColors.OnTertiary,
    tertiaryContainer = HealthColors.TertiaryContainer,
    onTertiaryContainer = HealthColors.OnTertiaryContainer,
    background = HealthColors.BackgroundLight,
    onBackground = HealthColors.OnBackgroundLight,
    surface = HealthColors.SurfaceLight,
    onSurface = HealthColors.OnSurfaceLight,
    surfaceVariant = HealthColors.SurfaceVariantLight,
    onSurfaceVariant = HealthColors.OnSurfaceVariantLight,
    outline = HealthColors.OutlineLight,
    outlineVariant = HealthColors.OutlineVariantLight,
    error = HealthColors.Error,
    onError = HealthColors.OnError,
    errorContainer = HealthColors.ErrorContainer,
    onErrorContainer = HealthColors.OnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = HealthColors.Primary,
    onPrimary = HealthColors.OnPrimary,
    primaryContainer = HealthColors.PrimaryVariant,
    onPrimaryContainer = HealthColors.PrimaryContainer,
    secondary = HealthColors.Secondary,
    onSecondary = HealthColors.OnSecondary,
    secondaryContainer = HealthColors.SecondaryVariant,
    onSecondaryContainer = HealthColors.SecondaryContainer,
    tertiary = HealthColors.Tertiary,
    onTertiary = HealthColors.OnTertiary,
    tertiaryContainer = HealthColors.TertiaryContainer,
    onTertiaryContainer = HealthColors.OnTertiaryContainer,
    background = HealthColors.BackgroundDark,
    onBackground = HealthColors.OnBackgroundDark,
    surface = HealthColors.SurfaceDark,
    onSurface = HealthColors.OnSurfaceDark,
    surfaceVariant = HealthColors.SurfaceContainerHighest,
    onSurfaceVariant = HealthColors.OnSurfaceVariantDark,
    outline = HealthColors.OutlineDark,
    outlineVariant = HealthColors.OutlineVariantDark,
    error = HealthColors.Error,
    onError = HealthColors.OnError,
    errorContainer = HealthColors.ErrorContainer,
    onErrorContainer = HealthColors.OnErrorContainer
)

@Composable
fun HealthTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

