package com.yusufteker.healthtrack.theme

import androidx.compose.ui.graphics.Color

/**
 * Available color theme presets for HealthTrack.
 */
enum class AppThemeColor(
    val id: String,
    val title: String,
    val primary: Color,
    val primaryGlow: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val gradient: List<Color>
) {
    TEAL(
        id = "teal",
        title = "Bio Turkuaz",
        primary = Color(0xFF06D6A0),
        primaryGlow = Color(0xFF47F3BB),
        primaryVariant = Color(0xFF27E0A9),
        onPrimary = Color(0xFF003827),
        primaryContainer = Color(0xFF00573F),
        onPrimaryContainer = Color(0xFF54FDC4),
        gradient = listOf(Color(0xFF47F3BB), Color(0xFF06D6A0))
    ),
    OCEAN(
        id = "ocean",
        title = "Okyanus Mavisi",
        primary = Color(0xFF0284C7),
        primaryGlow = Color(0xFF38BDF8),
        primaryVariant = Color(0xFF0369A1),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF0C4A6E),
        onPrimaryContainer = Color(0xFFBAE6FD),
        gradient = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
    ),
    PURPLE(
        id = "purple",
        title = "Asil Mor",
        primary = Color(0xFF9333EA),
        primaryGlow = Color(0xFFC084FC),
        primaryVariant = Color(0xFF7E22CE),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF581C87),
        onPrimaryContainer = Color(0xFFF3E8FF),
        gradient = listOf(Color(0xFFC084FC), Color(0xFF7E22CE))
    ),
    CORAL(
        id = "coral",
        title = "Günbatımı Mercan",
        primary = Color(0xFFEA580C),
        primaryGlow = Color(0xFFFB923C),
        primaryVariant = Color(0xFFC2410C),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF7C2D12),
        onPrimaryContainer = Color(0xFFFFEDD5),
        gradient = listOf(Color(0xFFFB923C), Color(0xFFEA580C))
    ),
    EMERALD(
        id = "emerald",
        title = "Doğal Zümrüt",
        primary = Color(0xFF059669),
        primaryGlow = Color(0xFF34D399),
        primaryVariant = Color(0xFF047857),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF064E3B),
        onPrimaryContainer = Color(0xFFD1FAE5),
        gradient = listOf(Color(0xFF34D399), Color(0xFF047857))
    );

    companion object {
        fun fromId(id: String?): AppThemeColor {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: OCEAN
        }
    }
}

/**
 * Bio-Telemetry Luxury Wellness & Planora Health Color Palette.
 * Faithfully matches the Stitch Luxury Telemetry design system while keeping Planora harmony.
 */
object HealthColors {
    var currentAccent: AppThemeColor = AppThemeColor.OCEAN

    // ── Primary (Dynamically reflects selected AppThemeColor) ────
    val Primary: Color get() = currentAccent.primary
    val PrimaryGlow: Color get() = currentAccent.primaryGlow
    val PrimaryVariant: Color get() = currentAccent.primaryVariant
    val OnPrimary: Color get() = currentAccent.onPrimary
    val PrimaryContainer: Color get() = currentAccent.primaryContainer
    val OnPrimaryContainer: Color get() = currentAccent.onPrimaryContainer

    // ── Secondary (Biometric Lavender / Protein) ──────────────
    val Secondary = Color(0xFFD0BCFF)
    val SecondaryVariant = Color(0xFF8B5CF6)
    val SecondaryAccent = Color(0xFFC084FC)
    val OnSecondary = Color(0xFF3C0091)
    val SecondaryContainer = Color(0xFF571BC1)
    val OnSecondaryContainer = Color(0xFFE9DDFF)

    // ── Tertiary (Metabolic Coral / Workout Strain / Carbs) ───
    val Tertiary = Color(0xFFFFA87A)
    val TertiaryVariant = Color(0xFFF97316)
    val TertiaryGlow = Color(0xFFFFCEB7)
    val OnTertiary = Color(0xFF552100)
    val TertiaryContainer = Color(0xFF803500)
    val OnTertiaryContainer = Color(0xFFFFDBCA)

    // ── Semantic Nutrient Group Colors ───────────────────────
    val NutrientProtein = Color(0xFFC084FC)   // Lavender / Purple
    val NutrientCarbs = Color(0xFFFB923C)     // Coral / Orange
    val NutrientLipids = Color(0xFFFDE047)    // Soft Gold / Amber
    val NutrientFiber = Color(0xFF34D399)     // Emerald Mint
    val NutrientVitC = Color(0xFF38BDF8)      // Cyan Sky
    val NutrientVitD = Color(0xFF0284C7)      // Ocean Blue
    val NutrientVitB12 = Color(0xFF818CF8)    // Indigo Violet
    val NutrientIron = Color(0xFFF43F5E)      // Crimson Rose
    val NutrientCalcium = Color(0xFFA78BFA)   // Pastel Lilac
    val NutrientMagnesium = Color(0xFF2DD4BF) // Deep Teal

    // ── Light Surfaces & Backgrounds ─────────────────────────
    val BackgroundLight = Color(0xFFF8F9FE)
    val OnBackgroundLight = Color(0xFF111318)
    val SurfaceLight = Color(0xFFFFFFFF)
    val OnSurfaceLight = Color(0xFF111318)
    val SurfaceVariantLight = Color(0xFFF0F0F8)
    val OnSurfaceVariantLight = Color(0xFF55605A)
    val OutlineLight = Color(0xFFD4D4DE)
    val OutlineVariantLight = Color(0xFFE8E8F0)

    // ── Dark Surfaces (Smoked Obsidian & Deep Slate) ─────────
    val BackgroundDark = Color(0xFF111318)             // Deep Obsidian Slate
    val OnBackgroundDark = Color(0xFFE2E2E9)
    val SurfaceDark = Color(0xFF111318)
    val OnSurfaceDark = Color(0xFFE2E2E9)
    val SurfaceContainerLowest = Color(0xFF0C0E13)
    val SurfaceContainerLow = Color(0xFF1A1B21)
    val SurfaceContainer = Color(0xFF1E1F25)          // Card Base
    val SurfaceContainerHigh = Color(0xFF282A2F)
    val SurfaceContainerHighest = Color(0xFF33353A)
    val OnSurfaceVariantDark = Color(0xFFBACAC1)       // Muted Sage Gray
    val OutlineDark = Color(0xFF3B4A43)
    val OutlineVariantDark = Color(0xFF232B27)

    // ── Error ────────────────────────────────────────────────
    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)

    // ── Gradients ────────────────────────────────────────────
    val GradientTeal = listOf(Color(0xFF47F3BB), Color(0xFF06D6A0), Color(0xFF27E0A9))
    val GradientPrimary: List<Color> get() = currentAccent.gradient
    val GradientProtein = listOf(Color(0xFF8B5CF6), Color(0xFFC084FC))
    val GradientCarbs = listOf(Color(0xFFF97316), Color(0xFFFB923C))
    val GradientLipids = listOf(Color(0xFFEAB308), Color(0xFFFDE047))
    val GradientFiber = listOf(Color(0xFF10B981), Color(0xFF34D399))
    val GradientCyan = listOf(Color(0xFF06B6D4), Color(0xFF38BDF8))
    val GradientBurn = listOf(Color(0xFFFFA87A), Color(0xFF06D6A0), Color(0xFF47F3BB))
}
