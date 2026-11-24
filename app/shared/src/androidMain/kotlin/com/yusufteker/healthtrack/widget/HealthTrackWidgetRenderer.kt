package com.yusufteker.healthtrack.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.yusufteker.healthtrack.app.shared.R
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import kotlin.math.roundToInt

internal object HealthTrackWidgetRenderer {

    const val ACTION_ADD_WATER = "com.yusufteker.healthtrack.widget.ACTION_ADD_WATER"
    const val ACTION_CYCLE_PAGE = "com.yusufteker.healthtrack.widget.ACTION_CYCLE_PAGE"
    const val ACTION_SET_PAGE = "com.yusufteker.healthtrack.widget.ACTION_SET_PAGE"
    const val ACTION_QUICK_WORKOUT = "com.yusufteker.healthtrack.widget.ACTION_QUICK_WORKOUT"
    const val ACTION_ADJUST_WEIGHT = "com.yusufteker.healthtrack.widget.ACTION_ADJUST_WEIGHT"
    const val ACTION_SAVE_WEIGHT = "com.yusufteker.healthtrack.widget.ACTION_SAVE_WEIGHT"

    const val EXTRA_WATER_DELTA_ML = "extra_water_delta_ml"
    const val EXTRA_APPWIDGET_ID = "extra_appwidget_id"
    const val EXTRA_PAGE_DELTA = "extra_page_delta"
    const val EXTRA_PAGE_INDEX = "extra_page_index"
    const val EXTRA_WORKOUT_TITLE_TR = "extra_workout_title_tr"
    const val EXTRA_WORKOUT_TITLE_EN = "extra_workout_title_en"
    const val EXTRA_WORKOUT_DURATION_MIN = "extra_workout_duration_min"
    const val EXTRA_WORKOUT_CALORIES = "extra_workout_calories"
    const val EXTRA_WEIGHT_DELTA_TENTHS = "extra_weight_delta_tenths"

    const val PAGE_SUMMARY = 0
    const val PAGE_AI_FOOD = 1
    const val PAGE_WORKOUT = 2
    const val PAGE_WEIGHT = 3
    const val PAGE_COUNT = 4

