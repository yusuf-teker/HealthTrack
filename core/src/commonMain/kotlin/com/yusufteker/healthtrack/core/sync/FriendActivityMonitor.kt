package com.yusufteker.healthtrack.core.sync

import com.yusufteker.healthtrack.core.model.FollowRequestItemDto
import com.yusufteker.healthtrack.core.model.FriendActivityType
import com.yusufteker.healthtrack.core.model.FriendBaselineEntry
import com.yusufteker.healthtrack.core.model.FriendNotificationBus
import com.yusufteker.healthtrack.core.model.FriendNotificationEvent
import com.yusufteker.healthtrack.core.model.LeaderboardCategory
import com.yusufteker.healthtrack.core.model.LeaderboardEntryDto
import com.yusufteker.healthtrack.core.model.LeaderboardPeriod
import com.yusufteker.healthtrack.core.model.LeaderboardScope
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Coordinates Friend Activity Notifications for both:
 * 1. **Stored/Queued Pushes** received while offline/closed — restored and shown at the top when the app opens.
 * 2. **Live In-App Push & Friend Activity Sync** while the app is open — immediately pops the top banner.
 */
class FriendActivityMonitor(
    private val apiClient: HealthApiClient,
    private val tokenStorage: TokenStorage
) {
    private val _queue = MutableStateFlow<List<FriendNotificationEvent>>(emptyList())
    private val _currentBanner = MutableStateFlow<FriendNotificationEvent?>(null)
    val currentBanner: StateFlow<FriendNotificationEvent?> = _currentBanner.asStateFlow()

    /**
     * Collects live push events emitted by Android FCM / iOS Notification bridges while the app is open.
     * Should be launched once in a long-lived app scope.
     */
    suspend fun observeLivePushBus() {
        FriendNotificationBus.liveEvents.collect { event ->
            if (tokenStorage.isLoggedIn()) {
                enqueueForImmediateDisplay(event)
            } else {
                // Save so it shows up as a top popup as soon as the user signs in / opens the app
                tokenStorage.enqueuePendingFriendNotification(event)
            }
        }
    }

    /**
     * Flushes push notifications persisted by Firebase/APNs while the app was closed/backgrounded.
     */
    fun flushPendingPushes() {
        val storedPushes = tokenStorage.getPendingFriendNotifications()
        if (storedPushes.isNotEmpty()) {
            tokenStorage.clearPendingFriendNotifications()
            storedPushes.forEach { enqueueForImmediateDisplay(it) }
        }
    }

    /**
     * Restores any push notifications that arrived while the app was closed/logged out,
     * and checks live friend daily activity deltas + follow requests from the backend.
     */
    suspend fun checkForUpdates(userId: Int, todayIso: String = getTodayIso()) {
        if (userId <= 0 || !tokenStorage.isLoggedIn()) return

        // 1. Flush any push events persisted while app was closed / in background
        flushPendingPushes()

        val nowMs = currentTimeMillis()
        val generatedEvents = mutableListOf<FriendNotificationEvent>()

        // 2. Check new incoming follow requests
        val seenRequestIds = tokenStorage.getSeenFollowRequestIds(userId)
        val requestsResult = apiClient.getFollowRequests()
        requestsResult.onSuccess { requests ->
            val currentIds = requests.map { it.id }.toSet()
            val newlyArrived = requests.filter { it.id !in seenRequestIds }
            newlyArrived.forEach { req ->
                generatedEvents.add(buildFollowRequestNotification(req, nowMs))
            }
            tokenStorage.saveSeenFollowRequestIds(userId, seenRequestIds + currentIds)
        }.onFailure { err ->
            if (err is CancellationException) throw err
            Napier.d("Follow request poll skipped: ${err.message}", tag = "FRIEND_NOTIFY")
        }

        // 3. Diff friend daily leaderboard metrics to surface real-time friend activity
        val leaderboardResult = apiClient.getLeaderboard(
            period = LeaderboardPeriod.DAILY,
            scope = LeaderboardScope.FRIENDS,
            category = LeaderboardCategory.DISCIPLINE_XP,
            dateIso = todayIso
        )
        leaderboardResult.onSuccess { dto ->
            val previousBaseline = tokenStorage.getFriendActivityBaseline(userId, todayIso)
            val currentFriends = dto.entries.filter { !it.isCurrentUser && it.userId != userId }

            if (previousBaseline != null) {
                val detected = detectFriendActivityChanges(
                    previous = previousBaseline,
                    current = currentFriends,
                    dateIso = todayIso,
                    nowMs = nowMs
                )
                generatedEvents.addAll(detected)
            }

            val updatedBaseline = currentFriends.map { entry ->
                FriendBaselineEntry(
                    userId = entry.userId,
                    score = entry.score,
                    caloriesBurned = entry.caloriesBurned,
                    workoutMinutes = entry.workoutMinutes,
                    healthyEatingScore = entry.healthyEatingScore,
                    streakDays = entry.streakDays
                )
            }
            tokenStorage.saveFriendActivityBaseline(userId, todayIso, updatedBaseline)
        }.onFailure { err ->
            if (err is CancellationException) throw err
            Napier.d("Friend leaderboard poll skipped: ${err.message}", tag = "FRIEND_NOTIFY")
        }

        generatedEvents.forEach { enqueueForImmediateDisplay(it) }
    }

    fun enqueueForImmediateDisplay(event: FriendNotificationEvent) {
        val current = _currentBanner.value
        val existingQueue = _queue.value
        if (current?.id == event.id || existingQueue.any { it.id == event.id }) return

        if (current == null) {
            _currentBanner.value = event
        } else {
            _queue.value = existingQueue + event
        }
    }

    fun dismissCurrentBanner() {
        val next = _queue.value.firstOrNull()
        if (next != null) {
            _queue.value = _queue.value.drop(1)
            _currentBanner.value = next
        } else {
            _currentBanner.value = null
        }
    }

    fun clearAll() {
        _queue.value = emptyList()
        _currentBanner.value = null
    }

    companion object {
        fun buildFollowRequestNotification(
            req: FollowRequestItemDto,
            nowMs: Long = currentTimeMillis()
        ): FriendNotificationEvent {
            val handle = req.requesterUsername.takeIf { it.isNotBlank() }?.let { " (@$it)" } ?: ""
            return FriendNotificationEvent(
                id = "follow_req_${req.id}",
                actorUserId = req.requesterId,
                actorName = req.requesterName,
                actorUsername = req.requesterUsername,
                type = FriendActivityType.FOLLOW_REQUEST,
                titleTr = "${req.requesterName}$handle",
                titleEn = "${req.requesterName}$handle",
                messageTr = "Sana arkadaşlık isteği gönderdi.",
                messageEn = "Sent you a friend request.",
                createdAtMs = nowMs
            )
        }

        /**
         * Pure diff engine comparing a friend's previous daily metrics to their latest metrics.
         * Produces localized activity notifications whenever a friend logs a workout, meal, streak, or XP gain.
         */
        fun detectFriendActivityChanges(
            previous: List<FriendBaselineEntry>,
            current: List<LeaderboardEntryDto>,
            dateIso: String,
            nowMs: Long = currentTimeMillis()
        ): List<FriendNotificationEvent> {
            val prevById = previous.associateBy { it.userId }
            val results = mutableListOf<FriendNotificationEvent>()

            current.forEach { entry ->
                val prev = prevById[entry.userId] ?: return@forEach
                val actor = entry.name.ifBlank { "@${entry.username}" }

                when {
                    entry.caloriesBurned > prev.caloriesBurned -> {
                        val deltaBurn = entry.caloriesBurned - prev.caloriesBurned
                        val deltaMin = (entry.workoutMinutes - prev.workoutMinutes).coerceAtLeast(0)
                        val minTextTr = if (deltaMin > 0) " ($deltaMin dk)" else ""
                        val minTextEn = if (deltaMin > 0) " ($deltaMin min)" else ""
                        results.add(
                            FriendNotificationEvent(
                                id = "act_${entry.userId}_${dateIso}_burn_${entry.caloriesBurned}",
                                actorUserId = entry.userId,
                                actorName = actor,
                                actorUsername = entry.username,
                                type = FriendActivityType.WORKOUT,
                                titleTr = "$actor spor yaptı",
                                titleEn = "$actor completed a workout",
                                messageTr = "+$deltaBurn kcal yaktı$minTextTr 💪",
                                messageEn = "Burned +$deltaBurn kcal$minTextEn 💪",
                                createdAtMs = nowMs
                            )
                        )
                    }

                    entry.workoutMinutes > prev.workoutMinutes -> {
                        val deltaMin = entry.workoutMinutes - prev.workoutMinutes
                        results.add(
                            FriendNotificationEvent(
                                id = "act_${entry.userId}_${dateIso}_min_${entry.workoutMinutes}",
                                actorUserId = entry.userId,
                                actorName = actor,
                                actorUsername = entry.username,
                                type = FriendActivityType.WORKOUT,
                                titleTr = "$actor antrenman ekledi",
                                titleEn = "$actor logged a workout",
                                messageTr = "+$deltaMin dk aktif egzersiz kaydetti 🏃",
                                messageEn = "Logged +$deltaMin min of active exercise 🏃",
                                createdAtMs = nowMs
                            )
                        )
                    }

                    entry.healthyEatingScore > prev.healthyEatingScore -> {
                        results.add(
                            FriendNotificationEvent(
                                id = "act_${entry.userId}_${dateIso}_meal_${entry.healthyEatingScore}",
                                actorUserId = entry.userId,
                                actorName = actor,
                                actorUsername = entry.username,
                                type = FriendActivityType.MEAL,
                                titleTr = "$actor beslenme kaydı girdi",
                                titleEn = "$actor logged a meal",
                                messageTr = "Günlük beslenme hedefinde ilerledi 🥗",
                                messageEn = "Made progress on daily nutrition goals 🥗",
                                createdAtMs = nowMs
                            )
                        )
                    }

                    entry.streakDays > prev.streakDays && entry.streakDays >= 2 -> {
                        results.add(
                            FriendNotificationEvent(
                                id = "act_${entry.userId}_${dateIso}_streak_${entry.streakDays}",
                                actorUserId = entry.userId,
                                actorName = actor,
                                actorUsername = entry.username,
                                type = FriendActivityType.STREAK,
                                titleTr = "$actor serisini uzattı!",
                                titleEn = "$actor extended their streak!",
                                messageTr = "${entry.streakDays} günlük aktif seri 🔥",
                                messageEn = "${entry.streakDays}-day active streak 🔥",
                                createdAtMs = nowMs
                            )
                        )
                    }

                    entry.score > prev.score -> {
                        val deltaXp = entry.score - prev.score
                        results.add(
                            FriendNotificationEvent(
                                id = "act_${entry.userId}_${dateIso}_xp_${entry.score}",
                                actorUserId = entry.userId,
                                actorName = actor,
                                actorUsername = entry.username,
                                type = FriendActivityType.GENERAL,
                                titleTr = "$actor yeni aktivite ekledi",
                                titleEn = "$actor logged a new update",
                                messageTr = "+$deltaXp XP kazandı (Toplam ${entry.score} XP) ⚡",
                                messageEn = "Earned +$deltaXp XP (Total ${entry.score} XP) ⚡",
                                createdAtMs = nowMs
                            )
                        )
                    }
                }
            }
            return results
        }
    }
}

