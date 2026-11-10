package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.NutrientSynergyTip
import com.yusufteker.healthtrack.core.model.SmartMealSuggestion

object SmartMealEngine {

    private val CURATED_MEAL_DATABASE: List<SmartMealSuggestion> = listOf(
        // ── 1. Lif & Magnezyum Odaklı (Hızlı, Az Malzemeli) ──
        SmartMealSuggestion(
            id = "meal_chia_oat_bowl",
            title = "Chia Tohumlu & Muzlu Yulaf Kasesi",
            subtitle = "Yüksek Lif & Magnezyum Deposu",
            tags = listOf("Az Malzemeli", "Hızlı (<5 dk)", "Pişirmesiz", "Lif Zengin"),
            prepTimeMinutes = 5,
            ingredients = listOf("40g Yulaf Ezmesi", "1 Tatlı Kaşığı Chia Tohumu", "150ml Yarım Yağlı Süt/Badem Sütü", "Yarım Muz"),
            calories = 275,
            proteinG = 11.5,
            carbsG = 42.0,
            fatG = 6.5,
            fiberG = 8.2,
            keyMicros = listOf(
                Micronutrient("Lif", 8.2, "g"),
                Micronutrient("Magnezyum", 85.0, "mg"),
                Micronutrient("Kalsiyum", 220.0, "mg")
            ),
            instructions = "Yulaf ve chia tohumunu süt ile karıştırıp 3-4 dakika bekletin. Üzerine dilimlenmiş yarım muz ekleyerek tüketin."
        ),
        SmartMealSuggestion(
            id = "meal_avocado_green_salad",
            title = "Yeşil Mercimek & Avokado Salatası",
            subtitle = "Bitkisel Lif, Demir ve Sağlıklı Yağ",
            tags = listOf("Lif Zengin", "Demir & Magnezyum", "Az Malzemeli"),
            prepTimeMinutes = 8,
            ingredients = listOf("1 Su Bardağı Haşlanmış Yeşil Mercimek", "Yarım Avokado", "1 Avuç Roka veya Maydanoz", "1 Yemek Kaşığı Limon & Zeytinyağı"),
            calories = 340,
            proteinG = 16.0,
            carbsG = 36.0,
            fatG = 14.5,
            fiberG = 12.4,
            keyMicros = listOf(
                Micronutrient("Lif", 12.4, "g"),
                Micronutrient("Demir", 5.2, "mg"),
                Micronutrient("Magnezyum", 72.0, "mg"),
                Micronutrient("C Vitamini", 28.0, "mg")
            ),
            instructions = "Haşlanmış mercimeği doğranmış avokado ve yeşilliklerle karıştırın, limon ve zeytinyağı sosu gezdirin."
        ),
        SmartMealSuggestion(
            id = "meal_pumpkin_seed_yogurt",
            title = "Kabak Çekirdekli & Keten Tohumlu Süzme Yoğurt",
            subtitle = "Magnezyum, Çinko ve Doğal Protein",
            tags = listOf("3 Malzeme", "Pişirmesiz (<3 dk)", "Yüksek Magnezyum"),
            prepTimeMinutes = 3,
            ingredients = listOf("150g Süzme Yoğurt", "1 Yemek Kaşığı Çiğ Kabak Çekirdeği", "1 Çay Kaşığı Öğütülmüş Keten Tohumu"),
            calories = 220,
            proteinG = 19.5,
            carbsG = 7.0,
            fatG = 11.5,
            fiberG = 4.5,
            keyMicros = listOf(
                Micronutrient("Magnezyum", 110.0, "mg"),
                Micronutrient("Kalsiyum", 210.0, "mg"),
                Micronutrient("Lif", 4.5, "g")
            ),
            instructions = "Süzme yoğurdun üzerine çiğ kabak çekirdeği ve keten tohumunu serpin, anında hazır."
        ),

        // ── 2. Yüksek Protein & Düşük Kalori Odaklı ──
        SmartMealSuggestion(
            id = "meal_spinach_mushroom_omelet",
            title = "Ispanaklı & Mantarlı Lorlu Omlet",
            subtitle = "Demir, B12 ve Yüksek Kas Proteini",
            tags = listOf("Yüksek Protein", "Demir & C Vitamini", "Hızlı (<10 dk)"),
            prepTimeMinutes = 10,
            ingredients = listOf("2 Adet Yumurta", "50g Lor Peyniri", "1 Avuç Taze Ispanak", "3-4 Adet Kültür Mantarı"),
            calories = 265,
            proteinG = 24.5,
            carbsG = 4.5,
            fatG = 15.0,
            fiberG = 2.8,
            keyMicros = listOf(
                Micronutrient("Demir", 3.8, "mg"),
                Micronutrient("Kalsiyum", 160.0, "mg"),
                Micronutrient("B12 Vitamini", 1.6, "µg"),
                Micronutrient("Magnezyum", 48.0, "mg")
            ),
            instructions = "Mantarları ve ıspanağı tavada 2 dakika soteleyin. Çırpılmış yumurtaları ve loru ekleyip kısık ateşte pişirin."
        ),
        SmartMealSuggestion(
            id = "meal_grilled_chicken_steamed_broccoli",
            title = "Izgara Tavuk Göğsü & Buharda Brokoli",
            subtitle = "Ultra Saf Protein & C Vitamini",
            tags = listOf("Yüksek Protein", "C Vitamini", "Az Malzemeli"),
            prepTimeMinutes = 15,
            ingredients = listOf("180g Tavuk Göğsü", "150g Brokoli", "Yarım Limonun Suyu", "1 Çay Kaşığı Zeytinyağı"),
            calories = 310,
            proteinG = 42.0,
            carbsG = 9.0,
            fatG = 7.5,
            fiberG = 4.8,
            keyMicros = listOf(
                Micronutrient("C Vitamini", 78.0, "mg"),
                Micronutrient("Demir", 2.4, "mg"),
                Micronutrient("B12 Vitamini", 0.6, "µg")
            ),
            instructions = "Tavuk göğsünü yağsız tavada ızgara yapın. Brokoliyi 5 dakika buharda haşlayıp üzerine limon suyu gezdirin."
        ),
        SmartMealSuggestion(
            id = "meal_tuna_arugula_bowl",
            title = "Ton Balıklı & Cevizli Roka Kasesi",
            subtitle = "Omega-3, B12 Vitamini ve Sağlıklı Yağlar",
            tags = listOf("Az Malzemeli", "Pişirmesiz (<5 dk)", "Omega-3"),
            prepTimeMinutes = 4,
            ingredients = listOf("1 Kutu Konserve Ton Balığı (Süzülmüş)", "1 Demet Roka", "3 Adet Bütün Ceviz İçi", "Yarım Limon"),
            calories = 295,
            proteinG = 31.0,
            carbsG = 4.0,
            fatG = 16.5,
            fiberG = 3.2,
            keyMicros = listOf(
                Micronutrient("B12 Vitamini", 2.2, "µg"),
                Micronutrient("Demir", 2.8, "mg"),
                Micronutrient("Magnezyum", 55.0, "mg"),
                Micronutrient("C Vitamini", 32.0, "mg")
            ),
            instructions = "Rokaları yıkayıp tabağa alın. Süzülmüş ton balığını ve kırılmış cevizleri üzerine ekleyip bol limon sıkın."
        ),

        // ── 3. Kalsiyum, C Vitamini & Pratik Atıştırmalıklar ──
        SmartMealSuggestion(
            id = "meal_berry_cottage_parfait",
            title = "Yaban Mersinli & Bademli Lor Kasesi",
            subtitle = "Kalsiyum & Antioksidan Gücü",
            tags = listOf("3 Malzeme", "Tatlı Krizine Birebir", "Pişirmesiz (<3 dk)"),
            prepTimeMinutes = 3,
            ingredients = listOf("120g Lor Peyniri veya Süzme Yoğurt", "1 Avuç Yaban Mersini / Çilek", "10 Adet Çiğ Badem"),
            calories = 210,
            proteinG = 18.0,
            carbsG = 16.0,
            fatG = 8.5,
            fiberG = 4.2,
            keyMicros = listOf(
                Micronutrient("Kalsiyum", 240.0, "mg"),
                Micronutrient("C Vitamini", 22.0, "mg"),
                Micronutrient("Magnezyum", 45.0, "mg")
            ),
            instructions = "Loru kaseye alın, üzerine meyveleri ve bademleri ekleyerek karıştırın."
        ),
        SmartMealSuggestion(
            id = "meal_boiled_eggs_walnuts_bellpepper",
            title = "Haşlanmış Yumurta, Kapya Biber & Ceviz Tabağı",
            subtitle = "D Vitamini, C Vitamini & Kolin",
            tags = listOf("Az Malzemeli", "D Vitamini", "Hızlı (<8 dk)"),
            prepTimeMinutes = 8,
            ingredients = listOf("2 Adet Haşlanmış Yumurta", "1 Adet Kırmızı Kapya Biber", "3 Adet Ceviz İçi"),
            calories = 280,
            proteinG = 16.5,
            carbsG = 7.0,
            fatG = 20.0,
            fiberG = 3.5,
            keyMicros = listOf(
                Micronutrient("D Vitamini", 2.0, "µg"),
                Micronutrient("C Vitamini", 95.0, "mg"),
                Micronutrient("Demir", 2.2, "mg"),
                Micronutrient("B12 Vitamini", 1.2, "µg")
            ),
            instructions = "Haşlanmış yumurtaları dilimleyin. Yanında taze kapya biber ve ceviz ile tüketin."
        )
    )

