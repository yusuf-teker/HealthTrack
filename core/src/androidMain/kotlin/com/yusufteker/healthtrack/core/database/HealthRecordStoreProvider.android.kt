package com.yusufteker.healthtrack.core.database

import android.app.Application
import android.content.Context
import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier

object HealthAndroidDbContext {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    internal fun resolveContext(): Context? {
        appContext?.let { return it }
        return try {
            val activityThread = Class.forName("android.app.ActivityThread")
            val method = activityThread.getMethod("currentApplication")
            (method.invoke(null) as? Application)?.applicationContext?.also {
                appContext = it
            }
        } catch (_: Throwable) {
            null
        }
    }
}

actual fun createDefaultHealthRecordStore(settings: Settings): HealthRecordStore {
    val isInMemoryTest = settings::class.simpleName == "MapSettings"
    if (isInMemoryTest) {
        return SqliteHealthRecordStore(":memory:")
    }
    val context = HealthAndroidDbContext.resolveContext()
    if (context == null) {
        Napier.w("Android Context unavailable; using in-memory SQLite store.", tag = "HEALTH_DB")
        return SqliteHealthRecordStore(":memory:")
    }
    val dbFile = context.getDatabasePath("healthtrack.db")
    dbFile.parentFile?.mkdirs()
    return SqliteHealthRecordStore(dbFile.absolutePath)
}
