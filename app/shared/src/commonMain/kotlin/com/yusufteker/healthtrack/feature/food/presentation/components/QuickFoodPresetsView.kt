package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.CustomFoodItem
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick

enum class FoodCategory(val labelTr: String) {
    ALL("Tümü"),
    FAVORITES("★ Favorilerim"),
    CUSTOM("🏷️ Envanterim"),
    PROTEIN("Protein"),
    BREAKFAST("Kahvaltılık"),
    CARBS("Karbonhidrat"),
    FRUITS_NUTS("Meyve & Kuruyemiş"),
    HEALTHY_FATS("Sağlıklı Yağlar")
}

data class FoodPreset(
    val id: String,
    val nameTr: String,
    val nameEn: String,
    val unitStep: Int,
    val unitLabelTr: String,
    val unitLabelEn: String,
    val baseCalories: Int,
    val baseProteinG: Double,
    val baseCarbsG: Double,
    val baseFatG: Double,
    val baseFiberG: Double = 0.0,
    val category: FoodCategory = FoodCategory.ALL,
    val tags: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val brand: String? = null,
    val iconKey: String? = null,
    val micronutrients: List<Micronutrient> = emptyList(),
    val isMix: Boolean = false,
    val ingredientCount: Int = 0
)

/**
 * 100% verified authoritative nutrition values for frequent staple foods,
 * Turkish traditional staples, high-protein foods, and daily essentials.
 */