    fun buildTelemetryRemoteViews(
        context: Context,
        snapshot: WidgetTelemetrySnapshot,
        options: Bundle? = null,
        appWidgetId: Int = 0
    ): RemoteViews {
        val minWidthDp = options?.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) ?: 250
        val minHeightDp = options?.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120) ?: 120

        if (minWidthDp in 1..200) {
            return buildCompactRemoteViews(context, snapshot)
        }

        val views = RemoteViews(context.packageName, R.layout.widget_healthtrack_telemetry)
        val isDark = snapshot.isDarkTheme
        val isTr = snapshot.languageCode.lowercase().startsWith("tr")
        val activePage = HealthTrackWidgetStore.getActivePage(context, appWidgetId)
        val draftWeight = HealthTrackWidgetStore.getDraftWeight(context, snapshot)
        val feedback = HealthTrackWidgetStore.getRecentFeedback(context)

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_widget_obsidian else R.drawable.bg_widget_light
        )

        val primaryTextColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        val secondaryTextColor = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
        val actionBg = if (isDark) R.drawable.bg_widget_pill_action else R.drawable.bg_widget_pill_action_light
        val inputBg = if (isDark) R.drawable.bg_widget_input_dark else R.drawable.bg_widget_input_light

        // Persistent 4-Page Bottom Navigation Bar
        val navItems = listOf(
            Triple(R.id.widget_nav_summary, if (isTr) "Özet" else "Stats", primaryTextColor),
            Triple(R.id.widget_nav_ai_food, if (isTr) "AI Öğün" else "AI Meal", Color.parseColor("#38BDF8")),
            Triple(R.id.widget_nav_workout, if (isTr) "Spor" else "Sport", Color.parseColor("#FB923C")),
            Triple(R.id.widget_nav_weight, if (isTr) "Kilo" else "Weight", Color.parseColor("#C084FC"))
        )
        navItems.forEachIndexed { idx, (navId, label, inactiveColor) ->
            val selected = idx == activePage
            views.setTextViewText(navId, label)
            views.setInt(navId, "setBackgroundResource", if (selected) R.drawable.bg_widget_tab_active else actionBg)
            views.setTextColor(navId, if (selected) Color.WHITE else inactiveColor)
            views.setOnClickPendingIntent(
                navId,
                createSetPagePendingIntent(context, appWidgetId, idx, 5100 + appWidgetId * 10 + idx)
            )
        }

        // Toggle active page container
        views.setViewVisibility(R.id.widget_page_summary, if (activePage == PAGE_SUMMARY) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_page_ai_food, if (activePage == PAGE_AI_FOOD) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_page_workout, if (activePage == PAGE_WORKOUT) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_page_weight, if (activePage == PAGE_WEIGHT) View.VISIBLE else View.GONE)

        // ==================== PAGE 0: SUMMARY ====================
        views.setTextColor(R.id.widget_protein_label, secondaryTextColor)
        views.setTextColor(R.id.widget_carbs_label, secondaryTextColor)
        views.setTextColor(R.id.widget_fat_label, secondaryTextColor)
        views.setTextColor(R.id.widget_protein_value, primaryTextColor)
        views.setTextColor(R.id.widget_carbs_value, primaryTextColor)
        views.setTextColor(R.id.widget_fat_value, primaryTextColor)

        val ringBitmap = renderCalorieRingBitmap(context, snapshot, sizePx = 220)
        views.setImageViewBitmap(R.id.widget_calorie_ring, ringBitmap)

        views.setTextViewText(R.id.widget_protein_label, "Protein")
        views.setTextViewText(R.id.widget_carbs_label, if (isTr) "Karb" else "Carbs")
        views.setTextViewText(R.id.widget_fat_label, if (isTr) "Yağ" else "Fat")
        views.setTextViewText(R.id.widget_protein_value, "${snapshot.proteinG}/${snapshot.proteinTargetG}g")
        views.setTextViewText(R.id.widget_carbs_value, "${snapshot.carbsG}/${snapshot.carbsTargetG}g")
        views.setTextViewText(R.id.widget_fat_value, "${snapshot.fatG}/${snapshot.fatTargetG}g")

        views.setProgressBar(R.id.widget_protein_progress, 100, (snapshot.proteinProgress * 100).roundToInt(), false)
        views.setProgressBar(R.id.widget_carbs_progress, 100, (snapshot.carbsProgress * 100).roundToInt(), false)
        views.setProgressBar(R.id.widget_fat_progress, 100, (snapshot.fatProgress * 100).roundToInt(), false)

        views.setTextViewText(R.id.widget_water_text, "💧 ${snapshot.waterMl} / ${snapshot.waterTargetMl} ml")
        views.setTextColor(
            R.id.widget_water_text,
            if (snapshot.isWaterGoalMet) Color.parseColor("#06D6A0") else Color.parseColor("#38BDF8")
        )
        views.setProgressBar(R.id.widget_water_progress, 100, (snapshot.waterProgress * 100).roundToInt(), false)

        views.setOnClickPendingIntent(
            R.id.widget_root,
            createDeepLinkPendingIntent(context, "healthtrack://open?tab=food", 1001)
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_add_water,
            createAddWaterPendingIntent(context, 250, 2001)
        )

        // ==================== PAGE 1: AI FOOD ====================
        views.setInt(R.id.widget_ai_food_input_box, "setBackgroundResource", inputBg)
        views.setTextColor(R.id.widget_ai_food_input_box, secondaryTextColor)
        views.setTextViewText(
            R.id.widget_ai_food_input_box,
            if (isTr) "✨ Ne yedin? Örn: 2 yumurta, 1 dilim ekmek..."
            else "✨ What did you eat? e.g. 2 eggs, toast..."
        )
        views.setTextViewText(
            R.id.widget_ai_food_cta_btn,
            if (isTr) "✨ AI ile Hesapla & Öğüne Ekle" else "✨ AI Calculate & Add Meal"
        )
        views.setTextViewText(
            R.id.widget_ai_food_kcal_summary,
            if (isTr) "Bugün: ${snapshot.consumedCalories} / ${snapshot.targetCalories} kcal"
            else "Today: ${snapshot.consumedCalories} / ${snapshot.targetCalories} kcal"
        )
        views.setTextViewText(
            R.id.widget_ai_food_manual_link,
            if (isTr) "+ Manuel Ekle" else "+ Manual"
        )
        views.setOnClickPendingIntent(
            R.id.widget_ai_food_input_box,
            createQuickLogPendingIntent(context, WidgetQuickLogActivity.MODE_AI_FOOD, 6010)
        )
        views.setOnClickPendingIntent(
            R.id.widget_ai_food_cta_btn,
            createQuickLogPendingIntent(context, WidgetQuickLogActivity.MODE_AI_FOOD, 6011)
        )
        views.setOnClickPendingIntent(
            R.id.widget_ai_food_manual_link,
            createDeepLinkPendingIntent(context, "healthtrack://open?tab=food&action=add_food", 1005)
        )

        // ==================== PAGE 2: WORKOUT ====================
        views.setInt(R.id.widget_workout_ai_input_box, "setBackgroundResource", inputBg)
        val workoutPromptText = if (!feedback.isNullOrBlank() && activePage == PAGE_WORKOUT) {
            feedback
        } else if (isTr) {
            "🏃 Spor yaz (AI hesaplar) • 🔥 ${snapshot.burnedCalories} kcal"
        } else {
            "🏃 Describe workout (AI) • 🔥 ${snapshot.burnedCalories} kcal"
        }
        views.setTextViewText(R.id.widget_workout_ai_input_box, workoutPromptText)
        views.setTextColor(
            R.id.widget_workout_ai_input_box,
            if (!feedback.isNullOrBlank() && activePage == PAGE_WORKOUT) Color.parseColor("#06D6A0") else secondaryTextColor
        )
        views.setTextViewText(
            R.id.widget_workout_preset_run,
            if (isTr) "🏃 30dk Koşu\n-310 kcal" else "🏃 30m Run\n-310 kcal"
        )
        views.setTextViewText(
            R.id.widget_workout_preset_gym,
            if (isTr) "🏋️ 45dk Fitness\n-260 kcal" else "🏋️ 45m Gym\n-260 kcal"
        )
        views.setTextViewText(
            R.id.widget_workout_preset_walk,
            if (isTr) "🚶 30dk Yürüyüş\n-150 kcal" else "🚶 30m Walk\n-150 kcal"
        )
        views.setOnClickPendingIntent(
            R.id.widget_workout_ai_input_box,
            createQuickLogPendingIntent(context, WidgetQuickLogActivity.MODE_AI_WORKOUT, 6020)
        )
        views.setOnClickPendingIntent(
            R.id.widget_workout_preset_run,
            createQuickWorkoutPendingIntent(context, "30 dk Koşu", "30 min Run", 30, 310, 6101)
        )
        views.setOnClickPendingIntent(
            R.id.widget_workout_preset_gym,
            createQuickWorkoutPendingIntent(context, "45 dk Fitness", "45 min Fitness", 45, 260, 6102)
        )
        views.setOnClickPendingIntent(
            R.id.widget_workout_preset_walk,
            createQuickWorkoutPendingIntent(context, "30 dk Yürüyüş", "30 min Walk", 30, 150, 6103)
        )

        // ==================== PAGE 3: WEIGHT ====================
        val formattedWeight = ((draftWeight * 10.0).roundToInt() / 10.0)
        views.setTextViewText(R.id.widget_weight_value_text, "$formattedWeight kg")
        views.setTextColor(R.id.widget_weight_value_text, primaryTextColor)
        val targetWeight = snapshot.targetWeightKg
        val weightSubtitle = when {
            !feedback.isNullOrBlank() && activePage == PAGE_WEIGHT -> feedback
            targetWeight != null -> {
                val targetFmt = ((targetWeight * 10.0).roundToInt() / 10.0)
                if (isTr) "Hedef: $targetFmt kg" else "Goal: $targetFmt kg"
            }
            else -> if (isTr) "Dokun & yaz" else "Tap to type"
        }
        views.setTextViewText(R.id.widget_weight_target_text, weightSubtitle)
        views.setTextColor(
            R.id.widget_weight_target_text,
            if (!feedback.isNullOrBlank() && activePage == PAGE_WEIGHT) Color.parseColor("#06D6A0") else Color.parseColor("#C084FC")
        )
        views.setTextViewText(R.id.widget_weight_save_btn, if (isTr) "✓ Kaydet" else "✓ Save")

        listOf(
            R.id.widget_weight_minus_05,
            R.id.widget_weight_minus_01,
            R.id.widget_weight_plus_01,
            R.id.widget_weight_plus_05
        ).forEach { id ->
            views.setInt(id, "setBackgroundResource", actionBg)
            views.setTextColor(id, primaryTextColor)
        }

        views.setOnClickPendingIntent(
            R.id.widget_weight_value_text,
            createQuickLogPendingIntent(context, WidgetQuickLogActivity.MODE_WEIGHT, 6030)
        )
        views.setOnClickPendingIntent(
            R.id.widget_weight_minus_05,
            createAdjustWeightPendingIntent(context, -5, 6201)
        )
        views.setOnClickPendingIntent(
            R.id.widget_weight_minus_01,
            createAdjustWeightPendingIntent(context, -1, 6202)
        )
        views.setOnClickPendingIntent(
            R.id.widget_weight_plus_01,
            createAdjustWeightPendingIntent(context, 1, 6203)
        )
        views.setOnClickPendingIntent(
            R.id.widget_weight_plus_05,
            createAdjustWeightPendingIntent(context, 5, 6204)
        )
        views.setOnClickPendingIntent(
            R.id.widget_weight_save_btn,
            createSaveWeightPendingIntent(context, 6205)
        )

        return views
    }

    fun buildStandaloneAiFoodRemoteViews(
        context: Context,
        snapshot: WidgetTelemetrySnapshot
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_healthtrack_ai_food)
        val isDark = snapshot.isDarkTheme
        val isTr = snapshot.languageCode.lowercase().startsWith("tr")
        val primaryText = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        val secondaryText = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")

        views.setInt(
            R.id.widget_ai_food_root,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_widget_obsidian else R.drawable.bg_widget_light
        )
        views.setInt(
            R.id.widget_standalone_ai_input,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_widget_input_dark else R.drawable.bg_widget_input_light
        )
        views.setTextColor(R.id.widget_standalone_ai_title, primaryText)
        views.setTextColor(R.id.widget_standalone_ai_input, secondaryText)

        views.setTextViewText(R.id.widget_standalone_ai_title, if (isTr) "AI Öğün Ekle" else "AI Meal Log")
        views.setTextViewText(R.id.widget_standalone_ai_kcal, "${snapshot.consumedCalories} / ${snapshot.targetCalories} kcal")
        views.setTextViewText(
            R.id.widget_standalone_ai_input,
            if (isTr) "✨ Ne yedin? Örn: 2 yumurta, 1 dilim ekmek..."
            else "✨ What did you eat? e.g. 2 eggs, toast..."
        )
        views.setTextViewText(
            R.id.widget_standalone_ai_btn,
            if (isTr) "✨ AI ile Hesapla & Öğüne Ekle" else "✨ AI Calculate & Add Meal"
        )

        val quickIntent = createQuickLogPendingIntent(context, WidgetQuickLogActivity.MODE_AI_FOOD, 7001)
        views.setOnClickPendingIntent(R.id.widget_ai_food_root, quickIntent)
        views.setOnClickPendingIntent(R.id.widget_standalone_ai_input, quickIntent)
        views.setOnClickPendingIntent(R.id.widget_standalone_ai_btn, quickIntent)
        return views
    }

    fun buildStandaloneWorkoutRemoteViews(
        context: Context,
        snapshot: WidgetTelemetrySnapshot
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_healthtrack_workout)
        val isDark = snapshot.isDarkTheme
        val isTr = snapshot.languageCode.lowercase().startsWith("tr")
        val primaryText = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        val secondaryText = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
        val feedback = HealthTrackWidgetStore.getRecentFeedback(context)

        views.setInt(
            R.id.widget_workout_root,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_widget_obsidian else R.drawable.bg_widget_light
        )
        views.setInt(
            R.id.widget_standalone_workout_input,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_widget_input_dark else R.drawable.bg_widget_input_light
        )
        views.setTextColor(R.id.widget_standalone_workout_title, primaryText)

        views.setTextViewText(R.id.widget_standalone_workout_title, if (isTr) "Hızlı Spor" else "Quick Workout")
        views.setTextViewText(R.id.widget_standalone_workout_burned, "🔥 ${snapshot.burnedCalories} kcal")
        views.setTextViewText(
            R.id.widget_standalone_workout_input,
            if (!feedback.isNullOrBlank()) feedback
            else if (isTr) "🏃 Spor yaz (AI hesaplar)..." else "🏃 Describe workout (AI)..."
        )
        views.setTextColor(
            R.id.widget_standalone_workout_input,
            if (!feedback.isNullOrBlank()) Color.parseColor("#06D6A0") else secondaryText
        )

        views.setTextViewText(
            R.id.widget_standalone_workout_run,
            if (isTr) "🏃 30dk Koşu\n-310 kcal" else "🏃 30m Run\n-310 kcal"
        )
        views.setTextViewText(
            R.id.widget_standalone_workout_gym,
            if (isTr) "🏋️ 45dk Fitness\n-260 kcal" else "🏋️ 45m Gym\n-260 kcal"
        )
        views.setTextViewText(
            R.id.widget_standalone_workout_walk,
            if (isTr) "🚶 30dk Yürüyüş\n-150 kcal" else "🚶 30m Walk\n-150 kcal"
        )

        views.setOnClickPendingIntent(
            R.id.widget_standalone_workout_input,
            createQuickLogPendingIntent(context, WidgetQuickLogActivity.MODE_AI_WORKOUT, 7010)
        )
        views.setOnClickPendingIntent(
            R.id.widget_standalone_workout_run,
            createQuickWorkoutPendingIntent(context, "30 dk Koşu", "30 min Run", 30, 310, 7101)
        )
        views.setOnClickPendingIntent(
            R.id.widget_standalone_workout_gym,
            createQuickWorkoutPendingIntent(context, "45 dk Fitness", "45 min Fitness", 45, 260, 7102)
        )
        views.setOnClickPendingIntent(
            R.id.widget_standalone_workout_walk,
            createQuickWorkoutPendingIntent(context, "30 dk Yürüyüş", "30 min Walk", 30, 150, 7103)
        )
        return views
    }

    fun buildStandaloneWeightRemoteViews(
        context: Context,
        snapshot: WidgetTelemetrySnapshot
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_healthtrack_weight)
        val isDark = snapshot.isDarkTheme
        val isTr = snapshot.languageCode.lowercase().startsWith("tr")
        val primaryText = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        val actionBg = if (isDark) R.drawable.bg_widget_pill_action else R.drawable.bg_widget_pill_action_light
        val draftWeight = HealthTrackWidgetStore.getDraftWeight(context, snapshot)
        val feedback = HealthTrackWidgetStore.getRecentFeedback(context)

        views.setInt(
            R.id.widget_weight_root,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_widget_obsidian else R.drawable.bg_widget_light
        )
        views.setTextColor(R.id.widget_standalone_weight_title, primaryText)
        views.setTextColor(R.id.widget_standalone_weight_value, primaryText)

        val formattedWeight = ((draftWeight * 10.0).roundToInt() / 10.0)
        views.setTextViewText(R.id.widget_standalone_weight_title, if (isTr) "Hızlı Kilo" else "Quick Weight")
        views.setTextViewText(R.id.widget_standalone_weight_value, "$formattedWeight kg")

        val targetWeight = snapshot.targetWeightKg
        val subtitle = when {
            !feedback.isNullOrBlank() -> feedback
            targetWeight != null -> {
                val targetFmt = ((targetWeight * 10.0).roundToInt() / 10.0)
                if (isTr) "Hedef: $targetFmt kg" else "Goal: $targetFmt kg"
            }
            else -> if (isTr) "Dokun & yaz" else "Tap to type"
        }
        views.setTextViewText(R.id.widget_standalone_weight_target, subtitle)
        views.setTextColor(
            R.id.widget_standalone_weight_target,
            if (!feedback.isNullOrBlank()) Color.parseColor("#06D6A0") else Color.parseColor("#C084FC")
        )
        views.setTextViewText(R.id.widget_standalone_weight_save, if (isTr) "✓ Kaydet" else "✓ Save")

        listOf(
            R.id.widget_standalone_weight_m05,
            R.id.widget_standalone_weight_m01,
            R.id.widget_standalone_weight_p01,
            R.id.widget_standalone_weight_p05
        ).forEach { id ->
            views.setInt(id, "setBackgroundResource", actionBg)
            views.setTextColor(id, primaryText)
        }

        views.setOnClickPendingIntent(
            R.id.widget_standalone_weight_value,
            createQuickLogPendingIntent(context, WidgetQuickLogActivity.MODE_WEIGHT, 7020)
        )
        views.setOnClickPendingIntent(R.id.widget_standalone_weight_m05, createAdjustWeightPendingIntent(context, -5, 7201))
        views.setOnClickPendingIntent(R.id.widget_standalone_weight_m01, createAdjustWeightPendingIntent(context, -1, 7202))
        views.setOnClickPendingIntent(R.id.widget_standalone_weight_p01, createAdjustWeightPendingIntent(context, 1, 7203))
        views.setOnClickPendingIntent(R.id.widget_standalone_weight_p05, createAdjustWeightPendingIntent(context, 5, 7204))
        views.setOnClickPendingIntent(R.id.widget_standalone_weight_save, createSaveWeightPendingIntent(context, 7205))
        return views
    }

    fun buildCompactRemoteViews(
        context: Context,
        snapshot: WidgetTelemetrySnapshot
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_healthtrack_compact)
        val isDark = snapshot.isDarkTheme
        val isTr = snapshot.languageCode.lowercase().startsWith("tr")

        views.setInt(
            R.id.widget_compact_root,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_widget_obsidian else R.drawable.bg_widget_light
        )

        val primaryTextColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        val actionBg = if (isDark) R.drawable.bg_widget_pill_action else R.drawable.bg_widget_pill_action_light

        views.setTextColor(R.id.widget_compact_kcal_left, primaryTextColor)
        views.setTextColor(R.id.widget_compact_btn_food, primaryTextColor)
        views.setInt(R.id.widget_compact_btn_food, "setBackgroundResource", actionBg)

        val leftLabel = if (snapshot.isOverCalories) {
            "+${snapshot.overCalories} ${if (isTr) "aşım" else "over"}"
        } else {
            "${snapshot.remainingCalories} ${if (isTr) "kaldı" else "left"}"
        }
        views.setTextViewText(R.id.widget_compact_kcal_left, leftLabel)
        views.setTextViewText(
            R.id.widget_compact_streak,
            "⚡${snapshot.streakDays}${if (isTr) "g" else "d"}"
        )

        val ringBitmap = renderCalorieRingBitmap(context, snapshot, sizePx = 180)
        views.setImageViewBitmap(R.id.widget_compact_ring, ringBitmap)

        val currentL = ((snapshot.waterMl / 1000.0) * 10).roundToInt() / 10.0
        val targetL = ((snapshot.waterTargetMl / 1000.0) * 10).roundToInt() / 10.0
        views.setTextViewText(R.id.widget_compact_water_text, "💧 $currentL / ${targetL}L")
        views.setTextColor(
            R.id.widget_compact_water_text,
            if (snapshot.isWaterGoalMet) Color.parseColor("#06D6A0") else Color.parseColor("#38BDF8")
        )
        views.setTextViewText(R.id.widget_compact_btn_food, if (isTr) "✨ AI Öğün" else "✨ AI Meal")

        views.setOnClickPendingIntent(
            R.id.widget_compact_root,
            createDeepLinkPendingIntent(context, "healthtrack://open?tab=food", 1010)
        )
        views.setOnClickPendingIntent(
            R.id.widget_compact_btn_water,
            createAddWaterPendingIntent(context, 250, 2002)
        )
        views.setOnClickPendingIntent(
            R.id.widget_compact_btn_food,
            createQuickLogPendingIntent(context, WidgetQuickLogActivity.MODE_AI_FOOD, 6099)
        )

        return views
    }

    private fun renderCalorieRingBitmap(
        context: Context,
        snapshot: WidgetTelemetrySnapshot,
        sizePx: Int
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val strokeWidth = sizePx * 0.105f
        val radius = (sizePx / 2f) - strokeWidth
        val arcRect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val isDark = snapshot.isDarkTheme
        val isTr = snapshot.languageCode.lowercase().startsWith("tr")
        val rawRatio = snapshot.rawCalorieRatio
        val isOver = snapshot.isOverCalories
        val sweepAngle = snapshot.calorieProgress * 360f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = if (isDark) Color.parseColor("#243047") else Color.parseColor("#E2E8F0")
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        if (isOver) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth * 1.45f
                strokeCap = Paint.Cap.ROUND
                color = Color.argb(55, 239, 68, 68)
            }
            canvas.drawArc(arcRect, -90f, 360f, false, haloPaint)
        }

        if (sweepAngle > 0f) {
            val colors = when {
                isOver -> intArrayOf(
                    Color.parseColor("#F97316"),
                    Color.parseColor("#EF4444"),
                    Color.parseColor("#F97316")
                )
                rawRatio >= 0.75f -> intArrayOf(
                    Color.parseColor("#0284C7"),
                    Color.parseColor("#EAB308"),
                    Color.parseColor("#F97316"),
                    Color.parseColor("#0284C7")
                )
                rawRatio >= 0.50f -> intArrayOf(
                    Color.parseColor("#0284C7"),
                    Color.parseColor("#EAB308"),
                    Color.parseColor("#0284C7")
                )
                else -> intArrayOf(
                    Color.parseColor("#0284C7"),
                    Color.parseColor("#38BDF8"),
                    Color.parseColor("#0284C7")
                )
            }
            val matrix = android.graphics.Matrix().apply {
                setRotate(-90f, cx, cy)
            }
            val sweepGradient = SweepGradient(cx, cy, colors, null).apply {
                setLocalMatrix(matrix)
            }
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth
                strokeCap = Paint.Cap.ROUND
                shader = sweepGradient
            }
            canvas.drawArc(arcRect, -90f, sweepAngle, false, arcPaint)
        }

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = sizePx * 0.21f
            color = when {
                isOver -> Color.parseColor("#EF4444")
                isDark -> Color.parseColor("#F8FAFC")
                else -> Color.parseColor("#0F172A")
            }
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = sizePx * 0.092f
            color = if (isDark) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = sizePx * 0.095f
            color = when {
                isOver -> Color.parseColor("#F87171")
                rawRatio >= 0.75f -> Color.parseColor("#FB923C")
                rawRatio >= 0.50f -> Color.parseColor("#FACC15")
                else -> Color.parseColor("#38BDF8")
            }
        }

        val topLabel = if (isOver) {
            if (isTr) "AŞILDI" else "OVER"
        } else {
            if (isTr) "ALINAN" else "INTAKE"
        }
        val bottomLabel = if (isOver) {
            "+${snapshot.overCalories} kcal"
        } else {
            "${snapshot.remainingCalories} ${if (isTr) "kaldı" else "left"}"
        }

        canvas.drawText(topLabel, cx, cy - sizePx * 0.14f, labelPaint)
        canvas.drawText("${snapshot.consumedCalories}", cx, cy + sizePx * 0.07f, valuePaint)
        canvas.drawText(bottomLabel, cx, cy + sizePx * 0.22f, subPaint)

        return bitmap
    }

    private fun createDeepLinkPendingIntent(
        context: Context,
        uriString: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createQuickLogPendingIntent(
        context: Context,
        mode: String,
        requestCode: Int
    ): PendingIntent {
        val intent = WidgetQuickLogActivity.createIntent(context, mode)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createAddWaterPendingIntent(
        context: Context,
        deltaMl: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, HealthTrackTelemetryWidgetProvider::class.java).apply {
            action = ACTION_ADD_WATER
            putExtra(EXTRA_WATER_DELTA_ML, deltaMl)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createCyclePagePendingIntent(
        context: Context,
        appWidgetId: Int,
        delta: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, HealthTrackTelemetryWidgetProvider::class.java).apply {
            action = ACTION_CYCLE_PAGE
            putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_PAGE_DELTA, delta)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createSetPagePendingIntent(
        context: Context,
        appWidgetId: Int,
        pageIndex: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, HealthTrackTelemetryWidgetProvider::class.java).apply {
            action = ACTION_SET_PAGE
            putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(EXTRA_PAGE_INDEX, pageIndex)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createQuickWorkoutPendingIntent(
        context: Context,
        titleTr: String,
        titleEn: String,
        durationMin: Int,
        caloriesBurned: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, HealthTrackTelemetryWidgetProvider::class.java).apply {
            action = ACTION_QUICK_WORKOUT
            putExtra(EXTRA_WORKOUT_TITLE_TR, titleTr)
            putExtra(EXTRA_WORKOUT_TITLE_EN, titleEn)
            putExtra(EXTRA_WORKOUT_DURATION_MIN, durationMin)
            putExtra(EXTRA_WORKOUT_CALORIES, caloriesBurned)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createAdjustWeightPendingIntent(
        context: Context,
        deltaTenths: Int,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, HealthTrackTelemetryWidgetProvider::class.java).apply {
            action = ACTION_ADJUST_WEIGHT
            putExtra(EXTRA_WEIGHT_DELTA_TENTHS, deltaTenths)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createSaveWeightPendingIntent(
        context: Context,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, HealthTrackTelemetryWidgetProvider::class.java).apply {
            action = ACTION_SAVE_WEIGHT
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
