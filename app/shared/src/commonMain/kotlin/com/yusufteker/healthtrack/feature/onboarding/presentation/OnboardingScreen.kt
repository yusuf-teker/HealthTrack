package com.yusufteker.healthtrack.feature.onboarding.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.i18n.AppLanguage
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.feature.analytics.presentation.AnalyticsScreen
import com.yusufteker.healthtrack.feature.food.presentation.FoodScreen
import com.yusufteker.healthtrack.feature.weight.presentation.WeightScreen
import com.yusufteker.healthtrack.feature.workout.presentation.WorkoutScreen
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.HealthBrandLogo
import com.yusufteker.healthtrack.ui.components.LanguageSelectorPill
import com.yusufteker.healthtrack.ui.components.LocalSpotlightController
import com.yusufteker.healthtrack.ui.components.SpotlightBubbleSpec
import com.yusufteker.healthtrack.ui.components.SpotlightOverlay
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import com.yusufteker.healthtrack.ui.components.rememberSpotlightController

@Composable
fun OnboardingScreen(
    state: OnboardingState,
    onIntent: (OnboardingIntent) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val lang = appLang()
    val spotlightController = rememberSpotlightController()

    LaunchedEffect(state.microStep) {
        spotlightController.activeTargetId = state.microStep.targetId
    }

    if (state.currentStep == OnboardingStep.READY_TO_START) {
        OnboardingReadyView(
            state = state,
            lang = lang,
            onLanguageSelected = onLanguageSelected,
            onIntent = onIntent,
            modifier = modifier
        )
        return
    }

    val isPopupActive = state.foodState.isCalculatingAi ||
        state.foodState.isAiReviewSheetOpen ||
        state.weightState.isAddDialogOpen

    CompositionLocalProvider(LocalSpotlightController provides spotlightController) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                OnboardingTopBar(
                    currentStep = state.currentStep,
                    lang = lang,
                    onGoToStep = { onIntent(OnboardingIntent.OnGoToStep(it)) },
                    onLanguageSelected = onLanguageSelected,
                    onSkip = { onIntent(OnboardingIntent.OnSkip) }
                )
            },
            bottomBar = {
                val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
                AnimatedVisibility(
                    visible = imeBottom == 0 && !state.foodState.isAiReviewSheetOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OnboardingBottomNavBar(
                        currentStep = state.currentStep,
                        lang = lang,
                        onSelectStep = { onIntent(OnboardingIntent.OnGoToStep(it)) }
                    )
                }
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
                // Full-size Real Feature Screens
                AnimatedContent(
                    targetState = state.currentStep,
                    transitionSpec = {
                        if (targetState.ordinal >= initialState.ordinal) {
                            (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { -it / 4 } + fadeOut())
                        } else {
                            (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                                (slideOutHorizontally { it / 4 } + fadeOut())
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { step ->
                    when (step) {
                        OnboardingStep.FOOD_AI_DEMO -> FoodScreen(
                            state = state.foodState,
                            onIntent = { onIntent(OnboardingIntent.OnFoodIntent(it)) },
                            onNavigateToWeight = {
                                onIntent(OnboardingIntent.OnGoToStep(OnboardingStep.WEIGHT_TREND_DEMO))
                            }
                        )
                        OnboardingStep.WORKOUT_SYNC_DEMO -> WorkoutScreen(
                            state = state.workoutState,
                            onIntent = { onIntent(OnboardingIntent.OnWorkoutIntent(it)) }
                        )
                        OnboardingStep.WEIGHT_TREND_DEMO -> WeightScreen(
                            state = state.weightState,
                            onIntent = { onIntent(OnboardingIntent.OnWeightIntent(it)) }
                        )
                        OnboardingStep.ANALYTICS_AI_DEMO -> AnalyticsScreen(
                            state = state.analyticsState,
                            onIntent = { onIntent(OnboardingIntent.OnAnalyticsIntent(it)) }
                        )
                        OnboardingStep.READY_TO_START -> Unit
                    }
                }

                // Floating Spotlight Cutout + Coachmark Bubble Overlay
                if (!isPopupActive) {
                    val bubbleSpec = buildBubbleSpec(state = state, lang = lang)
                    SpotlightOverlay(
                        controller = spotlightController,
                        targetId = state.microStep.targetId,
                        preferTopScroll = state.microStep.preferTopScroll,
                        bubbleSpec = bubbleSpec,
                        onPrimaryAction = { onIntent(OnboardingIntent.OnPrimaryBubbleAction) },
                        onSecondarySkip = { onIntent(OnboardingIntent.OnNextStep) },
                        onBack = { onIntent(OnboardingIntent.OnPreviousStep) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingTopBar(
    currentStep: OnboardingStep,
    lang: AppLanguage,
    onGoToStep: (OnboardingStep) -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HealthBrandLogo(size = 32.dp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OnboardingStep.entries.forEach { step ->
                            val isCurrent = step == currentStep
                            val isPast = step.ordinal < currentStep.ordinal
                            val pillWidth by animateDpAsState(targetValue = if (isCurrent) 24.dp else 8.dp)
                            val pillColor by animateColorAsState(
                                targetValue = when {
                                    isCurrent -> MaterialTheme.colorScheme.primary
                                    isPast -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .height(6.dp)
                                    .width(pillWidth)
                                    .clip(CircleShape)
                                    .background(pillColor)
                                    .clickable { onGoToStep(step) }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LanguageSelectorPill(
                        selectedLanguage = lang,
                        onLanguageSelected = onLanguageSelected
                    )
                    if (currentStep != OnboardingStep.READY_TO_START) {
                        TextButton(
                            onClick = onSkip,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = str("Atla", "Skip"),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )
        }
    }
}

@Composable
private fun OnboardingBottomNavBar(
    currentStep: OnboardingStep,
    lang: AppLanguage,
    onSelectStep: (OnboardingStep) -> Unit
) {
    Column {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
            thickness = 0.5.dp
        )
        val bottomInsetPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            windowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
            modifier = Modifier.height(56.dp + bottomInsetPadding)
        ) {
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
            NavigationBarItem(
                selected = currentStep == OnboardingStep.FOOD_AI_DEMO,
                onClick = { onSelectStep(OnboardingStep.FOOD_AI_DEMO) },
                icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                label = { Text(trEn("Beslenme", "Nutrition", lang)) },
                colors = navColors
            )
            NavigationBarItem(
                selected = currentStep == OnboardingStep.WORKOUT_SYNC_DEMO,
                onClick = { onSelectStep(OnboardingStep.WORKOUT_SYNC_DEMO) },
                icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null) },
                label = { Text(trEn("Egzersiz", "Workouts", lang)) },
                colors = navColors
            )
            NavigationBarItem(
                selected = currentStep == OnboardingStep.WEIGHT_TREND_DEMO,
                onClick = { onSelectStep(OnboardingStep.WEIGHT_TREND_DEMO) },
                icon = { Icon(Icons.Default.MonitorWeight, contentDescription = null) },
                label = { Text(trEn("Kilo", "Weight", lang)) },
                colors = navColors
            )
            NavigationBarItem(
                selected = currentStep == OnboardingStep.ANALYTICS_AI_DEMO,
                onClick = { onSelectStep(OnboardingStep.ANALYTICS_AI_DEMO) },
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                label = { Text(trEn("AI Analiz", "AI Coach", lang)) },
                colors = navColors
            )
        }
    }
}

private fun buildBubbleSpec(
    state: OnboardingState,
    lang: AppLanguage
): SpotlightBubbleSpec {
    return when (state.microStep) {
        OnboardingMicroStep.FOOD_INPUT -> {
            val isTyping = state.foodPhase == FoodDemoPhase.TYPING
            val hasText = state.foodState.rawText.isNotBlank()
            SpotlightBubbleSpec(
                badge = trEn("1/4 • BESLENME (1/4)", "1/4 • NUTRITION (1/4)", lang),
                title = trEn("Ne Yediğini Doğal Dilde Yaz", "Type What You Ate Naturally", lang),
                description = trEn(
                    "Vurgulanan kutuya dokunup kendin yazabilir veya aşağıdaki butona basarak örnek öğünün nasıl yazıldığını görebilirsin.",
                    "Tap the highlighted field to type your own meal, or tap below to watch a sample meal get typed."
                ),
                primaryActionLabel = when {
                    isTyping -> trEn("✍️ Yazılıyor...", "✍️ Typing...", lang)
                    hasText -> trEn("✨ Devam: AI ile Hesapla →", "✨ Next: Calculate with AI →", lang)
                    else -> trEn("✍️ Örnek Öğünü Yaz", "✍️ Type Sample Meal", lang)
                },
                isPrimaryLoading = isTyping,
                isPrimaryEnabled = !isTyping,
                secondarySkipLabel = trEn("Atla →", "Skip →", lang),
                showBackButton = false,
                preferAbove = true
            )
        }

        OnboardingMicroStep.FOOD_CALCULATE -> SpotlightBubbleSpec(
            badge = trEn("1/4 • BESLENME (2/4)", "1/4 • NUTRITION (2/4)", lang),
            title = trEn("Gemini AI ile Hesapla", "Calculate with Gemini AI", lang),
            description = trEn(
                "Harika! Şimdi parlayan 'AI ile Hesapla' butonuna dokun — Gemini her besini kalori ve makrolarına ayırıp özet panelini açsın.",
                "Nice! Tap the glowing 'Calculate with AI' button — Gemini will break down calories & macros in a review sheet."
            ),
            primaryActionLabel = trEn("✨ AI ile Hesapla", "✨ Calculate with AI", lang),
            secondarySkipLabel = null,
            showBackButton = true,
            preferAbove = true
        )

        OnboardingMicroStep.FOOD_REVIEW_SHEET -> SpotlightBubbleSpec(
            badge = trEn("1/4 • BESLENME (3/4)", "1/4 • NUTRITION (3/4)", lang),
            title = trEn("Öğünü İncele & Ekle", "Review & Add Meal", lang),
            description = trEn(
                "Gemini öğününü ayrıştırdı. 'Onayla ve Günlüğe Ekle'ye dokun ve kalori halkanın değişimini izle.",
                "Gemini broke down your meal. Tap 'Confirm & Add to Log' to watch your calorie ring update."
            ),
            primaryActionLabel = trEn("✓ Onayla ve Ekle", "✓ Confirm & Add", lang),
            showBackButton = true,
            preferAbove = true
        )

        OnboardingMicroStep.FOOD_RESULT_GAUGE -> SpotlightBubbleSpec(
            badge = trEn("1/4 • BESLENME (4/4)", "1/4 • NUTRITION (4/4)", lang),
            title = trEn("Kalori Halkan & Makro Barların Güncellendi!", "Calorie Ring & Macro Bars Updated!", lang),
            description = trEn(
                "Alınan kalori 380 → 665 kcal oldu; Protein, Karbonhidrat ve Yağ barların anında doldu.",
                "Calories jumped 380 → 665 kcal, and your Protein, Carbs, and Fat bars filled automatically."
            ),
            feedbackPill = state.demoFeedbackPill,
            primaryActionLabel = trEn("Sonraki: Spor & Denge →", "Next: Workout & Balance →", lang),
            showBackButton = true,
            preferAbove = false
        )

        OnboardingMicroStep.WORKOUT_INPUT -> {
            val isTyping = state.workoutPhase == WorkoutDemoPhase.TYPING
            val hasText = state.workoutState.rawText.isNotBlank()
            SpotlightBubbleSpec(
                badge = trEn("2/4 • SPOR (1/3)", "2/4 • WORKOUTS (1/3)", lang),
                title = trEn("Yaptığın Sporu Yaz", "Describe Your Workout", lang),
                description = trEn(
                    "İşaretli kutuya antrenmanını yaz veya örnek koşuyu doldur; güncel kilon (72.4 kg) baz alınarak yakım hesaplansın.",
                    "Type your workout in the highlighted box or fill the sample run — burn is calculated using your 72.4 kg weight."
                ),
                primaryActionLabel = when {
                    isTyping -> trEn("✍️ Yazılıyor...", "✍️ Typing...", lang)
                    hasText -> trEn("✨ Devam: Kalori Hesapla →", "✨ Next: Calculate Burn →", lang)
                    else -> trEn("🏃 Örnek Antrenmanı Yaz", "🏃 Type Sample Workout", lang)
                },
                isPrimaryLoading = isTyping,
                isPrimaryEnabled = !isTyping,
                secondarySkipLabel = trEn("Atla →", "Skip →", lang),
                showBackButton = true,
                preferAbove = true
            )
        }

        OnboardingMicroStep.WORKOUT_CALCULATE -> SpotlightBubbleSpec(
            badge = trEn("2/4 • SPOR (2/3)", "2/4 • WORKOUTS (2/3)", lang),
            title = trEn("Yakılan Kaloriyi Hesapla", "Calculate Burned Calories", lang),
            description = trEn(
                "Şimdi 'AI ile Hesapla'ya dokun! Yakılan kalori hem efor halkana hem de günlük beslenme bütçene eklenecek.",
                "Tap 'Calculate with AI'! Burned calories will update your effort ring and expand your daily food budget."
            ),
            primaryActionLabel = trEn("✨ AI ile Hesapla (-360 kcal)", "✨ Calculate (-360 kcal)", lang),
            showBackButton = true,
            preferAbove = true
        )

        OnboardingMicroStep.WORKOUT_RESULT_GAUGE -> SpotlightBubbleSpec(
            badge = trEn("2/4 • SPOR (3/3)", "2/4 • WORKOUTS (3/3)", lang),
            title = trEn("Efor Halkan & Net Bütçen Değişti!", "Effort Ring & Net Budget Updated!", lang),
            description = trEn(
                "Yakılan kalori 210 → 570 kcal'a yükseldi ve günlük hedefin %114 ile tamamlandı! Net kalori bütçen de güncellendi.",
                "Burned calories jumped 210 → 570 kcal (114% goal hit!), automatically adjusting your daily net calories."
            ),
            feedbackPill = state.demoFeedbackPill,
            primaryActionLabel = trEn("Sonraki: Akıllı Kilo →", "Next: Smart Weight →", lang),
            showBackButton = true,
            preferAbove = false
        )

        OnboardingMicroStep.WEIGHT_ADD -> SpotlightBubbleSpec(
            badge = trEn("3/4 • AKILLI KİLO (1/2)", "3/4 • SMART WEIGHT (1/2)", lang),
            title = trEn("Bugünün Tartımını Ekle", "Log Today's Weigh-in", lang),
            description = trEn(
                "'Kilo Ekle' butonuna dokunup kendi kilonu girebilir veya tek dokunuşla 72.1 kg örnek ölçümünü ekleyebilirsin.",
                "Tap 'Add Weight' to enter a weigh-in or tap below to log today's 72.1 kg sample weigh-in."
            ),
            primaryActionLabel = trEn("⚖️ Örnek Tartıyı Ekle (72.1 kg)", "⚖️ Log Sample Weight (72.1 kg)", lang),
            secondarySkipLabel = trEn("Atla →", "Skip →", lang),
            showBackButton = true,
            preferAbove = false
        )

        OnboardingMicroStep.WEIGHT_RESULT_CHART -> SpotlightBubbleSpec(
            badge = trEn("3/4 • AKILLI KİLO (2/2)", "3/4 • SMART WEIGHT (2/2)", lang),
            title = trEn("Kilo Eğrin & Trendin Güncellendi!", "Weight Curve & Trend Updated!", lang),
            description = trEn(
                "72.1 kg grafiğe eklendi ve trend -1.5 kg olarak güncellendi. Protein ve kalori hedeflerin her tartımda otomatik uyarlanır.",
                "72.1 kg is plotted on your curve (-1.5 kg trend). Protein & calorie targets automatically adapt to your new weight."
            ),
            feedbackPill = state.demoFeedbackPill,
            primaryActionLabel = trEn("Sonraki: AI Koç & Rapor →", "Next: AI Coach & Report →", lang),
            showBackButton = true,
            preferAbove = true
        )

        OnboardingMicroStep.ANALYTICS_GENERATE -> SpotlightBubbleSpec(
            badge = trEn("4/4 • AI KOÇ (1/2)", "4/4 • AI COACH (1/2)", lang),
            title = trEn("Haftalık AI Raporunu Oluştur", "Generate Weekly AI Report", lang),
            description = trEn(
                "Beslenme, spor ve kilo verilerini birleştiren kişisel analiz için 'Seçili Haftayı AI ile Analiz Et' butonuna dokun.",
                "Tap 'Analyze Selected Week with AI' to combine your nutrition, workout, and weight data into one report."
            ),
            primaryActionLabel = trEn("📊 Haftalık AI Raporunu Oluştur", "📊 Generate Weekly Report", lang),
            secondarySkipLabel = trEn("Atla →", "Skip →", lang),
            showBackButton = true,
            preferAbove = false
        )

        OnboardingMicroStep.ANALYTICS_RESULT_REPORT -> SpotlightBubbleSpec(
            badge = trEn("4/4 • AI KOÇ (2/2)", "4/4 • AI COACH (2/2)", lang),
            title = trEn("Haftalık Skorun & Önerilerin Hazır!", "Weekly Score & Insights Ready!", lang),
            description = trEn(
                "88/100 skorun, güçlü yönlerin ve kişisel önerilerin oluşturuldu — dilersen tek dokunuşla WhatsApp'tan da paylaşabilirsin.",
                "Your 88/100 score, highlights, and actionable tips are ready — and shareable to WhatsApp in one tap."
            ),
            feedbackPill = state.demoFeedbackPill,
            primaryActionLabel = trEn("Turu Tamamla 🚀", "Finish Tour 🚀", lang),
            showBackButton = true,
            preferAbove = false
        )

        OnboardingMicroStep.READY_TO_START -> SpotlightBubbleSpec(
            badge = trEn("HAZIRSIN", "YOU'RE READY", lang),
            title = trEn("Başlamaya Hazırsın!", "You're Ready!", lang),
            description = "",
            primaryActionLabel = trEn("Hemen Başla", "Get Started", lang)
        )
    }
}

@Composable
private fun OnboardingReadyView(
    state: OnboardingState,
    lang: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
    onIntent: (OnboardingIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            OnboardingTopBar(
                currentStep = state.currentStep,
                lang = lang,
                onGoToStep = { onIntent(OnboardingIntent.OnGoToStep(it)) },
                onLanguageSelected = onLanguageSelected,
                onSkip = { onIntent(OnboardingIntent.OnFinish) }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                HealthBrandLogo(size = 68.dp)

                GradientText(
                    text = "HealthTrack",
                    colors = HealthColors.GradientPrimary,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                )

                Text(
                    text = str(
                        "Tüm sağlık telemetrin tek yerde: yapay zeka destekli beslenme, akıllı saat & tartı senkronu ve haftalık koçluk.",
                        "All your health telemetry in one place: AI-powered nutrition, smartwatch & scale sync, and weekly coaching."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ReadyPillarCard(
                        icon = Icons.Default.Restaurant,
                        title = str("Gemini Beslenme & Makro", "Gemini Nutrition & Macros"),
                        subtitle = str("Doğal dilde yaz, fotoğraf veya barkod okut; makrolar otomatik hesaplansın.", "Write naturally, snap a photo or barcode; macros calculate automatically.")
                    )
                    ReadyPillarCard(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        title = str("Spor & Akıllı Saat Dengesi", "Workouts & Watch Balance"),
                        subtitle = str("Health Connect / Apple Health ile adımlarını ve antrenmanlarını senkronize et.", "Sync steps and workouts seamlessly with Health Connect / Apple Health.")
                    )
                    ReadyPillarCard(
                        icon = Icons.Default.MonitorWeight,
                        title = str("Dinamik Kilo & Protein Hedefi", "Dynamic Weight & Protein Targets"),
                        subtitle = str("Güncel kilona göre protein ve kalori hedeflerin her gün otomatik uyarlanır.", "Protein and calorie targets adapt daily to your current weight.")
                    )
                    ReadyPillarCard(
                        icon = Icons.Default.AutoAwesome,
                        title = str("Haftalık AI Koç & Seriler", "Weekly AI Coach & Streaks"),
                        subtitle = str("XP kazan, rozetlerin kilidini aç ve haftalık özetini WhatsApp'tan al.", "Earn XP, unlock badges, and get your weekly summary via WhatsApp.")
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { onIntent(OnboardingIntent.OnFinish) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = str("Hemen Başla", "Get Started"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    TextButton(
                        onClick = { onIntent(OnboardingIntent.OnPreviousStep) },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = str("Tura Geri Dön", "Back to Tour"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadyPillarCard(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