val VERIFIED_FOOD_PRESETS = listOf(
    // ═════════════════════════════════════════════════════════════════
    // PROTEİN KAYNAKLARI & ET / TAVUK / BALIK
    // ═════════════════════════════════════════════════════════════════
    FoodPreset(
        id = "chicken",
        nameTr = "Tavuk Göğsü",
        nameEn = "Chicken Breast",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 165,
        baseProteinG = 31.0,
        baseCarbsG = 0.0,
        baseFatG = 3.6,
        category = FoodCategory.PROTEIN,
        tags = listOf("tavuk", "göğüs", "protein", "et", "ızgara", "haşlama")
    ),
    FoodPreset(
        id = "chicken_thigh",
        nameTr = "Tavuk But (Derisiz Izgara)",
        nameEn = "Chicken Thigh (Skinless)",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 175,
        baseProteinG = 24.0,
        baseCarbsG = 0.0,
        baseFatG = 8.5,
        category = FoodCategory.PROTEIN,
        tags = listOf("tavuk", "but", "ızgara", "protein", "et")
    ),
    FoodPreset(
        id = "turkey",
        nameTr = "Hindi Göğsü",
        nameEn = "Turkey Breast",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 135,
        baseProteinG = 30.0,
        baseCarbsG = 0.0,
        baseFatG = 1.5,
        category = FoodCategory.PROTEIN,
        tags = listOf("hindi", "göğüs", "protein", "et", "diyet")
    ),
    FoodPreset(
        id = "kofte",
        nameTr = "Izgara Köfte",
        nameEn = "Grilled Meatballs",
        unitStep = 100,
        unitLabelTr = "g (3-4 Adet)",
        unitLabelEn = "g (3-4 pcs)",
        baseCalories = 220,
        baseProteinG = 18.0,
        baseCarbsG = 3.5,
        baseFatG = 15.0,
        category = FoodCategory.PROTEIN,
        tags = listOf("köfte", "ızgara", "et", "dana", "öğle", "akşam")
    ),
    FoodPreset(
        id = "beef",
        nameTr = "Yağsız Dana Biftek / Kıyma",
        nameEn = "Lean Beef",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 215,
        baseProteinG = 26.0,
        baseCarbsG = 0.0,
        baseFatG = 12.0,
        category = FoodCategory.PROTEIN,
        tags = listOf("et", "dana", "kıyma", "biftek", "protein", "bonfile")
    ),
    FoodPreset(
        id = "salmon",
        nameTr = "Somon Balığı (Fırın/Izgara)",
        nameEn = "Salmon",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 208,
        baseProteinG = 20.0,
        baseCarbsG = 0.0,
        baseFatG = 13.0,
        category = FoodCategory.PROTEIN,
        tags = listOf("balık", "somon", "omega3", "protein", "fırın")
    ),
    FoodPreset(
        id = "tuna",
        nameTr = "Ton Balığı (Konserve Süzme)",
        nameEn = "Canned Tuna",
        unitStep = 80,
        unitLabelTr = "g (1 Kutu)",
        unitLabelEn = "g (1 Can)",
        baseCalories = 104,
        baseProteinG = 24.0,
        baseCarbsG = 0.0,
        baseFatG = 0.8,
        category = FoodCategory.PROTEIN,
        tags = listOf("ton", "balık", "konserve", "protein", "salata")
    ),
    FoodPreset(
        id = "whey",
        nameTr = "Whey Protein Tozu",
        nameEn = "Whey Protein",
        unitStep = 30,
        unitLabelTr = "g (1 Ölçek)",
        unitLabelEn = "g (1 Scoop)",
        baseCalories = 120,
        baseProteinG = 24.0,
        baseCarbsG = 2.5,
        baseFatG = 1.5,
        category = FoodCategory.PROTEIN,
        tags = listOf("protein tozu", "whey", "shake", "supplement", "toz")
    ),
    FoodPreset(
        id = "protein_milk_500",
        nameTr = "Protein Süt (Kakaolu/Sade)",
        nameEn = "High-Protein Milk (500ml)",
        unitStep = 500,
        unitLabelTr = "ml (1 Kutu)",
        unitLabelEn = "ml (1 Bottle)",
        baseCalories = 250,
        baseProteinG = 26.0,
        baseCarbsG = 18.0,
        baseFatG = 1.5,
        category = FoodCategory.PROTEIN,
        tags = listOf("protein süt", "süt", "pınar fit", "içim fit", "kakaolu", "vanilyalı", "shake", "protein")
    ),
    FoodPreset(
        id = "protein_milk_250",
        nameTr = "Protein Süt (1 Bardak)",
        nameEn = "High-Protein Milk (250ml)",
        unitStep = 250,
        unitLabelTr = "ml (1 Bardak)",
        unitLabelEn = "ml (1 Glass)",
        baseCalories = 125,
        baseProteinG = 13.0,
        baseCarbsG = 9.0,
        baseFatG = 0.8,
        category = FoodCategory.PROTEIN,
        tags = listOf("protein süt", "süt", "bardak", "protein", "içecek")
    ),
    FoodPreset(
        id = "protein_bar",
        nameTr = "Protein Bar",
        nameEn = "Protein Bar",
        unitStep = 50,
        unitLabelTr = "g (1 Adet)",
        unitLabelEn = "g (1 Bar)",
        baseCalories = 180,
        baseProteinG = 15.0,
        baseCarbsG = 15.0,
        baseFatG = 5.5,
        baseFiberG = 6.0,
        category = FoodCategory.PROTEIN,
        tags = listOf("protein bar", "bar", "atıştırmalık", "züber", "fellah", "fıstıklı")
    ),

    // ═════════════════════════════════════════════════════════════════
    // KAHVALTILIK, YUMURTA & SÜT ÜRÜNLERİ
    // ═════════════════════════════════════════════════════════════════
    FoodPreset(
        id = "egg_m",
        nameTr = "Haşlanmış Yumurta (O)",
        nameEn = "Boiled Egg (M)",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 63,
        baseProteinG = 5.5,
        baseCarbsG = 0.3,
        baseFatG = 4.2,
        category = FoodCategory.BREAKFAST,
        tags = listOf("yumurta", "kahvaltı", "protein", "haşlanmış", "orta")
    ),
    FoodPreset(
        id = "egg_l",
        nameTr = "Haşlanmış Yumurta (B)",
        nameEn = "Boiled Egg (L)",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 72,
        baseProteinG = 6.3,
        baseCarbsG = 0.4,
        baseFatG = 4.8,
        category = FoodCategory.BREAKFAST,
        tags = listOf("yumurta", "kahvaltı", "protein", "büyük")
    ),
    FoodPreset(
        id = "egg_pan",
        nameTr = "Tavada / Sahanda Yumurta",
        nameEn = "Fried / Pan Egg",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 95,
        baseProteinG = 6.3,
        baseCarbsG = 0.4,
        baseFatG = 7.5,
        category = FoodCategory.BREAKFAST,
        tags = listOf("yumurta", "tavada", "sahanda", "yağda", "kahvaltı", "protein")
    ),
    FoodPreset(
        id = "omelet_plain",
        nameTr = "2 Yumurtalı Sade Omlet",
        nameEn = "2-Egg Plain Omelette",
        unitStep = 1,
        unitLabelTr = "Porsiyon",
        unitLabelEn = "Portion",
        baseCalories = 185,
        baseProteinG = 13.0,
        baseCarbsG = 0.8,
        baseFatG = 14.5,
        category = FoodCategory.BREAKFAST,
        tags = listOf("omlet", "yumurta", "kahvaltı", "protein", "çırpılmış")
    ),
    FoodPreset(
        id = "menemen",
        nameTr = "Menemen",
        nameEn = "Menemen (Turkish Scrambled Eggs)",
        unitStep = 200,
        unitLabelTr = "g (1 Porsiyon)",
        unitLabelEn = "g (1 Portion)",
        baseCalories = 175,
        baseProteinG = 9.5,
        baseCarbsG = 7.5,
        baseFatG = 12.0,
        baseFiberG = 2.0,
        category = FoodCategory.BREAKFAST,
        tags = listOf("menemen", "yumurta", "domates", "biber", "kahvaltı", "türk")
    ),
    FoodPreset(
        id = "egg_white",
        nameTr = "Yumurta Beyazı",
        nameEn = "Egg White",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 17,
        baseProteinG = 3.6,
        baseCarbsG = 0.2,
        baseFatG = 0.1,
        category = FoodCategory.BREAKFAST,
        tags = listOf("yumurta", "akı", "beyaz", "protein", "diyet")
    ),
    FoodPreset(
        id = "lor",
        nameTr = "Lor Peyniri",
        nameEn = "Curd Cheese",
        unitStep = 50,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 48,
        baseProteinG = 6.5,
        baseCarbsG = 1.6,
        baseFatG = 1.8,
        category = FoodCategory.BREAKFAST,
        tags = listOf("lor", "peynir", "protein", "kahvaltı", "diyet")
    ),
    FoodPreset(
        id = "white_cheese",
        nameTr = "Beyaz Peynir (Tam Yağlı)",
        nameEn = "Feta / White Cheese",
        unitStep = 30,
        unitLabelTr = "g (1 Dilim)",
        unitLabelEn = "g (1 Slice)",
        baseCalories = 85,
        baseProteinG = 5.2,
        baseCarbsG = 0.8,
        baseFatG = 6.8,
        category = FoodCategory.BREAKFAST,
        tags = listOf("peynir", "beyaz peynir", "kahvaltı", "dilim")
    ),
    FoodPreset(
        id = "suzme_cheese",
        nameTr = "Süzme Peynir",
        nameEn = "Strained Cheese",
        unitStep = 30,
        unitLabelTr = "g (1 Dilim)",
        unitLabelEn = "g (1 Slice)",
        baseCalories = 75,
        baseProteinG = 4.5,
        baseCarbsG = 1.2,
        baseFatG = 5.8,
        category = FoodCategory.BREAKFAST,
        tags = listOf("süzme peynir", "peynir", "kahvaltı", "sütaş")
    ),
    FoodPreset(
        id = "kasar",
        nameTr = "Kaşar Peyniri",
        nameEn = "Kashar Cheese",
        unitStep = 30,
        unitLabelTr = "g (1 Dilim)",
        unitLabelEn = "g (1 Slice)",
        baseCalories = 105,
        baseProteinG = 7.5,
        baseCarbsG = 0.5,
        baseFatG = 8.5,
        category = FoodCategory.BREAKFAST,
        tags = listOf("kaşar", "peynir", "tost", "dilim", "kahvaltı")
    ),
    FoodPreset(
        id = "labne",
        nameTr = "Labne Peyniri",
        nameEn = "Labneh Cheese",
        unitStep = 30,
        unitLabelTr = "g (1 Yk)",
        unitLabelEn = "g (1 Tbsp)",
        baseCalories = 60,
        baseProteinG = 2.1,
        baseCarbsG = 1.5,
        baseFatG = 5.2,
        category = FoodCategory.BREAKFAST,
        tags = listOf("labne", "peynir", "sürülebilir", "kahvaltı")
    ),
    FoodPreset(
        id = "yogurt",
        nameTr = "Doğal Yoğurt",
        nameEn = "Plain Yogurt",
        unitStep = 150,
        unitLabelTr = "g (1 Kase)",
        unitLabelEn = "g (1 Bowl)",
        baseCalories = 92,
        baseProteinG = 5.3,
        baseCarbsG = 7.0,
        baseFatG = 4.9,
        category = FoodCategory.BREAKFAST,
        tags = listOf("yoğurt", "kase", "probiyotik", "süt")
    ),
    FoodPreset(
        id = "suzme_yogurt",
        nameTr = "Süzme Yoğurt",
        nameEn = "Greek Yogurt",
        unitStep = 150,
        unitLabelTr = "g (1 Kase)",
        unitLabelEn = "g (1 Bowl)",
        baseCalories = 140,
        baseProteinG = 15.0,
        baseCarbsG = 5.5,
        baseFatG = 6.5,
        category = FoodCategory.BREAKFAST,
        tags = listOf("süzme yoğurt", "yoğurt", "protein", "kase")
    ),
    FoodPreset(
        id = "milk",
        nameTr = "Tam Yağlı Süt",
        nameEn = "Whole Milk",
        unitStep = 200,
        unitLabelTr = "ml (1 Bardak)",
        unitLabelEn = "ml (1 Glass)",
        baseCalories = 122,
        baseProteinG = 6.4,
        baseCarbsG = 9.6,
        baseFatG = 6.6,
        category = FoodCategory.BREAKFAST,
        tags = listOf("süt", "bardak", "içecek", "kahve")
    ),
    FoodPreset(
        id = "semi_milk",
        nameTr = "Yarım Yağlı Süt",
        nameEn = "Low Fat Milk",
        unitStep = 200,
        unitLabelTr = "ml (1 Bardak)",
        unitLabelEn = "ml (1 Glass)",
        baseCalories = 94,
        baseProteinG = 6.2,
        baseCarbsG = 9.8,
        baseFatG = 3.0,
        category = FoodCategory.BREAKFAST,
        tags = listOf("süt", "yarım yağlı", "bardak", "içecek")
    ),
    FoodPreset(
        id = "ayran",
        nameTr = "Ayran",
        nameEn = "Ayran (Yogurt Drink)",
        unitStep = 200,
        unitLabelTr = "ml (1 Bardak)",
        unitLabelEn = "ml (1 Glass)",
        baseCalories = 76,
        baseProteinG = 4.0,
        baseCarbsG = 5.6,
        baseFatG = 4.0,
        category = FoodCategory.BREAKFAST,
        tags = listOf("ayran", "yoğurt", "içecek", "öğle", "türk")
    ),
    FoodPreset(
        id = "kefir",
        nameTr = "Kefir",
        nameEn = "Kefir",
        unitStep = 200,
        unitLabelTr = "ml (1 Bardak)",
        unitLabelEn = "ml (1 Glass)",
        baseCalories = 105,
        baseProteinG = 6.0,
        baseCarbsG = 8.0,
        baseFatG = 4.5,
        category = FoodCategory.BREAKFAST,
        tags = listOf("kefir", "probiyotik", "içecek", "süt", "bağırsak")
    ),
    FoodPreset(
        id = "olives",
        nameTr = "Siyah Zeytin",
        nameEn = "Black Olives",
        unitStep = 30,
        unitLabelTr = "g (6 Adet)",
        unitLabelEn = "g (6 pcs)",
        baseCalories = 45,
        baseProteinG = 0.3,
        baseCarbsG = 1.8,
        baseFatG = 4.2,
        category = FoodCategory.BREAKFAST,
        tags = listOf("zeytin", "siyah zeytin", "kahvaltı")
    ),
    FoodPreset(
        id = "olives_green",
        nameTr = "Yeşil Zeytin",
        nameEn = "Green Olives",
        unitStep = 30,
        unitLabelTr = "g (6 Adet)",
        unitLabelEn = "g (6 pcs)",
        baseCalories = 40,
        baseProteinG = 0.4,
        baseCarbsG = 1.0,
        baseFatG = 4.0,
        category = FoodCategory.BREAKFAST,
        tags = listOf("yeşil zeytin", "zeytin", "kahvaltı")
    ),
    FoodPreset(
        id = "honey",
        nameTr = "Doğal Bal",
        nameEn = "Natural Honey",
        unitStep = 20,
        unitLabelTr = "g (1 Yk)",
        unitLabelEn = "g (1 Tbsp)",
        baseCalories = 64,
        baseProteinG = 0.1,
        baseCarbsG = 17.3,
        baseFatG = 0.0,
        category = FoodCategory.BREAKFAST,
        tags = listOf("bal", "tatlı", "kahvaltı", "enerji")
    ),
    FoodPreset(
        id = "jam",
        nameTr = "Reçel (Çilek/Vişne)",
        nameEn = "Fruit Jam",
        unitStep = 20,
        unitLabelTr = "g (1 Yk)",
        unitLabelEn = "g (1 Tbsp)",
        baseCalories = 54,
        baseProteinG = 0.1,
        baseCarbsG = 13.5,
        baseFatG = 0.0,
        category = FoodCategory.BREAKFAST,
        tags = listOf("reçel", "vişne", "çilek", "kahvaltı", "tatlı")
    ),
    FoodPreset(
        id = "tahin_pekmez",
        nameTr = "Tahin & Pekmez",
        nameEn = "Tahini & Molasses",
        unitStep = 20,
        unitLabelTr = "g (1 Yk)",
        unitLabelEn = "g (1 Tbsp)",
        baseCalories = 86,
        baseProteinG = 2.0,
        baseCarbsG = 11.0,
        baseFatG = 4.2,
        category = FoodCategory.BREAKFAST,
        tags = listOf("tahin", "pekmez", "kahvaltı", "enerji")
    ),

    // ═════════════════════════════════════════════════════════════════
    // KARBONHİDRAT, EKMEK & TAHIL
    // ═════════════════════════════════════════════════════════════════
    FoodPreset(
        id = "bread_white",
        nameTr = "Beyaz Ekmek (Somun Dilim)",
        nameEn = "White Bread (1 Slice)",
        unitStep = 30,
        unitLabelTr = "g (1 Dilim)",
        unitLabelEn = "g (1 Slice)",
        baseCalories = 75,
        baseProteinG = 2.5,
        baseCarbsG = 15.0,
        baseFatG = 0.6,
        baseFiberG = 0.8,
        category = FoodCategory.CARBS,
        tags = listOf("ekmek", "beyaz ekmek", "somun", "dilim", "fırın")
    ),
    FoodPreset(
        id = "bread_quarter",
        nameTr = "Çeyrek Ekmek (Somun)",
        nameEn = "Quarter Loaf Bread",
        unitStep = 50,
        unitLabelTr = "g (Çeyrek)",
        unitLabelEn = "g (Quarter)",
        baseCalories = 130,
        baseProteinG = 4.2,
        baseCarbsG = 26.5,
        baseFatG = 1.0,
        baseFiberG = 1.4,
        category = FoodCategory.CARBS,
        tags = listOf("çeyrek", "ekmek", "somun", "döner", "tost")
    ),
    FoodPreset(
        id = "bread_wheat",
        nameTr = "Tam Buğday Ekmeği",
        nameEn = "Whole Wheat Bread",
        unitStep = 30,
        unitLabelTr = "g (1 Dilim)",
        unitLabelEn = "g (1 Slice)",
        baseCalories = 74,
        baseProteinG = 3.2,
        baseCarbsG = 13.8,
        baseFatG = 1.0,
        baseFiberG = 2.0,
        category = FoodCategory.CARBS,
        tags = listOf("ekmek", "tam buğday", "dilim", "tahıl", "diyet")
    ),
    FoodPreset(
        id = "bread_rye",
        nameTr = "Çavdar Ekmeği",
        nameEn = "Rye Bread",
        unitStep = 30,
        unitLabelTr = "g (1 Dilim)",
        unitLabelEn = "g (1 Slice)",
        baseCalories = 68,
        baseProteinG = 2.7,
        baseCarbsG = 13.2,
        baseFatG = 0.7,
        baseFiberG = 2.2,
        category = FoodCategory.CARBS,
        tags = listOf("çavdar", "ekmek", "dilim", "lifli")
    ),
    FoodPreset(
        id = "simit",
        nameTr = "Sokak Simiti",
        nameEn = "Turkish Bagel (Simit)",
        unitStep = 100,
        unitLabelTr = "g (1 Adet)",
        unitLabelEn = "g (1 pc)",
        baseCalories = 320,
        baseProteinG = 10.0,
        baseCarbsG = 58.0,
        baseFatG = 4.5,
        baseFiberG = 3.2,
        category = FoodCategory.CARBS,
        tags = listOf("simit", "susam", "kahvaltı", "sokak simiti", "unlu")
    ),
    FoodPreset(
        id = "lavash",
        nameTr = "Lavaş / Dürüm Ekmeği",
        nameEn = "Lavash / Tortilla Flatbread",
        unitStep = 60,
        unitLabelTr = "g (1 Adet)",
        unitLabelEn = "g (1 pc)",
        baseCalories = 170,
        baseProteinG = 4.8,
        baseCarbsG = 32.0,
        baseFatG = 2.5,
        baseFiberG = 2.0,
        category = FoodCategory.CARBS,
        tags = listOf("lavaş", "dürüm", "tortilla", "ekmek")
    ),
    FoodPreset(
        id = "oats",
        nameTr = "Yulaf Ezmesi",
        nameEn = "Rolled Oats",
        unitStep = 40,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 152,
        baseProteinG = 5.3,
        baseCarbsG = 27.0,
        baseFatG = 2.6,
        baseFiberG = 4.0,
        category = FoodCategory.CARBS,
        tags = listOf("yulaf", "ezme", "karbonhidrat", "lif", "kahvaltı", "oatmeal")
    ),
    FoodPreset(
        id = "rice",
        nameTr = "Pirinç Pilavı",
        nameEn = "Cooked Rice",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 130,
        baseProteinG = 2.7,
        baseCarbsG = 28.2,
        baseFatG = 0.3,
        category = FoodCategory.CARBS,
        tags = listOf("pilav", "pirinç", "karbonhidrat", "öğle", "akşam")
    ),
    FoodPreset(
        id = "bulgur",
        nameTr = "Bulgur Pilavı",
        nameEn = "Bulgur Pilaf",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 115,
        baseProteinG = 3.1,
        baseCarbsG = 23.5,
        baseFatG = 0.8,
        baseFiberG = 3.0,
        category = FoodCategory.CARBS,
        tags = listOf("bulgur", "pilav", "karbonhidrat", "lif")
    ),
    FoodPreset(
        id = "pasta",
        nameTr = "Haşlanmış Makarna",
        nameEn = "Boiled Pasta",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 140,
        baseProteinG = 5.0,
        baseCarbsG = 28.0,
        baseFatG = 0.6,
        baseFiberG = 1.8,
        category = FoodCategory.CARBS,
        tags = listOf("makarna", "haşlanmış", "karbonhidrat", "spagetti")
    ),
    FoodPreset(
        id = "potato",
        nameTr = "Haşlanmış Patates",
        nameEn = "Boiled Potato",
        unitStep = 150,
        unitLabelTr = "g (1 Adet)",
        unitLabelEn = "g (1 pc)",
        baseCalories = 130,
        baseProteinG = 3.0,
        baseCarbsG = 29.5,
        baseFatG = 0.2,
        baseFiberG = 2.5,
        category = FoodCategory.CARBS,
        tags = listOf("patates", "haşlama", "karbonhidrat")
    ),
    FoodPreset(
        id = "baked_potato",
        nameTr = "Fırın Patates (Baharatlı)",
        nameEn = "Baked Potato",
        unitStep = 150,
        unitLabelTr = "g (1 Porsiyon)",
        unitLabelEn = "g (1 Portion)",
        baseCalories = 145,
        baseProteinG = 3.2,
        baseCarbsG = 33.0,
        baseFatG = 0.3,
        baseFiberG = 2.8,
        category = FoodCategory.CARBS,
        tags = listOf("fırın patates", "patates", "fırın")
    ),
    FoodPreset(
        id = "lentil_soup",
        nameTr = "Mercimek Çorbası",
        nameEn = "Lentil Soup",
        unitStep = 250,
        unitLabelTr = "ml (1 Kase)",
        unitLabelEn = "ml (1 Bowl)",
        baseCalories = 140,
        baseProteinG = 8.5,
        baseCarbsG = 21.0,
        baseFatG = 2.5,
        baseFiberG = 4.0,
        category = FoodCategory.CARBS,
        tags = listOf("çorba", "mercimek", "kase", "başlangıç")
    ),
    FoodPreset(
        id = "chickpeas",
        nameTr = "Haşlanmış Nohut",
        nameEn = "Boiled Chickpeas",
        unitStep = 100,
        unitLabelTr = "g",
        unitLabelEn = "g",
        baseCalories = 164,
        baseProteinG = 8.9,
        baseCarbsG = 27.4,
        baseFatG = 2.6,
        baseFiberG = 7.6,
        category = FoodCategory.CARBS,
        tags = listOf("nohut", "bakliyat", "protein", "lif")
    ),
    FoodPreset(
        id = "beans",
        nameTr = "Kuru Fasulye Yemeği",
        nameEn = "White Bean Stew",
        unitStep = 150,
        unitLabelTr = "g (1 Porsiyon)",
        unitLabelEn = "g (1 Portion)",
        baseCalories = 190,
        baseProteinG = 11.0,
        baseCarbsG = 26.0,
        baseFatG = 4.0,
        baseFiberG = 7.0,
        category = FoodCategory.CARBS,
        tags = listOf("kuru fasulye", "fasulye", "bakliyat", "yemek")
    ),

    // ═════════════════════════════════════════════════════════════════
    // MEYVE & KURUYEMİŞ & ATIŞTIRMALIK
    // ═════════════════════════════════════════════════════════════════
    FoodPreset(
        id = "banana",
        nameTr = "Muz",
        nameEn = "Banana",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 105,
        baseProteinG = 1.3,
        baseCarbsG = 27.0,
        baseFatG = 0.3,
        baseFiberG = 3.1,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("muz", "meyve", "potasyum", "atıştırmalık")
    ),
    FoodPreset(
        id = "apple",
        nameTr = "Elma",
        nameEn = "Apple",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 95,
        baseProteinG = 0.5,
        baseCarbsG = 25.0,
        baseFatG = 0.3,
        baseFiberG = 4.4,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("elma", "meyve", "lif", "yeşil elma", "kırmızı elma")
    ),
    FoodPreset(
        id = "orange",
        nameTr = "Portakal",
        nameEn = "Orange",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 62,
        baseProteinG = 1.2,
        baseCarbsG = 15.4,
        baseFatG = 0.2,
        baseFiberG = 3.1,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("portakal", "meyve", "c vitamini")
    ),
    FoodPreset(
        id = "mandarin",
        nameTr = "Mandalina",
        nameEn = "Mandarin",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 43,
        baseProteinG = 0.6,
        baseCarbsG = 10.5,
        baseFatG = 0.2,
        baseFiberG = 1.5,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("mandalina", "meyve", "c vitamini")
    ),
    FoodPreset(
        id = "strawberry",
        nameTr = "Çilek",
        nameEn = "Strawberries",
        unitStep = 100,
        unitLabelTr = "g (1 Kase)",
        unitLabelEn = "g (1 Bowl)",
        baseCalories = 32,
        baseProteinG = 0.7,
        baseCarbsG = 7.7,
        baseFatG = 0.3,
        baseFiberG = 2.0,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("çilek", "meyve", "düşük kalori", "kase")
    ),
    FoodPreset(
        id = "blueberry",
        nameTr = "Yaban Mersini",
        nameEn = "Blueberries",
        unitStep = 100,
        unitLabelTr = "g (1 Avuç)",
        unitLabelEn = "g (1 Handful)",
        baseCalories = 57,
        baseProteinG = 0.7,
        baseCarbsG = 14.5,
        baseFatG = 0.3,
        baseFiberG = 2.4,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("yaban mersini", "antioksidan", "meyve", "yulaf")
    ),
    FoodPreset(
        id = "watermelon",
        nameTr = "Karpuz",
        nameEn = "Watermelon",
        unitStep = 200,
        unitLabelTr = "g (1 Dilim)",
        unitLabelEn = "g (1 Slice)",
        baseCalories = 60,
        baseProteinG = 1.2,
        baseCarbsG = 15.0,
        baseFatG = 0.3,
        baseFiberG = 0.8,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("karpuz", "meyve", "yaz", "dilim")
    ),
    FoodPreset(
        id = "date_fruit",
        nameTr = "Hurma (Medine)",
        nameEn = "Date Fruit",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 56,
        baseProteinG = 0.5,
        baseCarbsG = 15.0,
        baseFatG = 0.1,
        baseFiberG = 1.6,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("hurma", "medine", "tatlı", "enerji", "lif")
    ),
    FoodPreset(
        id = "walnut",
        nameTr = "Ceviz İçi",
        nameEn = "Walnuts",
        unitStep = 25,
        unitLabelTr = "g (1 Avuç)",
        unitLabelEn = "g (1 Handful)",
        baseCalories = 163,
        baseProteinG = 3.8,
        baseCarbsG = 3.4,
        baseFatG = 16.3,
        baseFiberG = 1.7,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("ceviz", "kuruyemiş", "avuç", "omega3")
    ),
    FoodPreset(
        id = "almond",
        nameTr = "Çiğ Badem",
        nameEn = "Raw Almonds",
        unitStep = 25,
        unitLabelTr = "g (1 Avuç)",
        unitLabelEn = "g (1 Handful)",
        baseCalories = 145,
        baseProteinG = 5.3,
        baseCarbsG = 5.4,
        baseFatG = 12.5,
        baseFiberG = 3.0,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("badem", "çiğ badem", "kuruyemiş", "avuç")
    ),
    FoodPreset(
        id = "hazelnut",
        nameTr = "Fındık İçi",
        nameEn = "Hazelnuts",
        unitStep = 25,
        unitLabelTr = "g (1 Avuç)",
        unitLabelEn = "g (1 Handful)",
        baseCalories = 160,
        baseProteinG = 3.7,
        baseCarbsG = 4.2,
        baseFatG = 15.2,
        baseFiberG = 2.4,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("fındık", "kuruyemiş", "avuç", "enerji")
    ),
    FoodPreset(
        id = "peanut_butter",
        nameTr = "Fıstık Ezmesi (%100 Şekersiz)",
        nameEn = "Peanut Butter",
        unitStep = 20,
        unitLabelTr = "g (1 Yk)",
        unitLabelEn = "g (1 Tbsp)",
        baseCalories = 118,
        baseProteinG = 5.0,
        baseCarbsG = 4.0,
        baseFatG = 10.0,
        baseFiberG = 1.6,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("fıstık ezmesi", "fıstık", "ezme", "tatlı kaşığı")
    ),
    FoodPreset(
        id = "dark_chocolate",
        nameTr = "Bitter Çikolata (%70+)",
        nameEn = "Dark Chocolate (70%+)",
        unitStep = 20,
        unitLabelTr = "g (2 Kare)",
        unitLabelEn = "g (2 Squares)",
        baseCalories = 115,
        baseProteinG = 1.6,
        baseCarbsG = 9.0,
        baseFatG = 8.5,
        baseFiberG = 2.2,
        category = FoodCategory.FRUITS_NUTS,
        tags = listOf("çikolata", "bitter", "kakao", "atıştırmalık")
    ),

    // ═════════════════════════════════════════════════════════════════
    // SEBZE & SALATA & SAĞLIKLI YAĞLAR
    // ═════════════════════════════════════════════════════════════════
    FoodPreset(
        id = "salad",
        nameTr = "Mevsim / Çoban Salata",
        nameEn = "Seasonal Green Salad",
        unitStep = 150,
        unitLabelTr = "g (1 Porsiyon)",
        unitLabelEn = "g (1 Portion)",
        baseCalories = 35,
        baseProteinG = 1.5,
        baseCarbsG = 6.0,
        baseFatG = 0.4,
        baseFiberG = 2.5,
        category = FoodCategory.HEALTHY_FATS,
        tags = listOf("salata", "yeşillik", "çoban", "diyet", "lif")
    ),
    FoodPreset(
        id = "cucumber",
        nameTr = "Salatalık",
        nameEn = "Cucumber",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 16,
        baseProteinG = 0.8,
        baseCarbsG = 3.6,
        baseFatG = 0.1,
        baseFiberG = 0.8,
        category = FoodCategory.HEALTHY_FATS,
        tags = listOf("salatalık", "hıyar", "sebze", "diyet")
    ),
    FoodPreset(
        id = "tomato",
        nameTr = "Domates",
        nameEn = "Tomato",
        unitStep = 1,
        unitLabelTr = "Adet",
        unitLabelEn = "pcs",
        baseCalories = 22,
        baseProteinG = 1.1,
        baseCarbsG = 4.7,
        baseFatG = 0.2,
        baseFiberG = 1.4,
        category = FoodCategory.HEALTHY_FATS,
        tags = listOf("domates", "sebze", "kahvaltı", "salata")
    ),
    FoodPreset(
        id = "olive_oil",
        nameTr = "Zeytinyağı",
        nameEn = "Olive Oil",
        unitStep = 10,
        unitLabelTr = "g (1 Yk)",
        unitLabelEn = "g (1 Tbsp)",
        baseCalories = 88,
        baseProteinG = 0.0,
        baseCarbsG = 0.0,
        baseFatG = 10.0,
        category = FoodCategory.HEALTHY_FATS,
        tags = listOf("zeytinyağı", "yağ", "yemek kaşığı", "salata")
    ),
    FoodPreset(
        id = "butter",
        nameTr = "Tereyağı",
        nameEn = "Butter",
        unitStep = 10,
        unitLabelTr = "g (1 Yk)",
        unitLabelEn = "g (1 Tbsp)",
        baseCalories = 72,
        baseProteinG = 0.1,
        baseCarbsG = 0.0,
        baseFatG = 8.1,
        category = FoodCategory.HEALTHY_FATS,
        tags = listOf("tereyağı", "yağ", "kahvaltı")
    ),
    FoodPreset(
        id = "avocado",
        nameTr = "Avokado",
        nameEn = "Avocado",
        unitStep = 75,
        unitLabelTr = "g (Yarım)",
        unitLabelEn = "g (Half)",
        baseCalories = 120,
        baseProteinG = 1.5,
        baseCarbsG = 6.4,
        baseFatG = 11.0,
        baseFiberG = 5.0,
        category = FoodCategory.HEALTHY_FATS,
        tags = listOf("avokado", "yağ", "salata", "kahvaltı")
    )
)

