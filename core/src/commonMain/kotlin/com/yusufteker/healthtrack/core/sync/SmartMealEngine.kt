package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.CustomFoodItem
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.Micronutrient
import com.yusufteker.healthtrack.core.model.NutrientProgress
import com.yusufteker.healthtrack.core.model.NutrientSynergyTip
import com.yusufteker.healthtrack.core.model.SmartMealSuggestion

object SmartMealEngine {

    private fun curatedMealDatabase(language: AppLanguage): List<SmartMealSuggestion> = listOf(
        // ── 0. Grab & Go & Quick Snacks ──
        SmartMealSuggestion(
            id = "meal_banana_peanut_butter",
            title = trEn("Muz & Şekersiz Fıstık Ezmesi", "Banana & Unsweetened Peanut Butter", language),
            subtitle = trEn("Potasyum, Sağlıklı Yağ & Anında Enerji", "Potassium, Healthy Fats & Instant Energy", language),
            tags = if (language == AppLanguage.TR) listOf("Aç & Ye", "Pişirmesiz", "2 Malzeme", "Hızlı Enerji") else listOf("Ready-to-Eat", "No-Cook", "2 Ingredients", "Quick Energy"),
            prepTimeMinutes = 0,
            ingredients = if (language == AppLanguage.TR) listOf("1 Adet Orta Boy Muz", "1 Tatlı Kaşığı Şekersiz Fıstık Ezmesi") else listOf("1 Medium Banana", "1 tsp Unsweetened Peanut Butter"),
            calories = 185,
            proteinG = 4.5,
            carbsG = 28.0,
            fatG = 6.5,
            fiberG = 3.5,
            keyMicros = listOf(
                Micronutrient(trEn("Potasyum", "Potassium", language), 450.0, "mg"),
                Micronutrient(trEn("Magnezyum", "Magnesium", language), 42.0, "mg"),
                Micronutrient(trEn("Lif", "Fiber", language), 3.5, "g")
            ),
            instructions = trEn(
                "Muzun kabuğunu soyun, fıstık ezmesiyle anında aç-ye şeklinde tüketin.",
                "Peel the banana and enjoy immediately with a spoonful of peanut butter.",
                language
            )
        ),
        SmartMealSuggestion(
            id = "meal_green_apple_walnut",
            title = trEn("Yeşil Elma & Çiğ Ceviz", "Green Apple & Raw Walnuts", language),
            subtitle = trEn("Lif, Antioksidan & Tok Tutan Ara Öğün", "Fiber, Antioxidants & Satiety Snack", language),
            tags = if (language == AppLanguage.TR) listOf("Aç & Ye", "Pişirmesiz", "2 Malzeme", "Lif Zengin") else listOf("Ready-to-Eat", "No-Cook", "2 Ingredients", "High Fiber"),
            prepTimeMinutes = 0,
            ingredients = if (language == AppLanguage.TR) listOf("1 Adet Orta Boy Yeşil Elma", "3 Adet Bütün Ceviz İçi") else listOf("1 Medium Green Apple", "3 Whole Walnut Halves"),
            calories = 160,
            proteinG = 3.0,
            carbsG = 21.0,
            fatG = 8.5,
            fiberG = 4.2,
            keyMicros = listOf(
                Micronutrient(trEn("C Vitamini", "Vitamin C", language), 12.0, "mg"),
                Micronutrient(trEn("Magnezyum", "Magnesium", language), 35.0, "mg"),
                Micronutrient(trEn("Lif", "Fiber", language), 4.2, "g")
            ),
            instructions = trEn(
                "Elmayı yıkayıp cevizlerle birlikte aç-ye şeklinde doğrudan tüketin.",
                "Wash the apple and eat fresh together with raw walnuts.",
                language
            )
        ),

        // ── 1. High Fiber & Magnesium ──
        SmartMealSuggestion(
            id = "meal_chia_oat_bowl",
            title = trEn("Chia Tohumlu & Muzlu Yulaf Kasesi", "Chia & Banana Oatmeal Bowl", language),
            subtitle = trEn("Yüksek Lif & Magnezyum Deposu", "High Fiber & Magnesium Powerhouse", language),
            tags = if (language == AppLanguage.TR) listOf("Az Malzemeli", "Hızlı (2 dk)", "Pişirmesiz", "Lif Zengin") else listOf("Few Ingredients", "Quick (2 min)", "No-Cook", "High Fiber"),
            prepTimeMinutes = 2,
            ingredients = if (language == AppLanguage.TR) listOf("40g Yulaf Ezmesi", "1 Tatlı Kaşığı Chia Tohumu", "150ml Yarım Yağlı Süt/Badem Sütü", "Yarım Muz") else listOf("40g Rolled Oats", "1 tsp Chia Seeds", "150ml Skim/Almond Milk", "Half Banana"),
            calories = 275,
            proteinG = 11.5,
            carbsG = 42.0,
            fatG = 6.5,
            fiberG = 8.2,
            keyMicros = listOf(
                Micronutrient(trEn("Lif", "Fiber", language), 8.2, "g"),
                Micronutrient(trEn("Magnezyum", "Magnesium", language), 85.0, "mg"),
                Micronutrient(trEn("Kalsiyum", "Calcium", language), 220.0, "mg")
            ),
            instructions = trEn(
                "Yulaf ve chia tohumunu süt ile karıştırın. Üzerine dilimlenmiş yarım muz ekleyip afiyetle tüketin.",
                "Mix oats and chia seeds with milk. Top with sliced banana and enjoy.",
                language
            )
        ),
        SmartMealSuggestion(
            id = "meal_avocado_green_salad",
            title = trEn("Yeşil Mercimek & Avokado Salatası", "Green Lentil & Avocado Salad", language),
            subtitle = trEn("Bitkisel Lif, Demir ve Sağlıklı Yağ", "Plant Fiber, Iron & Healthy Fats", language),
            tags = if (language == AppLanguage.TR) listOf("Lif Zengin", "Demir & Magnezyum", "Az Malzemeli") else listOf("High Fiber", "Iron & Magnesium", "Few Ingredients"),
            prepTimeMinutes = 5,
            ingredients = if (language == AppLanguage.TR) listOf("1 Su Bardağı Haşlanmış Yeşil Mercimek", "Yarım Avokado", "1 Avuç Roka veya Maydanoz", "1 Yemek Kaşığı Limon & Zeytinyağı") else listOf("1 Cup Boiled Green Lentils", "Half Avocado", "1 Handful Arugula or Parsley", "1 tbsp Lemon & Olive Oil"),
            calories = 340,
            proteinG = 16.0,
            carbsG = 36.0,
            fatG = 14.5,
            fiberG = 12.4,
            keyMicros = listOf(
                Micronutrient(trEn("Lif", "Fiber", language), 12.4, "g"),
                Micronutrient(trEn("Demir", "Iron", language), 5.2, "mg"),
                Micronutrient(trEn("Magnezyum", "Magnesium", language), 72.0, "mg"),
                Micronutrient(trEn("C Vitamini", "Vitamin C", language), 28.0, "mg")
            ),
            instructions = trEn(
                "Haşlanmış mercimeği doğranmış avokado ve yeşilliklerle karıştırın, limon ve zeytinyağı sosu gezdirin.",
                "Toss boiled lentils with diced avocado and greens; dress with fresh lemon and olive oil.",
                language
            )
        ),
        SmartMealSuggestion(
            id = "meal_pumpkin_seed_yogurt",
            title = trEn("Kabak Çekirdekli & Keten Tohumlu Süzme Yoğurt", "Greek Yogurt with Pumpkin & Flax Seeds", language),
            subtitle = trEn("Magnezyum, Çinko ve Doğal Protein", "Magnesium, Zinc & Natural Protein", language),
            tags = if (language == AppLanguage.TR) listOf("Aç & Ye", "Pişirmesiz", "3 Malzeme", "Yüksek Magnezyum") else listOf("Ready-to-Eat", "No-Cook", "3 Ingredients", "High Magnesium"),
            prepTimeMinutes = 0,
            ingredients = if (language == AppLanguage.TR) listOf("150g Süzme Yoğurt", "1 Yemek Kaşığı Çiğ Kabak Çekirdeği", "1 Çay Kaşığı Öğütülmüş Keten Tohumu") else listOf("150g Greek Yogurt", "1 tbsp Raw Pumpkin Seeds", "1 tsp Ground Flaxseed"),
            calories = 220,
            proteinG = 19.5,
            carbsG = 7.0,
            fatG = 11.5,
            fiberG = 4.5,
            keyMicros = listOf(
                Micronutrient(trEn("Magnezyum", "Magnesium", language), 110.0, "mg"),
                Micronutrient(trEn("Kalsiyum", "Calcium", language), 210.0, "mg"),
                Micronutrient(trEn("Lif", "Fiber", language), 4.5, "g")
            ),
            instructions = trEn(
                "Süzme yoğurdun üzerine çiğ kabak çekirdeği ve keten tohumunu serpin, anında hazır.",
                "Sprinkle raw pumpkin seeds and ground flaxseed onto Greek yogurt and serve right away.",
                language
            )
        ),

        // ── 2. High Protein & Low Calorie ──
        SmartMealSuggestion(
            id = "meal_spinach_mushroom_omelet",
            title = trEn("Ispanaklı & Mantarlı Lorlu Omlet", "Spinach, Mushroom & Cottage Cheese Omelet", language),
            subtitle = trEn("Demir, B12 ve Yüksek Kas Proteini", "Iron, B12 & Lean Muscle Protein", language),
            tags = if (language == AppLanguage.TR) listOf("Yüksek Protein", "Demir & C Vitamini", "Tavada (7 dk)") else listOf("High Protein", "Iron & Vitamin C", "Pan (7 min)"),
            prepTimeMinutes = 7,
            ingredients = if (language == AppLanguage.TR) listOf("2 Adet Yumurta", "50g Lor Peyniri", "1 Avuç Taze Ispanak", "3-4 Adet Kültür Mantarı") else listOf("2 Eggs", "50g Cottage Cheese", "1 Handful Fresh Spinach", "3-4 White Mushrooms"),
            calories = 265,
            proteinG = 24.5,
            carbsG = 4.5,
            fatG = 15.0,
            fiberG = 2.8,
            keyMicros = listOf(
                Micronutrient(trEn("Demir", "Iron", language), 3.8, "mg"),
                Micronutrient(trEn("Kalsiyum", "Calcium", language), 160.0, "mg"),
                Micronutrient(trEn("B12 Vitamini", "Vitamin B12", language), 1.6, "µg"),
                Micronutrient(trEn("Magnezyum", "Magnesium", language), 48.0, "mg")
            ),
            instructions = trEn(
                "Mantarları ve ıspanağı tavada 2 dakika soteleyin. Çırpılmış yumurtaları ve loru ekleyip kısık ateşte pişirin.",
                "Sauté mushrooms and spinach for 2 min. Add beaten eggs and cottage cheese, cooking gently on low heat.",
                language
            )
        ),
        SmartMealSuggestion(
            id = "meal_grilled_chicken_steamed_broccoli",
            title = trEn("Izgara Tavuk Göğsü & Buharda Brokoli", "Grilled Chicken Breast & Steamed Broccoli", language),
            subtitle = trEn("Ultra Saf Protein & C Vitamini", "Ultra Lean Protein & Vitamin C", language),
            tags = if (language == AppLanguage.TR) listOf("Yüksek Protein", "C Vitamini", "Az Malzemeli") else listOf("High Protein", "Vitamin C", "Few Ingredients"),
            prepTimeMinutes = 15,
            ingredients = if (language == AppLanguage.TR) listOf("180g Tavuk Göğsü", "150g Brokoli", "Yarım Limonun Suyu", "1 Çay Kaşığı Zeytinyağı") else listOf("180g Chicken Breast", "150g Broccoli", "Juice of Half a Lemon", "1 tsp Olive Oil"),
            calories = 310,
            proteinG = 42.0,
            carbsG = 9.0,
            fatG = 7.5,
            fiberG = 4.8,
            keyMicros = listOf(
                Micronutrient(trEn("C Vitamini", "Vitamin C", language), 78.0, "mg"),
                Micronutrient(trEn("Demir", "Iron", language), 2.4, "mg"),
                Micronutrient(trEn("B12 Vitamini", "Vitamin B12", language), 0.6, "µg")
            ),
            instructions = trEn(
                "Tavuk göğsünü yağsız tavada ızgara yapın. Brokoliyi buharda haşlayıp üzerine limon suyu gezdirin.",
                "Grill chicken breast in a nonstick pan. Steam broccoli and drizzle with fresh lemon juice.",
                language
            )
        ),
        SmartMealSuggestion(
            id = "meal_tuna_arugula_bowl",
            title = trEn("Ton Balıklı & Cevizli Roka Kasesi", "Tuna & Walnut Arugula Bowl", language),
            subtitle = trEn("Omega-3, B12 Vitamini ve Sağlıklı Yağlar", "Omega-3, Vitamin B12 & Healthy Fats", language),
            tags = if (language == AppLanguage.TR) listOf("Az Malzemeli", "Pişirmesiz (2 dk)", "Omega-3") else listOf("Few Ingredients", "No-Cook (2 min)", "Omega-3"),
            prepTimeMinutes = 2,
            ingredients = if (language == AppLanguage.TR) listOf("1 Kutu Konserve Ton Balığı (Süzülmüş)", "1 Demet Roka", "3 Adet Bütün Ceviz İçi", "Yarım Limon") else listOf("1 Can Tuna (Drained)", "1 Bunch Arugula", "3 Whole Walnut Halves", "Half Lemon"),
            calories = 295,
            proteinG = 31.0,
            carbsG = 4.0,
            fatG = 16.5,
            fiberG = 3.2,
            keyMicros = listOf(
                Micronutrient(trEn("B12 Vitamini", "Vitamin B12", language), 2.2, "µg"),
                Micronutrient(trEn("Demir", "Iron", language), 2.8, "mg"),
                Micronutrient(trEn("Magnezyum", "Magnesium", language), 55.0, "mg"),
                Micronutrient(trEn("C Vitamini", "Vitamin C", language), 32.0, "mg")
            ),
            instructions = trEn(
                "Rokaları yıkayıp tabağa alın. Süzülmüş ton balığını ve kırılmış cevizleri üzerine ekleyip bol limon sıkın.",
                "Rinse arugula and place in a bowl. Top with drained tuna and crushed walnuts, then squeeze fresh lemon.",
                language
            )
        ),

        // ── 3. Calcium, Vitamin C & Practical Snacks ──
        SmartMealSuggestion(
            id = "meal_berry_cottage_parfait",
            title = trEn("Yaban Mersinli & Bademli Lor Kasesi", "Blueberry & Almond Cottage Bowl", language),
            subtitle = trEn("Kalsiyum & Antioksidan Gücü", "Calcium & Antioxidant Boost", language),
            tags = if (language == AppLanguage.TR) listOf("Aç & Ye", "3 Malzeme", "Tatlı Krizine Birebir", "Pişirmesiz") else listOf("Ready-to-Eat", "3 Ingredients", "Sweet Craving Fix", "No-Cook"),
            prepTimeMinutes = 0,
            ingredients = if (language == AppLanguage.TR) listOf("120g Lor Peyniri veya Süzme Yoğurt", "1 Avuç Yaban Mersini / Çilek", "10 Adet Çiğ Badem") else listOf("120g Cottage Cheese or Greek Yogurt", "1 Handful Blueberries / Strawberries", "10 Raw Almonds"),
            calories = 210,
            proteinG = 18.0,
            carbsG = 16.0,
            fatG = 8.5,
            fiberG = 4.2,
            keyMicros = listOf(
                Micronutrient(trEn("Kalsiyum", "Calcium", language), 240.0, "mg"),
                Micronutrient(trEn("C Vitamini", "Vitamin C", language), 22.0, "mg"),
                Micronutrient(trEn("Magnezyum", "Magnesium", language), 45.0, "mg")
            ),
            instructions = trEn(
                "Loru kaseye alın, üzerine meyveleri ve bademleri ekleyerek anında tüketin.",
                "Scoop cottage cheese into a bowl, top with berries and almonds, and enjoy right away.",
                language
            )
        ),
        SmartMealSuggestion(
            id = "meal_boiled_eggs_walnuts_bellpepper",
            title = trEn("Haşlanmış Yumurta, Kapya Biber & Ceviz Tabağı", "Boiled Eggs, Red Bell Pepper & Walnuts", language),
            subtitle = trEn("D Vitamini, C Vitamini & Kolin", "Vitamin D, Vitamin C & Choline", language),
            tags = if (language == AppLanguage.TR) listOf("Az Malzemeli", "D Vitamini", "Hızlı (6 dk)") else listOf("Few Ingredients", "Vitamin D", "Quick (6 min)"),
            prepTimeMinutes = 6,
            ingredients = if (language == AppLanguage.TR) listOf("2 Adet Haşlanmış Yumurta", "1 Adet Kırmızı Kapya Biber", "3 Adet Ceviz İçi") else listOf("2 Boiled Eggs", "1 Red Bell Pepper", "3 Walnut Halves"),
            calories = 280,
            proteinG = 16.5,
            carbsG = 7.0,
            fatG = 20.0,
            fiberG = 3.5,
            keyMicros = listOf(
                Micronutrient(trEn("D Vitamini", "Vitamin D", language), 2.0, "µg"),
                Micronutrient(trEn("C Vitamini", "Vitamin C", language), 95.0, "mg"),
                Micronutrient(trEn("Demir", "Iron", language), 2.2, "mg"),
                Micronutrient(trEn("B12 Vitamini", "Vitamin B12", language), 1.2, "µg")
            ),
            instructions = trEn(
                "Haşlanmış yumurtaları dilimleyin. Yanında taze kapya biber ve ceviz ile tüketin.",
                "Slice the boiled eggs and serve alongside fresh red bell pepper and walnuts.",
                language
            )
        )
    )

