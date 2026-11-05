package com.yusufteker.healthtrack.feature.health.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import io.github.aakira.napier.Napier
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.AppointmentEntry
import com.yusufteker.healthtrack.core.model.AttachmentType
import com.yusufteker.healthtrack.feature.health.presentation.components.AddAppointmentDialog
import com.yusufteker.healthtrack.feature.health.presentation.components.AddAttachmentDialog
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.HealthToastHost
import com.yusufteker.healthtrack.ui.components.ToastType
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun AppointmentScreen(
    state: AppointmentState,
    onIntent: (AppointmentIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(state.isAddDialogOpen) {
        if (state.isAddDialogOpen) Napier.d("SCREEN: AddAppointmentDialog açıldı")
    }
    LaunchedEffect(state.isAttachmentDialogOpen) {
        if (state.isAttachmentDialogOpen) Napier.d("SCREEN: AddAttachmentDialog açıldı")
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(AppointmentIntent.OnOpenAddDialog) },
                containerColor = HealthColors.Primary,
                contentColor = HealthColors.OnPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.bounceClick()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Randevu Ekle")
                    Text("Randevu Ekle", fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clearFocusOnTap()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── System Status Banner ("Clinical EMR Active & Planora Synced") ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                elevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(HealthColors.Primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(HealthColors.Primary)
                            )
                            Text(
                                text = "Klinik EMR & Planora Entegrasyonu",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = HealthColors.Primary
                            )
                        }
                        Text(
                            text = "Eklenen randevular Planora takviminiz ile otomatik eşitlenir. Reçeteler gizlilik nedeniyle sadece cihazınızda saklanır.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Basal Telemetry Glance Pods (Stitch Clinical Health Design) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ClinicalGlancePod(
                    title = "Dinlenik Nabız",
                    value = "58",
                    unit = "bpm",
                    badge = "Optimal",
                    badgeColor = HealthColors.Primary,
                    modifier = Modifier.weight(1f)
                )
                ClinicalGlancePod(
                    title = "Tansiyon",
                    value = "118/76",
                    unit = "mmHg",
                    badge = "Normal",
                    badgeColor = HealthColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
                ClinicalGlancePod(
                    title = "HRV İndeks",
                    value = "68",
                    unit = "ms",
                    badge = "Yüksek",
                    badgeColor = HealthColors.TertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Appointments List Header ──
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Yaklaşan Randevular",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(HealthColors.Primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${state.appointments.size} Randevu",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HealthColors.Primary
                        )
                    }
                }
            }

            if (state.appointments.isEmpty()) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    elevation = 2.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(42.dp)
                            )
                            Text(
                                text = "Kayıtlı doktor randevunuz bulunmuyor.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Sağ alttaki 'Randevu Ekle' butonundan ekleyebilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    items(state.appointments, key = { it.id }) { appt ->
                        LuxuryAppointmentCard(
                            appointment = appt,
                            onDelete = { onIntent(AppointmentIntent.OnDeleteAppointment(appt.id)) },
                            onAddAttachmentClick = { onIntent(AppointmentIntent.OnOpenAttachmentDialog(appt.id)) },
                            onDeleteAttachment = { aId -> onIntent(AppointmentIntent.OnDeleteAttachment(appt.id, aId)) }
                        )
                    }
                }
            }
        }

        // ── Floating Toast Notifications (Auto-dismissing) ──
        HealthToastHost(
            message = state.errorMessage,
            type = ToastType.ERROR,
            onDismiss = { onIntent(AppointmentIntent.OnDismissErrorMessage) }
        )
        HealthToastHost(
            message = state.infoMessage,
            type = ToastType.SUCCESS,
            onDismiss = { onIntent(AppointmentIntent.OnDismissInfoMessage) }
        )
    }
}

    // Add Dialog
    if (state.isAddDialogOpen) {
        AddAppointmentDialog(
            onDismiss = { onIntent(AppointmentIntent.OnCloseAddDialog) },
            onSave = { doc, hosp, dep, dt, n, rem ->
                onIntent(AppointmentIntent.OnCreateAppointment(doc, hosp, dep, dt, n, rem))
            }
        )
    }

    // Attachment Dialog
    if (state.isAttachmentDialogOpen && state.selectedAppointmentId != null) {
        AddAttachmentDialog(
            onDismiss = { onIntent(AppointmentIntent.OnCloseAttachmentDialog) },
            onSave = { name, type, bytes ->
                onIntent(AppointmentIntent.OnAddAttachment(state.selectedAppointmentId, name, type, bytes))
            }
        )
    }
}

/**
 * Clinical Glance Pod for Biomarkers.
 */
@Composable
private fun ClinicalGlancePod(
    title: String,
    value: String,
    unit: String,
    badge: String,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val isDark = LocalIsDarkTheme.current
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }
        }
    }
}

/**
 * Stitch Luxury Glowing Appointment Card with Distinct Date Pod.
 */
@Composable
private fun LuxuryAppointmentCard(
    appointment: AppointmentEntry,
    onDelete: () -> Unit,
    onAddAttachmentClick: () -> Unit,
    onDeleteAttachment: (String) -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val (monthName, dayNumber, timeStr) = formatAppointmentPod(appointment.dateTime)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Distinct Date Pod & Doctor Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Distinct Date Pod
                    Box(
                        modifier = Modifier
                            .width(58.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayNumber,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = monthName,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = HealthColors.Primary
                            )
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Doctor Details
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = appointment.doctorName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        val dept = appointment.department
                        if (!dept.isNullOrBlank()) {
                            Text(
                                text = dept,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = appointment.hospitalName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Optional Notes
            val notes = appointment.notes
            if (!notes.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Venue / Planora Sync Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE91E63).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Planora Takvimine Eşitlendi",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE91E63),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                TextButton(
                    onClick = onAddAttachmentClick,
                    modifier = Modifier.bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = HealthColors.Primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reçete / Belge",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = HealthColors.Primary
                    )
                }
            }

            // Local Attachments List
            if (appointment.attachments.isNotEmpty()) {
                HorizontalDivider(
                    thickness = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                Text(
                    text = "Yerel Ekli Belgeler (Cihazda Saklı):",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    appointment.attachments.forEach { att ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (att.fileType == AttachmentType.PDF) Icons.Default.Description else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = HealthColors.Primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = att.fileName,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onDeleteAttachment(att.id) },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Kaldır",
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatAppointmentPod(epochMillis: Long): Triple<String, String, String> {
    return try {
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        val ldt = instant.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val monthStr = when (ldt.monthNumber) {
            1 -> "OCA"
            2 -> "ŞUB"
            3 -> "MAR"
            4 -> "NİS"
            5 -> "MAY"
            6 -> "HAZ"
            7 -> "TEM"
            8 -> "AĞU"
            9 -> "EYL"
            10 -> "EKİ"
            11 -> "KAS"
            12 -> "ARA"
            else -> "AY"
        }
        val dayStr = ldt.dayOfMonth.toString()
        val hour = ldt.hour.toString().padStart(2, '0')
        val minute = ldt.minute.toString().padStart(2, '0')
        Triple(monthStr, dayStr, "$hour:$minute")
    } catch (_: Exception) {
        Triple("EKİ", "17", "10:30")
    }
}
