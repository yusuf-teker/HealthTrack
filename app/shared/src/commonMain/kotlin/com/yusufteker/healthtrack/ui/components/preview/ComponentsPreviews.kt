package com.yusufteker.healthtrack.ui.components.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.BadgeId
import com.yusufteker.healthtrack.core.model.CelebrationEvent
import com.yusufteker.healthtrack.core.model.CelebrationType
import com.yusufteker.healthtrack.core.model.MotivationalToastEvent
import com.yusufteker.healthtrack.core.model.SportType
import com.yusufteker.healthtrack.core.model.WidgetTelemetrySnapshot
import com.yusufteker.healthtrack.theme.HealthTrackTheme
import com.yusufteker.healthtrack.ui.components.*

@Preview
@Composable
fun CalorieProgressGaugePreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CalorieProgressGauge(
                consumedCalories = 1750,
                targetCalories = 2200,
                burnedCalories = 380,
                proteinCurrent = 118.0,
                proteinTarget = 145.0,
                carbsCurrent = 185.0,
                carbsTarget = 240.0,
                fatCurrent = 52.0,
                fatTarget = 70.0,
                onEditGoalsClick = {}
            )
        }
    }
}

@Preview
@Composable
fun CalorieProgressGaugeBlueInitialPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CalorieProgressGauge(
                consumedCalories = 650,
                targetCalories = 2200,
                burnedCalories = 150,
                proteinCurrent = 42.0,
                proteinTarget = 145.0,
                carbsCurrent = 70.0,
                carbsTarget = 240.0,
                fatCurrent = 18.0,
                fatTarget = 70.0
            )
        }
    }
}

@Preview
@Composable
fun CalorieProgressGaugeYellowApproachingPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CalorieProgressGauge(
                consumedCalories = 1350,
                targetCalories = 2200,
                burnedCalories = 280,
                proteinCurrent = 90.0,
                proteinTarget = 145.0,
                carbsCurrent = 140.0,
                carbsTarget = 240.0,
                fatCurrent = 42.0,
                fatTarget = 70.0
            )
        }
    }
}

@Preview
@Composable
fun CalorieProgressGaugeExceededRedPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            CalorieProgressGauge(
                consumedCalories = 2480,
                targetCalories = 2200,
                burnedCalories = 500,
                proteinCurrent = 165.0,
                proteinTarget = 145.0,
                carbsCurrent = 270.0,
                carbsTarget = 240.0,
                fatCurrent = 82.0,
                fatTarget = 70.0
            )
        }
    }
}

@Preview
@Composable
fun DaysStripSelectorPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            DaysStripSelector(
                selectedDate = "2026-11-06",
                onDateSelected = {},
                onPrevWeek = {},
                onNextWeek = {}
            )
        }
    }
}

@Preview
@Composable
fun GlassCardPreview() {
    HealthTrackTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    GradientText(
                        text = "Bioluminescent GlassCard",
                        style = TextStyle(fontSize = 18.sp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Smoked obsidian background with luxury glowing neon cyan/emerald border accents.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun HealthBrandLogoDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .background(Color(0xFF0B0F19))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            HealthBrandLogo(size = 64.dp)
        }
    }
}

@Preview
@Composable
fun PlusActionIconsPreviewDark() {
    HealthTrackTheme(darkTheme = true) {
        Row(
            modifier = Modifier
                .background(Color(0xFF0B0F19))
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ForkSpoonPlusIcon(
                modifier = Modifier.size(36.dp),
                tint = Color(0xFF0DF2A6)
            )
            CrossedBarbellPlusIcon(
                modifier = Modifier.size(36.dp),
                tint = Color(0xFFF97316)
            )
        }
    }
}

@Preview
@Composable
fun PlusActionIconsPreviewLight() {
    HealthTrackTheme(darkTheme = false) {
        Row(
            modifier = Modifier
                .background(Color(0xFFF8FAFC))
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ForkSpoonPlusIcon(
                modifier = Modifier.size(36.dp),
                tint = Color(0xFF007A55)
            )
            CrossedBarbellPlusIcon(
                modifier = Modifier.size(36.dp),
                tint = Color(0xFFC2410C)
            )
        }
    }
}

@Preview
@Composable
fun HealthBrandLogoLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .background(Color(0xFFF8FAFC))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            HealthBrandLogo(size = 64.dp)
        }
    }
}