    fun matchSmartMeals(
        remainingCalories: Int,
        missingProteinG: Double,
        nutrientBars: List<NutrientProgress>,
        recent3DaysFoodNames: Set<String>,
        customFoods: List<CustomFoodItem> = emptyList(),
        frequentFoods: List<FoodItem> = emptyList(),
        onlyQuick: Boolean = false,
        onlyFewIngredients: Boolean = false,
        excludeRecent3Days: Boolean = false,
        highProteinOnly: Boolean = false,
        fromUserDbOnly: Boolean = false,
        language: AppLanguage = LocalizationManager.currentLanguage
    ): List<SmartMealSuggestion> {
        val fiberProgress = nutrientBars.firstOrNull { it.key == "fiber" }?.percentage ?: 0f
        val magnesiumProgress = nutrientBars.firstOrNull { it.key == "magnesium" }?.percentage ?: 0f
        val ironProgress = nutrientBars.firstOrNull { it.key == "iron" }?.percentage ?: 0f
        val vitCProgress = nutrientBars.firstOrNull { it.key == "vit_c" }?.percentage ?: 0f
        val calciumProgress = nutrientBars.firstOrNull { it.key == "calcium" }?.percentage ?: 0f

        val userDbSuggestions = mutableListOf<SmartMealSuggestion>()
        customFoods.forEach { custom ->
            val ingredientsList = if (custom.isMix && custom.ingredients.isNotEmpty()) {
                custom.ingredients.map { "${it.defaultAmount} ${it.unitLabel} ${it.name}" }
            } else {
                listOf("${custom.portionUnit} ${custom.name}")
            }
            val prepTime = estimatePrepTimeMinutes(custom.name, custom.isMix)
            val tags = buildList {
                add(trEn("Kendi Ürününüz (DB)", "Your Custom Food (DB)", language))
                add(custom.portionUnit)
                if (prepTime <= 0) add(trEn("Aç & Ye", "Ready-to-Eat", language))
                else if (prepTime <= 2) add(trEn("Hızlı", "Quick", language))
                if (custom.proteinG >= 20.0) add(trEn("Yüksek Protein", "High Protein", language))
                if (custom.fiberG >= 4.0) add(trEn("Lif Zengin", "High Fiber", language))
            }
            val instructions = if (prepTime <= 0) {
                trEn(
                    "Doğrudan tüketime hazır ${custom.name} (${custom.portionUnit}) porsiyonu. Aç ve anında ye!",
                    "Ready-to-eat ${custom.name} (${custom.portionUnit}) serving. Grab and enjoy immediately!",
                    language
                )
            } else {
                trEn(
                    "Kendi veritabanınızda kayıtlı ${custom.name} (${custom.portionUnit}) porsiyonu.",
                    "Saved ${custom.name} (${custom.portionUnit}) serving from your personal library.",
                    language
                )
            }
            userDbSuggestions.add(
                SmartMealSuggestion(
                    id = "db_custom_${custom.id}",
                    title = custom.name,
                    subtitle = trEn(
                        "📦 Kendi Kütüphanenizden (${custom.brand ?: custom.portionUnit})",
                        "📦 From Your Library (${custom.brand ?: custom.portionUnit})",
                        language
                    ),
                    tags = tags,
                    prepTimeMinutes = prepTime,
                    ingredients = ingredientsList,
                    calories = custom.calories,
                    proteinG = custom.proteinG,
                    carbsG = custom.carbsG,
                    fatG = custom.fatG,
                    fiberG = custom.fiberG,
                    keyMicros = custom.micronutrients,
                    instructions = instructions
                )
            )
        }

        frequentFoods.take(6).forEach { freq ->
            if (userDbSuggestions.none { it.title.equals(freq.name, ignoreCase = true) }) {
                val prepTime = estimatePrepTimeMinutes(freq.name, isMix = false)
                val tags = buildList {
                    add(trEn("Sık Tüketilen (DB)", "Frequent (DB)", language))
                    add(freq.formattedPortion)
                    if (prepTime <= 0) add(trEn("Aç & Ye", "Ready-to-Eat", language))
                    else if (prepTime <= 2) add(trEn("Hızlı", "Quick", language))
                    if (freq.proteinG >= 20.0) add(trEn("Yüksek Protein", "High Protein", language))
                }
                val instructions = if (prepTime <= 0) {
                    trEn(
                        "Doğrudan tüketime hazır ${freq.name} porsiyonu. Aç ve anında tüket!",
                        "Ready-to-eat ${freq.name} serving. Grab and enjoy right away!",
                        language
                    )
                } else {
                    trEn(
                        "Daha önce sıkça tükettiğiniz pratik ${freq.name} porsiyonu.",
                        "Practical ${freq.name} serving you've frequently logged before.",
                        language
                    )
                }
                userDbSuggestions.add(
                    SmartMealSuggestion(
                        id = "db_freq_${freq.id.ifBlank { freq.name.hashCode().toString() }}",
                        title = freq.name,
                        subtitle = trEn(
                            "⭐ Sık Tükettiğiniz Favori (${freq.formattedPortion})",
                            "⭐ Frequent Favorite (${freq.formattedPortion})",
                            language
                        ),
                        tags = tags,
                        prepTimeMinutes = prepTime,
                        ingredients = listOf("${freq.formattedPortion} ${freq.name}"),
                        calories = freq.calories,
                        proteinG = freq.proteinG,
                        carbsG = freq.carbsG,
                        fatG = freq.fatG,
                        fiberG = freq.fiberG,
                        keyMicros = freq.micronutrients,
                        instructions = instructions
                    )
                )
            }
        }

        val curated = curatedMealDatabase(language)
        val candidatePool = if (fromUserDbOnly) {
            if (userDbSuggestions.isNotEmpty()) userDbSuggestions else curated
        } else {
            userDbSuggestions + curated
        }

        return candidatePool
            .filter { meal ->
                if (onlyQuick && meal.prepTimeMinutes > 10) return@filter false
                if (onlyFewIngredients && meal.ingredients.size > 4) return@filter false
                if (highProteinOnly && meal.proteinG < 20.0) return@filter false

                if (excludeRecent3Days && recent3DaysFoodNames.isNotEmpty()) {
                    val mealKeywords = (listOf(meal.title) + meal.ingredients).map { it.lowercase() }
                    val hasEatenRecently = recent3DaysFoodNames.any { eaten ->
                        val cleanEaten = eaten.lowercase()
                        mealKeywords.any { keyword ->
                            (cleanEaten.contains("yumurta") || cleanEaten.contains("egg")) && (keyword.contains("yumurta") || keyword.contains("egg")) ||
                                (cleanEaten.contains("tavuk") || cleanEaten.contains("chicken")) && (keyword.contains("tavuk") || keyword.contains("chicken")) ||
                                (cleanEaten.contains("yulaf") || cleanEaten.contains("oat")) && (keyword.contains("yulaf") || keyword.contains("oat")) ||
                                (cleanEaten.contains("ton balık") || cleanEaten.contains("tuna")) && (keyword.contains("ton balık") || keyword.contains("tuna")) ||
                                (cleanEaten.contains("mercimek") || cleanEaten.contains("lentil")) && (keyword.contains("mercimek") || keyword.contains("lentil")) ||
                                (cleanEaten.contains("yoğurt") || cleanEaten.contains("yogurt")) && (keyword.contains("yoğurt") || keyword.contains("yogurt"))
                        }
                    }
                    if (hasEatenRecently) return@filter false
                }

                true
            }
            .sortedByDescending { meal ->
                var score = 0.0
                if (meal.tags.any { it.contains("(DB)") }) {
                    score += 50.0
                }
                if (fiberProgress < 50f && meal.fiberG >= 6.0) score += 40.0
                if (magnesiumProgress < 50f && meal.keyMicros.any { (it.name.contains("Magnezyum", ignoreCase = true) || it.name.contains("Magnesium", ignoreCase = true)) && it.amount >= 60.0 }) score += 35.0
                if (ironProgress < 50f && meal.keyMicros.any { (it.name.contains("Demir", ignoreCase = true) || it.name.contains("Iron", ignoreCase = true)) && it.amount >= 3.0 }) score += 30.0
                if (vitCProgress < 50f && meal.keyMicros.any { (it.name.contains("C", ignoreCase = true)) && it.amount >= 25.0 }) score += 25.0
                if (calciumProgress < 50f && meal.keyMicros.any { (it.name.contains("Kalsiyum", ignoreCase = true) || it.name.contains("Calcium", ignoreCase = true)) && it.amount >= 150.0 }) score += 20.0
                if (missingProteinG > 20.0 && meal.proteinG >= 20.0) score += 35.0
                if (remainingCalories > 150 && meal.calories <= remainingCalories + 100) {
                    score += 25.0
                }
                score
            }
    }

