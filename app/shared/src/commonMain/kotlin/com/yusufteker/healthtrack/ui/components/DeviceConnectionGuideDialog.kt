package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.str

/**
 * Step-by-step interactive guide explaining how to connect Xiaomi smart scales
 * (Zepp Life / Mi Fitness) and Huawei smartwatches (Huawei Health / Health Sync)
 * with HealthTrack via Android Health Connect or Apple HealthKit.
 */
@Composable
fun DeviceConnectionGuideDialog(
    platformName: String = "Health Connect",
    onOpenHealthSettings: () -> Unit,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(HealthColors.Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Watch,
                        contentDescription = null,
                        tint = HealthColors.Primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = str("Tartı & Saat Nasıl Bağlanır?", "How to Connect Scale & Watch?"),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = str("$platformName Köprü Kurulum Rehberi", "$platformName Bridge Setup Guide"),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Step 1: Xiaomi Smart Scale
                GuideStepCard(
                    stepNumber = "1",
                    title = str("Xiaomi Akıllı Tartı (Kilo & Yağ Oranı)", "Xiaomi Smart Scale (Weight & Body Fat)"),
                    badge = "Zepp Life / Mi Fitness",
                    accentColor = HealthColors.Primary,
                    icon = Icons.Rounded.MonitorWeight,
                    steps = listOf(
                        str("Telefonunuzdaki Zepp Life veya Mi Fitness uygulamasını açın.", "Open the Zepp Life or Mi Fitness app on your phone."),
                        str("Profil → Üçüncü Taraf Verileri (veya Bağlı Uygulamalar) menüsüne girin.", "Go to Profile → Third-Party Data (or Connected Apps)."),
                        str("\"$platformName\" seçeneğini açıp Kilo ve Vücut Yağ Oranı izinlerini aktif edin.", "Select \"$platformName\" and enable Weight and Body Fat permissions."),
                        str("Tartıya çıktığınızda ölçümünüz otomatik olarak $platformName'e yazılır.", "Whenever you step on the scale, measurements sync automatically to $platformName.")
                    )
                )

                // Step 2: Huawei Watch
                GuideStepCard(
                    stepNumber = "2",
                    title = str("Huawei Watch (Adım, Spor & Kalori)", "Huawei Watch (Steps, Workouts & Calories)"),
                    badge = str("Huawei Sağlık veya Health Sync", "Huawei Health or Health Sync"),
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Rounded.DirectionsRun,
                    steps = listOf(
                        str("Yöntem A (Doğrudan): Huawei Sağlık → Ben → Gizlilik Yönetimi → Veri Paylaşımı ve Yetkilendirme → \"$platformName\" menüsünden Adım ve Egzersiz paylaşımını açın.", "Method A (Direct): Huawei Health → Me → Privacy Management → Data Sharing & Authorization → \"$platformName\", enable Steps and Workouts."),
                        str("Yöntem B (Geçmiş 30 Günü de Aktarmak İçin): Play Store'dan \"Health Sync\" uygulamasını açıp Kaynak: Huawei Health Kit, Hedef: \"$platformName\" seçin.", "Method B (Also sync past 30 days): Open \"Health Sync\" from Play Store and choose Source: Huawei Health Kit, Destination: \"$platformName\"."),
                        str("Saatten antrenman başlatmasanız bile günlük adım sayınız ve yakılan kaloriniz otomatik olarak çekilir.", "Your daily steps and burned calories are fetched automatically even without starting a watch workout.")
                    )
                )

                // Step 3: Conflict-free Merge
                GuideStepCard(
                    stepNumber = "3",
                    title = str("Çakışmasız Akıllı Birleştirme", "Conflict-Free Smart Merging"),
                    badge = str("Otomatik Koruma", "Auto Protection"),
                    accentColor = HealthColors.NutrientProtein,
                    icon = Icons.Rounded.VerifiedUser,
                    steps = listOf(
                        str("Aynı gün birden fazla tartım varsa en son ölçüm alınır ve yağ oranı korunur.", "If multiple weigh-ins occur on the same day, the latest reading is kept along with body fat."),
                        str("Aynı antrenman hem saat hem telefon tarafından kaydedilmişse 5 dk'lık zaman penceresiyle tekilleştirilir, kaloriler iki kez toplanmaz.", "Workouts logged by both watch and phone are deduplicated within a 5-min window so calories aren't double-counted."),
                        str("Uygulama içinden sildiğiniz kayıtlar tekrar senkronize edildiğinde geri gelmez.", "Records you delete inside the app will not come back on subsequent syncs.")
                    )
                )

                OutlinedButton(
                    onClick = onOpenHealthSettings,
                    modifier = Modifier.fillMaxWidth().bounceClick(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = str("$platformName İzin & Uygulama Ayarlarını Aç", "Open $platformName Permissions & App Settings"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onSyncNow()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HealthColors.Primary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(str("Şimdi Eşitle", "Sync Now"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(str("Kapat", "Close"))
            }
        }
    )
}

@Composable
private fun GuideStepCard(
    stepNumber: String,
    title: String,
    badge: String,
    accentColor: Color,
    icon: ImageVector,
    steps: List<String>
) {
    Surface(
        color = accentColor.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Surface(
                color = accentColor.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badge,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = accentColor
                )
            }

            steps.forEach { bullet ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

