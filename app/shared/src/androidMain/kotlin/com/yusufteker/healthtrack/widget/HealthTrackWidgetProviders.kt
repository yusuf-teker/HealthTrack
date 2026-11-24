package com.yusufteker.healthtrack.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import com.russhwolf.settings.SharedPreferencesSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.WidgetSnapshotBuilder
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import java.util.Calendar
import kotlin.math.roundToInt

internal object AndroidWidgetPinBus {
    private val _pinnedEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)
    val pinnedEvents: SharedFlow<Boolean> = _pinnedEvents.asSharedFlow()

    fun notifyPinned() {
        _pinnedEvents.tryEmit(true)
    }
}

internal object HealthTrackWidgetStore {
    private const val KEY_WIDGET_SNAPSHOT = "ht_widget_snapshot_json"
    private const val KEY_PAGE_PREFIX = "ht_widget_page_"
    private const val KEY_DRAFT_WEIGHT = "ht_widget_draft_weight_kg"
    private const val KEY_FEEDBACK_TEXT = "ht_widget_feedback_text"
    private const val KEY_FEEDBACK_TS = "ht_widget_feedback_ts"
    const val ACTION_MIDNIGHT_REFRESH = "com.yusufteker.healthtrack.widget.ACTION_MIDNIGHT_REFRESH"
    private const val MIDNIGHT_ALARM_REQUEST_CODE = 4024

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)

    fun saveSnapshot(context: Context, snapshot: WidgetTelemetrySnapshot) {
        try {
            prefs(context).edit().putString(KEY_WIDGET_SNAPSHOT, json.encodeToString(snapshot)).apply()
        } catch (_: Throwable) {
        }
    }

    fun getActivePage(context: Context, appWidgetId: Int): Int {
        val raw = prefs(context).getInt("$KEY_PAGE_PREFIX$appWidgetId", HealthTrackWidgetRenderer.PAGE_SUMMARY)
        return raw.coerceIn(0, HealthTrackWidgetRenderer.PAGE_COUNT - 1)
    }

    fun setActivePage(context: Context, appWidgetId: Int, pageIndex: Int) {
        val clamped = pageIndex.coerceIn(0, HealthTrackWidgetRenderer.PAGE_COUNT - 1)
        prefs(context).edit().putInt("$KEY_PAGE_PREFIX$appWidgetId", clamped).apply()
    }

    fun cycleActivePage(context: Context, appWidgetId: Int, delta: Int) {
        val current = getActivePage(context, appWidgetId)
        val count = HealthTrackWidgetRenderer.PAGE_COUNT
        val next = ((current + delta) % count + count) % count
        setActivePage(context, appWidgetId, next)
    }

    fun getDraftWeight(context: Context, snapshot: WidgetTelemetrySnapshot): Double {
        val stored = prefs(context).getFloat(KEY_DRAFT_WEIGHT, -1f).toDouble()
        if (stored in 25.0..280.0) {
            return (stored * 10.0).roundToInt() / 10.0
        }
        val base = snapshot.latestWeightKg ?: 72.0
        return (base * 10.0).roundToInt() / 10.0
    }

    fun setDraftWeight(context: Context, weightKg: Double) {
        val clamped = ((weightKg.coerceIn(25.0, 280.0) * 10.0).roundToInt() / 10.0).toFloat()
        prefs(context).edit().putFloat(KEY_DRAFT_WEIGHT, clamped).apply()
    }

    fun adjustDraftWeight(context: Context, deltaTenths: Int) {
        val snapshot = loadSnapshot(context)
        val current = getDraftWeight(context, snapshot)
        val updated = ((current * 10.0).roundToInt() + deltaTenths) / 10.0
        setDraftWeight(context, updated)
    }

    fun setFeedback(context: Context, message: String) {
        prefs(context).edit()
            .putString(KEY_FEEDBACK_TEXT, message)
            .putLong(KEY_FEEDBACK_TS, System.currentTimeMillis())
            .apply()
    }

    fun getRecentFeedback(context: Context): String? {
        val p = prefs(context)
        val ts = p.getLong(KEY_FEEDBACK_TS, 0L)
        if (ts <= 0L || System.currentTimeMillis() - ts > 8_000L) return null
        return p.getString(KEY_FEEDBACK_TEXT, null)
    }

    private fun buildFreshTodaySnapshot(context: Context): WidgetTelemetrySnapshot {
        val koin = GlobalContext.getOrNull()
        val builder = koin?.getOrNull<WidgetSnapshotBuilder>()
        if (builder != null) {
            return builder.buildTodaySnapshot()
        }
        val settings = SharedPreferencesSettings(prefs(context))
        val tokenStorage = TokenStorage(settings)
        val db = HealthDatabase(settings)
        val computed = WidgetSnapshotBuilder(db, tokenStorage, settings).buildTodaySnapshot()
        return if (computed.isLoggedIn) computed else WidgetTelemetrySnapshot.sample(
            languageCode = computed.languageCode,
            isDarkTheme = computed.isDarkTheme,
            themeAccentId = computed.themeAccentId
        )
    }

    fun loadSnapshot(context: Context): WidgetTelemetrySnapshot {
        val todayIso = getTodayIso()
        val raw = prefs(context).getString(KEY_WIDGET_SNAPSHOT, null)
        val decoded = if (!raw.isNullOrBlank()) {
            runCatching { json.decodeFromString<WidgetTelemetrySnapshot>(raw) }.getOrNull()
        } else null

        if (decoded != null && decoded.dateIso == todayIso) {
            return decoded
        }

        val fresh = buildFreshTodaySnapshot(context)
        if (fresh.isLoggedIn && fresh.dateIso == todayIso) {
            saveSnapshot(context, fresh)
            return fresh
        }

        if (decoded != null && decoded.isLoggedIn) {
            val resetSnapshot = decoded.copy(
                dateIso = todayIso,
                consumedCalories = 0,
                burnedCalories = 0,
                proteinG = 0,
                carbsG = 0,
                fatG = 0,
                waterMl = 0,
                steps = 0,
                isActiveToday = false
            )
            saveSnapshot(context, resetSnapshot)
            return resetSnapshot
        }

        return fresh
    }

    fun isDateOrMidnightAction(action: String?): Boolean =
        action == ACTION_MIDNIGHT_REFRESH ||
            action == Intent.ACTION_DATE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED

    fun scheduleMidnightRefresh(context: Context) {
        if (!isAnyWidgetInstalled(context)) return
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, HealthTrackTelemetryWidgetProvider::class.java).apply {
                action = ACTION_MIDNIGHT_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MIDNIGHT_ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
                set(Calendar.MILLISECOND, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, triggerCal.timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC, triggerCal.timeInMillis, pendingIntent)
            }
        }
    }

    fun isAnyWidgetInstalled(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(
            HealthTrackTelemetryWidgetProvider::class.java,
            HealthTrackHydrationWidgetProvider::class.java,
            HealthTrackAiFoodWidgetProvider::class.java,
            HealthTrackWorkoutWidgetProvider::class.java,
            HealthTrackWeightWidgetProvider::class.java
        ).any { cls ->
            manager.getAppWidgetIds(ComponentName(context, cls)).isNotEmpty()
        }
    }

    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val snapshot = loadSnapshot(context)

        val telemetryIds = manager.getAppWidgetIds(ComponentName(context, HealthTrackTelemetryWidgetProvider::class.java))
        for (appWidgetId in telemetryIds) {
            val options = manager.getAppWidgetOptions(appWidgetId)
            val views = HealthTrackWidgetRenderer.buildTelemetryRemoteViews(context, snapshot, options, appWidgetId)
            manager.updateAppWidget(appWidgetId, views)
        }

        val hydrationIds = manager.getAppWidgetIds(ComponentName(context, HealthTrackHydrationWidgetProvider::class.java))
        for (appWidgetId in hydrationIds) {
            manager.updateAppWidget(appWidgetId, HealthTrackWidgetRenderer.buildCompactRemoteViews(context, snapshot))
        }

        val aiFoodIds = manager.getAppWidgetIds(ComponentName(context, HealthTrackAiFoodWidgetProvider::class.java))
        for (appWidgetId in aiFoodIds) {
            manager.updateAppWidget(appWidgetId, HealthTrackWidgetRenderer.buildStandaloneAiFoodRemoteViews(context, snapshot))
        }

        val workoutIds = manager.getAppWidgetIds(ComponentName(context, HealthTrackWorkoutWidgetProvider::class.java))
        for (appWidgetId in workoutIds) {
            manager.updateAppWidget(appWidgetId, HealthTrackWidgetRenderer.buildStandaloneWorkoutRemoteViews(context, snapshot))
        }

        val weightIds = manager.getAppWidgetIds(ComponentName(context, HealthTrackWeightWidgetProvider::class.java))
        for (appWidgetId in weightIds) {
            manager.updateAppWidget(appWidgetId, HealthTrackWidgetRenderer.buildStandaloneWeightRemoteViews(context, snapshot))
        }

        scheduleMidnightRefresh(context)
    }

    suspend fun handleAddWaterFromWidget(context: Context, deltaMl: Int) {
        val koin = GlobalContext.getOrNull()
        val settings = koin?.getOrNull<com.russhwolf.settings.Settings>()
            ?: SharedPreferencesSettings(prefs(context))
        val tokenStorage = koin?.getOrNull<TokenStorage>() ?: TokenStorage(settings)
        val db = koin?.getOrNull<HealthDatabase>() ?: HealthDatabase(settings)
        val builder = koin?.getOrNull<WidgetSnapshotBuilder>() ?: WidgetSnapshotBuilder(db, tokenStorage, settings)

        val userId = tokenStorage.getUserId()
        if (tokenStorage.hasActiveSession() && userId != 0) {
            val today = getTodayIso()
            val currentSnapshot = builder.buildTodaySnapshot(userId, today)
            db.addWater(
                userId = userId,
                date = today,
                deltaMl = deltaMl,
                defaultTargetMl = currentSnapshot.waterTargetMl
            )
            val updatedSnapshot = builder.buildTodaySnapshot(userId, today)
            saveSnapshot(context, updatedSnapshot)
        } else {
            val existing = loadSnapshot(context)
            saveSnapshot(
                context,
                existing.copy(waterMl = (existing.waterMl + deltaMl).coerceAtLeast(0))
            )
        }
        updateAllWidgets(context)
    }

    suspend fun handleQuickWorkoutFromWidget(
        context: Context,
        titleTr: String,
        titleEn: String,
        durationMin: Int,
        caloriesBurned: Int
    ) {
        val koin = GlobalContext.getOrNull()
        val settings = koin?.getOrNull<com.russhwolf.settings.Settings>()
            ?: SharedPreferencesSettings(prefs(context))
        val tokenStorage = koin?.getOrNull<TokenStorage>() ?: TokenStorage(settings)
        val db = koin?.getOrNull<HealthDatabase>() ?: HealthDatabase(settings)
        val builder = koin?.getOrNull<WidgetSnapshotBuilder>() ?: WidgetSnapshotBuilder(db, tokenStorage, settings)
        val syncEngine = koin?.getOrNull<SyncEngine>()

        val currentSnap = loadSnapshot(context)
        val isTr = currentSnap.languageCode.lowercase().startsWith("tr")
        val rawText = if (isTr) titleTr else titleEn
        val userId = tokenStorage.getUserId()

        if (tokenStorage.hasActiveSession() && userId != 0) {
            val today = getTodayIso()
            val now = currentTimeMillis()
            db.upsertWorkout(
                WorkoutLog(
                    id = "widget_workout_${userId}_$now",
                    userId = userId,
                    date = today,
                    rawText = rawText,
                    caloriesBurned = caloriesBurned.coerceAtLeast(0),
                    durationMinutes = durationMin.coerceAtLeast(0),
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
            val updated = builder.buildTodaySnapshot(userId, today)
            saveSnapshot(context, updated)
            runCatching { syncEngine?.sync() }
        } else {
            saveSnapshot(
                context,
                currentSnap.copy(burnedCalories = (currentSnap.burnedCalories + caloriesBurned).coerceAtLeast(0))
            )
        }
        setFeedback(context, "✓ $rawText (-$caloriesBurned kcal)")
        updateAllWidgets(context)
    }

    suspend fun handleSaveWeightFromWidget(context: Context) {
        val koin = GlobalContext.getOrNull()
        val settings = koin?.getOrNull<com.russhwolf.settings.Settings>()
            ?: SharedPreferencesSettings(prefs(context))
        val tokenStorage = koin?.getOrNull<TokenStorage>() ?: TokenStorage(settings)
        val db = koin?.getOrNull<HealthDatabase>() ?: HealthDatabase(settings)
        val builder = koin?.getOrNull<WidgetSnapshotBuilder>() ?: WidgetSnapshotBuilder(db, tokenStorage, settings)
        val syncEngine = koin?.getOrNull<SyncEngine>()

        val currentSnap = loadSnapshot(context)
        val isTr = currentSnap.languageCode.lowercase().startsWith("tr")
        val weightKg = getDraftWeight(context, currentSnap)
        val userId = tokenStorage.getUserId()

        if (tokenStorage.hasActiveSession() && userId != 0) {
            val today = getTodayIso()
            val now = currentTimeMillis()
            db.upsertWeight(
                WeightEntry(
                    id = "widget_weight_${userId}_${today}_$now",
                    userId = userId,
                    date = today,
                    weightKg = weightKg,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
            )
            val updated = builder.buildTodaySnapshot(userId, today)
            saveSnapshot(context, updated)
            runCatching { syncEngine?.sync() }
        } else {
            saveSnapshot(context, currentSnap.copy(latestWeightKg = weightKg))
        }
        val fmt = ((weightKg * 10.0).roundToInt() / 10.0)
        setFeedback(context, if (isTr) "✓ $fmt kg kaydedildi" else "✓ Saved $fmt kg")
        updateAllWidgets(context)
    }
}

class HealthTrackTelemetryWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val snapshot = HealthTrackWidgetStore.loadSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val views = HealthTrackWidgetRenderer.buildTelemetryRemoteViews(context, snapshot, options, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
        HealthTrackWidgetStore.scheduleMidnightRefresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val snapshot = HealthTrackWidgetStore.loadSnapshot(context)
        val views = HealthTrackWidgetRenderer.buildTelemetryRemoteViews(context, snapshot, newOptions, appWidgetId)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appCtx = context.applicationContext
        when (intent.action) {
            HealthTrackWidgetRenderer.ACTION_ADD_WATER -> {
                val deltaMl = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_WATER_DELTA_ML, 250)
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        HealthTrackWidgetStore.handleAddWaterFromWidget(appCtx, deltaMl)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            HealthTrackWidgetRenderer.ACTION_CYCLE_PAGE -> {
                val widgetId = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_APPWIDGET_ID, 0)
                val delta = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_PAGE_DELTA, 1)
                HealthTrackWidgetStore.cycleActivePage(appCtx, widgetId, delta)
                HealthTrackWidgetStore.updateAllWidgets(appCtx)
            }
            HealthTrackWidgetRenderer.ACTION_SET_PAGE -> {
                val widgetId = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_APPWIDGET_ID, 0)
                val pageIndex = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_PAGE_INDEX, 0)
                HealthTrackWidgetStore.setActivePage(appCtx, widgetId, pageIndex)
                HealthTrackWidgetStore.updateAllWidgets(appCtx)
            }
            HealthTrackWidgetRenderer.ACTION_QUICK_WORKOUT -> {
                val titleTr = intent.getStringExtra(HealthTrackWidgetRenderer.EXTRA_WORKOUT_TITLE_TR) ?: "30 dk Koşu"
                val titleEn = intent.getStringExtra(HealthTrackWidgetRenderer.EXTRA_WORKOUT_TITLE_EN) ?: "30 min Run"
                val durationMin = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_WORKOUT_DURATION_MIN, 30)
                val calories = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_WORKOUT_CALORIES, 250)
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        HealthTrackWidgetStore.handleQuickWorkoutFromWidget(appCtx, titleTr, titleEn, durationMin, calories)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            HealthTrackWidgetRenderer.ACTION_ADJUST_WEIGHT -> {
                val deltaTenths = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_WEIGHT_DELTA_TENTHS, 1)
                HealthTrackWidgetStore.adjustDraftWeight(appCtx, deltaTenths)
                HealthTrackWidgetStore.updateAllWidgets(appCtx)
            }
            HealthTrackWidgetRenderer.ACTION_SAVE_WEIGHT -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        HealthTrackWidgetStore.handleSaveWeightFromWidget(appCtx)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            else -> {
                if (HealthTrackWidgetStore.isDateOrMidnightAction(intent.action)) {
                    HealthTrackWidgetStore.updateAllWidgets(appCtx)
                }
            }
        }
    }
}

class HealthTrackHydrationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = HealthTrackWidgetStore.loadSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId, HealthTrackWidgetRenderer.buildCompactRemoteViews(context, snapshot))
        }
        HealthTrackWidgetStore.scheduleMidnightRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (HealthTrackWidgetStore.isDateOrMidnightAction(intent.action)) {
            HealthTrackWidgetStore.updateAllWidgets(context.applicationContext)
        }
    }
}

class HealthTrackAiFoodWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = HealthTrackWidgetStore.loadSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                HealthTrackWidgetRenderer.buildStandaloneAiFoodRemoteViews(context, snapshot)
            )
        }
        HealthTrackWidgetStore.scheduleMidnightRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (HealthTrackWidgetStore.isDateOrMidnightAction(intent.action)) {
            HealthTrackWidgetStore.updateAllWidgets(context.applicationContext)
        }
    }
}

class HealthTrackWorkoutWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = HealthTrackWidgetStore.loadSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                HealthTrackWidgetRenderer.buildStandaloneWorkoutRemoteViews(context, snapshot)
            )
        }
        HealthTrackWidgetStore.scheduleMidnightRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (HealthTrackWidgetStore.isDateOrMidnightAction(intent.action)) {
            HealthTrackWidgetStore.updateAllWidgets(context.applicationContext)
        }
    }
}

class HealthTrackWeightWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = HealthTrackWidgetStore.loadSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidget(
                appWidgetId,
                HealthTrackWidgetRenderer.buildStandaloneWeightRemoteViews(context, snapshot)
            )
        }
        HealthTrackWidgetStore.scheduleMidnightRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (HealthTrackWidgetStore.isDateOrMidnightAction(intent.action)) {
            HealthTrackWidgetStore.updateAllWidgets(context.applicationContext)
        }
    }
}

class HealthTrackWidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidWidgetPinBus.notifyPinned()
        HealthTrackWidgetStore.updateAllWidgets(context.applicationContext)
    }
}
