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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class TokenStorage(private val settings: Settings = Settings()) {

    private val _sessionFlow = MutableStateFlow(loadSession())
    val sessionFlow: StateFlow<UserSession> = _sessionFlow.asStateFlow()

    private val _isPremiumFlow = MutableStateFlow(isPremium())
    val isPremiumFlow: StateFlow<Boolean> = _isPremiumFlow.asStateFlow()

    private val _premiumUntilFlow = MutableStateFlow(getPremiumUntil())
    val premiumUntilFlow: StateFlow<String?> = _premiumUntilFlow.asStateFlow()

    private val _hasConnectedDeviceFlow = MutableStateFlow(hasConnectedDevice())
    val hasConnectedDeviceFlow: StateFlow<Boolean> = _hasConnectedDeviceFlow.asStateFlow()

    private val _isHuaweiConnectedFlow = MutableStateFlow(isHuaweiConnected())
    val isHuaweiConnectedFlow: StateFlow<Boolean> = _isHuaweiConnectedFlow.asStateFlow()

    @OptIn(ExperimentalTime::class)
    fun isPremium(): Boolean {
        if (!settings.getBoolean(KEY_IS_PREMIUM, false)) return false
        val untilRaw = getPremiumUntil() ?: return true
        val parsed = runCatching { Instant.parse(untilRaw) }.getOrNull() ?: return true
        return parsed.toEpochMilliseconds() > com.yusufteker.healthtrack.core.utils.currentTimeMillis()
    }
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
    fun isGuest(): Boolean = settings.getBoolean(KEY_IS_GUEST, false) && getUserId() == UserSession.GUEST_USER_ID
    fun isAuthenticated(): Boolean = isLoggedIn() && !isGuest()
    fun hasActiveSession(): Boolean = isLoggedIn() || isGuest()
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

    fun getWeightReminderHour(): Int = settings.getInt(KEY_WEIGHT_REMINDER_HOUR, 8)
    fun setWeightReminderHour(hour: Int) {
        settings.putInt(KEY_WEIGHT_REMINDER_HOUR, hour.coerceIn(0, 23))
    }

    fun getWeightReminderMinute(): Int = settings.getInt(KEY_WEIGHT_REMINDER_MINUTE, 0)
    fun setWeightReminderMinute(minute: Int) {
        settings.putInt(KEY_WEIGHT_REMINDER_MINUTE, minute.coerceIn(0, 59))
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

    fun getLastWeeklyReportReminderDate(): String? = settings.getStringOrNull(KEY_LAST_WEEKLY_REPORT_REMINDER_DATE)
    fun setLastWeeklyReportReminderDate(dateIso: String) {
        settings.putString(KEY_LAST_WEEKLY_REPORT_REMINDER_DATE, dateIso)
    }

    fun getIgnoredFoodNotificationCount(): Int = settings.getInt(KEY_IGNORED_FOOD_NOTIF_COUNT, 0)
    fun setIgnoredFoodNotificationCount(count: Int) {
        settings.putInt(KEY_IGNORED_FOOD_NOTIF_COUNT, count.coerceAtLeast(0))
    }

    fun getIgnoredWeightNotificationCount(): Int = settings.getInt(KEY_IGNORED_WEIGHT_NOTIF_COUNT, 0)
    fun setIgnoredWeightNotificationCount(count: Int) {
        settings.putInt(KEY_IGNORED_WEIGHT_NOTIF_COUNT, count.coerceAtLeast(0))
    }

    fun getDailyReminderCountForDate(dateIso: String): Int {
        val storedDate = settings.getStringOrNull(KEY_DAILY_REMINDER_SENT_DATE)
        return if (storedDate == dateIso) settings.getInt(KEY_DAILY_REMINDER_SENT_COUNT, 0) else 0
    }

    fun incrementDailyReminderCount(dateIso: String): Int {
        val nextCount = getDailyReminderCountForDate(dateIso) + 1
        settings.putString(KEY_DAILY_REMINDER_SENT_DATE, dateIso)
        settings.putInt(KEY_DAILY_REMINDER_SENT_COUNT, nextCount)
        return nextCount
    }

    fun getLastLocalReminderSentAtMs(): Long = settings.getLong(KEY_LAST_LOCAL_REMINDER_SENT_MS, 0L)
    fun setLastLocalReminderSentAtMs(ms: Long) {
        settings.putLong(KEY_LAST_LOCAL_REMINDER_SENT_MS, ms)
    }

    fun getLastAppActiveAtMs(): Long = settings.getLong(KEY_LAST_APP_ACTIVE_MS, 0L)
    fun setLastAppActiveAtMs(ms: Long) {
        settings.putLong(KEY_LAST_APP_ACTIVE_MS, ms)
    }

    fun getLastSeenWeeklySummaryMonday(userId: Int = getUserId()): String? {
        if (userId == 0) return null
        return settings.getStringOrNull("${KEY_LAST_SEEN_WEEKLY_SUMMARY_MONDAY}_$userId")
    }

    fun markWeeklySummarySeen(weekMondayIso: String, userId: Int = getUserId()) {
        if (userId == 0 || weekMondayIso.isBlank()) return
        settings.putString("${KEY_LAST_SEEN_WEEKLY_SUMMARY_MONDAY}_$userId", weekMondayIso)
    }

    /**
     * Returns true strictly once per completed week (identified by [prevWeekMondayIso])
     * when the user has an active session and meaningful data for that week.
     */
    fun shouldShowWeeklySummaryPopup(
        userId: Int,
        prevWeekMondayIso: String,
        hasEnoughData: Boolean
    ): Boolean {
        if (!hasActiveSession() || userId == 0 || prevWeekMondayIso.isBlank() || !hasEnoughData) return false
        return getLastSeenWeeklySummaryMonday(userId) != prevWeekMondayIso
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
        if (!hasActiveSession() || isWidgetInstalled || !isWidgetReminderEnabled()) return false
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

    fun startGuestSession(guestName: String = "Misafir") {
        val prevUserId = getUserId()
        if (prevUserId != UserSession.GUEST_USER_ID) {
            clearUserScopedPreferences()
        }

        settings.remove(KEY_ACCESS_TOKEN)
        settings.remove(KEY_REFRESH_TOKEN)
        settings.remove(KEY_IS_PREMIUM)
        settings.remove(KEY_PREMIUM_UNTIL)
        settings.putInt(KEY_USER_ID, UserSession.GUEST_USER_ID)
        settings.putBoolean(KEY_IS_GUEST, true)
        settings.putString(KEY_NAME, guestName)
        settings.putString(KEY_USERNAME, "guest")
        _isPremiumFlow.value = false
        _premiumUntilFlow.value = null

        _sessionFlow.value = UserSession(
            userId = UserSession.GUEST_USER_ID,
            name = guestName,
            username = "guest",
            accessToken = "",
            refreshToken = "",
            avatarId = settings.getStringOrNull(KEY_AVATAR_ID),
            profileImageUrl = null,
            isPremium = false,
            isLoggedIn = false,
            isGuest = true
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
        isPremium: Boolean = false,
        preserveGuestPreferences: Boolean = false
    ) {
        val prevUserId = getUserId()
        val upgradingFromGuest = prevUserId == UserSession.GUEST_USER_ID && preserveGuestPreferences
        if (prevUserId != userId && !upgradingFromGuest) {
            clearUserScopedPreferences()
        } else if (upgradingFromGuest) {
            // Preserve local guest profile & device settings, migrate hidden frequent foods and meal preferences, reset sync cursor
            val guestHidden = getHiddenFrequentFoodNames(UserSession.GUEST_USER_ID)
            if (guestHidden.isNotEmpty()) {
                val mergedHidden = getHiddenFrequentFoodNames(userId) + guestHidden
                settings.putString("${KEY_HIDDEN_FREQUENT_FOODS}_$userId", mergedHidden.joinToString("||"))
                settings.remove("${KEY_HIDDEN_FREQUENT_FOODS}_${UserSession.GUEST_USER_ID}")
            }
            val guestExcluded = getExcludedFoodIngredients(UserSession.GUEST_USER_ID)
            if (guestExcluded.isNotEmpty()) {
                val mergedExcluded = getExcludedFoodIngredients(userId) + guestExcluded
                setExcludedFoodIngredients(mergedExcluded, userId)
                settings.remove("${KEY_EXCLUDED_FOOD_INGREDIENTS}_${UserSession.GUEST_USER_ID}")
            }
            val guestFavorites = getFavoriteFoodIngredients(UserSession.GUEST_USER_ID)
            if (guestFavorites.isNotEmpty()) {
                val mergedFavorites = getFavoriteFoodIngredients(userId) + guestFavorites
                setFavoriteFoodIngredients(mergedFavorites, userId)
                settings.remove("${KEY_FAVORITE_FOOD_INGREDIENTS}_${UserSession.GUEST_USER_ID}")
            }
            settings.remove(KEY_LAST_SYNC_TS)
        }

        settings.remove(KEY_IS_GUEST)
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
            isLoggedIn = true,
            isGuest = false
        )
    }

    fun getAvatarId(): String? = settings.getStringOrNull(KEY_AVATAR_ID)
    fun getProfileImageUrl(): String? = settings.getStringOrNull(KEY_PROFILE_IMAGE_URL)?.takeIf { it.isNotBlank() }

    fun setProfileImageUrl(url: String?) {
        val clean = url?.trim()?.takeIf { it.isNotBlank() }
        if (clean != null) {
            settings.putString(KEY_PROFILE_IMAGE_URL, clean)
        } else {
            settings.remove(KEY_PROFILE_IMAGE_URL)
        }
        _sessionFlow.value = _sessionFlow.value.copy(profileImageUrl = clean)
    }

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
        settings.remove(KEY_LAST_WEEKLY_REPORT_REMINDER_DATE)
        settings.remove(KEY_IGNORED_FOOD_NOTIF_COUNT)
        settings.remove(KEY_IGNORED_WEIGHT_NOTIF_COUNT)
        settings.remove(KEY_DAILY_REMINDER_SENT_DATE)
        settings.remove(KEY_DAILY_REMINDER_SENT_COUNT)
        settings.remove(KEY_LAST_LOCAL_REMINDER_SENT_MS)
        settings.remove(KEY_LAST_APP_ACTIVE_MS)
        settings.remove(KEY_CUSTOM_GEMINI_API_KEY)
        settings.remove(KEY_HAS_CONNECTED_DEVICE)
        settings.remove(KEY_LAST_DEVICE_SYNC_MS)
        settings.remove(KEY_LAST_DEVICE_SYNC_SUMMARY)
        settings.remove(KEY_HUAWEI_CONNECTED)
        settings.remove(KEY_HUAWEI_LAST_SYNC_MS)
        _hasConnectedDeviceFlow.value = false
        _isHuaweiConnectedFlow.value = false
    }

    fun clearSession() {
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_IS_GUEST)
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
            isLoggedIn = false,
            isGuest = false
        )
    }

    private fun loadSession(): UserSession {
        val token = getAccessToken()
        val userId = getUserId()
        return when {
            !token.isNullOrBlank() && userId > 0 -> UserSession(
                userId = userId,
                name = getName(),
                username = getUsername(),
                accessToken = token,
                refreshToken = getRefreshToken() ?: "",
                avatarId = settings.getStringOrNull(KEY_AVATAR_ID),
                profileImageUrl = getProfileImageUrl(),
                isPremium = isPremium(),
                isLoggedIn = true,
                isGuest = false
            )
            settings.getBoolean(KEY_IS_GUEST, false) && userId == UserSession.GUEST_USER_ID -> UserSession(
                userId = UserSession.GUEST_USER_ID,
                name = getName().ifBlank { "Misafir" },
                username = "guest",
                accessToken = "",
                refreshToken = "",
                avatarId = settings.getStringOrNull(KEY_AVATAR_ID),
                profileImageUrl = null,
                isPremium = false,
                isLoggedIn = false,
                isGuest = true
            )
            else -> UserSession(
                userId = 0,
                name = "",
                username = "",
                accessToken = "",
                refreshToken = "",
                isLoggedIn = false,
                isGuest = false
            )
        }
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "ht_access_token"
        private const val KEY_REFRESH_TOKEN = "ht_refresh_token"
        private const val KEY_USER_ID = "ht_user_id"
        private const val KEY_IS_GUEST = "ht_is_guest"
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
        private const val KEY_WEIGHT_REMINDER_HOUR = "ht_weight_reminder_hour"
        private const val KEY_WEIGHT_REMINDER_MINUTE = "ht_weight_reminder_minute"
        private const val KEY_WORKOUT_REMINDER_FREQ = "ht_workout_reminder_frequency"
        private const val KEY_WORKOUT_REMINDER_HOUR = "ht_workout_reminder_hour"
        private const val KEY_WORKOUT_REMINDER_MINUTE = "ht_workout_reminder_minute"
        private const val KEY_LAST_WORKOUT_REMINDER_DATE = "ht_last_workout_reminder_date"
        private const val KEY_IGNORED_WORKOUT_NOTIF_COUNT = "ht_ignored_workout_notif_count"
        private const val KEY_FOOD_REMINDER_ENABLED = "ht_food_reminder_enabled"
        private const val KEY_LAST_WEIGHT_REMINDER_DATE = "ht_last_weight_reminder_date"
        private const val KEY_LAST_FOOD_REMINDER_DATE = "ht_last_food_reminder_date"
        private const val KEY_LAST_WEEKLY_REPORT_REMINDER_DATE = "ht_last_weekly_report_reminder_date"
        private const val KEY_IGNORED_FOOD_NOTIF_COUNT = "ht_ignored_food_notif_count"
        private const val KEY_IGNORED_WEIGHT_NOTIF_COUNT = "ht_ignored_weight_notif_count"
        private const val KEY_DAILY_REMINDER_SENT_DATE = "ht_daily_reminder_sent_date"
        private const val KEY_DAILY_REMINDER_SENT_COUNT = "ht_daily_reminder_sent_count"
        private const val KEY_LAST_LOCAL_REMINDER_SENT_MS = "ht_last_local_reminder_sent_ms"
        private const val KEY_LAST_APP_ACTIVE_MS = "ht_last_app_active_ms"
        private const val KEY_LAST_SEEN_WEEKLY_SUMMARY_MONDAY = "ht_last_seen_weekly_summary_monday"
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
        private const val KEY_MUTED_WORKOUT_FRIENDS_PREFIX = "ht_muted_workout_friends"
        const val KEY_FCM_TOKEN = "ht_fcm_token"
        private const val KEY_AUTO_DEVICE_SYNC_ENABLED = "ht_auto_device_sync_enabled"
        const val KEY_HAS_CONNECTED_DEVICE = "ht_has_connected_device"
        private const val KEY_LAST_DEVICE_SYNC_MS = "ht_last_device_sync_ms"
        private const val KEY_LAST_DEVICE_SYNC_SUMMARY = "ht_last_device_sync_summary"
        private const val KEY_HUAWEI_CONNECTED = "ht_huawei_connected"
        private const val KEY_HUAWEI_LAST_SYNC_MS = "ht_huawei_last_sync_ms"
        private const val KEY_LAST_NOTIF_PERM_PROMPT_MS = "ht_last_notif_perm_prompt_ms"
        private const val KEY_HIDDEN_FREQUENT_FOODS = "ht_hidden_frequent_foods"
        private const val KEY_EXCLUDED_FOOD_INGREDIENTS = "ht_excluded_food_ingredients"
        private const val KEY_FAVORITE_FOOD_INGREDIENTS = "ht_favorite_food_ingredients"
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

    fun isHuaweiConnected(): Boolean = settings.getBoolean(KEY_HUAWEI_CONNECTED, false)

    fun setHuaweiConnected(connected: Boolean) {
        settings.putBoolean(KEY_HUAWEI_CONNECTED, connected)
        _isHuaweiConnectedFlow.value = connected
    }

    fun getLastHuaweiSyncMs(): Long = settings.getLong(KEY_HUAWEI_LAST_SYNC_MS, 0L)

    fun setLastHuaweiSyncMs(ms: Long) {
        settings.putLong(KEY_HUAWEI_LAST_SYNC_MS, ms)
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

    fun getExcludedFoodIngredients(userId: Int = getUserId()): Set<String> {
        if (userId == 0) return emptySet()
        val raw = settings.getStringOrNull("${KEY_EXCLUDED_FOOD_INGREDIENTS}_$userId") ?: return emptySet()
        return raw.split("||").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    fun setExcludedFoodIngredients(ingredients: Set<String>, userId: Int = getUserId()) {
        if (userId == 0) return
        val clean = ingredients.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val key = "${KEY_EXCLUDED_FOOD_INGREDIENTS}_$userId"
        if (clean.isEmpty()) {
            settings.remove(key)
        } else {
            settings.putString(key, clean.joinToString("||"))
        }
    }

    fun getFavoriteFoodIngredients(userId: Int = getUserId()): Set<String> {
        if (userId == 0) return emptySet()
        val raw = settings.getStringOrNull("${KEY_FAVORITE_FOOD_INGREDIENTS}_$userId") ?: return emptySet()
        return raw.split("||").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    fun setFavoriteFoodIngredients(ingredients: Set<String>, userId: Int = getUserId()) {
        if (userId == 0) return
        val clean = ingredients.map { it.trim() }.filter { it.isNotBlank() }.toSet()
        val key = "${KEY_FAVORITE_FOOD_INGREDIENTS}_$userId"
        if (clean.isEmpty()) {
            settings.remove(key)
        } else {
            settings.putString(key, clean.joinToString("||"))
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

    fun getMutedWorkoutFriendIds(userId: Int = getUserId()): Set<Int> {
        if (userId <= 0) return emptySet()
        val raw = settings.getStringOrNull("${KEY_MUTED_WORKOUT_FRIENDS_PREFIX}_$userId") ?: return emptySet()
        return raw.split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun setFriendWorkoutTrackingEnabled(friendUserId: Int, enabled: Boolean, userId: Int = getUserId()) {
        if (userId <= 0 || friendUserId <= 0) return
        val current = getMutedWorkoutFriendIds(userId).toMutableSet()
        if (enabled) {
            current.remove(friendUserId)
        } else {
            current.add(friendUserId)
        }
        if (current.isEmpty()) {
            settings.remove("${KEY_MUTED_WORKOUT_FRIENDS_PREFIX}_$userId")
        } else {
            settings.putString("${KEY_MUTED_WORKOUT_FRIENDS_PREFIX}_$userId", current.joinToString(","))
        }
    }

    fun isFriendWorkoutTrackingEnabled(friendUserId: Int, userId: Int = getUserId()): Boolean {
        if (friendUserId <= 0) return true
        return friendUserId !in getMutedWorkoutFriendIds(userId)
    }

    fun syncMutedWorkoutFriendIds(mutedIds: Collection<Int>, userId: Int = getUserId()) {
        if (userId <= 0) return
        val clean = mutedIds.filter { it > 0 }.toSet()
        if (clean.isEmpty()) {
            settings.remove("${KEY_MUTED_WORKOUT_FRIENDS_PREFIX}_$userId")
        } else {
            settings.putString("${KEY_MUTED_WORKOUT_FRIENDS_PREFIX}_$userId", clean.joinToString(","))
        }
    }
}
