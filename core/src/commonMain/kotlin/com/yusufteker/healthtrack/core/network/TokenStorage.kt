package com.yusufteker.healthtrack.core.network

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.model.FitnessGoal
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

    fun getUserWeight(): Double = settings.getDouble(KEY_USER_WEIGHT, 0.0)
    fun setUserWeight(weight: Double) {
        settings.putDouble(KEY_USER_WEIGHT, weight)
    }

    fun getUserTargetWeight(): Double = settings.getDouble(KEY_USER_TARGET_WEIGHT, 70.0)
    fun setUserTargetWeight(targetWeight: Double) {
        settings.putDouble(KEY_USER_TARGET_WEIGHT, targetWeight)
    }

    fun getUserHeight(): Double = settings.getDouble(KEY_USER_HEIGHT, 0.0)
    fun setUserHeight(height: Double) {
        settings.putDouble(KEY_USER_HEIGHT, height)
    }

    fun getUserAge(): Int = settings.getInt(KEY_USER_AGE, 0)
    fun setUserAge(age: Int) {
        settings.putInt(KEY_USER_AGE, age)
    }

    fun getUserGender(): String = settings.getString(KEY_USER_GENDER, "MALE")
    fun setUserGender(gender: String) {
        settings.putString(KEY_USER_GENDER, gender)
    }

    fun getUserFitnessGoal(): FitnessGoal {
        val raw = settings.getString(KEY_USER_FITNESS_GOAL, FitnessGoal.MAINTAIN.id)
        return FitnessGoal.fromId(raw)
    }

    fun setUserFitnessGoal(goal: FitnessGoal) {
        settings.putString(KEY_USER_FITNESS_GOAL, goal.id)
    }

    fun isProfileConfigured(): Boolean {
        return getUserWeight() > 0.0
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        settings.putString(KEY_ACCESS_TOKEN, accessToken)
        settings.putString(KEY_REFRESH_TOKEN, refreshToken)
        _sessionFlow.value = _sessionFlow.value.copy(
            accessToken = accessToken,
            refreshToken = refreshToken,
            isLoggedIn = true
        )
    }

    fun saveSession(
        userId: Int,
        name: String,
        username: String,
        accessToken: String,
        refreshToken: String,
        avatarId: String? = null
    ) {
        val prevUserId = getUserId()
        if (prevUserId != userId) {
            settings.remove(KEY_LAST_SYNC_TS)
        }

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
        settings.remove(KEY_USER_WEIGHT)
        settings.remove(KEY_USER_TARGET_WEIGHT)
        settings.remove(KEY_USER_HEIGHT)
        settings.remove(KEY_USER_AGE)
        settings.remove(KEY_USER_GENDER)
        settings.remove(KEY_USER_FITNESS_GOAL)
        settings.remove(KEY_LAST_SYNC_TS)

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
        private const val KEY_USER_WEIGHT = "ht_user_weight"
        private const val KEY_USER_TARGET_WEIGHT = "ht_user_target_weight"
        private const val KEY_USER_HEIGHT = "ht_user_height"
        private const val KEY_USER_AGE = "ht_user_age"
        private const val KEY_USER_GENDER = "ht_user_gender"
        private const val KEY_USER_FITNESS_GOAL = "ht_user_fitness_goal"
    }
}
