package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.FoodItem
import com.yusufteker.healthtrack.core.model.MealHealthEvaluation
import com.yusufteker.healthtrack.core.model.MealHealthEvaluator
import com.yusufteker.healthtrack.core.model.MealHealthTier
import com.yusufteker.healthtrack.core.network.FoodAiResultDto
import com.yusufteker.healthtrack.feature.food.presentation.FoodScanTarget
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.LocalSpotlightController
import com.yusufteker.healthtrack.ui.components.SpotlightInlineGuideBubble
import com.yusufteker.healthtrack.ui.components.SpotlightTargetId
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodAiReviewBottomSheet(
    aiResult: FoodAiResultDto,
    target: FoodScanTarget = FoodScanTarget.DAILY_MEAL,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onSaveToInventory: (() -> Unit)? = null,
    onSaveSingleItemToInventory: ((FoodItem) -> Unit)? = null
) {
    val spotlightController = LocalSpotlightController.current
    val isOnboardingGuide = spotlightController?.activeTargetId == SpotlightTargetId.FOOD_REVIEW_CONFIRM &&
        target == FoodScanTarget.DAILY_MEAL
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            !isOnboardingGuide || targetValue != SheetValue.Hidden
        }
    )
    val isDark = LocalIsDarkTheme.current
    val scrollState = rememberScrollState()

    var isConfirming by remember { mutableStateOf(false) }
    var isSavingInventory by remember { mutableStateOf(false) }

    val totalProtein = aiResult.items.sumOf { it.proteinG }
    val totalCarbs = aiResult.items.sumOf { it.carbsG }
    val totalFat = aiResult.items.sumOf { it.fatG }

    ModalBottomSheet(
        onDismissRequest = { if (!isOnboardingGuide) onDismiss() },
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        containerColor = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface,
        dragHandle = if (isOnboardingGuide) null else {
            { BottomSheetDefaults.DragHandle() }
        },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !isOnboardingGuide),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = if (isOnboardingGuide) 18.dp else 0.dp)
        ) {
            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .then(
                        if (isOnboardingGuide) {
                            Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        } else {
                            Modifier
                        }
                    )
                    .verticalScroll(scrollState, enabled = !isOnboardingGuide)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sheet Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (target == FoodScanTarget.INVENTORY) Icons.Default.Inventory2 else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (target == FoodScanTarget.INVENTORY) {
                                    str("AI Envanter & Ürün Analizi 🏷️", "AI Inventory & Product Analysis 🏷️")
                                } else {
                                    str("AI Besin Analizi ✨", "AI Nutrition Analysis ✨")
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (target == FoodScanTarget.INVENTORY) {
                                    str(
                                        "Tespit edilen ürünleri inceleyip envanterinize kaydedin",
                                        "Review detected items and save them to your inventory"
                                    )
                                } else {
                                    str(
                                        "Tespit edilen besinleri inceleyip onaylayın",
                                        "Review and confirm detected foods"
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).bounceClick()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = str("Kapat", "Close"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // AI Meal Health Score & Encouraging Quote Banner
                if (target == FoodScanTarget.DAILY_MEAL) {
                    val healthEvaluation = remember(aiResult) {
                        MealHealthEvaluator.evaluate(
                            items = aiResult.items,
                            totalCalories = aiResult.totalCalories,
                            summary = aiResult.summary
                        )
                    }
                    MealHealthScoreBanner(evaluation = healthEvaluation)
                }

                // AI Summary Banner
                if (aiResult.summary.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HealthColors.Primary.copy(alpha = 0.08f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = aiResult.summary,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Total Calories & Macros Overview Card
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = str("Toplam Kalori", "Total Calories"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${aiResult.totalCalories} kcal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MacroBadge(label = "P", value = "${totalProtein.toInt()}g", color = Color(0xFF38BDF8))
                                MacroBadge(label = str("K", "C"), value = "${totalCarbs.toInt()}g", color = Color(0xFFFBBF24))
                                MacroBadge(label = str("Y", "F"), value = "${totalFat.toInt()}g", color = Color(0xFFF87171))
                            }
                        }
                    }
                }

                // Items Section Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = str("Ayrıştırılan Öğeler (${aiResult.items.size})", "Parsed Items (${aiResult.items.size})"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Food Items List
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    aiResult.items.forEach { item ->
                        ReviewFoodItemCard(
                            item = item,
                            onSaveToInventory = onSaveSingleItemToInventory?.let { action ->
                                { action(item) }
                            }
                        )
                    }
                }

                // Disclaimer
                if (aiResult.disclaimer.isNotBlank()) {
                    Text(
                        text = "ℹ️ ${aiResult.disclaimer}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // Fixed Sticky Footer for Action Buttons (always visible & easily clickable)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (target == FoodScanTarget.INVENTORY) {
                        // Primary: Envanterime Kaydet
                        Button(
                            onClick = {
                                if (!isSavingInventory && !isConfirming) {
                                    isSavingInventory = true
                                    if (onSaveToInventory != null) onSaveToInventory() else onConfirm()
                                }
                            },
                            enabled = !isSavingInventory && !isConfirming,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HealthColors.Primary)
                        ) {
                            if (isSavingInventory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = str("Kaydediliyor...", "Saving..."),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (aiResult.items.size > 1) {
                                        str(
                                            "Tümünü Envanterime Kaydet (${aiResult.items.size})",
                                            "Save All to My Inventory (${aiResult.items.size})"
                                        )
                                    } else {
                                        str("Ürünü Envanterime Kaydet", "Save Product to Inventory")
                                    },
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Secondary: Günlüğe de Ekle & Vazgeç
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isSavingInventory && !isConfirming,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(str("Vazgeç", "Cancel"), color = MaterialTheme.colorScheme.onSurface)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isConfirming && !isSavingInventory) {
                                        isConfirming = true
                                        onConfirm()
                                    }
                                },
                                enabled = !isConfirming && !isSavingInventory,
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(46.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.5f))
                            ) {
                                if (isConfirming) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = HealthColors.Primary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(str("Ekleniyor...", "Adding..."), color = HealthColors.Primary, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription = null,
                                        tint = HealthColors.Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(str("Günlüğe de Ekle", "Also Add to Log"), color = HealthColors.Primary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    } else {
                        if (isOnboardingGuide) {
                            SpotlightInlineGuideBubble(
                                badge = str("3/4 • AI ÖZET PANELİ", "3/4 • AI REVIEW SHEET"),
                                title = str("Besinleri Kontrol Et ve Onayla", "Review & Confirm Foods"),
                                description = str(
                                    "Gemini her yiyeceği kalori ve makrolarıyla ayırdı. Şimdi 'Onayla ve Günlüğe Ekle'ye dokun ve kalori barının değiştiğini gör!",
                                    "Gemini split each item with calories and macros. Tap 'Confirm & Add to Log' below to watch your calorie bars update!"
                                ),
                                pointingDown = true
                            )
                        }

                        // Primary: Onayla ve Günlüğe Ekle
                        Button(
                            onClick = {
                                if (!isConfirming && !isSavingInventory) {
                                    isConfirming = true
                                    onConfirm()
                                }
                            },
                            enabled = !isConfirming && !isSavingInventory,
                            border = if (isOnboardingGuide) {
                                androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.85f))
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HealthColors.Primary)
                        ) {
                            if (isConfirming) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = str("Günlüğe Ekleniyor...", "Adding to Log..."),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = str("Onayla ve Günlüğe Ekle", "Confirm & Add to Log"),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Secondary: Vazgeç & Envantere de Ekle (hidden during guided onboarding)
                        if (!isOnboardingGuide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                enabled = !isConfirming && !isSavingInventory,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .bounceClick(),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Text(str("Vazgeç", "Cancel"), color = MaterialTheme.colorScheme.onSurface)
                            }

                            if (onSaveToInventory != null) {
                                OutlinedButton(
                                    onClick = {
                                        if (!isSavingInventory && !isConfirming) {
                                            isSavingInventory = true
                                            onSaveToInventory()
                                        }
                                    },
                                    enabled = !isSavingInventory && !isConfirming,
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .height(46.dp)
                                        .bounceClick(),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.5f))
                                ) {
                                    if (isSavingInventory) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = HealthColors.Primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(str("Kaydediliyor...", "Saving..."), color = HealthColors.Primary, fontWeight = FontWeight.SemiBold)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.BookmarkAdd,
                                            contentDescription = null,
                                            tint = HealthColors.Primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(str("Envantere de Ekle", "Also Add to Inventory"), color = HealthColors.Primary, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealHealthScoreBanner(
    evaluation: MealHealthEvaluation,
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val isHealthy = evaluation.isHealthyCelebrationWorthy

    val primaryAccent = when (evaluation.tier) {
        MealHealthTier.EXCELLENT -> Color(0xFF10B981)
        MealHealthTier.BALANCED -> Color(0xFFF59E0B)
        MealHealthTier.FLEXIBLE -> Color(0xFFFB7185)
    }
    val secondaryAccent = when (evaluation.tier) {
        MealHealthTier.EXCELLENT -> Color(0xFFFACC15)
        MealHealthTier.BALANCED -> Color(0xFF38BDF8)
        MealHealthTier.FLEXIBLE -> Color(0xFFF97316)
    }

    val scoreProgress = remember(evaluation.score) { Animatable(0f) }
    val cardScale = remember(evaluation.score) { Animatable(if (isHealthy) 0.90f else 0.97f) }

    LaunchedEffect(evaluation.score) {
        scoreProgress.snapTo(0f)
        scoreProgress.animateTo(
            targetValue = (evaluation.score / 100f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(evaluation.score, isHealthy) {
        if (isHealthy) {
            cardScale.snapTo(0.90f)
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        } else {
            cardScale.snapTo(1f)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "meal_health_sparkle")
    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = if (isHealthy) 0.95f else 1f,
        targetValue = if (isHealthy) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_pulse"
    )

    val displayedScore = (scoreProgress.value * 100f).roundToInt().coerceIn(0, 100)
    val highlights = evaluation.localizedHighlights(lang)

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale.value
                scaleY = cardScale.value
            },
        shape = RoundedCornerShape(18.dp),
        borderWidth = if (isHealthy) 1.5.dp else 1.dp,
        borderGradient = listOf(
            primaryAccent.copy(alpha = if (isHealthy) 0.72f else 0.42f),
            secondaryAccent.copy(alpha = if (isHealthy) 0.48f else 0.25f)
        ),
        backgroundColor = primaryAccent.copy(alpha = if (isHealthy) 0.11f else 0.06f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Animated Score Ring
            Box(
                modifier = Modifier.size(66.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(
                        color = primaryAccent.copy(alpha = 0.18f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(primaryAccent, secondaryAccent, primaryAccent)
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * scoreProgress.value,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$displayedScore",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryAccent,
                        lineHeight = 20.sp
                    )
                    Text(
                        text = "/100",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 11.sp
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = primaryAccent.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = primaryAccent,
                                modifier = Modifier
                                    .size(13.dp)
                                    .graphicsLayer {
                                        scaleX = sparkleScale
                                        scaleY = sparkleScale
                                    }
                            )
                            Text(
                                text = evaluation.localizedBadgeTitle(lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = primaryAccent
                            )
                        }
                    }
                }

                Text(
                    text = evaluation.localizedQuote(lang),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (highlights.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        highlights.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                border = BorderStroke(0.8.dp, primaryAccent.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroBadge(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReviewFoodItemCard(
    item: FoodItem,
    onSaveToInventory: (() -> Unit)? = null
) {
    var isSaved by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.formattedPortion,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${item.calories} kcal",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.Primary
                )
            }

            // Macros row + Single item inventory save button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Protein: ${item.proteinG.toInt()}g",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = str("Karb: ${item.carbsG.toInt()}g", "Carbs: ${item.carbsG.toInt()}g"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = str("Yağ: ${item.fatG.toInt()}g", "Fat: ${item.fatG.toInt()}g"),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (onSaveToInventory != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSaved) HealthColors.Primary.copy(alpha = 0.25f) else HealthColors.Primary.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = !isSaved, onClick = {
                                isSaved = true
                                onSaveToInventory()
                            })
                            .bounceClick()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Check else Icons.Default.BookmarkAdd,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isSaved) str("Eklendi ✓", "Added ✓") else str("Envantere Ekle", "Add to Inventory"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HealthColors.Primary
                            )
                        }
                    }
                }
            }

            // Micronutrients if present
            if (item.micronutrients.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item.micronutrients.take(3).forEach { micro ->
                        Text(
                            text = "${micro.name}: ${micro.amount}${micro.unit}",
                            fontSize = 10.sp,
                            color = HealthColors.Secondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(HealthColors.Secondary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
