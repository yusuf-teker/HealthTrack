package com.yusufteker.healthtrack.theme

import androidx.compose.runtime.Composable

/**
 * Updates system status bar and navigation bar icon contrast to match the active app theme.
 */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)