    fun detectSynergyTips(
        todayItems: List<FoodItem>,
        nutrientBars: List<NutrientProgress>,
        waterCurrentMl: Int,
        language: AppLanguage = LocalizationManager.currentLanguage
    ): List<NutrientSynergyTip> {
        val tips = mutableListOf<NutrientSynergyTip>()
        val allNames = todayItems.map { it.name.lowercase() }

        val hasPlantIron = allNames.any {
            it.contains("mercimek") || it.contains("ıspanak") || it.contains("yulaf") || it.contains("nohut") || it.contains("fasulye") ||
                it.contains("lentil") || it.contains("spinach") || it.contains("oat") || it.contains("chickpea") || it.contains("bean")
        }
        val hasVitC = allNames.any {
            it.contains("limon") || it.contains("portakal") || it.contains("mandalina") || it.contains("biber") || it.contains("kivi") || it.contains("çilek") ||
                it.contains("lemon") || it.contains("orange") || it.contains("pepper") || it.contains("kiwi") || it.contains("strawberr")
        }
        if (hasPlantIron && !hasVitC) {
            tips.add(
                NutrientSynergyTip(
                    id = "tip_iron_vit_c",
                    title = trEn("🌱 Demir Emilimini 3 Kat Artırın", "🌱 Boost Iron Absorption 3x", language),
                    description = trEn(
                        "Bugün bitkisel demir içeren gıdalar (mercimek, ıspanak veya bakliyat) tükettiniz. Bitkisel demirin vücutta emilimini 3 katına çıkarmak için öğününüze limon, kapya biber veya turunçgil ekleyin.",
                        "You ate plant-based iron today (lentils, spinach, or legumes). Pair your meal with lemon, red bell pepper, or citrus to triple non-heme iron absorption.",
                        language
                    ),
                    highlightNutrient = trEn("Demir + C Vitamini", "Iron + Vitamin C", language)
                )
            )
        }

        val totalFiber = todayItems.sumOf { it.fiberG }
        if (totalFiber >= 18.0 && waterCurrentMl < 1800) {
            tips.add(
                NutrientSynergyTip(
                    id = "tip_fiber_water",
                    title = trEn("🌾 Yüksek Lif Aldınız: Bol Su İçin", "🌾 High Fiber Intake: Hydrate Well", language),
                    description = trEn(
                        "Bugün ${totalFiber.toInt()}g lif aldınız. Liflerin bağırsakta şişip sindirimi rahatlatması ve kramp oluşmaması için lütfen su tüketiminizi en az 500ml artırın.",
                        "You consumed ${totalFiber.toInt()}g of fiber today. Drink at least 500ml more water so fiber digests smoothly and keeps you comfortable.",
                        language
                    ),
                    highlightNutrient = trEn("Lif + Hidrasyon", "Fiber + Hydration", language)
                )
            )
        }

        val hasEggOrSalmon = allNames.any {
            it.contains("yumurta") || it.contains("somon") || it.contains("havuç") || it.contains("domates") ||
                it.contains("egg") || it.contains("salmon") || it.contains("carrot") || it.contains("tomato")
        }
        val hasHealthyFat = allNames.any {
            it.contains("zeytinyağ") || it.contains("avokado") || it.contains("ceviz") || it.contains("badem") ||
                it.contains("olive oil") || it.contains("avocado") || it.contains("walnut") || it.contains("almond")
        }
        if (hasEggOrSalmon && !hasHealthyFat) {
            tips.add(
                NutrientSynergyTip(
                    id = "tip_fat_soluble_vitamins",
                    title = trEn("🥑 Yağda Çözünen Vitaminler (A, D, K)", "🥑 Fat-Soluble Vitamins (A, D, K)", language),
                    description = trEn(
                        "Yumurta ve sebzelerdeki A ve D vitaminlerinin hücreleriniz tarafından tam emilmesi için yanına 1 tatlı kaşığı sızma zeytinyağı veya 2-3 adet ceviz ekleyin.",
                        "Add 1 tsp of extra virgin olive oil or a few walnuts alongside eggs and veggies so your body absorbs fat-soluble vitamins A & D fully.",
                        language
                    ),
                    highlightNutrient = trEn("D Vitamini + Sağlıklı Yağ", "Vitamin D + Healthy Fats", language)
                )
            )
        }

        val hasCoffeeOrTea = allNames.any {
            it.contains("kahve") || it.contains("çay") || it.contains("coffee") || it.contains("tea")
        }
        val hasHighCalciumOrIron = allNames.any {
            it.contains("yoğurt") || it.contains("peynir") || it.contains("et") || it.contains("köfte") || it.contains("tavuk") ||
                it.contains("yogurt") || it.contains("cheese") || it.contains("beef") || it.contains("meat") || it.contains("chicken")
        }
        if (hasCoffeeOrTea && hasHighCalciumOrIron) {
            tips.add(
                NutrientSynergyTip(
                    id = "tip_caffeine_mineral_spacing",
                    title = trEn("☕ Çay/Kahve Zamanlaması", "☕ Coffee & Tea Timing", language),
                    description = trEn(
                        "Çay ve kahvedeki tanenler demir ve kalsiyum emilimini %50'ye kadar düşürebilir. Yemekten 45 dakika sonra içmek maksimum besin faydası sağlar.",
                        "Tannins in coffee and tea can reduce iron and calcium absorption by up to 50%. Waiting 45 minutes after meals maximizes mineral uptake.",
                        language
                    ),
                    highlightNutrient = trEn("Mineral Emilimi", "Mineral Absorption", language)
                )
            )
        }

        return tips.take(2)
    }