/**
 * Normalizes Turkish characters and trims for seamless search comparisons.
 */
private fun normalizeSearchText(input: String): String {
    return input.lowercase()
        .replace('ı', 'i')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ş', 's')
        .replace('ö', 'o')
        .replace('ç', 'c')
        .trim()
}

/**
 * Maps user-created CustomFoodItem to FoodPreset for unified rendering and search.
 */
private fun CustomFoodItem.toPreset(): FoodPreset {
    val displayTitle = if (!brand.isNullOrBlank()) "$brand $name" else name
    val mappedCategory = try {
        FoodCategory.valueOf(categoryName)
    } catch (_: Exception) {
        FoodCategory.PROTEIN
    }
    return FoodPreset(
        id = id,
        nameTr = displayTitle,
        nameEn = name,
        unitStep = unitStep,
        unitLabelTr = portionUnit,
        unitLabelEn = portionUnit,
        baseCalories = calories,
        baseProteinG = proteinG,
        baseCarbsG = carbsG,
        baseFatG = fatG,
        baseFiberG = fiberG,
        category = mappedCategory,
        tags = listOfNotNull(name, brand, "özel", "marka", portionUnit),
        isCustom = true,
        brand = brand,
        iconKey = iconKey,
        micronutrients = micronutrients,
        isMix = isMix,
        ingredientCount = ingredients.distinctBy { it.name.trim().lowercase() }.size
    )
}

