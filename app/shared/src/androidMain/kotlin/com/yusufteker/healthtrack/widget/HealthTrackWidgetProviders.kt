package com.yusufteker.healthtrack.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.russhwolf.settings.SharedPreferencesSettings
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.WidgetSnapshotBuilder
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

internal object AndroidWidgetPinBus {
    private val _pinnedEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)
    val pinnedEvents: SharedFlow<Boolean> = _pinnedEvents.asSharedFlow()

    fun notifyPinned() {
        _pinnedEvents.tryEmit(true)
    }
}

internal object HealthTrackWidgetStore {
    private const val KEY_WIDGET_SNAPSHOT = "ht_widget_snapshot_json"

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

    fun loadSnapshot(context: Context): WidgetTelemetrySnapshot {
        val raw = prefs(context).getString(KEY_WIDGET_SNAPSHOT, null)
        if (!raw.isNullOrBlank()) {
            try {
                return json.decodeFromString<WidgetTelemetrySnapshot>(raw)
            } catch (_: Throwable) {
            }
        }
        // Fallback: compute directly from SharedPreferences-backed HealthDatabase
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

    fun isAnyWidgetInstalled(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        val telemetryIds = manager.getAppWidgetIds(
            ComponentName(context, HealthTrackTelemetryWidgetProvider::class.java)
        )
        val hydrationIds = manager.getAppWidgetIds(
            ComponentName(context, HealthTrackHydrationWidgetProvider::class.java)
        )
        return telemetryIds.isNotEmpty() || hydrationIds.isNotEmpty()
    }

    fun updateAllWidgets(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val snapshot = loadSnapshot(context)

        val telemetryIds = manager.getAppWidgetIds(
            ComponentName(context, HealthTrackTelemetryWidgetProvider::class.java)
        )
        for (appWidgetId in telemetryIds) {
            val options = manager.getAppWidgetOptions(appWidgetId)
            val views = HealthTrackWidgetRenderer.buildTelemetryRemoteViews(context, snapshot, options)
            manager.updateAppWidget(appWidgetId, views)
        }

        val hydrationIds = manager.getAppWidgetIds(
            ComponentName(context, HealthTrackHydrationWidgetProvider::class.java)
        )
        for (appWidgetId in hydrationIds) {
            val views = HealthTrackWidgetRenderer.buildCompactRemoteViews(context, snapshot)
            manager.updateAppWidget(appWidgetId, views)
        }
    }

    suspend fun handleAddWaterFromWidget(context: Context, deltaMl: Int) {
        val koin = GlobalContext.getOrNull()
        val settings = koin?.getOrNull<com.russhwolf.settings.Settings>()
            ?: SharedPreferencesSettings(prefs(context))
        val tokenStorage = koin?.getOrNull<TokenStorage>() ?: TokenStorage(settings)
        val db = koin?.getOrNull<HealthDatabase>() ?: HealthDatabase(settings)
        val builder = koin?.getOrNull<WidgetSnapshotBuilder>() ?: WidgetSnapshotBuilder(db, tokenStorage, settings)

        val userId = tokenStorage.getUserId()
        if (tokenStorage.isLoggedIn() && userId > 0) {
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
            // Update sample preview gracefully if triggered before login
            val existing = loadSnapshot(context)
            saveSnapshot(
                context,
                existing.copy(waterMl = (existing.waterMl + deltaMl).coerceAtLeast(0))
            )
        }
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
            val views = HealthTrackWidgetRenderer.buildTelemetryRemoteViews(context, snapshot, options)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val snapshot = HealthTrackWidgetStore.loadSnapshot(context)
        val views = HealthTrackWidgetRenderer.buildTelemetryRemoteViews(context, snapshot, newOptions)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == HealthTrackWidgetRenderer.ACTION_ADD_WATER) {
            val deltaMl = intent.getIntExtra(HealthTrackWidgetRenderer.EXTRA_WATER_DELTA_ML, 250)
            val pendingResult = goAsync()
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    HealthTrackWidgetStore.handleAddWaterFromWidget(context.applicationContext, deltaMl)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

class HealthTrackHydrationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val snapshot = HealthTrackWidgetStore.loadSnapshot(context)
        for (appWidgetId in appWidgetIds) {
            val views = HealthTrackWidgetRenderer.buildCompactRemoteViews(context, snapshot)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}

class HealthTrackWidgetPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AndroidWidgetPinBus.notifyPinned()
        HealthTrackWidgetStore.updateAllWidgets(context.applicationContext)
    }
}

