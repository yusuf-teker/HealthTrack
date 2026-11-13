package com.yusufteker.healthtrack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.yusufteker.healthtrack.theme.HealthColors
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

class Phase2ThemeAndReminderTest {

    private fun contrastRatio(foreground: Color, background: Color): Double {
        val l1 = foreground.luminance().toDouble() + 0.05
        val l2 = background.luminance().toDouble() + 0.05
        return max(l1, l2) / min(l1, l2)
    }

    @Test
    fun lightThemeTertiaryAndErrorTokensMeetWcagContrast() {
        val errorOnLightSurface = contrastRatio(HealthColors.ErrorLight, HealthColors.SurfaceLight)
        val onErrorContrast = contrastRatio(HealthColors.OnErrorLight, HealthColors.ErrorLight)
        val tertiaryOnLightSurface = contrastRatio(HealthColors.TertiaryLight, HealthColors.SurfaceLight)
        val onTertiaryContrast = contrastRatio(HealthColors.OnTertiaryLight, HealthColors.TertiaryLight)

        assertTrue(errorOnLightSurface >= 4.5, "ErrorLight on SurfaceLight contrast was $errorOnLightSurface")
        assertTrue(onErrorContrast >= 4.5, "OnErrorLight on ErrorLight contrast was $onErrorContrast")
        assertTrue(tertiaryOnLightSurface >= 4.5, "TertiaryLight on SurfaceLight contrast was $tertiaryOnLightSurface")
        assertTrue(onTertiaryContrast >= 4.5, "OnTertiaryLight on TertiaryLight contrast was $onTertiaryContrast")
    }

    @Test
    fun darkThemeTertiaryAndErrorTokensMeetWcagContrast() {
        val errorOnDarkCard = contrastRatio(HealthColors.Error, HealthColors.SurfaceContainer)
        val onErrorContrast = contrastRatio(HealthColors.OnError, HealthColors.Error)
        val tertiaryOnDarkCard = contrastRatio(HealthColors.Tertiary, HealthColors.SurfaceContainer)
        val onTertiaryContrast = contrastRatio(HealthColors.OnTertiary, HealthColors.Tertiary)

        assertTrue(errorOnDarkCard >= 4.5, "Error on SurfaceContainer contrast was $errorOnDarkCard")
        assertTrue(onErrorContrast >= 4.5, "OnError on Error contrast was $onErrorContrast")
        assertTrue(tertiaryOnDarkCard >= 4.5, "Tertiary on SurfaceContainer contrast was $tertiaryOnDarkCard")
        assertTrue(onTertiaryContrast >= 4.5, "OnTertiary on Tertiary contrast was $onTertiaryContrast")
    }
}