    fun estimatePrepTimeMinutes(foodName: String, isMix: Boolean = false): Int {
        if (isMix) return 4
        val name = foodName.lowercase().trim()

        val isInstantReady = listOf(
            "muz", "elma", "armut", "mandalina", "portakal", "çilek", "kiraz", "vişne",
            "şeftali", "kayısı", "karpuz", "kavun", "incir", "üzüm", "hurma", "erik",
            "yaban mersini", "dut", "ananas", "kivi", "greyfurt", "avokado",
            "ceviz", "badem", "fındık", "fıstık", "kaju", "çekirdek", "leblebi",
            "süt", "kefir", "ayran", "yoğurt", "peynir", "kaşar", "lor",
            "su", "maden suyu", "soda",
            "protein bar", "bar", "bisküvi", "kraker", "galeta", "ekmek", "simit",
            "çikolata", "kuru üzüm", "kuru kayısı", "kuru incir",
            "banana", "apple", "pear", "orange", "strawberry", "blueberry", "avocado",
            "walnut", "almond", "hazelnut", "peanut", "cashew", "milk", "yogurt", "cheese", "water", "bread", "chocolate"
        ).any { name.contains(it) }

        if (isInstantReady) return 0

        val isQuickColdPrep = listOf(
            "shake", "protein tozu", "whey", "yulaf", "chia", "kase", "bowl",
            "sandviç", "tost", "müsli", "gevrek", "ton balık", "ton balığı",
            "oat", "sandwich", "toast", "muesli", "cereal", "tuna"
        ).any { name.contains(it) }

        if (isQuickColdPrep) return 2

        val isMediumCook = listOf(
            "omlet", "menemen", "haşlanmış yumurta", "haşlama yumurta", "makarna", "pilav",
            "sote", "tavada", "krep", "pankek",
            "omelet", "boiled egg", "pasta", "rice", "sauté", "pancake", "crepe"
        ).any { name.contains(it) }

        if (isMediumCook) return 6

        val isLongCook = listOf(
            "tavuk", "et", "biftek", "köfte", "balık", "somon", "fırın", "ızgara",
            "çorba", "güveç", "sebze yemeği", "nohut", "kuru fasulye", "mercimek yemeği",
            "chicken", "beef", "steak", "meatball", "fish", "salmon", "baked", "grilled", "soup", "stew", "lentil"
        ).any { name.contains(it) }

        if (isLongCook) return 15

        return 1
    }
}
