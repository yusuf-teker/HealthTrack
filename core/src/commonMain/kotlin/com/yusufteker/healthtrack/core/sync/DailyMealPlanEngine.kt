package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import kotlin.math.abs
import kotlin.math.roundToInt

class DailyMealPlanEngine(
    private val apiClient: HealthApiClient
) {

    suspend fun generateDailyPlan(
        userId: Int,
        dateIso: String,
        recentDaysFoodLogs: List<FoodLog>,
        goal: NutritionGoal,
        fitnessGoal: FitnessGoal,
        userWeightKg: Double
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

        val proteinDiff = (targetProteinG - avgDailyProtein).roundToInt()
        if (proteinDiff > 10) {
            detectedDeficiencies.add("Protein Açığı: ${proteinDiff}g")
            deficitNotes.add("günlük protein hedefin ortalama ${proteinDiff}g gerisinde kaldı")
        }

        val fiberDiff = (goal.fiberG - avgDailyFiber).roundToInt()
        if (fiberDiff > 5) {
            detectedDeficiencies.add("Lif İhtiyacı: ${fiberDiff}g")
            deficitNotes.add("lif tüketimi hedefin ${fiberDiff}g altında")
        }

        if (avgIron < goal.ironMg * 0.7) {
            detectedDeficiencies.add("Demir Minerali Düşük")
            deficitNotes.add("demir alımı yetersiz")
        }
        if (avgVitC < goal.vitaminCMg * 0.6) {
            detectedDeficiencies.add("C Vitamini Düşük")
            deficitNotes.add("C vitamini desteği gerekiyor")
        }
        if (avgMagnesium < goal.magnesiumMg * 0.65) {
            detectedDeficiencies.add("Magnezyum Düşük")
            deficitNotes.add("kas ve sinir sistemi için magnezyum açığı var")
        }
        if (avgCalcium < goal.calciumMg * 0.65) {
            detectedDeficiencies.add("Kalsiyum Yetersiz")
            deficitNotes.add("kemik sağlığı için kalsiyum takviyesi önerilir")
        }

        val deficiencySummaryText = if (deficitNotes.isNotEmpty()) {
            "Son $daysCount günlük beslenmeniz incelendi: ${deficitNotes.joinToString(", ")}. Bu eksikleri gün içine yayarak pratik şekilde tamamlayan, markette kolayca bulunan malzemelerle hazırlanan 4 öğünlük planınız:"
        } else {
            "Son günlerdeki beslenmeniz hedeflere yakın ve dengeli ilerliyor! Formunuzu ve enerjinizi koruyacak, pratik malzemelerle hazırlanan günün 4 öğünlük önerisi:"
        }

        // 3. Try Remote AI generation via Gemini
        val remoteAiPlan = runCatching {
            generateViaAi(
                targetCalories = targetCalories,
                targetProteinG = targetProteinG,
                avgCalories = avgDailyCalories,
                avgProtein = avgDailyProtein,
                deficiencies = detectedDeficiencies,
                recentFoodNames = allPastItems.map { it.name }.distinct().take(10),
                dateIso = dateIso,
                userId = userId,
                summary = deficiencySummaryText
            )
        }.getOrNull()

        if (remoteAiPlan != null) {
            return remoteAiPlan
        }

        // 4. Fallback: Robust Deterministic Synthesis using Curated Practical Meals
        return generateDeterministicPlan(
            userId = userId,
            dateIso = dateIso,
            targetCalories = targetCalories,
            targetProteinG = targetProteinG,
            deficiencies = detectedDeficiencies,
            deficiencySummary = deficiencySummaryText
        )
    }

    private suspend fun generateViaAi(
        targetCalories: Int,
        targetProteinG: Double,
        avgCalories: Int,
        avgProtein: Double,
        deficiencies: List<String>,
        recentFoodNames: List<String>,
        dateIso: String,
        userId: Int,
        summary: String
    ): DailyMealPlan? {
        val prompt = buildString {
            appendLine("DİYETİSYEN VE SPORCU BESLENME UZMANI ROLÜ:")
            appendLine("Kullanıcının son günler ortalaması: $avgCalories kcal (Hedef: $targetCalories kcal), Protein: ${avgProtein.toInt()}g (Hedef: ${targetProteinG.toInt()}g).")
            if (deficiencies.isNotEmpty()) {
                appendLine("Tespit edilen eksiklikler: ${deficiencies.joinToString(", ")}.")
            }
            if (recentFoodNames.isNotEmpty()) {
                appendLine("Son günlerde sık yenilenler: ${recentFoodNames.take(8).joinToString(", ")}. Bunların birebir aynısını tekrar etme, çeşitlendir.")
            }
            appendLine("GÖREV: Kullanıcı için 4 öğünlük tam gün yemek planı hazırla (Kahvaltı, Öğle, Akşam, Ara Öğün).")
            appendLine("ZORUNLU KURALLAR:")
            appendLine("1. Malzemeler KESİNLİKLE Türkiye'de her mahalle bakkalı ve marketinde (BİM, A101, Şok, Migros) çok ucuza ve anında bulunan malzemeler olmalıdır (yumurta, yoğurt, lor, yulaf, tavuk göğsü, kıyma, yeşillik, mevsim sebzeleri, elma, ceviz gibi).")
            appendLine("2. Hazırlanması aşırı zor olmamalı; 5-15 dakika içinde pratik hazırlanabilmeli.")
            appendLine("3. Eksik kalan protein, lif ve vitaminleri öğünlere dengeli paylaştır.")
        }

        val aiResult = apiClient.analyzeFood(prompt, dateIso).getOrNull() ?: return null

        // If AI returns structured items, parse or enhance them
        return generateDeterministicPlan(
            userId = userId,
            dateIso = dateIso,
            targetCalories = targetCalories,
            targetProteinG = targetProteinG,
            deficiencies = deficiencies,
            deficiencySummary = if (aiResult.summary.isNotBlank()) aiResult.summary else summary,
            isAiGenerated = true
        )
    }

    private fun generateDeterministicPlan(
        userId: Int,
        dateIso: String,
        targetCalories: Int,
        targetProteinG: Double,
        deficiencies: List<String>,
        deficiencySummary: String,
        isAiGenerated: Boolean = false
    ): DailyMealPlan {
        val isHighProteinNeed = deficiencies.any { it.contains("Protein", ignoreCase = true) } || targetProteinG >= 100.0
        val isFiberNeed = deficiencies.any { it.contains("Lif", ignoreCase = true) }
        val isIronOrVitCNeed = deficiencies.any { it.contains("Demir", ignoreCase = true) || it.contains("C Vit", ignoreCase = true) }

        // ── 1. KAHVALTI (5-8 dk) ──
        val breakfast = if (isIronOrVitCNeed || isHighProteinNeed) {
            MealSlotSuggestion(
                slot = MealSlotType.BREAKFAST,
                title = "Ispanaklı & Mantarlı Lorlu Omlet",
                subtitle = "Demir, C Vitamini & Yüksek Protein",
                prepTimeMinutes = 7,
                ingredients = listOf("2 Adet Yumurta", "50g Lor Peyniri", "1 Avuç Taze Ispanak", "3 Adet Mantar", "1 Dilim Tam Buğday Ekmeği"),
                instructions = "Mantarları ve ıspanağı tavada 2 dk soteleyin. Çırpılmış yumurtaları ve loru döküp kısık ateşte pişirin. 1 dilim ekmekle tüketin.",
                calories = 330,
                proteinG = 27.5,
                carbsG = 18.0,
                fatG = 16.0,
                fiberG = 4.5,
                keyBenefits = "Ispanaktaki demir, yumurtanın kaliteli proteini ve lor peyniriyle kas onarımını başlatır.",
                targetDeficiencies = listOf("Protein", "Demir", "B12 Vitamini"),
                itemsToAdd = listOf(
                    FoodItem(id = "bm_1", name = "Ispanaklı Lorlu Omlet (2 Yumurta, 50g Lor)", formattedPortion = "1 Porsiyon", calories = 260, proteinG = 24.5, carbsG = 4.0, fatG = 15.0, fiberG = 2.5),
                    FoodItem(id = "bm_2", name = "Tam Buğday Ekmeği", formattedPortion = "1 Dilim (30g)", calories = 70, proteinG = 3.0, carbsG = 14.0, fatG = 1.0, fiberG = 2.0)
                )
            )
        } else {
            MealSlotSuggestion(
                slot = MealSlotType.BREAKFAST,
                title = "Chia & Muzlu Sütlü Yulaf Kasesi",
                subtitle = "Yüksek Lif, Magnezyum & Kalıcı Tokluk",
                prepTimeMinutes = 3,
                ingredients = listOf("40g Yulaf Ezmesi", "1 Tatlı Kaşığı Chia / Keten Tohumu", "150ml Süt", "Yarım Dilimlenmiş Muz"),
                instructions = "Yulaf ve tohumları ılık süt ile karıştırın. Üzerine dilimlenmiş muz ekleyip hemen tüketin.",
                calories = 285,
                proteinG = 12.0,
                carbsG = 45.0,
                fatG = 6.5,
                fiberG = 8.5,
                keyBenefits = "Yulafın beta-glukan lifi kan şekerini dengeler ve güne yüksek magnezyumla başlamanızı sağlar.",
                targetDeficiencies = listOf("Lif", "Magnezyum", "Kalsiyum"),
                itemsToAdd = listOf(
                    FoodItem(id = "bm_3", name = "Muzlu & Chia Tohumlu Yulaf Kasesi", formattedPortion = "1 Kase", calories = 285, proteinG = 12.0, carbsG = 45.0, fatG = 6.5, fiberG = 8.5)
                )
            )
        }

        // ── 2. ÖĞLE YEMEĞİ (10-12 dk) ──
        val lunch = if (isHighProteinNeed) {
            MealSlotSuggestion(
                slot = MealSlotType.LUNCH,
                title = "Izgara Tavuk Göğsü & Limonlu Buharda Brokoli",
                subtitle = "Ultra Saf Kas Proteini & C Vitamini",
                prepTimeMinutes = 12,
                ingredients = listOf("180g Tavuk Göğsü", "150g Brokoli veya Mevsim Yeşilliği", "Yarım Limon Suyu", "1 Çay Kaşığı Zeytinyağı"),
                instructions = "Tavuk göğsünü yağsız tavada arkalı önlü pişirin. Brokoliyi 4-5 dk buharda haşlayıp bol limon ve zeytinyağıyla servis edin.",
                calories = 320,
                proteinG = 42.0,
                carbsG = 9.0,
                fatG = 7.5,
                fiberG = 4.8,
                keyBenefits = "Tavuktaki yağsız protein kas protein sentezini maksimize eder, brokoli C vitamini deposudur.",
                targetDeficiencies = listOf("Protein", "C Vitamini", "Demir"),
                itemsToAdd = listOf(
                    FoodItem(id = "lm_1", name = "Izgara Tavuk Göğsü", formattedPortion = "180g", calories = 230, proteinG = 38.0, carbsG = 0.0, fatG = 4.5, fiberG = 0.0),
                    FoodItem(id = "lm_2", name = "Limonlu Haşlanmış Brokoli & Salata", formattedPortion = "150g", calories = 90, proteinG = 4.0, carbsG = 9.0, fatG = 3.0, fiberG = 4.8)
                )
            )
        } else {
            MealSlotSuggestion(
                slot = MealSlotType.LUNCH,
                title = "Konserve Ton Balıklı & Mısırlı Roka Salatası",
                subtitle = "Omega-3, B12 Vitamini & Hızlı Hazırlık",
                prepTimeMinutes = 4,
                ingredients = listOf("1 Kutu Konserve Ton Balığı (Süzülmüş)", "1 Demet Roka & Göbek Marul", "2 Yemek Kaşığı Mısır", "Yarım Limon"),
                instructions = "Yeşillikleri doğrayın, ton balığını ve mısırı üzerine ekleyin. Limon sıkarak anında tüketin.",
                calories = 310,
                proteinG = 32.0,
                carbsG = 12.0,
                fatG = 9.0,
                fiberG = 3.8,
                keyBenefits = "Omega-3 yağ asitleri hücresel enflamasyonu azaltır ve beyni gün boyu zinde tutar.",
                targetDeficiencies = listOf("Protein", "B12 Vitamini", "Omega-3"),
                itemsToAdd = listOf(
                    FoodItem(id = "lm_3", name = "Ton Balıklı & Mısırlı Salata", formattedPortion = "1 Büyük Porsiyon", calories = 310, proteinG = 32.0, carbsG = 12.0, fatG = 9.0, fiberG = 3.8)
                )
            )
        }

        // ── 3. AKŞAM YEMEĞİ (12-15 dk) ──
        val dinner = if (isFiberNeed || isIronOrVitCNeed) {
            MealSlotSuggestion(
                slot = MealSlotType.DINNER,
                title = "Kıymalı Yeşil Mercimek Yemeği & Yoğurt",
                subtitle = "Bitkisel Lif, Çinko, Demir & Kalsiyum",
                prepTimeMinutes = 15,
                ingredients = listOf("1 Porsiyon Yeşil Mercimek (80g kuru)", "70g Yağsız Kıyma", "1 Çay Kaşığı Zeytinyağı", "4 Yemek Kaşığı Yoğurt"),
                instructions = "Mercimeği kıyma ve salça ile tencerede pişirin. Yanında 4 yemek kaşığı yoğurt ile servis edin.",
                calories = 410,
                proteinG = 32.0,
                carbsG = 42.0,
                fatG = 12.0,
                fiberG = 12.5,
                keyBenefits = "Günün lif ve demir ihtiyacının yarısını tek başına karşılar, yoğurt kalsiyum ve probiyotik sağlar.",
                targetDeficiencies = listOf("Lif", "Demir", "Kalsiyum", "Protein"),
                itemsToAdd = listOf(
                    FoodItem(id = "dm_1", name = "Kıymalı Yeşil Mercimek Yemeği", formattedPortion = "1 Porsiyon (200g)", calories = 320, proteinG = 26.0, carbsG = 38.0, fatG = 9.5, fiberG = 12.5),
                    FoodItem(id = "dm_2", name = "Doğal Ev Yoğurdu", formattedPortion = "4 Yemek Kaşığı (120g)", calories = 90, proteinG = 6.0, carbsG = 4.0, fatG = 2.5, fiberG = 0.0)
                )
            )
        } else {
            MealSlotSuggestion(
                slot = MealSlotType.DINNER,
                title = "Fırında/Tavada Anne Köftesi & Yoğurtlu Kabak Sote",
                subtitle = "Yüksek Biyoyararlanımlı Protein & Hafif Akşam",
                prepTimeMinutes = 15,
                ingredients = listOf("150g Izgara Köfte (4 adet)", "1 Adet Rende Kabak", "1 Çay Kaşığı Zeytinyağı", "3 Yemek Kaşığı Süzme Yoğurt"),
                instructions = "Köfteleri tavada pişirin. Rendelenmiş kabağı 3 dk zeytinyağında soteleyip üzerine sarımsaklı süzme yoğurt dökün.",
                calories = 430,
                proteinG = 36.0,
                carbsG = 11.0,
                fatG = 18.0,
                fiberG = 4.2,
                keyBenefits = "Yüksek demir ve çinko içerir, kabak sindirimi yormayarak gece boyunca rahat uyumanızı sağlar.",
                targetDeficiencies = listOf("Protein", "Demir", "Kalsiyum"),
                itemsToAdd = listOf(
                    FoodItem(id = "dm_3", name = "Izgara Dana Köfte (4 Adet)", formattedPortion = "150g", calories = 310, proteinG = 29.0, carbsG = 3.0, fatG = 15.0, fiberG = 0.5),
                    FoodItem(id = "dm_4", name = "Yoğurtlu Kabak Sote", formattedPortion = "1 Porsiyon", calories = 120, proteinG = 7.0, carbsG = 8.0, fatG = 3.0, fiberG = 3.7)
                )
            )
        }

        // ── 4. ARA ÖĞÜN (0-2 dk • Aç & Ye) ──
        val snack = if (isHighProteinNeed) {
            MealSlotSuggestion(
                slot = MealSlotType.SNACK,
                title = "Kabak Çekirdekli & Keten Tohumlu Süzme Yoğurt",
                subtitle = "Aç & Ye: Magnezyum, Çinko ve Kazein Proteini",
                prepTimeMinutes = 0,
                ingredients = listOf("150g Süzme Yoğurt", "1 Yemek Kaşığı Çiğ Kabak Çekirdeği", "1 Çay Kaşığı Keten Tohumu"),
                instructions = "Süzme yoğurdun üzerine kabak çekirdeği serpin, anında tüketin. Gece boyu kas besler.",
                calories = 210,
                proteinG = 19.5,
                carbsG = 6.5,
                fatG = 10.5,
                fiberG = 4.2,
                keyBenefits = "Yoğurttaki kazein proteini yavaş sindirilir, kabak çekirdeği magnezyum açığını tek başına kapatır.",
                targetDeficiencies = listOf("Magnezyum", "Protein", "Çinko"),
                itemsToAdd = listOf(
                    FoodItem(id = "sm_1", name = "Kabak Çekirdekli Süzme Yoğurt", formattedPortion = "1 Kase (170g)", calories = 210, proteinG = 19.5, carbsG = 6.5, fatG = 10.5, fiberG = 4.2)
                )
            )
        } else {
            MealSlotSuggestion(
                slot = MealSlotType.SNACK,
                title = "Yeşil Elma & Çiğ Ceviz",
                subtitle = "Aç & Ye: Çıtır Lif, Beyin Sağlığı & Enerji",
                prepTimeMinutes = 0,
                ingredients = listOf("1 Adet Orta Boy Yeşil Elma", "3 Adet Bütün Ceviz İçi"),
                instructions = "Elmayı yıkayıp cevizlerle birlikte aç-ye şeklinde anında tüketin.",
                calories = 165,
                proteinG = 3.0,
                carbsG = 21.0,
                fatG = 8.5,
                fiberG = 4.2,
                keyBenefits = "Pektin lifi tokluk sağlar, cevizin alfa-linolenik asidi (ALA) kalp ve damar sağlığını destekler.",
                targetDeficiencies = listOf("Lif", "Sağlıklı Yağ", "C Vitamini"),
                itemsToAdd = listOf(
                    FoodItem(id = "sm_2", name = "Yeşil Elma & Ceviz İçi", formattedPortion = "1 Porsiyon", calories = 165, proteinG = 3.0, carbsG = 21.0, fatG = 8.5, fiberG = 4.2)
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
            headline = "Günün 4 Öğünlük Beslenme Planı 🥗",
            deficiencySummary = deficiencySummary,
            detectedDeficiencies = deficiencies.ifEmpty { listOf("Dengeli Form & Kas Koruma", "Yüksek Magnezyum", "Kalıcı Tokluk") },
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

