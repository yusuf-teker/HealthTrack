package com.yusufteker.healthtrack.core.database

import com.russhwolf.settings.Settings
import java.io.File

actual fun createDefaultHealthRecordStore(settings: Settings): HealthRecordStore {
    val isInMemoryTest = settings::class.simpleName == "MapSettings"
    if (isInMemoryTest) {
        return SqliteHealthRecordStore(":memory:")
    }
    val dir = File(System.getProperty("user.home"), ".healthtrack").apply { mkdirs() }
    val dbFile = File(dir, "healthtrack.db")
    return SqliteHealthRecordStore(dbFile.absolutePath)
}
