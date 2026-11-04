package com.yusufteker.healthtrack.core.database

/**
 * Interface for saving and reading local attachments (Prescriptions, Medical Reports, PDFs, Images).
 * Designed for offline-first local storage, allowing seamless future migration to cloud storage (S3/GCS/Cloudinary).
 */
interface AttachmentStorage {
    suspend fun saveAttachment(appointmentId: String, fileName: String, bytes: ByteArray): String
    suspend fun readAttachment(localPath: String): ByteArray?
    suspend fun deleteAttachment(localPath: String): Boolean
}

/**
 * Default in-memory & local-path file attachment storage implementation for KMP.
 */
class LocalAttachmentStorage : AttachmentStorage {
    private val memoryStorage = mutableMapOf<String, ByteArray>()

    override suspend fun saveAttachment(appointmentId: String, fileName: String, bytes: ByteArray): String {
        val path = "health_attachments/${appointmentId}_${fileName}"
        memoryStorage[path] = bytes
        return path
    }

    override suspend fun readAttachment(localPath: String): ByteArray? {
        return memoryStorage[localPath]
    }

    override suspend fun deleteAttachment(localPath: String): Boolean {
        return memoryStorage.remove(localPath) != null
    }
}
