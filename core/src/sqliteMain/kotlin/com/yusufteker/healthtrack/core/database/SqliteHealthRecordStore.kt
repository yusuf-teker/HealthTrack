package com.yusufteker.healthtrack.core.database

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.github.aakira.napier.Napier

/**
 * Scalable SQLite-backed [HealthRecordStore] powered by `androidx.sqlite:sqlite-bundled`.
 * Uses a single-connection WAL database with indexed `(bucket, user_id)` lookups and
 * transactional batch writes so individual upserts never rewrite entire tables.
 */
class SqliteHealthRecordStore(
    private val dbPath: String
) : HealthRecordStore {

    private val driver = BundledSQLiteDriver()
    private val connection: SQLiteConnection = driver.open(dbPath).also { conn ->
        initializeSchema(conn)
    }

    private fun initializeSchema(conn: SQLiteConnection) {
        if (dbPath != ":memory:") {
            try {
                conn.execSQL("PRAGMA journal_mode = WAL;")
            } catch (e: Exception) {
                Napier.w("WAL mode pragma skipped: ${e.message}", tag = "HEALTH_DB")
            }
        }
        conn.execSQL("PRAGMA synchronous = NORMAL;")
        conn.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ht_records (
                bucket TEXT NOT NULL,
                record_id TEXT NOT NULL,
                user_id INTEGER NOT NULL DEFAULT 0,
                date TEXT NOT NULL DEFAULT '',
                updated_at INTEGER NOT NULL DEFAULT 0,
                is_deleted INTEGER NOT NULL DEFAULT 0,
                sync_status TEXT NOT NULL DEFAULT 'PENDING',
                payload_json TEXT NOT NULL,
                PRIMARY KEY (bucket, record_id)
            );
            """.trimIndent()
        )
        conn.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_ht_records_bucket_user ON ht_records(bucket, user_id);"
        )
    }

    override fun loadAllBuckets(): Map<String, List<String>> {
        val grouped = HashMap<String, MutableList<String>>(12)
        val stmt = connection.prepare(
            "SELECT bucket, payload_json FROM ht_records;"
        )
        try {
            while (stmt.step()) {
                val bucket = stmt.getText(0)
                val payload = stmt.getText(1)
                grouped.getOrPut(bucket) { mutableListOf() }.add(payload)
            }
        } finally {
            stmt.close()
        }
        return grouped
    }

    override fun loadBucket(bucket: String): List<String> {
        val result = mutableListOf<String>()
        val stmt = connection.prepare(
            "SELECT payload_json FROM ht_records WHERE bucket = ?;"
        )
        try {
            stmt.bindText(1, bucket)
            while (stmt.step()) {
                result.add(stmt.getText(0))
            }
        } finally {
            stmt.close()
        }
        return result
    }

    override fun upsertRecord(row: HealthRecordRow) {
        val stmt = connection.prepare(
            """
            INSERT OR REPLACE INTO ht_records
            (bucket, record_id, user_id, date, updated_at, is_deleted, sync_status, payload_json)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?);
            """.trimIndent()
        )
        try {
            stmt.bindText(1, row.bucket)
            stmt.bindText(2, row.recordId)
            stmt.bindLong(3, row.userId.toLong())
            stmt.bindText(4, row.date)
            stmt.bindLong(5, row.updatedAt)
            stmt.bindLong(6, if (row.isDeleted) 1L else 0L)
            stmt.bindText(7, row.syncStatus)
            stmt.bindText(8, row.payloadJson)
            stmt.step()
        } finally {
            stmt.close()
        }
    }

    override fun upsertRecords(rows: List<HealthRecordRow>) {
        if (rows.isEmpty()) return
        inTransaction {
            val stmt = connection.prepare(
                """
                INSERT OR REPLACE INTO ht_records
                (bucket, record_id, user_id, date, updated_at, is_deleted, sync_status, payload_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?);
                """.trimIndent()
            )
            try {
                for (row in rows) {
                    stmt.reset()
                    stmt.bindText(1, row.bucket)
                    stmt.bindText(2, row.recordId)
                    stmt.bindLong(3, row.userId.toLong())
                    stmt.bindText(4, row.date)
                    stmt.bindLong(5, row.updatedAt)
                    stmt.bindLong(6, if (row.isDeleted) 1L else 0L)
                    stmt.bindText(7, row.syncStatus)
                    stmt.bindText(8, row.payloadJson)
                    stmt.step()
                }
            } finally {
                stmt.close()
            }
        }
    }

    override fun deleteRecord(bucket: String, recordId: String) {
        val stmt = connection.prepare(
            "DELETE FROM ht_records WHERE bucket = ? AND record_id = ?;"
        )
        try {
            stmt.bindText(1, bucket)
            stmt.bindText(2, recordId)
            stmt.step()
        } finally {
            stmt.close()
        }
    }

    override fun deleteRecords(bucket: String, recordIds: List<String>) {
        if (recordIds.isEmpty()) return
        inTransaction {
            val stmt = connection.prepare(
                "DELETE FROM ht_records WHERE bucket = ? AND record_id = ?;"
            )
            try {
                for (id in recordIds) {
                    stmt.reset()
                    stmt.bindText(1, bucket)
                    stmt.bindText(2, id)
                    stmt.step()
                }
            } finally {
                stmt.close()
            }
        }
    }

    override fun deleteBucket(bucket: String) {
        val stmt = connection.prepare("DELETE FROM ht_records WHERE bucket = ?;")
        try {
            stmt.bindText(1, bucket)
            stmt.step()
        } finally {
            stmt.close()
        }
    }

    override fun clearAll() {
        connection.execSQL("DELETE FROM ht_records;")
    }

    override fun isEmpty(): Boolean {
        val stmt = connection.prepare("SELECT EXISTS(SELECT 1 FROM ht_records LIMIT 1);")
        return try {
            if (stmt.step()) stmt.getLong(0) == 0L else true
        } finally {
            stmt.close()
        }
    }

    override fun close() {
        try {
            connection.close()
        } catch (e: Exception) {
            Napier.e("Failed to close SQLite connection: ${e.message}", e, tag = "HEALTH_DB")
        }
    }

    private inline fun inTransaction(block: () -> Unit) {
        connection.execSQL("BEGIN IMMEDIATE;")
        try {
            block()
            connection.execSQL("COMMIT;")
        } catch (e: Throwable) {
            try {
                connection.execSQL("ROLLBACK;")
            } catch (_: Throwable) {
            }
            throw e
        }
    }
}