@Preview
@Composable
fun ShimmeringStreakPillDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        Row(
            modifier = Modifier
                .background(Color(0xFF0B0F19))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShimmeringStreakPill(streakDays = 0, isActiveToday = false, level = 1, onClick = {})
            ShimmeringStreakPill(streakDays = 5, isActiveToday = true, level = 3, onClick = {})
            ShimmeringStreakPill(streakDays = 16, isActiveToday = true, level = 8, onClick = {})
        }
    }
}

@Preview
@Composable
fun ConfettiCelebrationOverlayDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
        ) {
            ConfettiCelebrationOverlay(
                event = CelebrationEvent(
                    id = "preview_badge",
                    type = CelebrationType.BADGE_UNLOCKED,
                    titleTr = "Rozet Seviyesi Yükseldi!",
                    titleEn = "Badge Leveled Up!",
                    subtitleTr = "Seri Ateşi (Sv. 2/5) • 7 gün hedefine ulaştın!",
                    subtitleEn = "Streak Flame (Lv. 2/5) • Reached 7 days!",
                    xpBonus = 150,
                    badgeId = BadgeId.STREAK_MASTER,
                    badgeStageLevel = 2
                ),
                onDismiss = {}
            )
        }
    }
}

@Preview
@Composable
fun ShimmeringAiButtonDarkAndLightPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HealthTrackTheme(darkTheme = true) {
            Column(
                modifier = Modifier
                    .background(Color(0xFF0B0F19))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShimmeringAiButton(
                    text = "AI ile Hesapla (75.0 kg bazlı)",
                    isLoading = false,
                    enabled = true,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
                ShimmeringAiButton(
                    text = "AI ile Hesapla",
                    isLoading = false,
                    enabled = false,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        HealthTrackTheme(darkTheme = false) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFF8FAFC))
                    .padding(16.dp)
            ) {
                ShimmeringAiButton(
                    text = "AI ile Hesapla",
                    isLoading = false,
                    enabled = true,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview
@Composable
fun MotivationalToastHostPreview() {
    val sampleEvent = MotivationalToastEvent(
        id = "preview_workout",
        xpEarned = 30,
        titleTr = "Harika Antrenman! +30 XP",
        titleEn = "Awesome Workout! +30 XP",
        subtitleTr = "Günlük hedefin %82 tamamlandı • Kalan 55 kcal",
        subtitleEn = "82% of daily burn goal done • 55 kcal to go",
        progressFraction = 0.82f,
        accentColorHex = 0xFFF97316
    )
    Column {
        HealthTrackTheme(darkTheme = true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B0F19))
                    .padding(bottom = 24.dp)
            ) {
                MotivationalToastHost(event = sampleEvent, onDismiss = {})
            }
        }
        HealthTrackTheme(darkTheme = false) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(bottom = 24.dp)
            ) {
                MotivationalToastHost(event = sampleEvent, onDismiss = {})
            }
        }
    }
}

@Preview
@Composable
fun WidgetLivePreviewDarkPreview() {
    val sample = WidgetTelemetrySnapshot.sample(languageCode = "tr", isDarkTheme = true)
    HealthTrackTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF091224))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WidgetLivePreviewMedium(snapshot = sample, onSimulateWaterTap = {})
            WidgetLivePreviewCompact(snapshot = sample, onSimulateWaterTap = {})
        }
    }
}

@Preview
@Composable
fun WidgetLivePreviewLightPreview() {
    val sample = WidgetTelemetrySnapshot.sample(languageCode = "tr", isDarkTheme = false)
    HealthTrackTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE2EBF7))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WidgetLivePreviewMedium(snapshot = sample, onSimulateWaterTap = {})
            WidgetLivePreviewCompact(snapshot = sample, onSimulateWaterTap = {})
        }
    }
}

@Preview
@Composable
fun WidgetPromoDialogDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        WidgetPromoDialog(
            snapshot = WidgetTelemetrySnapshot.sample(languageCode = "tr", isDarkTheme = true),
            supportsOneTapPinning = true,
            isIosPlatform = false,
            onAddWidgetClick = {},
            onDismissForToday = {},
            onDisableDailyReminder = {}
        )
    }
}

@Preview
@Composable
fun WidgetPromoDialogLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        WidgetPromoDialog(
            snapshot = WidgetTelemetrySnapshot.sample(languageCode = "en", isDarkTheme = false),
            supportsOneTapPinning = false,
            isIosPlatform = true,
            onAddWidgetClick = {},
            onDismissForToday = {},
            onDisableDailyReminder = {}
        )
    }
}

