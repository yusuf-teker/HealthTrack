package com.yusufteker.healthtrack.feature.gamification.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.*
import com.yusufteker.healthtrack.core.utils.formatWeekRangeTr
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.ShimmeringStreakPill
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamificationScreen(
    state: GamificationState,
    onIntent: (GamificationIntent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val isDark = LocalIsDarkTheme.current

    if (state.isFriendsSheetOpen) {
        FriendsManagementBottomSheet(
            state = state,
            onIntent = onIntent,
            onDismiss = { onIntent(GamificationIntent.OnSetFriendsSheetOpen(false)) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        GradientText(
                            text = str("HealthTrack Arena", "HealthTrack Arena"),
                            colors = HealthColors.GradientPrimary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                        Text(
                            text = str("Seri, Rozetler & Liderlik", "Streaks, Badges & Leaderboard"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.bounceClick()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = str("Geri", "Back")
                        )
                    }
                },
                actions = {
                    if (state.selectedSection == GamificationSection.LEADERBOARD) {
                        Box {
                            IconButton(
                                onClick = { onIntent(GamificationIntent.OnSetFriendsSheetOpen(true)) },
                                modifier = Modifier.bounceClick()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PersonAdd,
                                    contentDescription = str("Arkadaş Ekle", "Add Friends"),
                                    tint = HealthColors.PrimaryGlow
                                )
                            }
                            if (state.followRequests.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 8.dp, end = 8.dp)
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                )
                            }
                        }
                        IconButton(
                            onClick = { onIntent(GamificationIntent.OnRefreshLeaderboard) },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = str("Yenile", "Refresh")
                            )
                        }
                    } else {
                        ShimmeringStreakPill(
                            streakDays = state.summary.currentStreakDays,
                            isActiveToday = state.summary.isActiveToday,
                            onClick = {},
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .clearFocusOnTap()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Top Section Switcher (Rozetler & Seri vs Liderlik)
                SectionSegmentedControl(
                    selected = state.selectedSection,
                    pendingRequestsCount = state.followRequests.size,
                    onSelect = { onIntent(GamificationIntent.OnSectionSelected(it)) }
                )

                when (state.selectedSection) {
                    GamificationSection.OVERVIEW -> {
                        OverviewSection(state = state, onIntent = onIntent, isDark = isDark)
                    }
                    GamificationSection.LEADERBOARD -> {
                        LeaderboardSection(state = state, onIntent = onIntent, isDark = isDark)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionSegmentedControl(
    selected: GamificationSection,
    pendingRequestsCount: Int,
    onSelect: (GamificationSection) -> Unit
) {
    val lang = appLang()
    val isDark = LocalIsDarkTheme.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF131B2E) else Color(0xFFE8EEF5),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GamificationSection.entries.forEach { section ->
                val isSelected = section == selected
                val bg = if (isSelected) {
                    Brush.horizontalGradient(HealthColors.GradientPrimary)
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bg)
                        .bounceClick()
                        .clickable { onSelect(section) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (section == GamificationSection.OVERVIEW) {
                                Icons.Rounded.WorkspacePremium
                            } else {
                                Icons.Rounded.EmojiEvents
                            },
                            contentDescription = null,
                            tint = if (isSelected) HealthColors.OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = section.localizedTitle(lang),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                            ),
                            color = if (isSelected) HealthColors.OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (section == GamificationSection.LEADERBOARD && pendingRequestsCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEF4444)
                            ) {
                                Text(
                                    text = "$pendingRequestsCount",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// TAB 1: OVERVIEW (LEVEL, SHIMMERING STREAK, QUESTS, BADGES)
// ═════════════════════════════════════════════════════════════

@Composable
private fun OverviewSection(
    state: GamificationState,
    onIntent: (GamificationIntent) -> Unit,
    isDark: Boolean
) {
    val summary = state.summary
    val lang = appLang()
    val tierColor = Color(summary.tier.colorHex)

    val animatedXpProgress by animateFloatAsState(
        targetValue = summary.levelProgress,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "xp_progress"
    )
    val animatedBioScore by animateFloatAsState(
        targetValue = summary.todayScore / 100f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "bio_score"
    )

    // Hero Level & Streak Card
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Animated Bio-Score & Level Ring
                    Box(
                        modifier = Modifier.size(66.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 6.dp.toPx()
                            drawArc(
                                color = tierColor.copy(alpha = 0.16f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                            drawArc(
                                brush = Brush.sweepGradient(listOf(tierColor, HealthColors.PrimaryGlow, tierColor)),
                                startAngle = -90f,
                                sweepAngle = 360f * animatedBioScore.coerceAtLeast(0.04f),
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Lv.${summary.level}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                color = tierColor
                            )
                            Text(
                                text = "${summary.todayScore}p",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = tierColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, tierColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = summary.tier.localizedTitle(lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = tierColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${summary.totalXp} XP",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = str(
                                "Bugün +${summary.todayEarnedXp} XP • Bio-Skor ${summary.todayScore}/100",
                                "Today +${summary.todayEarnedXp} XP • Bio-Score ${summary.todayScore}/100"
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // XP Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = str("Sonraki Seviye: Lv.${summary.level + 1}", "Next Level: Lv.${summary.level + 1}"),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${summary.currentLevelXp} / ${summary.nextLevelXp} XP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = tierColor
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedXpProgress.coerceIn(0.02f, 1f))
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.horizontalGradient(listOf(tierColor, HealthColors.PrimaryGlow)))
                    )
                }
            }

            // Streak Highlight Strip
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) Color(0xFF151E32) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ShimmeringStreakPill(
                            streakDays = summary.currentStreakDays,
                            isActiveToday = summary.isActiveToday,
                            onClick = {}
                        )
                        Column {
                            Text(
                                text = str("Aktif Seri", "Current Streak"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = when {
                                    summary.isActiveToday -> str("Bugün seri ateşlendi!", "Streak ignited today!")
                                    summary.isRestDayProtected -> str("Dinlenme günü koruması aktif", "Rest day protection active")
                                    else -> str("Seriyi sürdürmek için kayıt gir", "Log today to keep your streak")
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = str("Rekor", "Best"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${summary.longestStreakDays} ${str("gün", "d")}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }
        }
    }

    // Daily Quests
    QuestsCard(
        title = str("Günlük Görevler", "Daily Quests"),
        subtitle = str("Tamamlandığında konfeti & ekstra disiplin skoru", "Complete all 3 for a daily celebration"),
        quests = summary.dailyQuests,
        accentColor = HealthColors.PrimaryGlow
    )

    // Weekly Quests
    QuestsCard(
        title = str("Haftalık Görevler", "Weekly Quests"),
        subtitle = str("Haftalık istikrar bonusları", "Weekly consistency XP bonuses"),
        quests = summary.weeklyQuests,
        accentColor = Color(0xFFA855F7)
    )

    // Achievements / Badges Section
    var inspectedAchievement by remember { mutableStateOf<GamificationAchievement?>(null) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = str("Başarı Rozetleri", "Achievement Badges"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = str(
                    "${summary.unlockedBadgesCount}/${summary.totalBadgesCount} rozet • ${summary.unlockedTiersCount}/${summary.totalTiersCount} seviye",
                    "${summary.unlockedBadgesCount}/${summary.totalBadgesCount} badges • ${summary.unlockedTiersCount}/${summary.totalTiersCount} tiers"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Badge category filter pills
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChipPill(
            label = str("Tümü", "All"),
            selected = state.selectedBadgeCategory == null,
            onClick = { onIntent(GamificationIntent.OnBadgeCategorySelected(null)) }
        )
        BadgeCategory.entries.forEach { cat ->
            FilterChipPill(
                label = cat.localizedTitle(lang),
                selected = state.selectedBadgeCategory == cat,
                onClick = { onIntent(GamificationIntent.OnBadgeCategorySelected(cat)) }
            )
        }
    }

    val filteredBadges = summary.achievements.filter {
        state.selectedBadgeCategory == null || it.badgeId.category == state.selectedBadgeCategory
    }

    // 2-column grid without nested LazyVerticalGrid
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        filteredBadges.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { ach ->
                    BadgeGridCard(
                        achievement = ach,
                        onClick = { inspectedAchievement = ach },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    inspectedAchievement?.let { activeAch ->
        BadgeStagesDetailSheet(
            achievement = activeAch,
            onDismiss = { inspectedAchievement = null }
        )
    }
}

@Composable
private fun QuestsCard(
    title: String,
    subtitle: String,
    quests: List<GamificationQuest>,
    accentColor: Color
) {
    val lang = appLang()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            quests.forEach { quest ->
                val animatedFraction by animateFloatAsState(
                    targetValue = quest.progressFraction,
                    animationSpec = tween(550),
                    label = "quest_${quest.id}"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (quest.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = if (quest.isCompleted) Color(0xFF10B981) else accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = quest.localizedTitle(lang),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (quest.isCompleted) FontWeight.Bold else FontWeight.Medium
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = (if (quest.isCompleted) Color(0xFF10B981) else accentColor).copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = if (quest.isCompleted) {
                                    "✓ +${quest.xpReward} XP"
                                } else {
                                    "+${quest.xpReward} XP"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (quest.isCompleted) Color(0xFF10B981) else accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(if (quest.isCompleted) Color(0xFF10B981) else accentColor)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun badgeIconFor(badgeId: BadgeId): ImageVector = when (badgeId) {
    BadgeId.FIRST_LOG -> Icons.Rounded.AutoAwesome
    BadgeId.STREAK_MASTER -> Icons.Rounded.LocalFireDepartment
    BadgeId.BIO_SCORE_ELITE -> Icons.Rounded.WorkspacePremium
    BadgeId.WEEKEND_WARRIOR -> Icons.Rounded.Shield
    BadgeId.PROTEIN_PRO -> Icons.Rounded.Restaurant
    BadgeId.CALORIE_SNIPER -> Icons.Rounded.EmojiEvents
    BadgeId.FIBER_CHAMPION -> Icons.Rounded.AutoAwesome
    BadgeId.HYDRATION_HERO -> Icons.Rounded.WaterDrop
    BadgeId.WORKOUT_DISCIPLINE -> Icons.AutoMirrored.Rounded.DirectionsRun
    BadgeId.CALORIE_ENGINE -> Icons.Rounded.LocalFireDepartment
    BadgeId.ENDURANCE_MINUTES -> Icons.Rounded.Timer
    BadgeId.SCALE_TRACKER -> Icons.Rounded.MonitorWeight
}

@Composable
private fun BadgeGridCard(
    achievement: GamificationAchievement,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val badge = achievement.badgeId
    val accent = Color(badge.accentColorHex)
    val unlocked = achievement.isUnlocked
    val isMaxed = achievement.isMaxed
    val rank = badge.rankForLevel(achievement.currentLevel)
    val rankColor = Color(rank.accentHex)
    val icon = badgeIconFor(badge)

    GlassCard(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .bounceClick()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        borderGradient = when {
            isMaxed -> listOf(rankColor.copy(alpha = 0.8f), accent.copy(alpha = 0.55f))
            unlocked -> listOf(accent.copy(alpha = 0.55f), rankColor.copy(alpha = 0.35f))
            else -> null
        },
        borderWidth = when {
            isMaxed -> 1.6.dp
            unlocked -> 1.3.dp
            else -> 1.dp
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (unlocked) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        if (unlocked) rankColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (achievement.maxLevel > 1) {
                    val stageBadgeBg = if (unlocked) rankColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    val stageBadgeFg = if (unlocked) rankColor else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = stageBadgeBg,
                        border = if (unlocked) BorderStroke(0.8.dp, rankColor.copy(alpha = 0.45f)) else null
                    ) {
                        Text(
                            text = if (isMaxed) {
                                str("MAKS ${achievement.currentLevel}/${achievement.maxLevel}", "MAX ${achievement.currentLevel}/${achievement.maxLevel}")
                            } else {
                                str("Sv.${achievement.currentLevel}/${achievement.maxLevel}", "Lv.${achievement.currentLevel}/${achievement.maxLevel}")
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = stageBadgeFg,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else if (unlocked) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = "+${achievement.earnedXp} XP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp
                            ),
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = badge.localizedTitle(lang),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = badge.localizedDescription(lang),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Mini stage pips for tiered badges
            if (achievement.maxLevel > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (lvl in 1..achievement.maxLevel) {
                        val filled = lvl <= achievement.currentLevel
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (filled) rankColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                )
                        )
                    }
                }
            }

            // Progress bar to next stage (or maxed XP summary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMaxed) {
                            str("Tamamlandı • +${achievement.earnedXp} XP", "Maxed • +${achievement.earnedXp} XP")
                        } else {
                            "${achievement.progressCurrent}/${achievement.progressTarget} ${badge.localizedUnit(lang)}"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                        color = if (isMaxed) rankColor else if (unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isMaxed) {
                        Text(
                            text = "+${achievement.nextStageXpReward} XP",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = accent.copy(alpha = 0.9f)
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(achievement.progressFraction.coerceIn(0f, 1f))
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isMaxed) rankColor else if (unlocked) accent else accent.copy(alpha = 0.55f))
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeStagesDetailSheet(
    achievement: GamificationAchievement,
    onDismiss: () -> Unit
) {
    val lang = appLang()
    val badge = achievement.badgeId
    val accent = Color(badge.accentColorHex)
    val rank = badge.rankForLevel(achievement.currentLevel)
    val rankColor = Color(rank.accentHex)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.16f),
                    border = BorderStroke(1.2.dp, rankColor.copy(alpha = 0.65f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = badgeIconFor(badge),
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = badge.localizedTitle(lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "${badge.category.localizedTitle(lang)} • ${rank.localizedLabel(lang)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = rankColor
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "+${achievement.earnedXp} XP",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = badge.localizedDescription(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = str("Mevcut Toplam İlerleme", "Total Current Progress"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${achievement.rawProgress} ${badge.localizedUnit(lang)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = accent
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                badge.stages.forEach { stage ->
                    val unlockedStage = achievement.currentLevel >= stage.level
                    val isNextTarget = !achievement.isMaxed && stage.level == achievement.currentLevel + 1
                    val stageRankColor = Color(badge.rankForLevel(stage.level).accentHex)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            unlockedStage -> stageRankColor.copy(alpha = 0.12f)
                            isNextTarget -> accent.copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                unlockedStage -> stageRankColor.copy(alpha = 0.45f)
                                isNextTarget -> accent.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (unlockedStage) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = if (unlockedStage) stageRankColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = if (badge.maxLevel > 1) {
                                            str("Seviye ${stage.level}", "Level ${stage.level}")
                                        } else {
                                            str("Başlangıç Rozeti", "Starter Badge")
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (unlockedStage || isNextTarget) FontWeight.Bold else FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        text = "${stage.targetCount} ${badge.localizedUnit(lang)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = (if (unlockedStage) stageRankColor else accent).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (unlockedStage) "✓ +${stage.xpReward} XP" else "+${stage.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (unlockedStage) stageRankColor else accent,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(str("Kapat", "Close"))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════
// TAB 2: LEADERBOARD (GLOBAL & FRIENDS, DAILY & WEEKLY)
// ═════════════════════════════════════════════════════════════

@Composable
private fun LeaderboardSection(
    state: GamificationState,
    onIntent: (GamificationIntent) -> Unit,
    isDark: Boolean
) {
    val lang = appLang()

    // Scope & Period Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Global vs Friends
        LeaderboardScope.entries.forEach { scopeItem ->
            val selected = state.scope == scopeItem
            val icon = if (scopeItem == LeaderboardScope.GLOBAL) Icons.Rounded.Public else Icons.Rounded.Group
            FilterChipPill(
                label = if (scopeItem == LeaderboardScope.FRIENDS && state.leaderboard != null) {
                    "${scopeItem.localizedTitle(lang)} (${state.leaderboard.friendsCount})"
                } else {
                    scopeItem.localizedTitle(lang)
                },
                icon = icon,
                selected = selected,
                onClick = { onIntent(GamificationIntent.OnScopeSelected(scopeItem)) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Daily vs Weekly
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (isDark) Color(0xFF131B2E) else Color(0xFFE8EEF5),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        ) {
            Row(modifier = Modifier.padding(3.dp)) {
                LeaderboardPeriod.entries.forEach { periodItem ->
                    val selected = state.period == periodItem
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { onIntent(GamificationIntent.OnPeriodSelected(periodItem)) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = periodItem.localizedTitle(lang),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Category Horizontal Chips
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LeaderboardCategory.entries.forEach { cat ->
            val icon = when (cat) {
                LeaderboardCategory.DISCIPLINE_XP -> Icons.Rounded.Shield
                LeaderboardCategory.CALORIES_BURNED -> Icons.Rounded.LocalFireDepartment
                LeaderboardCategory.HEALTHY_EATING -> Icons.Rounded.Restaurant
                LeaderboardCategory.WORKOUT_MINUTES -> Icons.Rounded.Timer
            }
            FilterChipPill(
                label = cat.localizedTitle(lang),
                icon = icon,
                selected = state.category == cat,
                onClick = { onIntent(GamificationIntent.OnCategorySelected(cat)) }
            )
        }
    }

    // Pending Friend Requests Banner
    AnimatedVisibility(visible = state.followRequests.isNotEmpty()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            onClick = { onIntent(GamificationIntent.OnSetFriendsSheetOpen(true)) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint = Color(0xFFFB923C)
                    )
                    Text(
                        text = str(
                            "${state.followRequests.size} bekleyen arkadaşlık isteğin var",
                            "You have ${state.followRequests.size} pending friend request(s)"
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = str("Görüntüle", "View"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = HealthColors.PrimaryGlow
                )
            }
        }
    }

    // Loading / Error / Data States
    when {
        state.isLoadingLeaderboard && state.leaderboard == null -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = HealthColors.PrimaryGlow)
            }
        }
        state.leaderboardError != null && state.leaderboard == null -> {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = state.leaderboardError,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { onIntent(GamificationIntent.OnRefreshLeaderboard) },
                        modifier = Modifier.bounceClick()
                    ) {
                        Text(str("Tekrar Dene", "Try Again"))
                    }
                }
            }
        }
        else -> {
            val data = state.leaderboard
            if (data != null) {
                // Date range & info pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.period == LeaderboardPeriod.WEEKLY) {
                            formatWeekRangeTr(data.startDate, data.endDate, lang)
                        } else {
                            str("Bugün • ${data.startDate}", "Today • ${data.startDate}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = str("${data.totalParticipants} katılımcı", "${data.totalParticipants} participants"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Friends Empty Prompt
                if (state.scope == LeaderboardScope.FRIENDS && data.friendsCount == 0) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Group,
                                contentDescription = null,
                                tint = HealthColors.PrimaryGlow,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = str("Henüz arkadaş eklemedin", "No friends added yet"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = str(
                                    "Arkadaşlarını takip ederek günlük ve haftalık sağlık yarışına katıl!",
                                    "Follow friends to compete on daily and weekly health rankings!"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { onIntent(GamificationIntent.OnSetFriendsSheetOpen(true)) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.bounceClick()
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(str("Arkadaş Bul & Ekle", "Find & Add Friends"))
                            }
                        }
                    }
                }

                if (data.entries.isNotEmpty()) {
                    // My Rank Banner (if user exists)
                    data.myEntry?.let { me ->
                        MyRankSummaryBanner(entry = me)
                    }

                    // Top 3 Podium (show when at least 2 entries exist, or even 1)
                    val topThree = data.entries.take(3)
                    LeaderboardPodium(entries = topThree, isDark = isDark)

                    // Ranks #4+
                    val remaining = data.entries.drop(3)
                    if (remaining.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            remaining.forEach { entry ->
                                LeaderboardRowCard(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyRankSummaryBanner(entry: LeaderboardEntryDto) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        borderGradient = listOf(HealthColors.PrimaryGlow.copy(alpha = 0.55f), HealthColors.Primary.copy(alpha = 0.25f)),
        borderWidth = 1.4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = HealthColors.PrimaryGlow.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, HealthColors.PrimaryGlow.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "#${entry.rank}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = HealthColors.PrimaryGlow,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
                Column {
                    Text(
                        text = str("Senin Sıran", "Your Rank"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entry.primaryValueLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "🔥 ${entry.streakDays} ${str("gün seri", "d streak")}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFB923C)
                )
                Text(
                    text = entry.secondaryLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LeaderboardPodium(
    entries: List<LeaderboardEntryDto>,
    isDark: Boolean
) {
    val first = entries.getOrNull(0)
    val second = entries.getOrNull(1)
    val third = entries.getOrNull(2)

    val infiniteTransition = rememberInfiniteTransition(label = "podium_crown")
    val crownGlow by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_glow"
    )

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (second != null) {
                PodiumSlot(
                    entry = second,
                    medalColor = Color(0xFFCBD5E1),
                    podiumHeight = 76.dp,
                    glowAlpha = 0.5f,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (first != null) {
                PodiumSlot(
                    entry = first,
                    medalColor = Color(0xFFFACC15),
                    podiumHeight = 100.dp,
                    glowAlpha = crownGlow,
                    isDark = isDark,
                    isChampion = true,
                    modifier = Modifier.weight(1.15f)
                )
            }

            if (third != null) {
                PodiumSlot(
                    entry = third,
                    medalColor = Color(0xFFD97706),
                    podiumHeight = 62.dp,
                    glowAlpha = 0.45f,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PodiumSlot(
    entry: LeaderboardEntryDto,
    medalColor: Color,
    podiumHeight: androidx.compose.ui.unit.Dp,
    glowAlpha: Float,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    isChampion: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isChampion) {
            Icon(
                imageVector = Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = medalColor.copy(alpha = glowAlpha),
                modifier = Modifier.size(24.dp)
            )
        }

        // Avatar circle
        Box(
            modifier = Modifier
                .size(if (isChampion) 56.dp else 46.dp)
                .clip(CircleShape)
                .background(medalColor.copy(alpha = 0.18f))
                .border(2.dp, medalColor.copy(alpha = glowAlpha), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.name.take(1).uppercase().ifBlank { "?" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = medalColor
            )
        }

        Text(
            text = if (entry.isCurrentUser) str("Sen", "You") else entry.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        Text(
            text = entry.primaryValueLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = medalColor,
            maxLines = 1
        )

        // Podium pedestal
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            medalColor.copy(alpha = if (isDark) 0.28f else 0.22f),
                            medalColor.copy(alpha = 0.06f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = medalColor.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = medalColor
                )
                if (entry.streakDays > 0) {
                    Text(
                        text = "🔥${entry.streakDays}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRowCard(entry: LeaderboardEntryDto) {
    val highlight = entry.isCurrentUser
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        borderGradient = if (highlight) listOf(HealthColors.PrimaryGlow.copy(alpha = 0.55f), HealthColors.Primary.copy(alpha = 0.22f)) else null,
        borderWidth = if (highlight) 1.4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (highlight) HealthColors.PrimaryGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(30.dp)
                )

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            (if (highlight) HealthColors.PrimaryGlow else MaterialTheme.colorScheme.primary)
                                .copy(alpha = 0.16f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.name.take(1).uppercase().ifBlank { "?" },
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (highlight) HealthColors.PrimaryGlow else MaterialTheme.colorScheme.primary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (highlight) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = HealthColors.PrimaryGlow.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = str("Sen", "You"),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                    color = HealthColors.PrimaryGlow,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "@${entry.username} • ${entry.secondaryLabel}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.primaryValueLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (highlight) HealthColors.PrimaryGlow else MaterialTheme.colorScheme.onSurface
                )
                if (entry.streakDays > 0) {
                    Text(
                        text = "🔥 ${entry.streakDays} ${str("g", "d")}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color(0xFFFB923C)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════
// FRIENDS SEARCH & FOLLOW REQUESTS BOTTOM SHEET
// ═════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FriendsManagementBottomSheet(
    state: GamificationState,
    onIntent: (GamificationIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .clearFocusOnTap()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = str("Arkadaşlar & İstekler", "Friends & Requests"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = str(
                            "Kullanıcı adı veya isimle ara, arkadaş listene ekle",
                            "Search by username or name to add friends"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = str("Kapat", "Close"))
                }
            }

            OutlinedTextField(
                value = state.friendSearchQuery,
                onValueChange = { onIntent(GamificationIntent.OnFriendSearchQueryChanged(it)) },
                placeholder = { Text(str("Kullanıcı ara (en az 2 harf)...", "Search users (min 2 chars)...")) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.friendSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { onIntent(GamificationIntent.OnFriendSearchQueryChanged("")) }) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HealthColors.PrimaryGlow,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Incoming Follow Requests
            if (state.followRequests.isNotEmpty()) {
                Text(
                    text = str("Gelen İstekler (${state.followRequests.size})", "Incoming Requests (${state.followRequests.size})"),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFB923C)
                )
                state.followRequests.forEach { req ->
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = req.requesterName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "@${req.requesterUsername}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { onIntent(GamificationIntent.OnRejectFollowRequest(req.id)) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(str("Reddet", "Decline"), fontSize = 12.sp)
                                }
                                Button(
                                    onClick = { onIntent(GamificationIntent.OnAcceptFollowRequest(req.id)) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(str("Kabul Et", "Accept"), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            }

            // Search Results
            if (state.isSearchingFriends) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(26.dp), color = HealthColors.PrimaryGlow)
                }
            } else if (state.friendSearchQuery.trim().length >= 2 && state.searchResults.isEmpty()) {
                Text(
                    text = str("Eşleşen kullanıcı bulunamadı.", "No matching users found."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                state.searchResults.forEach { user ->
                    val isBusy = state.pendingActionUserIds.contains(user.id)
                    val buttonLabel = when {
                        user.isFollowedByMe -> str("Takip Ediliyor", "Following")
                        user.followRequestStatus == "PENDING" -> str("İstek Gönderildi", "Requested")
                        else -> str("Takip Et", "Follow")
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (user.isFollowedByMe || user.followRequestStatus == "PENDING") {
                                OutlinedButton(
                                    onClick = { if (!isBusy) onIntent(GamificationIntent.OnToggleFollowUser(user.id)) },
                                    enabled = !isBusy,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(buttonLabel, fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { if (!isBusy) onIntent(GamificationIntent.OnToggleFollowUser(user.id)) },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HealthColors.PrimaryGlow,
                                        contentColor = Color(0xFF06121E)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    val isDark = LocalIsDarkTheme.current
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) {
            HealthColors.PrimaryGlow.copy(alpha = if (isDark) 0.22f else 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) HealthColors.PrimaryGlow else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .bounceClick()
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) HealthColors.PrimaryGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) HealthColors.PrimaryGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