    /**
     * Recommends smart meals based on user deficiencies, time constraints and ingredient filters.
     */
    fun matchSmartMeals(
        remainingCalories: Int,
        missingProteinG: Double,
        nutrientBars: List<NutrientProgress>,
        recent3DaysFoodNames: Set<String>,
        onlyQuick: Boolean,
        onlyFewIngredients: Boolean,
        excludeRecent3Days: Boolean,
        highProteinOnly: Boolean
    ): List<SmartMealSuggestion> {
        // 1. Identify key deficiencies
        val fiberProgress = nutrientBars.firstOrNull { it.key == "fiber" }?.percentage ?: 0f
        val magnesiumProgress = nutrientBars.firstOrNull { it.key == "magnesium" }?.percentage ?: 0f
        val ironProgress = nutrientBars.firstOrNull { it.key == "iron" }?.percentage ?: 0f
        val vitCProgress = nutrientBars.firstOrNull { it.key == "vit_c" }?.percentage ?: 0f
        val calciumProgress = nutrientBars.firstOrNull { it.key == "calcium" }?.percentage ?: 0f

        return CURATED_MEAL_DATABASE
            .filter { meal ->
                // Filter by quick prep (<10 mins)
                if (onlyQuick && meal.prepTimeMinutes > 10) return@filter false

                // Filter by few ingredients (<= 4 ingredients)
                if (onlyFewIngredients && meal.ingredients.size > 4) return@filter false

                // Filter by high protein
                if (highProteinOnly && meal.proteinG < 20.0) return@filter false

                // Filter by recent 3 days rotation: exclude if meal title or main ingredient appeared in last 3 days
                if (excludeRecent3Days && recent3DaysFoodNames.isNotEmpty()) {
                    val mealKeywords = (listOf(meal.title) + meal.ingredients).map { it.lowercase() }
                    val hasEatenRecently = recent3DaysFoodNames.any { eaten ->
                        val cleanEaten = eaten.lowercase()
                        mealKeywords.any { keyword ->
                            cleanEaten.contains("yumurta") && keyword.contains("yumurta") ||
                            cleanEaten.contains("tavuk") && keyword.contains("tavuk") ||
                            cleanEaten.contains("yulaf") && keyword.contains("yulaf") ||
                            cleanEaten.contains("ton balık") && keyword.contains("ton balık") ||
                            cleanEaten.contains("mercimek") && keyword.contains("mercimek") ||
                            cleanEaten.contains("yoğurt") && keyword.contains("yoğurt")
                        }
                    }
                    if (hasEatenRecently) return@filter false
                }

                true
            }
            .sortedByDescending { meal ->
                var score = 0.0

                // Bonus for tackling the weakest micronutrients
                if (fiberProgress < 50f && meal.fiberG >= 6.0) score += 40.0
                if (magnesiumProgress < 50f && meal.keyMicros.any { it.name.contains("Magnezyum", ignoreCase = true) && it.amount >= 60.0 }) score += 35.0
                if (ironProgress < 50f && meal.keyMicros.any { it.name.contains("Demir", ignoreCase = true) && it.amount >= 3.0 }) score += 30.0
                if (vitCProgress < 50f && meal.keyMicros.any { it.name.contains("C", ignoreCase = true) && it.amount >= 25.0 }) score += 25.0
                if (calciumProgress < 50f && meal.keyMicros.any { it.name.contains("Kalsiyum", ignoreCase = true) && it.amount >= 150.0 }) score += 20.0

                // Bonus for meeting missing protein
                if (missingProteinG > 20.0 && meal.proteinG >= 20.0) score += 35.0

                // Fit remaining calories: prefer meals that fit within remaining calories
                if (remainingCalories > 150) {
                    if (meal.calories <= remainingCalories + 100) {
                        score += 25.0
                    }
                }

                score
            }
    }

