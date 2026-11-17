package com.yusufteker.healthtrack.core.database

import com.russhwolf.settings.Settings
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun createDefaultHealthRecordStore(settings: Settings): HealthRecordStore {
    val isInMemoryTest = settings::class.simpleName == "MapSettings"
    if (isInMemoryTest) {
        return SqliteHealthRecordStore(":memory:")
    }
    val baseDir = NSSearchPathForDirectoriesInDomains(
        NSApplicationSupportDirectory,
        NSUserDomainMask,
        true
    ).firstOrNull()?.toString() ?: return SqliteHealthRecordStore(":memory:")

    val dbDir = "$baseDir/HealthTrack"
    NSFileManager.defaultManager.createDirectoryAtPath(
        path = dbDir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )
    return SqliteHealthRecordStore("$dbDir/healthtrack.db")
}
