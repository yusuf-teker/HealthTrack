package com.yusufteker.healthtrack.feature.profile.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@Composable
internal fun CustomApiKeyDialog(
    initialKey: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember(initialKey) { mutableStateOf(initialKey) }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        icon = {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = str("Özel Gemini API Key", "Custom Gemini API Key"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = str(
                        "Kendi Google Gemini API anahtarınızı (aistudio.google.com) girerek yemek ve spor AI analizlerini kendi kotanızla çalıştırabilirsiniz. Anahtarınız yalnızca cihazınızda saklanır.",
                        "Enter your own Google Gemini API key (aistudio.google.com) to run meal and workout AI analyses with your own quota. Your key is stored only on this device."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("AIza...") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(keyInput)
                    onDismiss()
                },
                enabled = keyInput.trim().isNotBlank()
            ) {
                Text(str("Kaydet", "Save"))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (initialKey.isNotBlank()) {
                    TextButton(
                        onClick = {
                            onClear()
                            onDismiss()
                        }
                    ) {
                        Text(str("Kaldır", "Remove"), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(str("Vazgeç", "Cancel"))
                }
            }
        }
    )
}

@Composable
internal fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = str("Çıkış Yap", "Sign Out"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = str(
                    "Hesabınızdan çıkış yapmak istediğinize emin misiniz?",
                    "Are you sure you want to sign out of your account?"
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(str("Çıkış Yap", "Sign Out"), color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(str("Vazgeç", "Cancel"))
            }
        }
    )
}

@Composable
internal fun DeleteAccountConfirmDialog(
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isDeleting) onDismiss() },
        modifier = Modifier.imePadding(),
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = str("Hesabı Kalıcı Olarak Sil", "Permanently Delete Account"),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = str(
                    "Hesabınızı sildiğinizde tüm HealthTrack ve Planora verileriniz (beslenme, antrenman, kilo, görevler ve profil) kalıcı olarak silinir. Bu işlem geri alınamaz.",
                    "Deleting your account will permanently erase all your HealthTrack and Planora data (nutrition, workouts, weight, tasks, and profile). This action cannot be undone."
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(str("Evet, Hesabımı Sil", "Yes, Delete My Account"), color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting
            ) {
                Text(str("Vazgeç", "Cancel"))
            }
        }
    )
}

@Composable
internal fun ProfileStatPill(
    label: String,
    value: String,
    subLabel: String? = null,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        if (!subLabel.isNullOrBlank()) {
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun EditBodyProfileDialog(
    state: ProfileState,
    onIntent: (ProfileIntent) -> Unit,
    onDismiss: () -> Unit
) {
    var targetWeightInput by remember { mutableStateOf(state.targetWeightKg.toString()) }
    var heightInput by remember { mutableStateOf(state.heightCm.toInt().toString()) }
    var ageInput by remember { mutableStateOf(state.age.toString()) }
    var selectedGender by remember { mutableStateOf(state.gender) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = str("Fiziksel Bilgileri Düzenle", "Edit Body Profile"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = str("Kapat", "Close"), modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clearFocusOnTap()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = str(
                        "Güncel kilonuz (${state.weightKg} kg) Kilo sekmesinden otomatik güncellenir.",
                        "Your current weight (${state.weightKg} kg) updates automatically from the Weight tab."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = targetWeightInput,
                    onValueChange = { targetWeightInput = it },
                    label = { Text(str("Hedef Kilo (kg)", "Target Weight (kg)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text(str("Boy (cm)", "Height (cm)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it },
                        label = { Text(str("Yaş", "Age")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = str("Biyolojik Cinsiyet", "Biological Sex"),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GenderOptionCard(
                        title = str("Erkek", "Male"),
                        icon = Icons.Rounded.Male,
                        isSelected = selectedGender == "MALE",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedGender = "MALE" }
                    )

                    GenderOptionCard(
                        title = str("Kadın", "Female"),
                        icon = Icons.Rounded.Female,
                        isSelected = selectedGender == "FEMALE",
                        accentColor = HealthColors.Secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedGender = "FEMALE" }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    targetWeightInput.replace(',', '.').toDoubleOrNull()?.let {
                        onIntent(ProfileIntent.OnTargetWeightChanged(it))
                    }
                    heightInput.replace(',', '.').toDoubleOrNull()?.let {
                        onIntent(ProfileIntent.OnHeightChanged(it))
                    }
                    ageInput.toIntOrNull()?.let {
                        onIntent(ProfileIntent.OnAgeChanged(it))
                    }
                    if (selectedGender != state.gender) {
                        onIntent(ProfileIntent.OnGenderChanged(selectedGender))
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(str("Kaydet", "Save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(str("İptal", "Cancel"))
            }
        }
    )
}

@Composable
internal fun FitnessGoalOptionCard(
    title: String,
    multiplier: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.15f) else if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .bounceClick()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 11.5.sp
                ),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = multiplier,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun GenderOptionCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val borderColor = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val bgColor = if (isSelected) accentColor.copy(alpha = 0.15f) else if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.4f) else HealthColors.SurfaceVariantLight.copy(alpha = 0.5f)

    Surface(
        modifier = modifier
            .bounceClick()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 10.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