@Preview
@Composable
fun SportIconsDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        Row(
            modifier = Modifier
                .background(Color(0xFF0B0F19))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SportIcons.SELECTABLE_SPORTS.forEach { sport ->
                val accent = sport.accentColor()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = sport.iconVector(),
                        contentDescription = sport.titleTr,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SportIconsLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        Row(
            modifier = Modifier
                .background(Color(0xFFF8FAFC))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SportIcons.SELECTABLE_SPORTS.forEach { sport ->
                val accent = sport.accentColor()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accent.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = sport.iconVector(),
                        contentDescription = sport.titleTr,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun SportProgressPopupDarkPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
        ) {
            ConfettiCelebrationOverlay(
                event = CelebrationEvent(
                    id = "preview_tennis_progress",
                    type = CelebrationType.SPORT_PROGRESS,
                    titleTr = "Tenis Ustası • Sv. 2 İlerlemesi",
                    titleEn = "Tennis Ace • Lv. 2 Progress",
                    subtitleTr = "Seviye 2 (Gümüş Madalya) için 1/3 tamamlandı • 2 antrenman kaldı!",
                    subtitleEn = "1/3 completed toward Lv. 2 (Silver Medal) • 2 left!",
                    xpBonus = 30,
                    badgeId = BadgeId.SPORT_TENNIS,
                    badgeStageLevel = 1,
                    nextStageLevel = 2,
                    progressCurrent = 1,
                    progressTarget = 3,
                    previousProgressFraction = 0f,
                    targetProgressFraction = 1f / 3f,
                    showConfetti = false
                ),
                onDismiss = {}
            )
        }
    }
}

@Preview
@Composable
fun SportMedalUnlockedLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            ConfettiCelebrationOverlay(
                event = CelebrationEvent(
                    id = "preview_tennis_lv2",
                    type = CelebrationType.BADGE_UNLOCKED,
                    titleTr = "Yeni Madalya Kazandın! • Sv. 2",
                    titleEn = "Medal Upgraded! • Lv. 2",
                    subtitleTr = "Tenis Ustası Sv. 2 (Gümüş Madalya) açıldı! 3/3 tamamlandı.",
                    subtitleEn = "Tennis Ace Lv. 2 (Silver Medal) unlocked! 3/3 completed.",
                    xpBonus = 150,
                    badgeId = BadgeId.SPORT_TENNIS,
                    badgeStageLevel = 2,
                    nextStageLevel = 2,
                    progressCurrent = 3,
                    progressTarget = 3,
                    previousProgressFraction = 2f / 3f,
                    targetProgressFraction = 1f,
                    showConfetti = true
                ),
                onDismiss = {}
            )
        }
    }
}

@Preview
@Composable
fun HealthyMealCelebrationOverlayPreview() {
    HealthTrackTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F19))
        ) {
            ConfettiCelebrationOverlay(
                event = CelebrationEvent(
                    id = "preview_healthy_meal",
                    type = CelebrationType.HEALTHY_MEAL_LOGGED,
                    titleTr = "Tebrikler! Sağlıklı Öğün 🥗",
                    titleEn = "Congrats! Healthy Meal 🥗",
                    subtitleTr = "Muhteşem bir seçim! Yüksek protein ve temiz içerikle vücudunu en iyi şekilde besledin.",
                    subtitleEn = "Outstanding choice! You fueled your body with clean nutrients and strong protein.",
                    xpBonus = 25,
                    showConfetti = true,
                    autoDismissMs = 2000L,
                    healthScore = 92
                ),
                onDismiss = {}
            )
        }
    }
}

@Preview
@Composable
fun HealthyMealCelebrationOverlayLightPreview() {
    HealthTrackTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            ConfettiCelebrationOverlay(
                event = CelebrationEvent(
                    id = "preview_healthy_meal_light",
                    type = CelebrationType.HEALTHY_MEAL_LOGGED,
                    titleTr = "Tebrikler! Sağlıklı Öğün 🥗",
                    titleEn = "Congrats! Healthy Meal 🥗",
                    subtitleTr = "Muhteşem bir seçim! Yüksek protein ve temiz içerikle vücudunu en iyi şekilde besledin.",
                    subtitleEn = "Outstanding choice! You fueled your body with clean nutrients and strong protein.",
                    xpBonus = 25,
                    showConfetti = true,
                    autoDismissMs = 2000L,
                    healthScore = 92
                ),
                onDismiss = {}
            )
        }
    }
}
