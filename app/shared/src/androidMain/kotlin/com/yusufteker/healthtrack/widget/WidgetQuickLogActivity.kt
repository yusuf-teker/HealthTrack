package com.yusufteker.healthtrack.widget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.FoodLog
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.model.WeightEntry
import com.yusufteker.healthtrack.core.model.WorkoutLog
import com.yusufteker.healthtrack.core.network.AiQuotaExceededException
import com.yusufteker.healthtrack.core.network.HealthApiClient
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.network.formatActionableApiError
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.sync.WidgetSnapshotBuilder
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import com.yusufteker.healthtrack.core.utils.getTodayIso
import com.yusufteker.healthtrack.core.utils.toLocaleDoubleOrNull
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.HealthTrackTheme
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.context.GlobalContext
import kotlin.math.roundToInt
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Lightweight transparent floating dialog Activity launched directly from Android Home Screen Widgets.
 * Allows instant AI Food analysis & logging, AI Workout analysis & logging, or Weight entry
 * without launching the full application UI.
 */
class WidgetQuickLogActivity : ComponentActivity() {

    private val backgroundSyncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val EXTRA_MODE = "extra_widget_quick_log_mode"
        const val MODE_AI_FOOD = "ai_food"
        const val MODE_AI_WORKOUT = "ai_workout"
        const val MODE_WEIGHT = "weight"

        private val ORDERED_MODES = listOf(MODE_AI_FOOD, MODE_AI_WORKOUT, MODE_WEIGHT)

        fun createIntent(context: Context, mode: String): Intent =
            Intent(context, WidgetQuickLogActivity::class.java).apply {
                putExtra(EXTRA_MODE, mode)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Fire an immediate background warm-up ping while the user is typing so Render wakes up early
        backgroundSyncScope.launch {
            runCatching {
                val apiClient = GlobalContext.getOrNull()?.getOrNull<HealthApiClient>() ?: return@launch
                apiClient.httpClient.get("/health")
            }
        }

        val initialMode = intent?.getStringExtra(EXTRA_MODE) ?: MODE_AI_FOOD
        val snapshot = HealthTrackWidgetStore.loadSnapshot(applicationContext)

        setContent {
            HealthTrackTheme(darkTheme = snapshot.isDarkTheme) {
                WidgetQuickLogOverlay(
                    initialMode = initialMode,
                    isDark = snapshot.isDarkTheme,
                    isTr = snapshot.languageCode.lowercase().startsWith("tr"),
                    defaultWeightKg = snapshot.latestWeightKg,
                    onDismiss = { finish() },
                    onSubmitFood = { text -> submitAiFood(text) },
                    onSubmitWorkout = { text -> submitAiWorkout(text) },
                    onSubmitWeight = { weightKg -> submitWeight(weightKg) }
                )
            }
        }
    }

    private fun isRetryableColdStartError(err: Throwable): Boolean {
        if (err is AiQuotaExceededException) return false
        val msg = err.message.orEmpty().lowercase()
        return msg.contains("timeout") ||
            msg.contains("timed out") ||
            msg.contains("502") ||
            msg.contains("503") ||
            msg.contains("504") ||
            msg.contains("connect") ||
            msg.contains("reset")
    }

