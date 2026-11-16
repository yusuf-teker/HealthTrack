package com.yusufteker.healthtrack

import com.russhwolf.settings.MapSettings
import com.yusufteker.healthtrack.core.model.FriendActivityType
import com.yusufteker.healthtrack.core.model.FriendBaselineEntry
import com.yusufteker.healthtrack.core.model.FriendNotificationEvent
import com.yusufteker.healthtrack.core.model.FriendNotificationJsonCodec
import com.yusufteker.healthtrack.core.model.LeaderboardEntryDto
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.FriendActivityMonitor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FriendActivityMonitorTest {

    @Test
    fun pendingFriendNotificationsPersistDeduplicateAndClear() {
        val storage = TokenStorage(MapSettings())

        val first = FriendNotificationEvent(
            id = "push_1",
            actorUserId = 10,
            actorName = "Ayşe",
            type = FriendActivityType.WORKOUT,
            titleTr = "Ayşe spor yaptı",
            titleEn = "Ayşe completed a workout",
            messageTr = "+320 kcal yaktı",
            messageEn = "Burned +320 kcal",
            createdAtMs = 1_000L
        )
        val duplicate = first.copy(titleTr = "Duplicate should be ignored")
        val second = first.copy(
            id = "push_2",
            actorUserId = 11,
            actorName = "Emre",
            type = FriendActivityType.MEAL
        )

        storage.enqueuePendingFriendNotification(first)
        storage.enqueuePendingFriendNotification(duplicate)
        storage.enqueuePendingFriendNotification(second)

        val pending = storage.getPendingFriendNotifications()
        assertEquals(2, pending.size)
        assertEquals(listOf("push_1", "push_2"), pending.map { it.id })
        assertEquals("Ayşe spor yaptı", pending.first().titleTr)

        storage.clearPendingFriendNotifications()
        assertTrue(storage.getPendingFriendNotifications().isEmpty())
    }

    @Test
    fun detectFriendActivityChangesGeneratesOnlyWorkoutEvents() {
        val previous = listOf(
            FriendBaselineEntry(userId = 1, score = 100, caloriesBurned = 200, workoutMinutes = 20, healthyEatingScore = 60, streakDays = 2),
            FriendBaselineEntry(userId = 2, score = 80, caloriesBurned = 0, workoutMinutes = 0, healthyEatingScore = 50, streakDays = 1),
            FriendBaselineEntry(userId = 3, score = 50, caloriesBurned = 0, workoutMinutes = 0, healthyEatingScore = 70, streakDays = 2),
            FriendBaselineEntry(userId = 4, score = 120, caloriesBurned = 0, workoutMinutes = 0, healthyEatingScore = 40, streakDays = 1),
            FriendBaselineEntry(userId = 5, score = 90, caloriesBurned = 150, workoutMinutes = 15, healthyEatingScore = 80, streakDays = 3),
            FriendBaselineEntry(userId = 6, score = 60, caloriesBurned = 100, workoutMinutes = 10, healthyEatingScore = 50, streakDays = 1)
        )

        val current = listOf(
            // 1: Burned calories increased -> WORKOUT
            LeaderboardEntryDto(rank = 1, userId = 1, name = "Ayşe", username = "ayse", score = 140, caloriesBurned = 450, workoutMinutes = 45, healthyEatingScore = 60, streakDays = 2),
            // 2: Workout minutes increased -> WORKOUT
            LeaderboardEntryDto(rank = 2, userId = 2, name = "Mert", username = "mert", score = 90, caloriesBurned = 0, workoutMinutes = 30, healthyEatingScore = 50, streakDays = 1),
            // 3: Healthy eating score increased -> ignored (meal notifications disabled)
            LeaderboardEntryDto(rank = 3, userId = 3, name = "Deniz", username = "deniz", score = 70, caloriesBurned = 0, workoutMinutes = 0, healthyEatingScore = 85, streakDays = 2),
            // 4: Streak increased -> ignored
            LeaderboardEntryDto(rank = 4, userId = 4, name = "Zeynep", username = "zeynep", score = 135, caloriesBurned = 0, workoutMinutes = 0, healthyEatingScore = 40, streakDays = 2),
            // 5: XP increased without workout -> ignored
            LeaderboardEntryDto(rank = 5, userId = 5, name = "Can", username = "can", score = 125, caloriesBurned = 150, workoutMinutes = 15, healthyEatingScore = 80, streakDays = 3),
            // 6: Unchanged -> ignored
            LeaderboardEntryDto(rank = 6, userId = 6, name = "Ege", username = "ege", score = 60, caloriesBurned = 100, workoutMinutes = 10, healthyEatingScore = 50, streakDays = 1)
        )

        val events = FriendActivityMonitor.detectFriendActivityChanges(
            previous = previous,
            current = current,
            dateIso = "2026-11-15",
            nowMs = 5_000L
        )

        assertEquals(2, events.size)
        assertTrue(events.all { it.type == FriendActivityType.WORKOUT })
        assertEquals("Ayşe", events[0].actorName)
        assertTrue(events[0].messageTr.contains("+250 kcal"))
        assertTrue(events[0].messageTr.contains("25 dk"))

        assertEquals("Mert", events[1].actorName)
        assertTrue(events[1].messageTr.contains("+30 dk"))
    }

    @Test
    fun jsonCodecRoundTripsEventsAndBaselines() {
        val events = listOf(
            FriendNotificationEvent(
                id = "ev_1",
                actorUserId = 42,
                actorName = "Kaan",
                actorUsername = "kaan_fit",
                type = FriendActivityType.BADGE,
                titleTr = "Rozet kazandı",
                titleEn = "Unlocked a badge",
                messageTr = "Altın seri",
                messageEn = "Gold streak",
                createdAtMs = 99_000L
            )
        )
        val encodedEvents = FriendNotificationJsonCodec.encodeList(events)
        assertEquals(events, FriendNotificationJsonCodec.decodeList(encodedEvents))

        val baselines = listOf(
            FriendBaselineEntry(userId = 42, score = 500, caloriesBurned = 600, workoutMinutes = 55, healthyEatingScore = 90, streakDays = 7)
        )
        val encodedBaseline = FriendNotificationJsonCodec.encodeBaseline(baselines)
        assertEquals(baselines, FriendNotificationJsonCodec.decodeBaseline(encodedBaseline))
    }

    @Test
    fun monitorBannerQueueDisplaysFifoAndClearsCleanly() {
        val storage = TokenStorage(MapSettings())
        val apiClient = HealthApiClient(storage, baseUrl = "http://127.0.0.1:0")
        val monitor = FriendActivityMonitor(apiClient, storage)

        val e1 = FriendNotificationEvent("id_1", 1, "A", type = FriendActivityType.WORKOUT, titleTr = "T1", titleEn = "T1", messageTr = "M1", messageEn = "M1")
        val e2 = FriendNotificationEvent("id_2", 2, "B", type = FriendActivityType.MEAL, titleTr = "T2", titleEn = "T2", messageTr = "M2", messageEn = "M2")
        val e3 = FriendNotificationEvent("id_3", 3, "C", type = FriendActivityType.FOLLOW_REQUEST, titleTr = "T3", titleEn = "T3", messageTr = "M3", messageEn = "M3")

        assertNull(monitor.currentBanner.value)
        monitor.enqueueForImmediateDisplay(e1)
        monitor.enqueueForImmediateDisplay(e1) // duplicate ignored
        monitor.enqueueForImmediateDisplay(e2) // MEAL ignored
        monitor.enqueueForImmediateDisplay(e3)

        assertEquals("id_1", monitor.currentBanner.value?.id)
        monitor.dismissCurrentBanner()
        assertEquals("id_3", monitor.currentBanner.value?.id)

        monitor.clearAll()
        assertNull(monitor.currentBanner.value)
    }
}

