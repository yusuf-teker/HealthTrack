package com.yusufteker.healthtrack.core.network

import com.russhwolf.settings.Settings
import com.yusufteker.healthtrack.core.model.FitnessGoal
import com.yusufteker.healthtrack.core.model.FriendBaselineEntry
import com.yusufteker.healthtrack.core.model.FriendNotificationEvent
import com.yusufteker.healthtrack.core.model.FriendNotificationJsonCodec
import com.yusufteker.healthtrack.core.model.UserSession
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TokenStorage(private val settings: Settings = Settings()) {

    private val _sessionFlow = MutableStateFlow(loadSession())
    val sessionFlow: StateFlow<UserSession> = _sessionFlow.asStateFlow()

    private val _isPremiumFlow = MutableStateFlow(isPremium())
    val isPremiumFlow: StateFlow<Boolean> = _isPremiumFlow.asStateFlow()

    private val _premiumUntilFlow = MutableStateFlow(getPremiumUntil())
    val premiumUntilFlow: StateFlow<String?> = _premiumUntilFlow.asStateFlow()

    private val _hasConnectedDeviceFlow = MutableStateFlow(hasConnectedDevice())
    val hasConnectedDeviceFlow: StateFlow<Boolean> = _hasConnectedDeviceFlow.asStateFlow()

    fun isPremium(): Boolean = settings.getBoolean(KEY_IS_PREMIUM, false)
    fun getPremiumUntil(): String? = settings.getStringOrNull(KEY_PREMIUM_UNTIL)

    fun setPremium(isPremium: Boolean, premiumUntil: String? = null) {
        settings.putBoolean(KEY_IS_PREMIUM, isPremium)
        if (!premiumUntil.isNullOrBlank()) {
            settings.putString(KEY_PREMIUM_UNTIL, premiumUntil)
        } else {
            settings.remove(KEY_PREMIUM_UNTIL)
        }
        _isPremiumFlow.value = isPremium
        _premiumUntilFlow.value = premiumUntil?.takeIf { it.isNotBlank() }
    }

    fun getAccessToken(): String? = settings.getStringOrNull(KEY_ACCESS_TOKEN)
    fun getRefreshToken(): String? = settings.getStringOrNull(KEY_REFRESH_TOKEN)
    fun getUserId(): Int = settings.getInt(KEY_USER_ID, 0)
    fun isLoggedIn(): Boolean = !getAccessToken().isNullOrBlank() && getUserId() > 0
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

    fun getWhatsAppNumber(): String = settings.getString(KEY_WHATSAPP_NUMBER, "")
    fun setWhatsAppNumber(phone: String) {
        settings.putString(KEY_WHATSAPP_NUMBER, phone.trim())
    }

    fun isAutoWeeklyWhatsAppEnabled(): Boolean = settings.getBoolean(KEY_AUTO_WEEKLY_WHATSAPP, true)
    fun setAutoWeeklyWhatsAppEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_AUTO_WEEKLY_WHATSAPP, enabled)
    }

    fun getLastAutoWeeklyReportMonday(): String = settings.getString(KEY_LAST_AUTO_WEEK_MONDAY, "")
    fun setLastAutoWeeklyReportMonday(mondayIso: String) {
        settings.putString(KEY_LAST_AUTO_WEEK_MONDAY, mondayIso)
    }

    fun getDailyMealPlanDismissedDate(): String? = settings.getStringOrNull(KEY_DAILY_MEAL_PLAN_DISMISSED_DATE)
    fun setDailyMealPlanDismissedDate(dateIso: String) {
        settings.putString(KEY_DAILY_MEAL_PLAN_DISMISSED_DATE, dateIso)
    }

    fun getWeightReminderFrequency(): WeightReminderFrequency {
        val raw = settings.getStringOrNull(KEY_WEIGHT_REMINDER_FREQ)
        return WeightReminderFrequency.fromId(raw)
    }

    fun setWeightReminderFrequency(freq: WeightReminderFrequency) {
        settings.putString(KEY_WEIGHT_REMINDER_FREQ, freq.id)
    }

    fun getWorkoutReminderFrequency(): com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency {
        val raw = settings.getStringOrNull(KEY_WORKOUT_REMINDER_FREQ)
        return com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency.fromId(raw)
    }

    fun setWorkoutReminderFrequency(freq: com.yusufteker.healthtrack.core.model.WorkoutReminderFrequency) {
        settings.putString(KEY_WORKOUT_REMINDER_FREQ, freq.id)
    }

    fun getWorkoutReminderHour(): Int = settings.getInt(KEY_WORKOUT_REMINDER_HOUR, 18)
    fun setWorkoutReminderHour(hour: Int) {
        settings.putInt(KEY_WORKOUT_REMINDER_HOUR, hour)
    }

    fun getWorkoutReminderMinute(): Int = settings.getInt(KEY_WORKOUT_REMINDER_MINUTE, 0)
    fun setWorkoutReminderMinute(minute: Int) {
        settings.putInt(KEY_WORKOUT_REMINDER_MINUTE, minute)
    }

    fun getLastWorkoutReminderDate(): String? = settings.getStringOrNull(KEY_LAST_WORKOUT_REMINDER_DATE)
    fun setLastWorkoutReminderDate(dateIso: String) {
        settings.putString(KEY_LAST_WORKOUT_REMINDER_DATE, dateIso)
    }

    fun getIgnoredWorkoutNotificationCount(): Int = settings.getInt(KEY_IGNORED_WORKOUT_NOTIF_COUNT, 0)
    fun setIgnoredWorkoutNotificationCount(count: Int) {
        settings.putInt(KEY_IGNORED_WORKOUT_NOTIF_COUNT, count)
    }

    fun isFoodReminderEnabled(): Boolean = settings.getBoolean(KEY_FOOD_REMINDER_ENABLED, true)

    fun setFoodReminderEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_FOOD_REMINDER_ENABLED, enabled)
    }

    fun getLastWeightReminderDate(): String? = settings.getStringOrNull(KEY_LAST_WEIGHT_REMINDER_DATE)
    fun setLastWeightReminderDate(dateIso: String) {
        settings.putString(KEY_LAST_WEIGHT_REMINDER_DATE, dateIso)
    }

    fun getLastFoodReminderDate(): String? = settings.getStringOrNull(KEY_LAST_FOOD_REMINDER_DATE)
    fun setLastFoodReminderDate(dateIso: String) {
        settings.putString(KEY_LAST_FOOD_REMINDER_DATE, dateIso)
    }

    fun getLastWidgetReminderDate(): String? = settings.getStringOrNull(KEY_LAST_WIDGET_REMINDER_DATE)
    fun setLastWidgetReminderDate(dateIso: String) {
        settings.putString(KEY_LAST_WIDGET_REMINDER_DATE, dateIso)
    }

    fun isWidgetReminderEnabled(): Boolean = settings.getBoolean(KEY_WIDGET_REMINDER_ENABLED, true)
    fun setWidgetReminderEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_WIDGET_REMINDER_ENABLED, enabled)
    }

    /**
     * Returns true at most once per calendar day when the user has not yet added a home-screen widget.
     */
    fun shouldShowDailyWidgetReminder(todayIso: String, isWidgetInstalled: Boolean): Boolean {
        if (!isLoggedIn() || isWidgetInstalled || !isWidgetReminderEnabled()) return false
        return getLastWidgetReminderDate() != todayIso
    }

    fun getCustomGeminiApiKey(): String? =
        settings.getStringOrNull(KEY_CUSTOM_GEMINI_API_KEY)?.takeIf { it.isNotBlank() }

    fun setCustomGeminiApiKey(apiKey: String?) {
        val cleaned = apiKey?.trim()?.takeIf { it.isNotBlank() }
        if (cleaned != null) {
            settings.putString(KEY_CUSTOM_GEMINI_API_KEY, cleaned)
        } else {
            settings.remove(KEY_CUSTOM_GEMINI_API_KEY)
        }
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
        avatarId: String? = null,
        profileImageUrl: String? = null,
        isPremium: Boolean = false
    ) {
        val prevUserId = getUserId()
        if (prevUserId != userId) {
            clearUserScopedPreferences()
        }

        settings.putInt(KEY_USER_ID, userId)
        settings.putString(KEY_NAME, name)
        settings.putString(KEY_USERNAME, username)
        settings.putString(KEY_ACCESS_TOKEN, accessToken)
        settings.putString(KEY_REFRESH_TOKEN, refreshToken)
        settings.putBoolean(KEY_IS_PREMIUM, isPremium)
        if (avatarId != null) {
            settings.putString(KEY_AVATAR_ID, avatarId)
        } else {
            settings.remove(KEY_AVATAR_ID)
        }
        if (!profileImageUrl.isNullOrBlank()) {
            settings.putString(KEY_PROFILE_IMAGE_URL, profileImageUrl)
        } else {
            settings.remove(KEY_PROFILE_IMAGE_URL)
        }

        _sessionFlow.value = UserSession(
            userId = userId,
            name = name,
            username = username,
            accessToken = accessToken,
            refreshToken = refreshToken,
            avatarId = avatarId,
            profileImageUrl = profileImageUrl?.takeIf { it.isNotBlank() },
            isPremium = isPremium,
            isLoggedIn = true
        )
    }

    fun getAvatarId(): String? = settings.getStringOrNull(KEY_AVATAR_ID)
    fun getProfileImageUrl(): String? = settings.getStringOrNull(KEY_PROFILE_IMAGE_URL)?.takeIf { it.isNotBlank() }

    private fun clearUserScopedPreferences() {
        settings.remove(KEY_AVATAR_ID)
        settings.remove(KEY_PROFILE_IMAGE_URL)
        settings.remove(KEY_USER_WEIGHT)
        settings.remove(KEY_USER_TARGET_WEIGHT)
        settings.remove(KEY_USER_HEIGHT)
        settings.remove(KEY_USER_AGE)
        settings.remove(KEY_USER_GENDER)
        settings.remove(KEY_USER_FITNESS_GOAL)
        settings.remove(KEY_LAST_SYNC_TS)
        settings.remove(KEY_WHATSAPP_NUMBER)
        settings.remove(KEY_LAST_AUTO_WEEK_MONDAY)
        settings.remove(KEY_DAILY_MEAL_PLAN_DISMISSED_DATE)
        settings.remove(KEY_LAST_WORKOUT_REMINDER_DATE)
        settings.remove(KEY_IGNORED_WORKOUT_NOTIF_COUNT)
        settings.remove(KEY_LAST_WEIGHT_REMINDER_DATE)
        settings.remove(KEY_LAST_FOOD_REMINDER_DATE)
        settings.remove(KEY_CUSTOM_GEMINI_API_KEY)
        settings.remove(KEY_HAS_CONNECTED_DEVICE)
        settings.remove(KEY_LAST_DEVICE_SYNC_MS)
        settings.remove(KEY_LAST_DEVICE_SYNC_SUMMARY)
        _hasConnectedDeviceFlow.value = false
    }

    fun clearSession() {
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_NAME)
        settings.remove(KEY_USERNAME)
        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_IS_PREMIUM)
        settings.remove(KEY_PREMIUM_UNTIL)
        clearUserScopedPreferences()
        _isPremiumFlow.value = false
        _premiumUntilFlow.value = null

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
                profileImageUrl = getProfileImageUrl(),
                isPremium = isPremium(),
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
        private const val KEY_PROFILE_IMAGE_URL = "ht_profile_image_url"
        private const val KEY_LAST_SYNC_TS = "ht_last_sync_timestamp"
        private const val KEY_USER_WEIGHT = "ht_user_weight"
        private const val KEY_USER_TARGET_WEIGHT = "ht_user_target_weight"
        private const val KEY_USER_HEIGHT = "ht_user_height"
        private const val KEY_USER_AGE = "ht_user_age"
        private const val KEY_USER_GENDER = "ht_user_gender"
        private const val KEY_USER_FITNESS_GOAL = "ht_user_fitness_goal"
        private const val KEY_WHATSAPP_NUMBER = "ht_whatsapp_number"
        private const val KEY_AUTO_WEEKLY_WHATSAPP = "ht_auto_weekly_whatsapp"
        private const val KEY_LAST_AUTO_WEEK_MONDAY = "ht_last_auto_week_monday"
        private const val KEY_DAILY_MEAL_PLAN_DISMISSED_DATE = "ht_daily_meal_plan_dismissed_date"
        private const val KEY_WEIGHT_REMINDER_FREQ = "ht_weight_reminder_frequency"
        private const val KEY_WORKOUT_REMINDER_FREQ = "ht_workout_reminder_frequency"
        private const val KEY_WORKOUT_REMINDER_HOUR = "ht_workout_reminder_hour"
        private const val KEY_WORKOUT_REMINDER_MINUTE = "ht_workout_reminder_minute"
        private const val KEY_LAST_WORKOUT_REMINDER_DATE = "ht_last_workout_reminder_date"
        private const val KEY_IGNORED_WORKOUT_NOTIF_COUNT = "ht_ignored_workout_notif_count"
        private const val KEY_FOOD_REMINDER_ENABLED = "ht_food_reminder_enabled"
        private const val KEY_LAST_WEIGHT_REMINDER_DATE = "ht_last_weight_reminder_date"
        private const val KEY_LAST_FOOD_REMINDER_DATE = "ht_last_food_reminder_date"
        private const val KEY_CUSTOM_GEMINI_API_KEY = "ht_custom_gemini_api_key"
        private const val KEY_IS_PREMIUM = "ht_is_premium"
        private const val KEY_PREMIUM_UNTIL = "ht_premium_until"
        private const val KEY_HAS_SEEN_ONBOARDING = "ht_has_seen_onboarding"
        private const val KEY_LAST_WIDGET_REMINDER_DATE = "ht_last_widget_reminder_date"
        private const val KEY_WIDGET_REMINDER_ENABLED = "ht_widget_reminder_enabled"
        const val KEY_PENDING_FRIEND_NOTIFS = "ht_pending_friend_notifs"
        private const val KEY_FRIEND_BASELINE_PREFIX = "ht_friend_baseline"
        private const val KEY_FRIEND_BASELINE_LAST_DATE = "ht_friend_baseline_last_date"
        private const val KEY_SEEN_FOLLOW_REQUESTS = "ht_seen_follow_requests"
        const val KEY_FCM_TOKEN = "ht_fcm_token"
        private const val KEY_AUTO_DEVICE_SYNC_ENABLED = "ht_auto_device_sync_enabled"
        const val KEY_HAS_CONNECTED_DEVICE = "ht_has_connected_device"
        private const val KEY_LAST_DEVICE_SYNC_MS = "ht_last_device_sync_ms"
        private const val KEY_LAST_DEVICE_SYNC_SUMMARY = "ht_last_device_sync_summary"
        private const val KEY_LAST_NOTIF_PERM_PROMPT_MS = "ht_last_notif_perm_prompt_ms"
        private const val KEY_HIDDEN_FREQUENT_FOODS = "ht_hidden_frequent_foods"
    }

    fun isAutoDeviceSyncEnabled(): Boolean = settings.getBoolean(KEY_AUTO_DEVICE_SYNC_ENABLED, true)

    fun setAutoDeviceSyncEnabled(enabled: Boolean) {
        settings.putBoolean(KEY_AUTO_DEVICE_SYNC_ENABLED, enabled)
    }

    fun hasConnectedDevice(): Boolean = settings.getBoolean(KEY_HAS_CONNECTED_DEVICE, false)

    fun setHasConnectedDevice(connected: Boolean) {
        settings.putBoolean(KEY_HAS_CONNECTED_DEVICE, connected)
        _hasConnectedDeviceFlow.value = connected
    }

    fun getLastDeviceSyncMs(): Long = settings.getLong(KEY_LAST_DEVICE_SYNC_MS, 0L)

    fun setLastDeviceSyncMs(ms: Long) {
        settings.putLong(KEY_LAST_DEVICE_SYNC_MS, ms)
    }

    fun getLastDeviceSyncSummary(): String? =
        settings.getStringOrNull(KEY_LAST_DEVICE_SYNC_SUMMARY)?.takeIf { it.isNotBlank() }

    fun setLastDeviceSyncSummary(summary: String?) {
        val clean = summary?.trim()?.takeIf { it.isNotBlank() }
        if (clean != null) {
            settings.putString(KEY_LAST_DEVICE_SYNC_SUMMARY, clean)
        } else {
            settings.remove(KEY_LAST_DEVICE_SYNC_SUMMARY)
        }
    }

    fun getLastNotifPermPromptMs(): Long = settings.getLong(KEY_LAST_NOTIF_PERM_PROMPT_MS, 0L)

    fun setLastNotifPermPromptMs(ms: Long) {
        settings.putLong(KEY_LAST_NOTIF_PERM_PROMPT_MS, ms)
    }

    fun getHiddenFrequentFoodNames(userId: Int): Set<String> {
        val raw = settings.getStringOrNull("${KEY_HIDDEN_FREQUENT_FOODS}_$userId") ?: return emptySet()
        return raw.split("||").map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    fun hideFrequentFoodName(userId: Int, foodName: String) {
        val normalized = foodName.trim().lowercase()
        if (normalized.isBlank()) return
        val updated = getHiddenFrequentFoodNames(userId) + normalized
        settings.putString("${KEY_HIDDEN_FREQUENT_FOODS}_$userId", updated.joinToString("||"))
    }

    fun unhideFrequentFoodName(userId: Int, foodName: String) {
        val normalized = foodName.trim().lowercase()
        if (normalized.isBlank()) return
        val updated = getHiddenFrequentFoodNames(userId) - normalized
        if (updated.isEmpty()) {
            settings.remove("${KEY_HIDDEN_FREQUENT_FOODS}_$userId")
        } else {
            settings.putString("${KEY_HIDDEN_FREQUENT_FOODS}_$userId", updated.joinToString("||"))
        }
    }

    fun hasSeenOnboarding(): Boolean = settings.getBoolean(KEY_HAS_SEEN_ONBOARDING, false)

    fun setOnboardingSeen(seen: Boolean = true) {
        settings.putBoolean(KEY_HAS_SEEN_ONBOARDING, seen)
    }

    // ── Friend Activity Push Queue & Baselines ──

    fun getPendingFriendNotifications(): List<FriendNotificationEvent> {
        return FriendNotificationJsonCodec.decodeList(settings.getStringOrNull(KEY_PENDING_FRIEND_NOTIFS))
    }

    fun enqueuePendingFriendNotification(event: FriendNotificationEvent) {
        val current = getPendingFriendNotifications().toMutableList()
        if (current.none { it.id == event.id }) {
            current.add(event)
            settings.putString(KEY_PENDING_FRIEND_NOTIFS, FriendNotificationJsonCodec.encodeList(current))
        }
    }

    fun replacePendingFriendNotifications(events: List<FriendNotificationEvent>) {
        if (events.isEmpty()) {
            settings.remove(KEY_PENDING_FRIEND_NOTIFS)
        } else {
            settings.putString(KEY_PENDING_FRIEND_NOTIFS, FriendNotificationJsonCodec.encodeList(events))
        }
    }

    fun removePendingFriendNotification(id: String) {
        val remaining = getPendingFriendNotifications().filterNot { it.id == id }
        replacePendingFriendNotifications(remaining)
    }

    fun clearPendingFriendNotifications() {
        settings.remove(KEY_PENDING_FRIEND_NOTIFS)
    }

    fun getFriendActivityBaseline(userId: Int, dateIso: String): List<FriendBaselineEntry>? {
        val key = "${KEY_FRIEND_BASELINE_PREFIX}_${userId}_$dateIso"
        val raw = settings.getStringOrNull(key) ?: return null
        return FriendNotificationJsonCodec.decodeBaseline(raw)
    }

    fun saveFriendActivityBaseline(userId: Int, dateIso: String, entries: List<FriendBaselineEntry>) {
        val prevDateKey = "${KEY_FRIEND_BASELINE_LAST_DATE}_$userId"
        val prevDate = settings.getStringOrNull(prevDateKey)
        if (!prevDate.isNullOrBlank() && prevDate != dateIso) {
            settings.remove("${KEY_FRIEND_BASELINE_PREFIX}_${userId}_$prevDate")
        }
        settings.putString(prevDateKey, dateIso)
        settings.putString(
            "${KEY_FRIEND_BASELINE_PREFIX}_${userId}_$dateIso",
            FriendNotificationJsonCodec.encodeBaseline(entries)
        )
    }

    fun getSeenFollowRequestIds(userId: Int): Set<Int> {
        val raw = settings.getStringOrNull("${KEY_SEEN_FOLLOW_REQUESTS}_$userId") ?: return emptySet()
        return raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun saveSeenFollowRequestIds(userId: Int, ids: Set<Int>) {
        if (ids.isEmpty()) {
            settings.remove("${KEY_SEEN_FOLLOW_REQUESTS}_$userId")
        } else {
            settings.putString("${KEY_SEEN_FOLLOW_REQUESTS}_$userId", ids.joinToString(","))
        }
    }

    fun getFcmToken(): String? = settings.getStringOrNull(KEY_FCM_TOKEN)?.takeIf { it.isNotBlank() }

    fun setFcmToken(token: String?) {
        val clean = token?.trim()?.takeIf { it.isNotBlank() }
        if (clean != null) {
            settings.putString(KEY_FCM_TOKEN, clean)
        } else {
            settings.remove(KEY_FCM_TOKEN)
        }
    }
}