    /**
     * Generates actionable nutrient synergy and absorption tips based on today's logged food items.
     */
    fun detectSynergyTips(
        todayItems: List<FoodItem>,
        nutrientBars: List<NutrientProgress>,
        waterCurrentMl: Int
    ): List<NutrientSynergyTip> {
        val tips = mutableListOf<NutrientSynergyTip>()
        val allNames = todayItems.map { it.name.lowercase() }

        // 1. Plant-Based Iron + Vitamin C Synergy
        val hasPlantIron = allNames.any { it.contains("mercimek") || it.contains("ıspanak") || it.contains("yulaf") || it.contains("nohut") || it.contains("fasulye") }
        val hasVitC = allNames.any { it.contains("limon") || it.contains("portakal") || it.contains("mandalina") || it.contains("biber") || it.contains("kivi") || it.contains("çilek") }
        if (hasPlantIron && !hasVitC) {
            tips.add(
                NutrientSynergyTip(
                    id = "tip_iron_vit_c",
                    title = "🌱 Demir Emilimini 3 Kat Artırın",
                    description = "Bugün bitkisel demir içeren gıdalar (mercimek, ıspanak veya bakliyat) tükettiniz. Bitkisel demirin vücutta emilimini 3 katına çıkarmak için öğününüze limon, kapya biber veya turunçgil ekleyin.",
                    highlightNutrient = "Demir + C Vitamini"
                )
            )
        }

        // 2. High Fiber + Water Intake Hydration
        val totalFiber = todayItems.sumOf { it.fiberG }
        if (totalFiber >= 18.0 && waterCurrentMl < 1800) {
            tips.add(
                NutrientSynergyTip(
                    id = "tip_fiber_water",
                    title = "🌾 Yüksek Lif Aldınız: Bol Su İçin",
                    description = "Bugün ${totalFiber.toInt()}g lif aldınız. Liflerin bağırsakta şişip sindirimi rahatlatması ve kramp oluşmaması için lütfen su tüketiminizi en az 500ml artırın.",
                    highlightNutrient = "Lif + Hidrasyon"
                )
            )
        }

        // 3. Vitamin D & Carotenoids with Healthy Fats
        val hasEggOrSalmon = allNames.any { it.contains("yumurta") || it.contains("somon") || it.contains("havuç") || it.contains("domates") }
        val hasHealthyFat = allNames.any { it.contains("zeytinyağ") || it.contains("avokado") || it.contains("ceviz") || it.contains("badem") }
        if (hasEggOrSalmon && !hasHealthyFat) {
            tips.add(
                NutrientSynergyTip(
                    id = "tip_fat_soluble_vitamins",
                    title = "🥑 Yağda Çözünen Vitaminler (A, D, K)",
                    description = "Yumurta ve sebzelerdeki A ve D vitaminlerinin hücreleriniz tarafından tam emilmesi için yanına 1 tatlı kaşığı sızma zeytinyağı veya 2-3 adet ceviz ekleyin.",
                    highlightNutrient = "D Vitamini + Sağlıklı Yağ"
                )
            )
        }

        // 4. Coffee/Tea Spacing with High Calcium / Iron Meals
        val hasCoffeeOrTea = allNames.any { it.contains("kahve") || it.contains("çay") }
        val hasHighCalciumOrIron = allNames.any { it.contains("yoğurt") || it.contains("peynir") || it.contains("et") || it.contains("köfte") || it.contains("tavuk") }
        if (hasCoffeeOrTea && hasHighCalciumOrIron) {
            tips.add(
                NutrientSynergyTip(
                    id = "tip_caffeine_mineral_spacing",
                    title = "☕ Çay/Kahve Zamanlaması",
                    description = "Çay ve kahvedeki tanenler demir ve kalsiyum emilimini %50'ye kadar düşürebilir. Yemekten 45 dakika sonra içmek maksimum besin faydası sağlar.",
                    highlightNutrient = "Mineral Emilimi"
                )
            )
        }

        return tips.take(2)
    }
}

