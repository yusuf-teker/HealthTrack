package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.LocalizationManager
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import kotlin.math.roundToInt

@Suppress("UNUSED_PARAMETER")
class DailyMealPlanEngine(
    private val apiClient: HealthApiClient
) {

    suspend fun generateDailyPlan(
        userId: Int,
        dateIso: String,
        recentDaysFoodLogs: List<FoodLog>,
        goal: NutritionGoal,
        fitnessGoal: FitnessGoal,
        userWeightKg: Double,
        language: AppLanguage = LocalizationManager.currentLanguage
    ): DailyMealPlan {
        val targetProteinG = calculateProteinTarget(userWeightKg, fitnessGoal)
        val targetCalories = goal.calories.coerceAtLeast(1200)

        // 1. Analyze recent days food intake (last 3-5 days excluding today or including past days)
        val pastDaysFoods = recentDaysFoodLogs.filter { !it.isDeleted && it.date < dateIso }
        val distinctPastDates = pastDaysFoods.map { it.date }.distinct()
        val daysCount = distinctPastDates.size.coerceAtLeast(1)

        val totalPastCalories = pastDaysFoods.sumOf { it.totalCalories }
        val allPastItems = pastDaysFoods.flatMap { it.items }
        val totalPastProtein = allPastItems.sumOf { it.proteinG }
        val totalPastFiber = allPastItems.sumOf { it.fiberG }

        val avgDailyCalories = totalPastCalories / daysCount
        val avgDailyProtein = totalPastProtein / daysCount
        val avgDailyFiber = totalPastFiber / daysCount

        val allPastMicros = allPastItems.flatMap { it.micronutrients }
        val avgVitC = allPastMicros.filter { it.name.contains("C Vit", ignoreCase = true) || it.name.contains("Vitamin C", ignoreCase = true) }.sumOf { it.amount } / daysCount
        val avgIron = allPastMicros.filter { it.name.contains("Demir", ignoreCase = true) || it.name.contains("Iron", ignoreCase = true) }.sumOf { it.amount } / daysCount
        val avgMagnesium = allPastMicros.filter { it.name.contains("Magnezyum", ignoreCase = true) || it.name.contains("Magnesium", ignoreCase = true) }.sumOf { it.amount } / daysCount
        val avgCalcium = allPastMicros.filter { it.name.contains("Kalsiyum", ignoreCase = true) || it.name.contains("Calcium", ignoreCase = true) }.sumOf { it.amount } / daysCount

        // 2. Identify Deficiencies
        val detectedDeficiencies = mutableListOf<String>()
        val deficitNotes = mutableListOf<String>()

        var isHighProteinNeed = targetProteinG >= 100.0
        var isFiberNeed = false
        var isIronOrVitCNeed = false

        val proteinDiff = (targetProteinG - avgDailyProtein).roundToInt()
        if (proteinDiff > 10) {
            isHighProteinNeed = true
            detectedDeficiencies.add(trEn("Protein Açığı: ${proteinDiff}g", "Protein Gap: ${proteinDiff}g", language))
            deficitNotes.add(
                trEn(
                    "günlük protein hedefin ortalama ${proteinDiff}g gerisinde kaldı",
                    "daily protein averaged ${proteinDiff}g below target",
                    language
                )
            )
        }

        val fiberDiff = (goal.fiberG - avgDailyFiber).roundToInt()
        if (fiberDiff > 5) {
            isFiberNeed = true
            detectedDeficiencies.add(trEn("Lif İhtiyacı: ${fiberDiff}g", "Fiber Need: ${fiberDiff}g", language))
            deficitNotes.add(
                trEn(
                    "lif tüketimi hedefin ${fiberDiff}g altında",
                    "fiber intake was ${fiberDiff}g below target",
                    language
                )
            )
        }

        if (avgIron < goal.ironMg * 0.7) {
            isIronOrVitCNeed = true
            detectedDeficiencies.add(trEn("Demir Minerali Düşük", "Low Iron", language))
            deficitNotes.add(trEn("demir alımı yetersiz", "iron intake is insufficient", language))
        }
        if (avgVitC < goal.vitaminCMg * 0.6) {
            isIronOrVitCNeed = true
            detectedDeficiencies.add(trEn("C Vitamini Düşük", "Low Vitamin C", language))
            deficitNotes.add(trEn("C vitamini desteği gerekiyor", "vitamin C support is needed", language))
        }
        if (avgMagnesium < goal.magnesiumMg * 0.65) {
            detectedDeficiencies.add(trEn("Magnezyum Düşük", "Low Magnesium", language))
            deficitNotes.add(
                trEn(
                    "kas ve sinir sistemi için magnezyum açığı var",
                    "magnesium gap detected for muscle & nervous system",
                    language
                )
            )
        }
        if (avgCalcium < goal.calciumMg * 0.65) {
            detectedDeficiencies.add(trEn("Kalsiyum Yetersiz", "Low Calcium", language))
            deficitNotes.add(
                trEn(
                    "kemik sağlığı için kalsiyum takviyesi önerilir",
                    "calcium support recommended for bone health",
                    language
                )
            )
        }

        val deficiencySummaryText = if (deficitNotes.isNotEmpty()) {
            trEn(
                "Son $daysCount günlük beslenmeniz incelendi: ${deficitNotes.joinToString(", ")}. Bu eksikleri gün içine yayarak pratik şekilde tamamlayan, markette kolayca bulunan malzemelerle hazırlanan 4 öğünlük planınız:",
                "Analyzed your last $daysCount day(s) of nutrition: ${deficitNotes.joinToString(", ")}. Here is your practical 4-meal plan built with everyday ingredients to close these gaps:",
                language
            )
        } else {
            trEn(
                "Son günlerdeki beslenmeniz hedeflere yakın ve dengeli ilerliyor! Formunuzu ve enerjinizi koruyacak, pratik malzemelerle hazırlanan günün 4 öğünlük önerisi:",
                "Your recent nutrition is balanced and close to your targets! Here is today's practical 4-meal plan to keep your energy and physique on track:",
                language
            )
        }

        return generateDeterministicPlan(
            userId = userId,
            dateIso = dateIso,
            targetCalories = targetCalories,
            targetProteinG = targetProteinG,
            isHighProteinNeed = isHighProteinNeed,
            isFiberNeed = isFiberNeed,
            isIronOrVitCNeed = isIronOrVitCNeed,
            deficiencies = detectedDeficiencies,
            deficiencySummary = deficiencySummaryText,
            isAiGenerated = false,
            language = language
        )
    }

    private fun generateDeterministicPlan(
        userId: Int,
        dateIso: String,
        targetCalories: Int,
        targetProteinG: Double,
        isHighProteinNeed: Boolean,
        isFiberNeed: Boolean,
        isIronOrVitCNeed: Boolean,
        deficiencies: List<String>,
        deficiencySummary: String,
        isAiGenerated: Boolean = false,
        language: AppLanguage = LocalizationManager.currentLanguage
    ): DailyMealPlan {
        // ── 1. BREAKFAST ──
        val breakfast = if (isIronOrVitCNeed || isHighProteinNeed) {
            MealSlotSuggestion(
                slot = MealSlotType.BREAKFAST,
                title = trEn("Ispanaklı & Mantarlı Lorlu Omlet", "Spinach, Mushroom & Cottage Cheese Omelet", language),
                subtitle = trEn("Demir, C Vitamini & Yüksek Protein", "Iron, Vitamin C & High Protein", language),
                prepTimeMinutes = 7,
                ingredients = if (language == AppLanguage.TR) {
                    listOf("2 Adet Yumurta", "50g Lor Peyniri", "1 Avuç Taze Ispanak", "3 Adet Mantar", "1 Dilim Tam Buğday Ekmeği")
                } else {
                    listOf("2 Eggs", "50g Cottage Cheese", "1 Handful Fresh Spinach", "3 Mushrooms", "1 Slice Whole Wheat Bread")
                },
                instructions = trEn(
                    "Mantarları ve ıspanağı tavada 2 dk soteleyin. Çırpılmış yumurtaları ve loru döküp kısık ateşte pişirin. 1 dilim ekmekle tüketin.",
                    "Sauté mushrooms and spinach in a pan for 2 min. Pour in beaten eggs and cottage cheese, cook on low heat, and serve with 1 slice of whole wheat toast.",
                    language
                ),
                calories = 330,
                proteinG = 27.5,
                carbsG = 18.0,
                fatG = 16.0,
                fiberG = 4.5,
                keyBenefits = trEn(
                    "Ispanaktaki demir, yumurtanın kaliteli proteini ve lor peyniriyle kas onarımını başlatır.",
                    "Iron from spinach combined with complete egg & cottage cheese protein kickstarts muscle recovery.",
                    language
                ),
                targetDeficiencies = if (language == AppLanguage.TR) listOf("Protein", "Demir", "B12 Vitamini") else listOf("Protein", "Iron", "Vitamin B12"),
                itemsToAdd = listOf(
                    FoodItem(
                        id = "bm_1",
                        name = trEn("Ispanaklı Lorlu Omlet (2 Yumurta, 50g Lor)", "Spinach Cottage Cheese Omelet (2 Eggs, 50g)", language),
                        formattedPortion = trEn("1 Porsiyon", "1 Serving", language),
                        calories = 260, proteinG = 24.5, carbsG = 4.0, fatG = 15.0, fiberG = 2.5
                    ),
                    FoodItem(
                        id = "bm_2",
                        name = trEn("Tam Buğday Ekmeği", "Whole Wheat Bread", language),
                        formattedPortion = trEn("1 Dilim (30g)", "1 Slice (30g)", language),
                        calories = 70, proteinG = 3.0, carbsG = 14.0, fatG = 1.0, fiberG = 2.0
                    )
                )
            )
        } else {
            MealSlotSuggestion(
                slot = MealSlotType.BREAKFAST,
                title = trEn("Chia & Muzlu Sütlü Yulaf Kasesi", "Chia & Banana Milk Oatmeal Bowl", language),
                subtitle = trEn("Yüksek Lif, Magnezyum & Kalıcı Tokluk", "High Fiber, Magnesium & Lasting Satiety", language),
                prepTimeMinutes = 3,
                ingredients = if (language == AppLanguage.TR) {
                    listOf("40g Yulaf Ezmesi", "1 Tatlı Kaşığı Chia / Keten Tohumu", "150ml Süt", "Yarım Dilimlenmiş Muz")
                } else {
                    listOf("40g Rolled Oats", "1 tsp Chia / Flax Seeds", "150ml Milk", "Half Sliced Banana")
                },
                instructions = trEn(
                    "Yulaf ve tohumları ılık süt ile karıştırın. Üzerine dilimlenmiş muz ekleyip hemen tüketin.",
                    "Mix oats and seeds with warm milk. Top with sliced banana and enjoy immediately.",
                    language
                ),
                calories = 285,
                proteinG = 12.0,
                carbsG = 45.0,
                fatG = 6.5,
                fiberG = 8.5,
                keyBenefits = trEn(
                    "Yulafın beta-glukan lifi kan şekerini dengeler ve güne yüksek magnezyumla başlamanızı sağlar.",
                    "Oat beta-glucan fiber stabilizes blood sugar and starts your morning rich in magnesium.",
                    language
                ),
                targetDeficiencies = if (language == AppLanguage.TR) listOf("Lif", "Magnezyum", "Kalsiyum") else listOf("Fiber", "Magnesium", "Calcium"),
                itemsToAdd = listOf(
                    FoodItem(
                        id = "bm_3",
                        name = trEn("Muzlu & Chia Tohumlu Yulaf Kasesi", "Banana & Chia Oatmeal Bowl", language),
                        formattedPortion = trEn("1 Kase", "1 Bowl", language),
                        calories = 285, proteinG = 12.0, carbsG = 45.0, fatG = 6.5, fiberG = 8.5
                    )
                )
            )
        }

        // ── 2. LUNCH ──
        val lunch = if (isHighProteinNeed) {
            MealSlotSuggestion(
                slot = MealSlotType.LUNCH,
                title = trEn("Izgara Tavuk Göğsü & Limonlu Buharda Brokoli", "Grilled Chicken Breast & Lemon Steamed Broccoli", language),
                subtitle = trEn("Ultra Saf Kas Proteini & C Vitamini", "Lean Muscle Protein & Vitamin C", language),
                prepTimeMinutes = 12,
                ingredients = if (language == AppLanguage.TR) {
                    listOf("180g Tavuk Göğsü", "150g Brokoli veya Mevsim Yeşilliği", "Yarım Limon Suyu", "1 Çay Kaşığı Zeytinyağı")
                } else {
                    listOf("180g Chicken Breast", "150g Broccoli or Seasonal Greens", "Juice of Half a Lemon", "1 tsp Olive Oil")
                },
                instructions = trEn(
                    "Tavuk göğsünü yağsız tavada arkalı önlü pişirin. Brokoliyi 4-5 dk buharda haşlayıp bol limon ve zeytinyağıyla servis edin.",
                    "Pan-sear chicken breast on both sides. Steam broccoli for 4–5 min and serve drizzled with lemon juice and olive oil.",
                    language
                ),
                calories = 320,
                proteinG = 42.0,
                carbsG = 9.0,
                fatG = 7.5,
                fiberG = 4.8,
                keyBenefits = trEn(
                    "Tavuktaki yağsız protein kas protein sentezini maksimize eder, brokoli C vitamini deposudur.",
                    "Lean chicken maximizes muscle protein synthesis while broccoli delivers an antioxidant Vitamin C boost.",
                    language
                ),
                targetDeficiencies = if (language == AppLanguage.TR) listOf("Protein", "C Vitamini", "Demir") else listOf("Protein", "Vitamin C", "Iron"),
                itemsToAdd = listOf(
                    FoodItem(
                        id = "lm_1",
                        name = trEn("Izgara Tavuk Göğsü", "Grilled Chicken Breast", language),
                        formattedPortion = "180g",
                        calories = 230, proteinG = 38.0, carbsG = 0.0, fatG = 4.5, fiberG = 0.0
                    ),
                    FoodItem(
                        id = "lm_2",
                        name = trEn("Limonlu Haşlanmış Brokoli & Salata", "Lemon Steamed Broccoli & Salad", language),
                        formattedPortion = "150g",
                        calories = 90, proteinG = 4.0, carbsG = 9.0, fatG = 3.0, fiberG = 4.8
                    )
                )
            )
        } else {
            MealSlotSuggestion(
                slot = MealSlotType.LUNCH,
                title = trEn("Konserve Ton Balıklı & Mısırlı Roka Salatası", "Tuna, Sweetcorn & Arugula Salad", language),
                subtitle = trEn("Omega-3, B12 Vitamini & Hızlı Hazırlık", "Omega-3, Vitamin B12 & Quick Prep", language),
                prepTimeMinutes = 4,
                ingredients = if (language == AppLanguage.TR) {
                    listOf("1 Kutu Konserve Ton Balığı (Süzülmüş)", "1 Demet Roka & Göbek Marul", "2 Yemek Kaşığı Mısır", "Yarım Limon")
                } else {
                    listOf("1 Can Tuna (Drained)", "1 Bunch Arugula & Iceberg Lettuce", "2 tbsp Sweetcorn", "Half Lemon")
                },
                instructions = trEn(
                    "Yeşillikleri doğrayın, ton balığını ve mısırı üzerine ekleyin. Limon sıkarak anında tüketin.",
                    "Chop greens, top with drained tuna and sweetcorn. Squeeze fresh lemon and serve.",
                    language
                ),
                calories = 310,
                proteinG = 32.0,
                carbsG = 12.0,
                fatG = 9.0,
                fiberG = 3.8,
                keyBenefits = trEn(
                    "Omega-3 yağ asitleri hücresel enflamasyonu azaltır ve beyni gün boyu zinde tutar.",
                    "Omega-3 fatty acids reduce inflammation and support sharp focus all day.",
                    language
                ),
                targetDeficiencies = if (language == AppLanguage.TR) listOf("Protein", "B12 Vitamini", "Omega-3") else listOf("Protein", "Vitamin B12", "Omega-3"),
                itemsToAdd = listOf(
                    FoodItem(
                        id = "lm_3",
                        name = trEn("Ton Balıklı & Mısırlı Salata", "Tuna & Sweetcorn Salad", language),
                        formattedPortion = trEn("1 Büyük Porsiyon", "1 Large Serving", language),
                        calories = 310, proteinG = 32.0, carbsG = 12.0, fatG = 9.0, fiberG = 3.8
                    )
                )
            )
        }

        // ── 3. DINNER ──
        val dinner = if (isFiberNeed || isIronOrVitCNeed) {
            MealSlotSuggestion(
                slot = MealSlotType.DINNER,
                title = trEn("Kıymalı Yeşil Mercimek Yemeği & Yoğurt", "Green Lentil & Lean Beef Stew with Yogurt", language),
                subtitle = trEn("Bitkisel Lif, Çinko, Demir & Kalsiyum", "Plant Fiber, Zinc, Iron & Calcium", language),
                prepTimeMinutes = 15,
                ingredients = if (language == AppLanguage.TR) {
                    listOf("1 Porsiyon Yeşil Mercimek (80g kuru)", "70g Yağsız Kıyma", "1 Çay Kaşığı Zeytinyağı", "4 Yemek Kaşığı Yoğurt")
                } else {
                    listOf("1 Serving Green Lentils (80g dry)", "70g Lean Ground Beef", "1 tsp Olive Oil", "4 tbsp Plain Yogurt")
                },
                instructions = trEn(
                    "Mercimeği kıyma ve salça ile tencerede pişirin. Yanında 4 yemek kaşığı yoğurt ile servis edin.",
                    "Simmer lentils with lean beef and tomato paste. Serve alongside 4 tbsp of plain yogurt.",
                    language
                ),
                calories = 410,
                proteinG = 32.0,
                carbsG = 42.0,
                fatG = 12.0,
                fiberG = 12.5,
                keyBenefits = trEn(
                    "Günün lif ve demir ihtiyacının yarısını tek başına karşılar, yoğurt kalsiyum ve probiyotik sağlar.",
                    "Covers half your daily fiber and iron needs in one dish while yogurt adds calcium and probiotics.",
                    language
                ),
                targetDeficiencies = if (language == AppLanguage.TR) listOf("Lif", "Demir", "Kalsiyum", "Protein") else listOf("Fiber", "Iron", "Calcium", "Protein"),
                itemsToAdd = listOf(
                    FoodItem(
                        id = "dm_1",
                        name = trEn("Kıymalı Yeşil Mercimek Yemeği", "Green Lentil & Beef Stew", language),
                        formattedPortion = trEn("1 Porsiyon (200g)", "1 Serving (200g)", language),
                        calories = 320, proteinG = 26.0, carbsG = 38.0, fatG = 9.5, fiberG = 12.5
                    ),
                    FoodItem(
                        id = "dm_2",
                        name = trEn("Doğal Ev Yoğurdu", "Natural Plain Yogurt", language),
                        formattedPortion = trEn("4 Yemek Kaşığı (120g)", "4 tbsp (120g)", language),
                        calories = 90, proteinG = 6.0, carbsG = 4.0, fatG = 2.5, fiberG = 0.0
                    )
                )
            )
        } else {
            MealSlotSuggestion(
                slot = MealSlotType.DINNER,
                title = trEn("Fırında/Tavada Anne Köftesi & Yoğurtlu Kabak Sote", "Grilled Meatballs & Garlic Yogurt Zucchini", language),
                subtitle = trEn("Yüksek Biyoyararlanımlı Protein & Hafif Akşam", "Bioavailable Protein & Light Dinner", language),
                prepTimeMinutes = 15,
                ingredients = if (language == AppLanguage.TR) {
                    listOf("150g Izgara Köfte (4 adet)", "1 Adet Rende Kabak", "1 Çay Kaşığı Zeytinyağı", "3 Yemek Kaşığı Süzme Yoğurt")
                } else {
                    listOf("150g Grilled Beef Patties (4 pcs)", "1 Grated Zucchini", "1 tsp Olive Oil", "3 tbsp Greek Yogurt")
                },
                instructions = trEn(
                    "Köfteleri tavada pişirin. Rendelenmiş kabağı 3 dk zeytinyağında soteleyip üzerine sarımsaklı süzme yoğurt dökün.",
                    "Grill meatballs in a pan. Sauté grated zucchini in olive oil for 3 min and top with garlic Greek yogurt.",
                    language
                ),
                calories = 430,
                proteinG = 36.0,
                carbsG = 11.0,
                fatG = 18.0,
                fiberG = 4.2,
                keyBenefits = trEn(
                    "Yüksek demir ve çinko içerir, kabak sindirimi yormayarak gece boyunca rahat uyumanızı sağlar.",
                    "Rich in iron and zinc; light zucchini eases digestion for restful sleep.",
                    language
                ),
                targetDeficiencies = if (language == AppLanguage.TR) listOf("Protein", "Demir", "Kalsiyum") else listOf("Protein", "Iron", "Calcium"),
                itemsToAdd = listOf(
                    FoodItem(
                        id = "dm_3",
                        name = trEn("Izgara Dana Köfte (4 Adet)", "Grilled Beef Meatballs (4 pcs)", language),
                        formattedPortion = "150g",
                        calories = 310, proteinG = 29.0, carbsG = 3.0, fatG = 15.0, fiberG = 0.5
                    ),
                    FoodItem(
                        id = "dm_4",
                        name = trEn("Yoğurtlu Kabak Sote", "Zucchini Sauté with Yogurt", language),
                        formattedPortion = trEn("1 Porsiyon", "1 Serving", language),
                        calories = 120, proteinG = 7.0, carbsG = 8.0, fatG = 3.0, fiberG = 3.7
                    )
                )
            )
        }

        // ── 4. SNACK ──
        val snack = if (isHighProteinNeed) {
            MealSlotSuggestion(
                slot = MealSlotType.SNACK,
                title = trEn("Kabak Çekirdekli & Keten Tohumlu Süzme Yoğurt", "Greek Yogurt with Pumpkin & Flax Seeds", language),
                subtitle = trEn("Aç & Ye: Magnezyum, Çinko ve Kazein Proteini", "Ready-to-Eat: Magnesium, Zinc & Casein Protein", language),
                prepTimeMinutes = 0,
                ingredients = if (language == AppLanguage.TR) {
                    listOf("150g Süzme Yoğurt", "1 Yemek Kaşığı Çiğ Kabak Çekirdeği", "1 Çay Kaşığı Keten Tohumu")
                } else {
                    listOf("150g Strained / Greek Yogurt", "1 tbsp Raw Pumpkin Seeds", "1 tsp Ground Flaxseed")
                },
                instructions = trEn(
                    "Süzme yoğurdun üzerine kabak çekirdeği serpin, anında tüketin. Gece boyu kas besler.",
                    "Sprinkle pumpkin and flax seeds over Greek yogurt and enjoy right away for slow-release recovery protein.",
                    language
                ),
                calories = 210,
                proteinG = 19.5,
                carbsG = 6.5,
                fatG = 10.5,
                fiberG = 4.2,
                keyBenefits = trEn(
                    "Yoğurttaki kazein proteini yavaş sindirilir, kabak çekirdeği magnezyum açığını tek başına kapatır.",
                    "Slow-digesting casein fuels overnight recovery while pumpkin seeds replenish magnesium.",
                    language
                ),
                targetDeficiencies = if (language == AppLanguage.TR) listOf("Magnezyum", "Protein", "Çinko") else listOf("Magnesium", "Protein", "Zinc"),
                itemsToAdd = listOf(
                    FoodItem(
                        id = "sm_1",
                        name = trEn("Kabak Çekirdekli Süzme Yoğurt", "Greek Yogurt with Pumpkin Seeds", language),
                        formattedPortion = trEn("1 Kase (170g)", "1 Bowl (170g)", language),
                        calories = 210, proteinG = 19.5, carbsG = 6.5, fatG = 10.5, fiberG = 4.2
                    )
                )
            )
        } else {
            MealSlotSuggestion(
                slot = MealSlotType.SNACK,
                title = trEn("Yeşil Elma & Çiğ Ceviz", "Green Apple & Raw Walnuts", language),
                subtitle = trEn("Aç & Ye: Çıtır Lif, Beyin Sağlığı & Enerji", "Ready-to-Eat: Crisp Fiber, Brain Health & Energy", language),
                prepTimeMinutes = 0,
                ingredients = if (language == AppLanguage.TR) {
                    listOf("1 Adet Orta Boy Yeşil Elma", "3 Adet Bütün Ceviz İçi")
                } else {
                    listOf("1 Medium Green Apple", "3 Whole Walnut Halves")
                },
                instructions = trEn(
                    "Elmayı yıkayıp cevizlerle birlikte aç-ye şeklinde anında tüketin.",
                    "Wash the apple and enjoy immediately paired with raw walnuts.",
                    language
                ),
                calories = 165,
                proteinG = 3.0,
                carbsG = 21.0,
                fatG = 8.5,
                fiberG = 4.2,
                keyBenefits = trEn(
                    "Pektin lifi tokluk sağlar, cevizin alfa-linolenik asidi (ALA) kalp ve damar sağlığını destekler.",
                    "Pectin fiber keeps you full while walnut ALA omega-3s support cardiovascular health.",
                    language
                ),
                targetDeficiencies = if (language == AppLanguage.TR) listOf("Lif", "Sağlıklı Yağ", "C Vitamini") else listOf("Fiber", "Healthy Fats", "Vitamin C"),
                itemsToAdd = listOf(
                    FoodItem(
                        id = "sm_2",
                        name = trEn("Yeşil Elma & Ceviz İçi", "Green Apple & Walnuts", language),
                        formattedPortion = trEn("1 Porsiyon", "1 Serving", language),
                        calories = 165, proteinG = 3.0, carbsG = 21.0, fatG = 8.5, fiberG = 4.2
                    )
                )
            )
        }

        val totalCals = breakfast.calories + lunch.calories + dinner.calories + snack.calories
        val totalProt = breakfast.proteinG + lunch.proteinG + dinner.proteinG + snack.proteinG
        val totalCarb = breakfast.carbsG + lunch.carbsG + dinner.carbsG + snack.carbsG
        val totalFat = breakfast.fatG + lunch.fatG + dinner.fatG + snack.fatG
        val totalFib = breakfast.fiberG + lunch.fiberG + dinner.fiberG + snack.fiberG

        return DailyMealPlan(
            id = "plan_${userId}_$dateIso",
            userId = userId,
            dateIso = dateIso,
            headline = trEn("Günün 4 Öğünlük Beslenme Planı 🥗", "Today's 4-Meal Nutrition Plan 🥗", language),
            deficiencySummary = deficiencySummary,
            detectedDeficiencies = deficiencies.ifEmpty {
                if (language == AppLanguage.TR) {
                    listOf("Dengeli Form & Kas Koruma", "Yüksek Magnezyum", "Kalıcı Tokluk")
                } else {
                    listOf("Balanced Physique & Muscle Care", "Rich in Magnesium", "Lasting Satiety")
                }
            },
            breakfast = breakfast,
            lunch = lunch,
            dinner = dinner,
            snack = snack,
            totalCalories = totalCals,
            totalProteinG = (totalProt * 10.0).roundToInt() / 10.0,
            totalCarbsG = (totalCarb * 10.0).roundToInt() / 10.0,
            totalFatG = (totalFat * 10.0).roundToInt() / 10.0,
            totalFiberG = (totalFib * 10.0).roundToInt() / 10.0,
            isAiGenerated = isAiGenerated,
            createdAt = currentTimeMillis()
        )
    }
}