private fun formatScaledPresetPortion(preset: FoodPreset, count: Int): String {
    val label = preset.unitLabelTr.trim()
    if (preset.unitStep > 1) {
        val totalAmount = preset.unitStep * count
        return if (count > 1) {
            "$totalAmount ${label.substringBefore(" (")} (${count}x)"
        } else {
            "$totalAmount $label"
        }
    }
    // unitStep == 1 (e.g., CustomFoodItem where unitLabelTr is "50ml", "30ml", "1 Shot", or built-in "Adet")
    if (count <= 1) {
        return if (label.firstOrNull()?.isDigit() == true) label else "1 $label"
    }
    val lower = label.lowercase()
    if (lower.contains("shot")) {
        return "Shot x$count"
    }
    val numUnitMatch = Regex("""^(\d+(?:[.,]\d+)?)\s*(ml|g|gr|gram)\b""", RegexOption.IGNORE_CASE).find(label)
    if (numUnitMatch != null) {
        val baseNum = numUnitMatch.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 1.0
        val unitStr = if (numUnitMatch.groupValues[2].lowercase().startsWith("m")) "ml" else "g"
        val totalNum = kotlin.math.round(baseNum * count).toInt()
        return "$totalNum$unitStr (${count}x)"
    }
    val leadingOneStripped = label.replace(Regex("""^1\s+"""), "")
    return "$leadingOneStripped (${count}x)"
}

/**
 * Telemetry Quick Food Presets Explorer with Live Real-time Search,
 * Favorite foods, Custom/Brand Food Products, and Nutritional Category Filters.
 */
@Composable
fun QuickFoodPresetsView(
    onAddFood: (name: String, portion: String, calories: Int, protein: Double, carbs: Double, fat: Double, fiber: Double) -> Unit,
    modifier: Modifier = Modifier,
    customFoods: List<CustomFoodItem> = emptyList(),
    favoriteFoodIds: Set<String> = emptySet(),
    frequentFoods: List<FoodItem> = emptyList(),
    onToggleFavorite: ((String) -> Unit)? = null,
    onOpenAddCustomFood: (() -> Unit)? = null,
    onDeleteCustomFood: ((String) -> Unit)? = null,
    onSelectFood: ((preset: FoodPreset, quantity: Int) -> Unit)? = null,
    onAddFoodWithDetails: ((name: String, portion: String, calories: Int, protein: Double, carbs: Double, fat: Double, fiber: Double, micronutrients: List<Micronutrient>, iconKey: String?) -> Unit)? = null,
    onOpenInventoryStudio: ((CustomFoodItem?) -> Unit)? = null
) {
    val isDark = LocalIsDarkTheme.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FoodCategory.ALL) }
    val multipliers = remember { mutableStateMapOf<String, Int>() }

    // Unified Presets = Built-in Presets + User Custom Items
    val allCombinedPresets = remember(customFoods) {
        val customPresets = customFoods.map { it.toPreset() }
        customPresets + VERIFIED_FOOD_PRESETS
    }

    // Real-time Turkish-aware filter
    val filteredPresets = remember(allCombinedPresets, searchQuery, selectedCategory, favoriteFoodIds) {
        val queryNorm = normalizeSearchText(searchQuery)
        allCombinedPresets.filter { preset ->
            val matchesCategory = when (selectedCategory) {
                FoodCategory.ALL -> true
                FoodCategory.FAVORITES -> favoriteFoodIds.contains(preset.id)
                FoodCategory.CUSTOM -> preset.isCustom
                else -> preset.category == selectedCategory
            }
            if (!matchesCategory) return@filter false

            if (queryNorm.isBlank()) return@filter true

            val nameTrNorm = normalizeSearchText(preset.nameTr)
            val nameEnNorm = normalizeSearchText(preset.nameEn)
            val brandNorm = normalizeSearchText(preset.brand ?: "")
            val matchesTags = preset.tags.any { normalizeSearchText(it).contains(queryNorm) }

            nameTrNorm.contains(queryNorm) || nameEnNorm.contains(queryNorm) || brandNorm.contains(queryNorm) || matchesTags
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 1. Category Filter Strip (Top, Clean & Direct) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodCategory.entries.forEach { category ->
                // Show badge counts for Favorites and Custom
                val badgeCount = when (category) {
                    FoodCategory.FAVORITES -> favoriteFoodIds.size
                    FoodCategory.CUSTOM -> customFoods.size
                    else -> null
                }

                val isSelected = selectedCategory == category
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) {
                        HealthColors.Primary.copy(alpha = 0.2f)
                    } else if (isDark) {
                        HealthColors.SurfaceContainerHigh.copy(alpha = 0.6f)
                    } else {
                        HealthColors.SurfaceVariantLight.copy(alpha = 0.7f)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable {
                            selectedCategory = if (isSelected && category != FoodCategory.ALL) {
                                FoodCategory.ALL
                            } else {
                                category
                            }
                        }
                        .bounceClick()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category.labelTr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (badgeCount != null && badgeCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = "$badgeCount",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) HealthColors.OnPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── 2. Results View: Compact Empty State OR Horizontal Cards Strip ──
        if (filteredPresets.isEmpty()) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (selectedCategory) {
                            FoodCategory.FAVORITES -> "Henüz favori yiyecek eklemediniz"
                            FoodCategory.CUSTOM -> "Envanterinizde henüz ürün bulunmuyor"
                            else -> "\"$searchQuery\" ile eşleşen besin bulunamadı"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selectedCategory == FoodCategory.CUSTOM && onOpenAddCustomFood != null) {
                            Button(
                                onClick = onOpenAddCustomFood,
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).bounceClick(),
                                colors = ButtonDefaults.buttonColors(containerColor = HealthColors.Primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Envanterime Ekle", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = FoodCategory.ALL
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp).bounceClick()
                            ) {
                                Text(
                                    text = "Tüm Besinleri Göster",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HealthColors.Primary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Horizontal Lazy Scrollable Cards Strip (Only composes visible cards)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(
                    items = filteredPresets,
                    key = { it.id }
                ) { preset ->
                    val isFavorite = favoriteFoodIds.contains(preset.id)
                    val count = multipliers.getOrPut(preset.id) { 1 }
                    val totalAmount = preset.unitStep * count
                    val totalCalories = preset.baseCalories * count
                    val totalProtein = ((preset.baseProteinG * count * 10).toInt()) / 10.0
                    val totalCarbs = ((preset.baseCarbsG * count * 10).toInt()) / 10.0
                    val totalFat = ((preset.baseFatG * count * 10).toInt()) / 10.0
                    val totalFiber = ((preset.baseFiberG * count * 10).toInt()) / 10.0

                    val portionLabel = formatScaledPresetPortion(preset, count)

                    val matchingCustomItem = if (preset.isCustom) {
                        customFoods.firstOrNull { it.id == preset.id }
                    } else null

                    PresetFoodCard(
                        preset = preset,
                        isFavorite = isFavorite,
                        portionLabel = portionLabel,
                        totalCalories = totalCalories,
                        totalProtein = totalProtein,
                        totalCarbs = totalCarbs,
                        totalFat = totalFat,
                        onCardClick = {
                            if (matchingCustomItem != null && onOpenInventoryStudio != null) {
                                onOpenInventoryStudio(matchingCustomItem)
                            } else {
                                onSelectFood?.invoke(preset, count)
                            }
                        },
                        onOpenStudio = if (matchingCustomItem != null && onOpenInventoryStudio != null) {
                            { onOpenInventoryStudio(matchingCustomItem) }
                        } else null,
                        onToggleFavorite = {
                            onToggleFavorite?.invoke(preset.id)
                        },
                        onDeleteCustom = if (preset.isCustom && onDeleteCustomFood != null) {
                            { onDeleteCustomFood(preset.id) }
                        } else null,
                        onDecrement = {
                            if (count > 1) {
                                multipliers[preset.id] = count - 1
                            }
                        },
                        onIncrement = {
                            if (count < 20) {
                                multipliers[preset.id] = count + 1
                            }
                        },
                        onAdd = {
                            val scaledMicros = preset.micronutrients.map { micro ->
                                micro.copy(amount = kotlin.math.round(micro.amount * count * 10.0) / 10.0)
                            }
                            if (onAddFoodWithDetails != null) {
                                onAddFoodWithDetails(
                                    preset.nameTr,
                                    portionLabel,
                                    totalCalories,
                                    totalProtein,
                                    totalCarbs,
                                    totalFat,
                                    totalFiber,
                                    scaledMicros,
                                    preset.iconKey
                                )
                            } else {
                                onAddFood(
                                    preset.nameTr,
                                    portionLabel,
                                    totalCalories,
                                    totalProtein,
                                    totalCarbs,
                                    totalFat,
                                    totalFiber
                                )
                            }
                            // Reset count back to 1
                            multipliers[preset.id] = 1
                        }
                    )
                }
            }
        }

        // ── 3. Bottom Search Bar (Moved to Bottom to Reduce Top Clutter) ──
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Besin veya marka ara...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Ara",
                    tint = HealthColors.Primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(19.dp)
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            keyboardController?.hide()
                        },
                        modifier = Modifier.size(28.dp).bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Temizle",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HealthColors.Primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)
            )
        )
    }
}

