package com.yusufteker.healthtrack.core.network

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenStorage(private val settings: Settings = Settings()) {

    private val _sessionFlow = MutableStateFlow(loadSession())
    val sessionFlow: StateFlow<UserSession> = _sessionFlow.asStateFlow()

    fun getAccessToken(): String? = settings.getStringOrNull(KEY_ACCESS_TOKEN)
    fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH_TOKEN)
    fun getUserId(): Int = settings.getInt(KEY_USER_ID, 0)
    fun getUsername(): String = settings.getString(KEY_USERNAME, "")
    fun getName(): String = settings.getString(KEY_NAME, "")
    fun getLastSyncTimestamp(): Long = settings.getLong(KEY_LAST_SYNC_TS, 0L)

    fun setLastSyncTimestamp(ts: Long) {
        settings.putLong(KEY_LAST_SYNC_TS, ts)
    }

    fun saveSession(
        userId: Int,
        name: String,
        username: String,
        accessToken: String,
        refreshToken: String,
        avatarId: String? = null
    ) {
        settings.putInt(KEY_USER_ID, userId)
        settings.putString(KEY_NAME, name)
        settings.putString(KEY_USERNAME, username)
        settings.putString(KEY_ACCESS_TOKEN, accessToken)
        settings.putString(KEY_REFRESH_TOKEN, refreshToken)
        if (avatarId != null) settings.putString(KEY_AVATAR_ID, avatarId)

        _sessionFlow.value = UserSession(
            userId = userId,
            name = name,
            username = username,
            accessToken = accessToken,
            refreshToken = refreshToken,
            avatarId = avatarId,
            isLoggedIn = true
        )
    }

    fun clearSession() {
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_NAME)
        settings.remove(KEY_USERNAME)
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_AVATAR_ID)

        _sessionFlow.value = UserSession(
            userId = 0,
            name = "",
            username = "",
            accessToken = "",
            refreshToken = "",
            isLoggedIn = false
        )
    }

    private fun loadSession(): UserSession {
        val token = getAccessToken()
        val userId = getUserId()
        return if (!token.isNullOrBlank() && userId > 0) {
            UserSession(
                userId = userId,
                name = getName(),
                username = getUsername(),
                accessToken = token,
                refreshToken = getRefreshToken() ?: "",
                avatarId = settings.getStringOrNull(KEY_AVATAR_ID),
                isLoggedIn = true
            )
        } else {
            UserSession(
                userId = 0,
                name = "",
                username = "",
                accessToken = "",
                refreshToken = "",
                isLoggedIn = false
            )
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "ht_access_token"
        private const val KEY_REFRESH_TOKEN = "ht_refresh_token"
        private const val KEY_USER_ID = "ht_user_id"
        private const val KEY_NAME = "ht_name"
        private const val KEY_USERNAME = "ht_username"
        private const val KEY_AVATAR_ID = "ht_avatar_id"
        private const val KEY_LAST_SYNC_TS = "ht_last_sync_timestamp"
    }
}
