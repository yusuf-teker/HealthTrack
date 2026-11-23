package com.yusufteker.healthtrack.core.database

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier

/**
 * Row representation for normalized local persistence across SQLite (Android/iOS/JVM)
 * and Settings fallback (JS/WasmJS).
 */
data class HealthRecordRow(
    val bucket: String,
    val recordId: String,
    val userId: Int = 0,
    val date: String = "",
    val updatedAt: Long = 0L,
    val isDeleted: Boolean = false,
    val syncStatus: String = "PENDING",
    val payloadJson: String
)

interface HealthRecordStore {
    fun loadBucket(bucket: String): List<String>
    fun loadAllBuckets(): Map<String, List<String>>? = null
    fun upsertRecord(row: HealthRecordRow)
    fun upsertRecords(rows: List<HealthRecordRow>)
    fun deleteRecord(bucket: String, recordId: String)
    fun deleteRecords(bucket: String, recordIds: List<String>)
    fun deleteBucket(bucket: String)
    fun clearAll()
    fun isEmpty(): Boolean
    fun close() {}
}

/**
 * Lightweight fallback store backed by [Settings] for platforms without SQLite (JS/WasmJS).
 */
class SettingsHealthRecordStore(
    private val settings: Settings
) : HealthRecordStore {

    override fun loadBucket(bucket: String): List<String> = emptyList()
    override fun upsertRecord(row: HealthRecordRow) {}
    override fun upsertRecords(rows: List<HealthRecordRow>) {}
    override fun deleteRecord(bucket: String, recordId: String) {}
    override fun deleteRecords(bucket: String, recordIds: List<String>) {}
    override fun deleteBucket(bucket: String) {}

    override fun clearAll() {
        try {
            LEGACY_KEYS.forEach { settings.remove(it) }
        } catch (e: Exception) {
            Napier.e("SettingsHealthRecordStore.clearAll failed: ${e.message}", e, tag = "HEALTH_DB")
        }
    }

    override fun isEmpty(): Boolean = true

    companion object {
        val LEGACY_KEYS = listOf(
            "ht_db_foods",
            "ht_db_goals",
            "ht_db_workouts",
            "ht_db_weights",
            "ht_db_custom_foods",
            "ht_db_favorite_foods",
            "ht_db_ai_reports",
            "ht_db_water",
            "ht_db_daily_meal_plans"
        )
    }
}

expect fun createDefaultHealthRecordStore(settings: Settings): HealthRecordStore