@Composable
private fun PresetFoodCard(
    preset: FoodPreset,
    isFavorite: Boolean,
    portionLabel: String,
    totalCalories: Int,
    totalProtein: Double,
    totalCarbs: Double,
    totalFat: Double,
    onCardClick: () -> Unit,
    onOpenStudio: (() -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    onDeleteCustom: (() -> Unit)?,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onAdd: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    GlassCard(
        modifier = Modifier
            .width(190.dp)
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(20.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Food Title & Star Favorite Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (preset.iconKey != null) {
                        FoodIconBadge(
                            iconKey = preset.iconKey,
                            size = 32.dp,
                            tint = if (preset.isCustom) HealthColors.Secondary else HealthColors.Primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        if (preset.isCustom) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = HealthColors.Secondary.copy(alpha = 0.15f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            ) {
                                Text(
                                    text = if (preset.isMix) "🏷️ ${preset.ingredientCount} İçerik" else (preset.brand ?: "Envanterim"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HealthColors.Secondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = preset.nameTr,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onDeleteCustom != null) {
                        IconButton(
                            onClick = onDeleteCustom,
                            modifier = Modifier.size(24.dp).bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(24.dp).bounceClick()
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "Favorilerden çıkar" else "Favorilere ekle",
                            tint = if (isFavorite) HealthColors.Tertiary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Calorie & Protein Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = HealthColors.Primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "$totalCalories kcal",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${totalProtein}g P",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = HealthColors.NutrientProtein
                )
            }

            // Macro Mini Badges (Carbs & Fat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${totalCarbs}g K",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = HealthColors.NutrientCarbs,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "${totalFat}g Y",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = HealthColors.NutrientLipids,
                    fontWeight = FontWeight.Medium
                )
            }

            if (onOpenStudio != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HealthColors.Secondary.copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenStudio() }
                        .bounceClick()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = HealthColors.Secondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (preset.isMix) "İçerik Seviyelerini Ayarla" else "Seviye / Ölçek Değiştir",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = HealthColors.Secondary,
                            maxLines = 1
                        )
                    }
                }
            } else if (preset.micronutrients.isNotEmpty()) {
                // Micronutrient badge if preset has micronutrients
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = HealthColors.Secondary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "🧬 ${preset.micronutrients.size} Mikro Besin",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Secondary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Quantity Stepper: [-]  portion  [+]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) HealthColors.SurfaceContainerLowest else HealthColors.SurfaceVariantLight)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(28.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Azalt",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = portionLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(28.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Artır",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Action Add Button
            Button(
                onClick = onAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HealthColors.Primary,
                    contentColor = HealthColors.OnPrimary
                ),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Öğüne Ekle",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
