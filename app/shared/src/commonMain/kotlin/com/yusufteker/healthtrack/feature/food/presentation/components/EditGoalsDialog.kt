package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yusufteker.healthtrack.core.model.NutritionGoal

@Composable
fun EditGoalsDialog(
    currentGoal: NutritionGoal,
    onDismiss: () -> Unit,
    onSave: (NutritionGoal) -> Unit
) {
    var caloriesText by remember { mutableStateOf(currentGoal.calories.toString()) }
    var proteinText by remember { mutableStateOf(currentGoal.proteinG.toInt().toString()) }
    var carbsText by remember { mutableStateOf(currentGoal.carbsG.toInt().toString()) }
    var fatText by remember { mutableStateOf(currentGoal.fatG.toInt().toString()) }
    var fiberText by remember { mutableStateOf(currentGoal.fiberG.toInt().toString()) }

    var vitCText by remember { mutableStateOf(currentGoal.vitaminCMg.toInt().toString()) }
    var vitDText by remember { mutableStateOf(currentGoal.vitaminDUg.toInt().toString()) }
    var vitB12Text by remember { mutableStateOf(currentGoal.vitaminB12Ug.toString()) }
    var ironText by remember { mutableStateOf(currentGoal.ironMg.toInt().toString()) }
    var calciumText by remember { mutableStateOf(currentGoal.calciumMg.toInt().toString()) }
    var magnesiumText by remember { mutableStateOf(currentGoal.magnesiumMg.toInt().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Günlük Besin Hedeflerini Belirle", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Makro Besinler", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                OutlinedTextField(
                    value = caloriesText,
                    onValueChange = { caloriesText = it },
                    label = { Text("Kalori (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proteinText,
                        onValueChange = { proteinText = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbsText,
                        onValueChange = { carbsText = it },
                        label = { Text("Karbonhidrat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("Yağ (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fiberText,
                        onValueChange = { fiberText = it },
                        label = { Text("Lif (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Mikro Besinler & Vitaminler", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vitCText,
                        onValueChange = { vitCText = it },
                        label = { Text("C Vit (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = vitDText,
                        onValueChange = { vitDText = it },
                        label = { Text("D Vit (µg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vitB12Text,
                        onValueChange = { vitB12Text = it },
                        label = { Text("B12 (µg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = ironText,
                        onValueChange = { ironText = it },
                        label = { Text("Demir (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calciumText,
                        onValueChange = { calciumText = it },
                        label = { Text("Kalsiyum (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = magnesiumText,
                        onValueChange = { magnesiumText = it },
                        label = { Text("Magnezyum (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = currentGoal.copy(
                        calories = caloriesText.toIntOrNull() ?: currentGoal.calories,
                        proteinG = proteinText.toDoubleOrNull() ?: currentGoal.proteinG,
                        carbsG = carbsText.toDoubleOrNull() ?: currentGoal.carbsG,
                        fatG = fatText.toDoubleOrNull() ?: currentGoal.fatG,
                        fiberG = fiberText.toDoubleOrNull() ?: currentGoal.fiberG,
                        vitaminCMg = vitCText.toDoubleOrNull() ?: currentGoal.vitaminCMg,
                        vitaminDUg = vitDText.toDoubleOrNull() ?: currentGoal.vitaminDUg,
                        vitaminB12Ug = vitB12Text.toDoubleOrNull() ?: currentGoal.vitaminB12Ug,
                        ironMg = ironText.toDoubleOrNull() ?: currentGoal.ironMg,
                        calciumMg = calciumText.toDoubleOrNull() ?: currentGoal.calciumMg,
                        magnesiumMg = magnesiumText.toDoubleOrNull() ?: currentGoal.magnesiumMg
                    )
                    onSave(updated)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
