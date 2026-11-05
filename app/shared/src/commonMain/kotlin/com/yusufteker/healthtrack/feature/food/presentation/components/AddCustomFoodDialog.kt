package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@Composable
fun AddCustomFoodDialog(
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        brand: String?,
        portionUnit: String,
        calories: Int,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double,
        categoryName: String,
        makeFavorite: Boolean
    ) -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var portionUnit by remember { mutableStateOf("") }
    var caloriesText by remember { mutableStateOf("") }
    var proteinText by remember { mutableStateOf("") }
    var carbsText by remember { mutableStateOf("") }
    var fatText by remember { mutableStateOf("") }
    var fiberText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FoodCategory.PROTEIN) }
    var makeFavorite by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val commonPortionSuggestions = listOf(
        "100g",
        "500ml (1 Kutu)",
        "250ml (1 Bardak)",
        "1 Adet",
        "1 Porsiyon",
        "1 Dilim",
        "1 Paket",
        "1 Ölçek (30g)"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clearFocusOnTap()
                .padding(16.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                elevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header with Icon & Close
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(HealthColors.Primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddBusiness,
                                    contentDescription = null,
                                    tint = HealthColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Kendi Marka Ürününü Ekle",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Değerleri manuel girerek hazır listenize kaydedin",
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
                                contentDescription = "Kapat",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Product Name
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Ürün / Besin Adı *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                errorMessage = null
                            },
                            placeholder = { Text("Örn: Kahveli Protein Süt, Yulaflı Bisküvi...") },
                            leadingIcon = {
                                Icon(Icons.Default.Restaurant, contentDescription = null, tint = HealthColors.Primary, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    // Brand (Optional)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Marka (İsteğe Bağlı)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = brand,
                            onValueChange = { brand = it },
                            placeholder = { Text("Örn: Pınar, İçim Fit, Züber, Eti...") },
                            leadingIcon = {
                                Icon(Icons.Default.Sell, contentDescription = null, tint = HealthColors.Secondary, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    // Portion Unit & Suggestions
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Porsiyon / Ambalaj Birimi *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = portionUnit,
                            onValueChange = {
                                portionUnit = it
                                errorMessage = null
                            },
                            placeholder = { Text("Örn: 500ml (1 Kutu), 100g, 1 Adet...") },
                            leadingIcon = {
                                Icon(Icons.Default.Straighten, contentDescription = null, tint = HealthColors.Secondary, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        // Quick chips for portion
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            commonPortionSuggestions.forEach { suggestion ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (portionUnit == suggestion) {
                                        HealthColors.Primary.copy(alpha = 0.2f)
                                    } else {
                                        if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                                    },
                                    modifier = Modifier
                                        .clickable { portionUnit = suggestion }
                                        .bounceClick()
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (portionUnit == suggestion) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Calorie (Required)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Porsiyon Kalorisi (kcal) *",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedTextField(
                            value = caloriesText,
                            onValueChange = {
                                caloriesText = it.filter { ch -> ch.isDigit() }
                                errorMessage = null
                            },
                            placeholder = { Text("Örn: 250") },
                            leadingIcon = {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = HealthColors.Tertiary, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                Text("kcal", style = MaterialTheme.typography.labelMedium, color = HealthColors.Tertiary, modifier = Modifier.padding(end = 12.dp))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )
                    }

                    // Macros: Protein, Carbs, Fat
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Makro Değerleri (Porsiyon Başına)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = proteinText,
                                onValueChange = { proteinText = it.replace(',', '.') },
                                placeholder = { Text("0") },
                                label = { Text("Protein (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = carbsText,
                                onValueChange = { carbsText = it.replace(',', '.') },
                                placeholder = { Text("0") },
                                label = { Text("Karb (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = fatText,
                                onValueChange = { fatText = it.replace(',', '.') },
                                placeholder = { Text("0") },
                                label = { Text("Yağ (g)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // Fiber (Optional)
                        OutlinedTextField(
                            value = fiberText,
                            onValueChange = { fiberText = it.replace(',', '.') },
                            placeholder = { Text("0") },
                            label = { Text("Lif (g, İsteğe Bağlı)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    // Category Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Kategori",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                FoodCategory.PROTEIN,
                                FoodCategory.BREAKFAST,
                                FoodCategory.CARBS,
                                FoodCategory.FRUITS_NUTS,
                                FoodCategory.HEALTHY_FATS
                            ).forEach { cat ->
                                val isSelected = selectedCategory == cat
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isSelected) {
                                        HealthColors.Primary.copy(alpha = 0.2f)
                                    } else {
                                        if (isDark) HealthColors.SurfaceContainerHigh else HealthColors.SurfaceVariantLight
                                    },
                                    modifier = Modifier
                                        .clickable { selectedCategory = cat }
                                        .bounceClick()
                                ) {
                                    Text(
                                        text = cat.labelTr,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Favorite Toggle Checkbox / Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.6f))
                            .clickable { makeFavorite = !makeFavorite }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (makeFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (makeFavorite) HealthColors.Tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Favori Listeme de Ekle",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = makeFavorite,
                            onCheckedChange = { makeFavorite = it }
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val trimmedPortion = portionUnit.trim()
                            val cal = caloriesText.toIntOrNull()

                            if (trimmedName.isBlank()) {
                                errorMessage = "Lütfen ürün veya besin adını giriniz."
                                return@Button
                            }
                            if (trimmedPortion.isBlank()) {
                                errorMessage = "Lütfen porsiyon veya ambalaj birimini giriniz (örn: 500ml, 100g)."
                                return@Button
                            }
                            if (cal == null || cal <= 0) {
                                errorMessage = "Lütfen geçerli bir kalori değeri giriniz."
                                return@Button
                            }

                            val prot = proteinText.toDoubleOrNull() ?: 0.0
                            val carb = carbsText.toDoubleOrNull() ?: 0.0
                            val fat = fatText.toDoubleOrNull() ?: 0.0
                            val fib = fiberText.toDoubleOrNull() ?: 0.0

                            onSave(
                                trimmedName,
                                brand.trim().ifBlank { null },
                                trimmedPortion,
                                cal,
                                prot,
                                carb,
                                fat,
                                fib,
                                selectedCategory.name,
                                makeFavorite
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = HealthColors.OnPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ürünü Kaydet & Kullanıma Aç",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
