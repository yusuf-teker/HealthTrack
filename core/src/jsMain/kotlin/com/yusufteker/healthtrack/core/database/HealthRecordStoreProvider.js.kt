package com.yusufteker.healthtrack.core.database

import com.russhwolf.settings.Settings

actual fun createDefaultHealthRecordStore(settings: Settings): HealthRecordStore {
    return SettingsHealthRecordStore(settings)
}
