package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.MealIngredientOption
import com.yusufteker.healthtrack.core.model.MealPreferencesCatalog
import com.yusufteker.healthtrack.core.model.SmartMealSuggestion
import com.yusufteker.healthtrack.feature.food.presentation.SmartMealFilterType
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

internal enum class SmartMealTab {
    SUGGESTIONS,
    PREFERENCES
}

private data class FilterChipItem(
    val type: SmartMealFilterType,
    val label: String,
    val selected: Boolean
)

private data class PreferenceChipItem(
    val key: String,
    val displayLabel: String,
    val selected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartMealMatcherBottomSheet(
    suggestions: List<SmartMealSuggestion>,
    latestAiRecipe: SmartMealSuggestion? = null,
    isFromUserDb: Boolean = false,
    isOnlyQuick: Boolean,
    isOnlyFewIngredients: Boolean,
    isExcludeRecent3Days: Boolean,
    isHighProtein: Boolean,
    isGeneratingAi: Boolean,
    excludedIngredients: Set<String> = emptySet(),
    favoriteIngredients: Set<String> = emptySet(),
    onToggleFilter: (SmartMealFilterType) -> Unit,
    onToggleExcludedIngredient: (String) -> Unit = {},
    onAddCustomExcludedIngredient: (String) -> Unit = {},
    onToggleFavoriteIngredient: (String) -> Unit = {},
    onAddCustomFavoriteIngredient: (String) -> Unit = {},
    onAddMeal: (SmartMealSuggestion) -> Unit,
    onOpenGroceryForMeal: (title: String, ingredients: List<String>) -> Unit,
    onRequestAiRecipe: () -> Unit,
    onDismissAiRecipe: () -> Unit = {},
    onOpenDailyMealPlan: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .clearFocusOnTap()
    ) {
        SmartMealMatcherContent(
            suggestions = suggestions,
            latestAiRecipe = latestAiRecipe,
            isFromUserDb = isFromUserDb,
            isOnlyQuick = isOnlyQuick,
            isOnlyFewIngredients = isOnlyFewIngredients,
            isExcludeRecent3Days = isExcludeRecent3Days,
            isHighProtein = isHighProtein,
            isGeneratingAi = isGeneratingAi,
            excludedIngredients = excludedIngredients,
            favoriteIngredients = favoriteIngredients,
            onToggleFilter = onToggleFilter,
            onToggleExcludedIngredient = onToggleExcludedIngredient,
            onAddCustomExcludedIngredient = onAddCustomExcludedIngredient,
            onToggleFavoriteIngredient = onToggleFavoriteIngredient,
            onAddCustomFavoriteIngredient = onAddCustomFavoriteIngredient,
            onAddMeal = onAddMeal,
            onOpenGroceryForMeal = onOpenGroceryForMeal,
            onRequestAiRecipe = onRequestAiRecipe,
            onDismissAiRecipe = onDismissAiRecipe,
            onOpenDailyMealPlan = onOpenDailyMealPlan,
            onDismiss = onDismiss
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SmartMealMatcherContent(
    suggestions: List<SmartMealSuggestion>,
    latestAiRecipe: SmartMealSuggestion? = null,
    isFromUserDb: Boolean = false,
    isOnlyQuick: Boolean,
    isOnlyFewIngredients: Boolean,
    isExcludeRecent3Days: Boolean,
    isHighProtein: Boolean,
    isGeneratingAi: Boolean,
    excludedIngredients: Set<String> = emptySet(),
    favoriteIngredients: Set<String> = emptySet(),
    onToggleFilter: (SmartMealFilterType) -> Unit,
    onToggleExcludedIngredient: (String) -> Unit = {},
    onAddCustomExcludedIngredient: (String) -> Unit = {},
    onToggleFavoriteIngredient: (String) -> Unit = {},
    onAddCustomFavoriteIngredient: (String) -> Unit = {},
    onAddMeal: (SmartMealSuggestion) -> Unit,
    onOpenGroceryForMeal: (title: String, ingredients: List<String>) -> Unit,
    onRequestAiRecipe: () -> Unit,
    onDismissAiRecipe: () -> Unit = {},
    onOpenDailyMealPlan: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    initialTab: SmartMealTab = SmartMealTab.SUGGESTIONS,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(initialTab) }
    val language = appLang()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .clearFocusOnTap()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Compact Single-Row Header ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(HealthColors.Primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = str("Ne Yesem? 🥗", "What Should I Eat? 🥗"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
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

        // ── 2-Segment Tab Switcher ──
        val prefCount = excludedIngredients.size + favoriteIngredients.size
        val prefTabTitle = if (prefCount > 0) {
            str("Alerji & Favori ($prefCount)", "Allergies & Favs ($prefCount)")
        } else {
            str("Alerji & Favori", "Allergies & Favs")
        }

        SmartMealTabSwitcher(
            activeTab = activeTab,
            suggestionsTitle = str("Öneriler", "Suggestions"),
            preferencesTitle = prefTabTitle,
            onSelectTab = { activeTab = it }
        )

        if (activeTab == SmartMealTab.SUGGESTIONS) {
            // ── 2-Row Filter Chips (Selected items always sorted first) ──
            val filterItems = listOf(
                FilterChipItem(
                    type = SmartMealFilterType.FROM_USER_DB,
                    label = str("📦 Envanterim", "📦 My Inventory"),
                    selected = isFromUserDb
                ),
                FilterChipItem(
                    type = SmartMealFilterType.QUICK,
                    label = str("⚡ <10 dk", "⚡ <10 min"),
                    selected = isOnlyQuick
                ),
                FilterChipItem(
                    type = SmartMealFilterType.FEW_INGREDIENTS,
                    label = str("🥣 ≤4 Malzeme", "🥣 ≤4 Items"),
                    selected = isOnlyFewIngredients
                ),
                FilterChipItem(
                    type = SmartMealFilterType.EXCLUDE_RECENT_3_DAYS,
                    label = str("🔄 Son 3 Gün Hariç", "🔄 Skip 3 Days"),
                    selected = isExcludeRecent3Days
                ),
                FilterChipItem(
                    type = SmartMealFilterType.HIGH_PROTEIN,
                    label = str("💪 Yüksek Protein", "💪 High Protein"),
                    selected = isHighProtein
                )
            )
            val orderedFilters = filterItems.sortedByDescending { it.selected }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                orderedFilters.forEach { item ->
                    FilterPill(
                        label = item.label,
                        selected = item.selected,
                        onClick = { onToggleFilter(item.type) }
                    )
                }
            }

            // ── Compact 1-Line Active Preferences Summary Pill (if any set) ──
            if (excludedIngredients.isNotEmpty() || favoriteIngredients.isNotEmpty()) {
                val summaryParts = buildList {
                    if (favoriteIngredients.isNotEmpty()) {
                        val favText = favoriteIngredients
                            .take(3)
                            .joinToString(", ") { MealPreferencesCatalog.localizedLabel(it, language) }
                        val extra = if (favoriteIngredients.size > 3) " +${favoriteIngredients.size - 3}" else ""
                        add("❤️ $favText$extra")
                    }
                    if (excludedIngredients.isNotEmpty()) {
                        val excText = excludedIngredients
                            .take(2)
                            .joinToString(", ") { MealPreferencesCatalog.localizedLabel(it, language) }
                        val extra = if (excludedIngredients.size > 2) " +${excludedIngredients.size - 2}" else ""
                        add("🚫 $excText$extra")
                    }
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { activeTab = SmartMealTab.PREFERENCES }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = summaryParts.joinToString("  •  "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = str("Tercihleri düzenle", "Edit preferences"),
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            val visibleSuggestions = remember(suggestions, latestAiRecipe?.id) {
                suggestions.filterNot { it.id == latestAiRecipe?.id }
            }

            // ── Scrollable Suggestions List ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Single slim AI Actions Row
                item(key = "ai_actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRequestAiRecipe,
                            enabled = !isGeneratingAi,
                            modifier = Modifier.weight(1f).height(40.dp).bounceClick(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = HealthColors.Primary
                            )
                        ) {
                            if (isGeneratingAi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = HealthColors.Primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = str("Hazırlanıyor...", "Crafting..."),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = HealthColors.Primary
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = str("AI Tarif İste", "AI Recipe"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (onOpenDailyMealPlan != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = HealthColors.Primary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenDailyMealPlan() }
                                    .bounceClick()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        tint = HealthColors.Primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = str("4 Öğün Planı", "4-Meal Plan"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HealthColors.Primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Recipe Spotlight Hero Card
                if (latestAiRecipe != null) {
                    item(key = "ai_spotlight") {
                        AiRecipeSpotlightCard(
                            meal = latestAiRecipe,
                            onAddMeal = { onAddMeal(latestAiRecipe) },
                            onOpenGrocery = { onOpenGroceryForMeal(latestAiRecipe.title, latestAiRecipe.ingredients) },
                            onDismiss = onDismissAiRecipe
                        )
                    }
                }

                // Matching Meal List or Empty State
                if (visibleSuggestions.isEmpty() && latestAiRecipe == null) {
                    item(key = "empty_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = str("Uygun öğün bulunamadı", "No matching meals found"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = str(
                                    "Filtreleri gevşetebilir veya AI tarif isteyebilirsiniz.",
                                    "Try relaxing filters or ask AI for a custom recipe."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (excludedIngredients.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { activeTab = SmartMealTab.PREFERENCES },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = str("Alerji & Tercihleri Düzenle", "Edit Allergies & Prefs"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(visibleSuggestions, key = { it.id }) { meal ->
                        SmartMealCard(
                            meal = meal,
                            onAddMeal = { onAddMeal(meal) },
                            onOpenGrocery = { onOpenGroceryForMeal(meal.title, meal.ingredients) }
                        )
                    }
                }
            }
        } else {
            // ── Preferences Tab: Allergies/Dislikes & Favorite Ingredients ──
            var customExcludedInput by remember { mutableStateOf("") }
            var customFavoriteInput by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IngredientPreferencesSection(
                    title = str("🚫 Alerji & Yemediklerim", "🚫 Allergies & Dislikes"),
                    subtitle = str(
                        "Bunları içeren öneriler gizlenir.",
                        "Meals containing these are hidden."
                    ),
                    presets = MealPreferencesCatalog.allergyPresets,
                    selectedKeys = excludedIngredients,
                    accentColor = Color(0xFFEF4444),
                    selectedIcon = Icons.Default.Close,
                    inputPlaceholder = str("Örn: Mantar, süt, fıstık...", "e.g., Mushroom, dairy, nuts..."),
                    customInput = customExcludedInput,
                    onCustomInputChange = { customExcludedInput = it },
                    onToggleKey = onToggleExcludedIngredient,
                    onSubmitCustom = { raw ->
                        onAddCustomExcludedIngredient(raw)
                        customExcludedInput = ""
                    }
                )

                IngredientPreferencesSection(
                    title = str("❤️ Favori Malzemeler", "❤️ Favorite Ingredients"),
                    subtitle = str(
                        "Seçtiklerinizi içeren öğünler üst sıralarda önerilir.",
                        "Meals with these ingredients appear higher."
                    ),
                    presets = MealPreferencesCatalog.favoritePresets,
                    selectedKeys = favoriteIngredients,
                    accentColor = HealthColors.Primary,
                    selectedIcon = Icons.Default.Favorite,
                    inputPlaceholder = str("Örn: Tavuk, salata, somon...", "e.g., Chicken, salad, salmon..."),
                    customInput = customFavoriteInput,
                    onCustomInputChange = { customFavoriteInput = it },
                    onToggleKey = onToggleFavoriteIngredient,
                    onSubmitCustom = { raw ->
                        onAddCustomFavoriteIngredient(raw)
                        customFavoriteInput = ""
                    }
                )

                Button(
                    onClick = { activeTab = SmartMealTab.SUGGESTIONS },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = str("Önerileri Gör (${suggestions.size})", "View Suggestions (${suggestions.size})"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SmartMealTabSwitcher(
    activeTab: SmartMealTab,
    suggestionsTitle: String,
    preferencesTitle: String,
    onSelectTab: (SmartMealTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TabSegment(
                label = suggestionsTitle,
                selected = activeTab == SmartMealTab.SUGGESTIONS,
                onClick = { onSelectTab(SmartMealTab.SUGGESTIONS) },
                modifier = Modifier.weight(1f)
            )
            TabSegment(
                label = preferencesTitle,
                selected = activeTab == SmartMealTab.PREFERENCES,
                onClick = { onSelectTab(SmartMealTab.PREFERENCES) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TabSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(9.dp),
        color = if (selected) HealthColors.Primary else Color.Transparent,
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientPreferencesSection(
    title: String,
    subtitle: String,
    presets: List<MealIngredientOption>,
    selectedKeys: Set<String>,
    accentColor: Color,
    selectedIcon: ImageVector,
    inputPlaceholder: String,
    customInput: String,
    onCustomInputChange: (String) -> Unit,
    onToggleKey: (String) -> Unit,
    onSubmitCustom: (String) -> Unit
) {
    val language = appLang()
    val presetIds = remember(presets) { presets.map { it.id }.toSet() }

    val chipItems = remember(presets, selectedKeys, language) {
        val presetChips = presets.map { option ->
            PreferenceChipItem(
                key = option.id,
                displayLabel = "${option.emoji} ${option.localizedLabel(language)}",
                selected = option.id in selectedKeys
            )
        }
        val customChips = selectedKeys
            .filterNot { it in presetIds }
            .sorted()
            .map { customKey ->
                PreferenceChipItem(
                    key = customKey,
                    displayLabel = MealPreferencesCatalog.localizedLabel(customKey, language),
                    selected = true
                )
            }
        (presetChips + customChips).sortedByDescending { it.selected }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                chipItems.forEach { item ->
                    PreferenceChip(
                        label = item.displayLabel,
                        selected = item.selected,
                        accentColor = accentColor,
                        selectedIcon = selectedIcon,
                        onClick = { onToggleKey(item.key) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = onCustomInputChange,
                    placeholder = {
                        Text(
                            text = inputPlaceholder,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (customInput.isNotBlank()) onSubmitCustom(customInput)
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilledIconButton(
                    onClick = {
                        if (customInput.isNotBlank()) onSubmitCustom(customInput)
                    },
                    enabled = customInput.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.size(44.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = str("Ekle", "Add"),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PreferenceChip(
    label: String,
    selected: Boolean,
    accentColor: Color,
    selectedIcon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) accentColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = if (selected) BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)) else null,
        contentColor = if (selected) accentColor else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = selectedIcon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (selected) HealthColors.Primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .bounceClick()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartMealCard(
    meal: SmartMealSuggestion,
    onAddMeal: () -> Unit,
    onOpenGrocery: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Title + Prep Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = meal.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    val timeText = when {
                        meal.prepTimeMinutes <= 0 -> str("Aç Ye", "Ready")
                        meal.prepTimeMinutes == 1 -> str("1 dk", "1m")
                        else -> str("${meal.prepTimeMinutes} dk", "${meal.prepTimeMinutes}m")
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Concise single-line Favorite Match Pill or Subtitle
            if (meal.matchedFavorites.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = HealthColors.Primary.copy(alpha = 0.13f)
                ) {
                    Text(
                        text = "❤️ ${meal.matchedFavorites.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = HealthColors.Primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                Text(
                    text = meal.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3 Crisp Macro Pills (no redundant labels or clutter tags)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MacroMiniPill("${meal.calories} kcal", Color(0xFFFF5722))
                MacroMiniPill("${meal.proteinG.toInt()}g Protein", Color(0xFF8E24AA))
                MacroMiniPill("${meal.fiberG.toInt()}g ${str("Lif", "Fiber")}", Color(0xFF43A047))
            }

            // Single Unified Bottom Row: Ingredients toggle + Cart + Log
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${str("Malzemeler", "Ingredients")} (${meal.ingredients.size}) ${if (expanded) "▴" else "▾"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenGrocery)
                            .bounceClick()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = str("Market Listesi", "Grocery List"),
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Button(
                        onClick = onAddMeal,
                        modifier = Modifier.height(34.dp).bounceClick(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = str("Ekle", "Log"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    meal.ingredients.forEach { ingredient ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(HealthColors.Primary)
                            )
                            Text(
                                text = ingredient,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    meal.instructions?.let { instr ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = instr,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroMiniPill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiRecipeSpotlightCard(
    meal: SmartMealSuggestion,
    onAddMeal: () -> Unit,
    onOpenGrocery: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = HealthColors.Primary.copy(alpha = 0.08f),
        border = BorderStroke(1.5.dp, HealthColors.Primary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
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
                    shape = RoundedCornerShape(999.dp),
                    color = HealthColors.Primary.copy(alpha = 0.20f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = str("YAPAY ZEKA ÖZEL TARİFİ", "AI CUSTOM RECIPE"),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Black,
                            color = HealthColors.Primary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(26.dp).bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = str("Gizle", "Hide"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = meal.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            meal.instructions?.let { instr ->
                Text(
                    text = instr,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MacroMiniPill("${meal.calories} kcal", Color(0xFFFF5722))
                MacroMiniPill("${meal.proteinG.toInt()}g Protein", Color(0xFF8E24AA))
                MacroMiniPill("${meal.fiberG.toInt()}g ${str("Lif", "Fiber")}", Color(0xFF43A047))
                if (meal.carbsG > 0) {
                    MacroMiniPill("${meal.carbsG.toInt()}g ${str("Karb", "Carbs")}", Color(0xFFFB923C))
                }
            }

            if (meal.ingredients.isNotEmpty()) {
                Text(
                    text = "${str("Malzemeler", "Ingredients")} (${meal.ingredients.size}) ${if (expanded) "▴" else "▾"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = HealthColors.Primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 2.dp)
                )

                AnimatedVisibility(visible = expanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        meal.ingredients.forEach { ing ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(HealthColors.Primary)
                                )
                                Text(
                                    text = ing,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddMeal,
                    modifier = Modifier.weight(1f).height(38.dp).bounceClick(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = str("Günlüğe Ekle", "Add to Log"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onOpenGrocery,
                    modifier = Modifier.height(38.dp).bounceClick(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HealthColors.Primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HealthColors.Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Planora To-Do",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
