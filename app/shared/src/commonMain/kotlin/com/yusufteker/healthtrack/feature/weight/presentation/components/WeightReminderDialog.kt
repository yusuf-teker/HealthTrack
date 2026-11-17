package com.yusufteker.healthtrack.feature.weight.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.model.WeightReminderFrequency
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.bounceClick

@Composable
fun WeightReminderDialog(
    currentFrequency: WeightReminderFrequency,
    initialHour: Int = 8,
    initialMinute: Int = 0,
    onDismiss: () -> Unit,
    onSave: (frequency: WeightReminderFrequency, hour: Int, minute: Int) -> Unit
) {
    var selectedFrequency by remember { mutableStateOf(currentFrequency) }
    var selectedHour by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var selectedMinute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(scrollState),
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
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = HealthColors.Primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = str("Tartı Hatırlatıcısı", "Weigh-in Reminder"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = str("Kilo Takip Periyodu & Saati", "Weight Cadence & Time"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = str("Kapat", "Close"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Info Callout
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HealthColors.Primary.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(20.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = str(
                                    "Sabah aç karna ve tuvalet sonrası aynı saatte tartılmak en tutarlı vücut ağırlığı verisini sağlar.",
                                    "Weighing in fasted in the morning at the same hour gives the most consistent body weight trend."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // ── Time Selector (Visible when frequency is not OFF) ──
                    AnimatedVisibility(
                        visible = selectedFrequency != WeightReminderFrequency.OFF,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Time Header
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
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(HealthColors.Primary.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = HealthColors.Primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = str("Hatırlatma Saati", "Reminder Time"),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = str("Sabah bildirim saati", "Morning notification time"),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (selectedHour == 8 && selectedMinute == 0) {
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = HealthColors.Primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = str("Varsayılan (08:00)", "Default (08:00)"),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = HealthColors.Primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // Big Clock Stepper
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Hour Stepper
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                selectedHour = if (selectedHour >= 23) 0 else selectedHour + 1
                                            },
                                            modifier = Modifier.size(34.dp).bounceClick(),
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = str("Saat Artır", "Increase Hour"), modifier = Modifier.size(18.dp))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.width(68.dp)
                                        ) {
                                            Text(
                                                text = selectedHour.toString().padStart(2, '0'),
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 30.sp
                                                ),
                                                color = HealthColors.Primary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            )
                                        }

                                        FilledTonalIconButton(
                                            onClick = {
                                                selectedHour = if (selectedHour <= 0) 23 else selectedHour - 1
                                            },
                                            modifier = Modifier.size(34.dp).bounceClick(),
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = str("Saat Azalt", "Decrease Hour"), modifier = Modifier.size(18.dp))
                                        }

                                        Text(
                                            text = str("Saat", "Hour"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Colon Separator
                                    Text(
                                        text = ":",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 32.sp
                                        ),
                                        color = HealthColors.Primary,
                                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 20.dp)
                                    )

                                    // Minute Stepper
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                selectedMinute = ((selectedMinute / 5 + 1) * 5) % 60
                                            },
                                            modifier = Modifier.size(34.dp).bounceClick(),
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = str("Dakika Artır", "Increase Minute"), modifier = Modifier.size(18.dp))
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.width(68.dp)
                                        ) {
                                            Text(
                                                text = selectedMinute.toString().padStart(2, '0'),
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 30.sp
                                                ),
                                                color = HealthColors.Primary,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            )
                                        }

                                        FilledTonalIconButton(
                                            onClick = {
                                                val next = (selectedMinute / 5 - 1) * 5
                                                selectedMinute = if (next < 0) 55 else next
                                            },
                                            modifier = Modifier.size(34.dp).bounceClick(),
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = str("Dakika Azalt", "Decrease Minute"), modifier = Modifier.size(18.dp))
                                        }

                                        Text(
                                            text = str("Dakika", "Minute"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Quick Presets
                                val presets = listOf(
                                    Pair(7, 0) to "07:00",
                                    Pair(7, 30) to "07:30",
                                    Pair(8, 0) to str("08:00 (Varsayılan)", "08:00 (Default)"),
                                    Pair(8, 30) to "08:30",
                                    Pair(9, 0) to "09:00"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    presets.forEach { (time, label) ->
                                        val (h, m) = time
                                        val isChipSelected = selectedHour == h && selectedMinute == m
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (isChipSelected) {
                                                HealthColors.Primary.copy(alpha = 0.22f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            },
                                            border = BorderStroke(
                                                width = if (isChipSelected) 1.5.dp else 1.dp,
                                                color = if (isChipSelected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .clickable {
                                                    selectedHour = h
                                                    selectedMinute = m
                                                }
                                                .bounceClick()
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isChipSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isChipSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Options List
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        WeightReminderFrequency.entries.forEach { option ->
                            val isSelected = option == selectedFrequency
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) {
                                    HealthColors.Primary.copy(alpha = if (isDark) 0.18f else 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedFrequency = option }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) HealthColors.Primary else Color.Transparent
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = HealthColors.OnPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.localizedTitle(lang),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) HealthColors.Primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val timeSuffix = if (option == WeightReminderFrequency.OFF) "" else {
                                            val h = selectedHour.toString().padStart(2, '0')
                                            val m = selectedMinute.toString().padStart(2, '0')
                                            " ($h:$m)"
                                        }
                                        val desc = when (option) {
                                            WeightReminderFrequency.EVERY_3_DAYS -> str("Son tartımından sonra 3 gün geçtiğinde hatırlatır$timeSuffix.", "Reminds you 3 days after your last weigh-in$timeSuffix.")
                                            WeightReminderFrequency.WEEKLY -> str("Haftada 1 kez tartılma hatırlatması yapar$timeSuffix.", "Sends a weigh-in reminder once a week$timeSuffix.")
                                            WeightReminderFrequency.DAILY -> str("Her gün tartılma hatırlatması yapar$timeSuffix.", "Sends a daily weigh-in reminder$timeSuffix.")
                                            WeightReminderFrequency.EVERY_2_DAYS -> str("2 günde bir tartılma hatırlatması yapar$timeSuffix.", "Sends a weigh-in reminder every 2 days$timeSuffix.")
                                            WeightReminderFrequency.OFF -> str("Tartı hatırlatıcısı gönderilmez.", "No weigh-in reminders will be sent.")
                                        }
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(str("Vazgeç", "Cancel"))
                        }

                        Button(
                            onClick = { onSave(selectedFrequency, selectedHour, selectedMinute) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .bounceClick(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HealthColors.Primary)
                        ) {
                            Text(str("Kaydet", "Save"), color = HealthColors.OnPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility overload for callers who only pass a frequency callback.
 */
@Composable
fun WeightReminderDialog(
    currentFrequency: WeightReminderFrequency,
    onDismiss: () -> Unit,
    onSave: (WeightReminderFrequency) -> Unit
) = WeightReminderDialog(
    currentFrequency = currentFrequency,
    initialHour = 8,
    initialMinute = 0,
    onDismiss = onDismiss,
    onSave = { freq, _, _ -> onSave(freq) }
)