    private fun triggerBackgroundSync(syncEngine: SyncEngine?) {
        if (syncEngine == null) return
        backgroundSyncScope.launch {
            runCatching { syncEngine.sync() }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun submitAiFood(rawText: String): Result<String> = withContext(Dispatchers.IO) {
        val koin = GlobalContext.getOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Uygulama servisleri hazır değil."))
        val tokenStorage = koin.get<TokenStorage>()
        val database = koin.get<HealthDatabase>()
        val apiClient = koin.get<HealthApiClient>()
        val snapshotBuilder = koin.get<WidgetSnapshotBuilder>()
        val syncEngine = koin.getOrNull<SyncEngine>()

        val userId = tokenStorage.getUserId()
        if (!tokenStorage.isAuthenticated() || userId <= 0) {
            return@withContext Result.failure(
                IllegalStateException("AI analizini kullanmak için ücretsiz hesap oluşturun veya giriş yapın.")
            )
        }

        val today = getTodayIso()
        var result = apiClient.analyzeFood(rawText, today)
        if (result.isFailure && isRetryableColdStartError(result.exceptionOrNull()!!)) {
            // Render cold-start retry: second attempt succeeds quickly once instance is awake
            delay(1200L)
            result = apiClient.analyzeFood(rawText, today)
        }

        result.fold(
            onSuccess = { aiResult ->
                val logId = "food_${userId}_${today}"
                val existingLog = database.getFoodById(logId)
                val now = currentTimeMillis()
                val incomingItems = aiResult.items.map { item ->
                    if (item.id.isBlank()) item.copy(id = Uuid.random().toString()) else item
                }
                val combinedItems = incomingItems + (existingLog?.items ?: emptyList())
                val totalCals = combinedItems.sumOf { it.calories }
                val newLog = FoodLog(
                    id = logId,
                    userId = userId,
                    date = today,
                    rawText = combinedItems.joinToString(", ") { "${it.name} (${it.formattedPortion})" },
                    summary = aiResult.summary,
                    totalCalories = totalCals,
                    items = combinedItems,
                    createdAt = existingLog?.createdAt ?: now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
                database.upsertFood(newLog)

                val updatedSnapshot = snapshotBuilder.buildTodaySnapshot(userId, today)
                HealthTrackWidgetStore.saveSnapshot(applicationContext, updatedSnapshot)
                HealthTrackWidgetStore.updateAllWidgets(applicationContext)
                triggerBackgroundSync(syncEngine)

                val addedKcal = aiResult.totalCalories.takeIf { it > 0 }
                    ?: incomingItems.sumOf { it.calories }
                val isTr = updatedSnapshot.languageCode.lowercase().startsWith("tr")
                Result.success(
                    if (isTr) "✓ +$addedKcal kcal öğüne eklendi!"
                    else "✓ Added +$addedKcal kcal to today's log!"
                )
            },
            onFailure = { err ->
                val message = if (err is AiQuotaExceededException) {
                    err.message
                } else {
                    formatActionableApiError(
                        error = err,
                        operationTr = "AI öğün hesaplama",
                        operationEn = "AI meal analysis"
                    )
                }
                Result.failure(IllegalStateException(message))
            }
        )
    }

    private suspend fun submitAiWorkout(rawText: String): Result<String> = withContext(Dispatchers.IO) {
        val koin = GlobalContext.getOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Uygulama servisleri hazır değil."))
        val tokenStorage = koin.get<TokenStorage>()
        val database = koin.get<HealthDatabase>()
        val apiClient = koin.get<HealthApiClient>()
        val snapshotBuilder = koin.get<WidgetSnapshotBuilder>()
        val syncEngine = koin.getOrNull<SyncEngine>()

        val userId = tokenStorage.getUserId()
        if (!tokenStorage.isAuthenticated() || userId <= 0) {
            return@withContext Result.failure(
                IllegalStateException("AI analizini kullanmak için ücretsiz hesap oluşturun veya giriş yapın.")
            )
        }

        val today = getTodayIso()
        val currentSnap = snapshotBuilder.buildTodaySnapshot(userId, today)
        var result = apiClient.analyzeWorkout(rawText, currentSnap.latestWeightKg, today)
        if (result.isFailure && isRetryableColdStartError(result.exceptionOrNull()!!)) {
            delay(1200L)
            result = apiClient.analyzeWorkout(rawText, currentSnap.latestWeightKg, today)
        }

        result.fold(
            onSuccess = { aiResult ->
                val now = currentTimeMillis()
                val log = WorkoutLog(
                    id = "widget_workout_${userId}_${now}",
                    userId = userId,
                    date = today,
                    rawText = rawText,
                    caloriesBurned = aiResult.caloriesBurned.coerceAtLeast(0),
                    durationMinutes = aiResult.durationMinutes.coerceAtLeast(0),
                    aiAnalysis = aiResult.summary,
                    createdAt = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING
                )
                database.upsertWorkout(log)

                val updatedSnapshot = snapshotBuilder.buildTodaySnapshot(userId, today)
                HealthTrackWidgetStore.saveSnapshot(applicationContext, updatedSnapshot)
                HealthTrackWidgetStore.updateAllWidgets(applicationContext)
                triggerBackgroundSync(syncEngine)

                val isTr = updatedSnapshot.languageCode.lowercase().startsWith("tr")
                Result.success(
                    if (isTr) "✓ -${aiResult.caloriesBurned} kcal (${aiResult.durationMinutes} dk) eklendi!"
                    else "✓ Logged -${aiResult.caloriesBurned} kcal (${aiResult.durationMinutes} min)!"
                )
            },
            onFailure = { err ->
                val message = if (err is AiQuotaExceededException) {
                    err.message
                } else {
                    formatActionableApiError(
                        error = err,
                        operationTr = "AI spor hesaplama",
                        operationEn = "AI workout analysis"
                    )
                }
                Result.failure(IllegalStateException(message))
            }
        )
    }

    private suspend fun submitWeight(weightKg: Double): Result<String> = withContext(Dispatchers.IO) {
        val koin = GlobalContext.getOrNull()
            ?: return@withContext Result.failure(IllegalStateException("Uygulama servisleri hazır değil."))
        val tokenStorage = koin.get<TokenStorage>()
        val database = koin.get<HealthDatabase>()
        val snapshotBuilder = koin.get<WidgetSnapshotBuilder>()
        val syncEngine = koin.getOrNull<SyncEngine>()

        val userId = tokenStorage.getUserId()
        if (!tokenStorage.hasActiveSession() || userId == 0) {
            return@withContext Result.failure(
                IllegalStateException("Önce HealthTrack uygulamasına giriş yapmalısınız.")
            )
        }

        val today = getTodayIso()
        val now = currentTimeMillis()
        val rounded = (weightKg * 10.0).roundToInt() / 10.0
        database.upsertWeight(
            WeightEntry(
                id = "widget_weight_${userId}_${today}_${now}",
                userId = userId,
                date = today,
                weightKg = rounded,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )
        )

        val updatedSnapshot = snapshotBuilder.buildTodaySnapshot(userId, today)
        HealthTrackWidgetStore.saveSnapshot(applicationContext, updatedSnapshot)
        HealthTrackWidgetStore.setDraftWeight(applicationContext, rounded)
        HealthTrackWidgetStore.updateAllWidgets(applicationContext)
        triggerBackgroundSync(syncEngine)

        val isTr = updatedSnapshot.languageCode.lowercase().startsWith("tr")
        Result.success(
            if (isTr) "✓ $rounded kg kaydedildi!"
            else "✓ Saved $rounded kg!"
        )
    }
}

@Composable
private fun WidgetQuickLogOverlay(
    initialMode: String,
    isDark: Boolean,
    isTr: Boolean,
    defaultWeightKg: Double?,
    onDismiss: () -> Unit,
    onSubmitFood: suspend (String) -> Result<String>,
    onSubmitWorkout: suspend (String) -> Result<String>,
    onSubmitWeight: suspend (Double) -> Result<String>
) {
    val orderedModes = remember {
        listOf(
            WidgetQuickLogActivity.MODE_AI_FOOD,
            WidgetQuickLogActivity.MODE_AI_WORKOUT,
            WidgetQuickLogActivity.MODE_WEIGHT
        )
    }
    var mode by remember { mutableStateOf(initialMode) }
    var inputText by remember(mode) {
        mutableStateOf(
            if (mode == WidgetQuickLogActivity.MODE_WEIGHT && defaultWeightKg != null) {
                (((defaultWeightKg * 10.0).roundToInt()) / 10.0).toString()
            } else {
                ""
            }
        )
    }
    var isLoading by remember { mutableStateOf(false) }
    var loadingElapsedSec by remember { mutableStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(mode) {
        errorMessage = null
        successMessage = null
        delay(120L)
        runCatching { focusRequester.requestFocus() }
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            loadingElapsedSec = 0
            return@LaunchedEffect
        }
        loadingElapsedSec = 0
        while (isLoading) {
            delay(1000L)
            loadingElapsedSec++
        }
    }

    val performSubmit: () -> Unit = {
        val trimmed = inputText.trim()
        if (!isLoading && trimmed.isNotEmpty()) {
            scope.launch {
                isLoading = true
                errorMessage = null
                val result = when (mode) {
                    WidgetQuickLogActivity.MODE_AI_WORKOUT -> onSubmitWorkout(trimmed)
                    WidgetQuickLogActivity.MODE_WEIGHT -> {
                        val parsed = trimmed.toLocaleDoubleOrNull()
                        if (parsed == null || parsed < 25.0 || parsed > 300.0) {
                            Result.failure(
                                IllegalArgumentException(
                                    if (isTr) "Geçerli bir kilo giriniz (25 - 300 kg)."
                                    else "Please enter a valid weight (25 - 300 kg)."
                                )
                            )
                        } else {
                            onSubmitWeight(parsed)
                        }
                    }
                    else -> onSubmitFood(trimmed)
                }
                isLoading = false
                result.onSuccess { msg ->
                    successMessage = msg
                    delay(950L)
                    onDismiss()
                }.onFailure { err ->
                    errorMessage = err.message ?: if (isTr) "İşlem başarısız oldu." else "Action failed."
                }
            }
        }
    }

    val surfaceBg = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
    val borderCol = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.08f)
    val primaryText = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val secondaryText = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onDismiss() }
            )
            .clearFocusOnTap()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        var totalDragX by remember { mutableStateOf(0f) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(mode, isLoading) {
                    detectHorizontalDragGestures(
                        onDragStart = { totalDragX = 0f },
                        onHorizontalDrag = { _, dragAmount -> totalDragX += dragAmount },
                        onDragEnd = {
                            if (!isLoading && kotlin.math.abs(totalDragX) > 70f) {
                                val currentIdx = orderedModes.indexOf(mode).coerceAtLeast(0)
                                val nextIdx = if (totalDragX < 0f) {
                                    (currentIdx + 1) % orderedModes.size
                                } else {
                                    (currentIdx - 1 + orderedModes.size) % orderedModes.size
                                }
                                mode = orderedModes[nextIdx]
                            }
                            totalDragX = 0f
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .border(1.dp, borderCol, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = surfaceBg,
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header + Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("H", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Text(
                            text = when (mode) {
                                WidgetQuickLogActivity.MODE_AI_WORKOUT -> if (isTr) "AI Hızlı Spor" else "AI Quick Workout"
                                WidgetQuickLogActivity.MODE_WEIGHT -> if (isTr) "Hızlı Kilo Kaydet" else "Quick Weight Log"
                                else -> if (isTr) "AI Hızlı Öğün" else "AI Quick Meal"
                            },
                            color = primaryText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = secondaryText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Mode Switcher Pills (Swipe left/right or tap)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickModeChip(
                        label = if (isTr) "AI Öğün" else "AI Meal",
                        icon = Icons.Default.AutoAwesome,
                        selected = mode == WidgetQuickLogActivity.MODE_AI_FOOD,
                        tint = Color(0xFF38BDF8),
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isLoading) mode = WidgetQuickLogActivity.MODE_AI_FOOD }
                    )
                    QuickModeChip(
                        label = if (isTr) "Spor" else "Sport",
                        icon = Icons.Default.FitnessCenter,
                        selected = mode == WidgetQuickLogActivity.MODE_AI_WORKOUT,
                        tint = Color(0xFFFB923C),
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isLoading) mode = WidgetQuickLogActivity.MODE_AI_WORKOUT }
                    )
                    QuickModeChip(
                        label = if (isTr) "Kilo" else "Weight",
                        icon = Icons.Default.Scale,
                        selected = mode == WidgetQuickLogActivity.MODE_WEIGHT,
                        tint = Color(0xFFC084FC),
                        isDark = isDark,
                        modifier = Modifier.weight(1f),
                        onClick = { if (!isLoading) mode = WidgetQuickLogActivity.MODE_WEIGHT }
                    )
                }

                // Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        errorMessage = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    enabled = !isLoading && successMessage == null,
                    placeholder = {
                        Text(
                            text = when (mode) {
                                WidgetQuickLogActivity.MODE_AI_WORKOUT -> if (isTr) {
                                    "Örn: 30 dk tempolu koşu, 20 dk ağırlık..."
                                } else {
                                    "e.g. 30 min run, 20 min weights..."
                                }
                                WidgetQuickLogActivity.MODE_WEIGHT -> if (isTr) "Örn: 74.2" else "e.g. 74.2"
                                else -> if (isTr) {
                                    "Ne yedin? Örn: 2 haşlanmış yumurta, 1 dilim ekmek..."
                                } else {
                                    "What did you eat? e.g. 2 boiled eggs, 1 slice toast..."
                                }
                            },
                            color = secondaryText.copy(alpha = 0.65f),
                            fontSize = 13.sp
                        )
                    },
                    suffix = if (mode == WidgetQuickLogActivity.MODE_WEIGHT) {
                        { Text("kg", color = Color(0xFFC084FC), fontWeight = FontWeight.Bold) }
                    } else null,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = when (mode) {
                            WidgetQuickLogActivity.MODE_AI_WORKOUT -> Color(0xFFFB923C)
                            WidgetQuickLogActivity.MODE_WEIGHT -> Color(0xFFC084FC)
                            else -> HealthColors.Primary
                        },
                        unfocusedBorderColor = borderCol,
                        focusedTextColor = primaryText,
                        unfocusedTextColor = primaryText
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = if (mode == WidgetQuickLogActivity.MODE_WEIGHT) {
                            KeyboardCapitalization.None
                        } else {
                            KeyboardCapitalization.Sentences
                        },
                        keyboardType = if (mode == WidgetQuickLogActivity.MODE_WEIGHT) {
                            KeyboardType.Decimal
                        } else {
                            KeyboardType.Text
                        },
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = { performSubmit() }),
                    minLines = 1,
                    maxLines = 3
                )

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedVisibility(visible = successMessage != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF06D6A0).copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF06D6A0), modifier = Modifier.size(18.dp))
                        Text(
                            text = successMessage.orEmpty(),
                            color = Color(0xFF06D6A0),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                val buttonColor = when (mode) {
                    WidgetQuickLogActivity.MODE_AI_WORKOUT -> Color(0xFFFB923C)
                    WidgetQuickLogActivity.MODE_WEIGHT -> Color(0xFFC084FC)
                    else -> Color(0xFF0284C7)
                }

                Button(
                    onClick = performSubmit,
                    enabled = !isLoading && inputText.isNotBlank() && successMessage == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = Color.White,
                        disabledContainerColor = buttonColor.copy(alpha = 0.55f),
                        disabledContentColor = Color.White.copy(alpha = 0.9f)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        val statusLabel = when {
                            loadingElapsedSec >= 12 -> if (isTr) "Sunucu uyanıyor, az kaldı..." else "Server waking up, almost ready..."
                            loadingElapsedSec >= 4 -> if (isTr) "Sunucuya bağlanıyor..." else "Connecting to server..."
                            else -> if (isTr) "AI hesaplıyor..." else "Calculating with AI..."
                        }
                        Text(
                            text = statusLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = when (mode) {
                                WidgetQuickLogActivity.MODE_AI_WORKOUT -> if (isTr) "✨ AI Hesapla & Sporu Ekle" else "✨ AI Calculate & Log Workout"
                                WidgetQuickLogActivity.MODE_WEIGHT -> if (isTr) "✓ Kiloyu Kaydet" else "✓ Save Weight"
                                else -> if (isTr) "✨ AI Hesapla & Öğüne Ekle" else "✨ AI Calculate & Add Meal"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) tint.copy(alpha = 0.2f) else if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
    val stroke = if (selected) tint.copy(alpha = 0.65f) else Color.Transparent
    val textColor = if (selected) tint else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, stroke, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.size(4.dp))
        Text(label, color = textColor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}
