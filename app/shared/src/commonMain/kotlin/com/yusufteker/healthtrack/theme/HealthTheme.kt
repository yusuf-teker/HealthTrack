package com.yusufteker.healthtrack.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

val LocalIsDarkTheme = compositionLocalOf { false }
val LocalThemeAccent = compositionLocalOf { AppThemeColor.TEAL }
val LocalBottomBarVisibility = compositionLocalOf<MutableState<Boolean>> { mutableStateOf(true) }

@Composable
fun HealthTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeAccent: AppThemeColor = AppThemeColor.TEAL,
    content: @Composable () -> Unit
) {
    HealthColors.currentAccent = themeAccent

    val lightScheme = lightColorScheme(
        primary = themeAccent.primary,
        onPrimary = themeAccent.onPrimary,
        primaryContainer = themeAccent.primaryContainer,
        onPrimaryContainer = themeAccent.onPrimaryContainer,
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

    val darkScheme = darkColorScheme(
        primary = themeAccent.primary,
        onPrimary = themeAccent.onPrimary,
        primaryContainer = themeAccent.primaryVariant,
        onPrimaryContainer = themeAccent.primaryContainer,
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

    val colorScheme = if (darkTheme) darkScheme else lightScheme

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalThemeAccent provides themeAccent
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

