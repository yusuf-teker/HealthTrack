package com.yusufteker.healthtrack.feature.weight.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yusufteker.healthtrack.core.utils.BleScaleBridge
import com.yusufteker.healthtrack.core.utils.BleScaleLiveState
import com.yusufteker.healthtrack.core.utils.BleScaleReading
import com.yusufteker.healthtrack.core.utils.BleScaleScanState
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlinx.coroutines.Dispatchers

/**
 * Interactive modal sheet/dialog that manages a live Bluetooth LE smart scale weigh-in session
 * via [BleScaleBridge] and emits decoded weight + body fat + muscle metrics on save.
 */
@Composable
fun BleScaleMeasureSheet(
    bleScaleBridge: BleScaleBridge,
    userHeightCm: Double,
    userAge: Int,
    userIsMale: Boolean,
    onSaveReading: (BleScaleReading) -> Unit,
    onDismiss: () -> Unit
) {
    val bleState by bleScaleBridge.state.collectAsStateWithLifecycle(context = Dispatchers.Main.immediate)

    LaunchedEffect(userHeightCm, userAge, userIsMale) {
        bleScaleBridge.startScan(
            heightCm = userHeightCm,
            age = userAge,
            isMale = userIsMale
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            bleScaleBridge.stopScan()
        }
    }

    BleScaleMeasureDialogContent(
        bleState = bleState,
        userHeightCm = userHeightCm,
        userAge = userAge,
        userIsMale = userIsMale,
        onRetryScan = {
            bleScaleBridge.startScan(
                heightCm = userHeightCm,
                age = userAge,
                isMale = userIsMale
            )
        },
        onSaveReading = { reading ->
            bleScaleBridge.stopScan()
            onSaveReading(reading)
        },
        onDismiss = {
            bleScaleBridge.stopScan()
            onDismiss()
        }
    )
}

/**
 * Stateless dialog presentation for BLE smart scale weigh-in, usable directly in previews.
 */
@Composable
fun BleScaleMeasureDialogContent(
    bleState: BleScaleLiveState,
    userHeightCm: Double,
    userAge: Int,
    userIsMale: Boolean,
    onRetryScan: () -> Unit,
    onSaveReading: (BleScaleReading) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val reading = bleState.liveReading
    val isErrorState = bleState.scanState == BleScaleScanState.BLUETOOTH_OFF ||
        bleState.scanState == BleScaleScanState.PERMISSION_DENIED ||
        bleState.scanState == BleScaleScanState.NOT_SUPPORTED ||
        bleState.scanState == BleScaleScanState.ERROR

    val canSave = reading != null &&
        (bleState.scanState == BleScaleScanState.STABILIZED || reading.isStabilized)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .imePadding()
                .clearFocusOnTap(),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) HealthColors.SurfaceContainer else Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = str("Bluetooth Tartı Ölçümü", "Bluetooth Scale Weigh-in"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = str("Xiaomi Mi Scale 1 & 2 (Doğrudan Okuma)", "Xiaomi Mi Scale 1 & 2 (Direct Read)"),
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Status banner
                val statusColor = when {
                    isErrorState -> MaterialTheme.colorScheme.error
                    bleState.scanState == BleScaleScanState.STABILIZED -> Color(0xFF4CAF50)
                    else -> HealthColors.Primary
                }
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (bleState.scanState == BleScaleScanState.SCANNING ||
                            bleState.scanState == BleScaleScanState.MEASURING ||
                            bleState.scanState == BleScaleScanState.REQUESTING_PERMISSION
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = statusColor
                            )
                        } else if (bleState.scanState == BleScaleScanState.STABILIZED) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = bleState.statusMessage ?: str(
                                "Tartı aranıyor... Lütfen çıplak ayakla tartıya çıkın.",
                                "Searching for scale... Step onto the scale barefoot."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = statusColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Live Telemetry Card
                Surface(
                    color = if (isDark) HealthColors.SurfaceContainerHigh.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = reading?.weightKg?.toString() ?: "--.-",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "kg",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 7.dp)
                                )
                            }
                            Text(
                                text = reading?.scaleModel ?: str("Canlı Ağırlık", "Live Weight"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = HealthColors.NutrientCarbs.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, HealthColors.NutrientCarbs.copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = str("Vücut Yağ Oranı", "Body Fat"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = HealthColors.NutrientCarbs,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = reading?.bodyFatPercent?.let { "%$it" } ?: "--%",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (reading?.impedanceOhm != null) {
                                            "${reading.impedanceOhm} Ω"
                                        } else {
                                            str("Çıplak ayak gerekli", "Barefoot needed")
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                color = HealthColors.NutrientProtein.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, HealthColors.NutrientProtein.copy(alpha = 0.25f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = str("Kas Oranı", "Muscle"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = HealthColors.NutrientProtein,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = reading?.musclePercent?.let { "%$it" } ?: "--%",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = reading?.muscleMassKg?.let { "$it kg" }
                                            ?: reading?.boneMassKg?.let { str("Kemik $it kg", "Bone $it kg") }
                                            ?: str("Empedans bekliyor", "Awaiting BIA"),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Anthropometric BIA hint
                val sexLabel = if (userIsMale) str("Erkek", "Male") else str("Kadın", "Female")
                Text(
                    text = str(
                        "BIA profili: ${userHeightCm.toInt()} cm • $userAge yaş • $sexLabel (Profil ekranından güncellenir).",
                        "BIA profile: ${userHeightCm.toInt()} cm • $userAge yrs • $sexLabel (updated from Profile)."
                    ),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isErrorState && bleState.scanState != BleScaleScanState.NOT_SUPPORTED) {
                        OutlinedButton(
                            onClick = onRetryScan,
                            modifier = Modifier.weight(1f).height(48.dp).bounceClick(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(str("Tekrar Dene", "Retry"), fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(str("Vazgeç", "Cancel"))
                        }
                    }

                    Button(
                        onClick = { if (reading != null) onSaveReading(reading) },
                        enabled = canSave,
                        modifier = Modifier.weight(1.3f).height(48.dp).bounceClick(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HealthColors.Primary,
                            contentColor = HealthColors.OnPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(str("Ölçümü Kaydet", "Save Measurement"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
