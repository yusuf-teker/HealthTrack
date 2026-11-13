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
    const val EXTRA_WATER_DELTA_ML = "extra_water_delta_ml"

    fun buildTelemetryRemoteViews(
        context: Context,
        snapshot: WidgetTelemetrySnapshot,
        options: Bundle? = null
    ): RemoteViews {
        val minWidthDp = options?.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250) ?: 250
        val minHeightDp = options?.getInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120) ?: 120

        if (minWidthDp in 1..200) {
            return buildCompactRemoteViews(context, snapshot)
        }

        val views = RemoteViews(context.packageName, R.layout.widget_healthtrack_telemetry)
        val isDark = snapshot.isDarkTheme
        val isTr = snapshot.languageCode.lowercase().startsWith("tr")

        views.setInt(
            R.id.widget_root,
            "setBackgroundResource",
            if (isDark) R.drawable.bg_widget_obsidian else R.drawable.bg_widget_light
        )

        val primaryTextColor = if (isDark) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
        val secondaryTextColor = if (isDark) Color.parseColor("#CBD5E1") else Color.parseColor("#475569")
        val actionBg = if (isDark) R.drawable.bg_widget_pill_action else R.drawable.bg_widget_pill_action_light

        views.setTextColor(R.id.widget_brand_title, primaryTextColor)
        views.setTextColor(R.id.widget_protein_label, secondaryTextColor)
        views.setTextColor(R.id.widget_carbs_label, secondaryTextColor)
        views.setTextColor(R.id.widget_fat_label, secondaryTextColor)
        views.setTextColor(R.id.widget_protein_value, primaryTextColor)
        views.setTextColor(R.id.widget_carbs_value, primaryTextColor)
        views.setTextColor(R.id.widget_fat_value, primaryTextColor)

        val streakLabel = if (snapshot.streakDays > 0) "⚡ ${snapshot.streakDays}${if (isTr) "g" else "d"}" else "⚡ 0"
        views.setTextViewText(R.id.widget_streak_pill, streakLabel)

        val latestWeight = snapshot.latestWeightKg
        val burnedText = if (snapshot.burnedCalories > 0) {
            "🔥 ${snapshot.burnedCalories} kcal"
        } else if (latestWeight != null) {
            val formattedWeight = ((latestWeight * 10).roundToInt() / 10.0)
            "⚖️ $formattedWeight kg"
        } else {
            ""
        }
        views.setTextViewText(R.id.widget_burned_chip, burnedText)
        views.setViewVisibility(
            R.id.widget_burned_chip,
            if (burnedText.isNotEmpty()) View.VISIBLE else View.GONE
        )

        // Calorie Ring Bitmap
        val ringBitmap = renderCalorieRingBitmap(context, snapshot, sizePx = 220)
        views.setImageViewBitmap(R.id.widget_calorie_ring, ringBitmap)

        // Macros
        views.setTextViewText(R.id.widget_protein_label, if (isTr) "Protein" else "Protein")
        views.setTextViewText(R.id.widget_carbs_label, if (isTr) "Karb" else "Carbs")
        views.setTextViewText(R.id.widget_fat_label, if (isTr) "Yağ" else "Fat")

        views.setTextViewText(R.id.widget_protein_value, "${snapshot.proteinG}/${snapshot.proteinTargetG}g")
        views.setTextViewText(R.id.widget_carbs_value, "${snapshot.carbsG}/${snapshot.carbsTargetG}g")
        views.setTextViewText(R.id.widget_fat_value, "${snapshot.fatG}/${snapshot.fatTargetG}g")

        views.setProgressBar(R.id.widget_protein_progress, 100, (snapshot.proteinProgress * 100).roundToInt(), false)
        views.setProgressBar(R.id.widget_carbs_progress, 100, (snapshot.carbsProgress * 100).roundToInt(), false)
        views.setProgressBar(R.id.widget_fat_progress, 100, (snapshot.fatProgress * 100).roundToInt(), false)

        // Hydration
        views.setTextViewText(R.id.widget_water_text, "💧 ${snapshot.waterMl} / ${snapshot.waterTargetMl} ml")
        views.setTextColor(
            R.id.widget_water_text,
            if (snapshot.isWaterGoalMet) Color.parseColor("#06D6A0") else Color.parseColor("#38BDF8")
        )
        views.setProgressBar(R.id.widget_water_progress, 100, (snapshot.waterProgress * 100).roundToInt(), false)

        // Quick Actions visibility for shorter heights
        val showActions = minHeightDp >= 130 || minHeightDp <= 0
        views.setViewVisibility(R.id.widget_actions_row, if (showActions) View.VISIBLE else View.GONE)

        views.setInt(R.id.widget_action_add_food, "setBackgroundResource", actionBg)
        views.setInt(R.id.widget_action_workout, "setBackgroundResource", actionBg)
        views.setInt(R.id.widget_action_weight, "setBackgroundResource", actionBg)

        views.setTextViewText(R.id.widget_action_add_food, if (isTr) "+ Öğün" else "+ Meal")
        views.setTextViewText(R.id.widget_action_workout, if (isTr) "Spor" else "Workout")
        views.setTextViewText(R.id.widget_action_weight, if (isTr) "Tartım" else "Weight")

        // Pending Intents
        views.setOnClickPendingIntent(
            R.id.widget_root,
            createDeepLinkPendingIntent(context, "healthtrack://open?tab=food", 1001)
        )
        views.setOnClickPendingIntent(
            R.id.widget_btn_add_water,
            createAddWaterPendingIntent(context, 250, 2001)
        )
        views.setOnClickPendingIntent(
            R.id.widget_action_add_food,
            createDeepLinkPendingIntent(context, "healthtrack://open?tab=food&action=add_food", 1002)
        )
        views.setOnClickPendingIntent(
            R.id.widget_action_workout,
            createDeepLinkPendingIntent(context, "healthtrack://open?tab=workout", 1003)
        )
        views.setOnClickPendingIntent(
            R.id.widget_action_weight,
            createDeepLinkPendingIntent(context, "healthtrack://open?tab=weight", 1004)
        )

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
        views.setTextViewText(R.id.widget_compact_btn_food, if (isTr) "+ Öğün" else "+ Meal")

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
            createDeepLinkPendingIntent(context, "healthtrack://open?tab=food&action=add_food", 1011)
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

        // Track ring
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
            strokeCap = Paint.Cap.ROUND
            color = if (isDark) Color.parseColor("#243047") else Color.parseColor("#E2E8F0")
        }
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)

        // Over halo
        if (isOver) {
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = strokeWidth * 1.45f
                strokeCap = Paint.Cap.ROUND
                color = Color.argb(55, 239, 68, 68)
            }
            canvas.drawArc(arcRect, -90f, 360f, false, haloPaint)
        }

        // Progress gradient arc
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

        // Center Text: Consumed Calories
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
}

