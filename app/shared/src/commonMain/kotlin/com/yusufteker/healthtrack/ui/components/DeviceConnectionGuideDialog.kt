package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Step-by-step interactive guide explaining how to connect smart scales and smartwatches
 * (Xiaomi, Huawei, Samsung Health, Garmin/Amazfit/Polar, WearOS/Google Fit & Apple Watch)
 * with HealthTrack via Android Health Connect or Apple HealthKit.
 */
private enum class DeviceGuideFilter(val labelTr: String, val labelEn: String) {
    ALL("Tümü", "All"),
    XIAOMI("Xiaomi", "Xiaomi"),
    HUAWEI("Huawei", "Huawei"),
    SAMSUNG("Samsung", "Samsung"),
    GARMIN_ZEPP("Garmin / Amazfit", "Garmin / Amazfit"),
    WEAR_APPLE("WearOS / Apple", "WearOS / Apple")
}

@Composable
fun DeviceConnectionGuideDialog(
    platformName: String = "Health Connect",
    onOpenHealthSettings: () -> Unit,
    onSyncNow: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(DeviceGuideFilter.ALL) }
    val isAppleHealth = remember(platformName) {
        platformName.contains("Apple", ignoreCase = true)
    }

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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeviceGuideFilter.entries.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = {
                                Text(
                                    text = str(filter.labelTr, filter.labelEn),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        )
                    }
                }

                // 1: Xiaomi Smart Scale & Band
                if (selectedFilter == DeviceGuideFilter.ALL || selectedFilter == DeviceGuideFilter.XIAOMI) {
                    GuideStepCard(
                        stepNumber = "1",
                        title = str("Xiaomi Akıllı Tartı & Bileklik", "Xiaomi Smart Scale & Band"),
                        badge = "Zepp Life / Mi Fitness",
                        accentColor = HealthColors.Primary,
                        icon = Icons.Rounded.MonitorWeight,
                        steps = if (isAppleHealth) {
                            listOf(
                                str("iPhone'unuzda Zepp Life veya Mi Fitness uygulamasını açın.", "Open the Zepp Life or Mi Fitness app on your iPhone."),
                                str("Profil → Üçüncü Taraf Erişimi / Bağlı Uygulamalar → \"Apple Sağlık\" seçeneğini aktif edin.", "Go to Profile → Third-Party Access / Connected Apps → enable \"Apple Health\"."),
                                str("Açılan Apple Sağlık ekranında Kilo, Vücut Yağ Oranı, Adım ve Egzersiz yazma izinlerini verin.", "Grant write access for Weight, Body Fat, Steps, and Workouts in the Apple Health prompt."),
                                str("Tartıya çıktığınızda veya saatiniz eşitlendiğinde veriler HealthTrack'e otomatik akar.", "Readings from your scale or watch sync into HealthTrack automatically via Apple Health.")
                            )
                        } else {
                            listOf(
                                str("Telefonunuzdaki Zepp Life veya Mi Fitness uygulamasını açın.", "Open the Zepp Life or Mi Fitness app on your phone."),
                                str("Profil → Üçüncü Taraf Verileri (Bağlı Uygulamalar) → \"$platformName\" yolunu izleyin.", "Go to Profile → Third-Party Data (Connected Apps) → \"$platformName\"."),
                                str("Kilo, Vücut Yağ Oranı, Adım ve Egzersiz okuma/yazma izinlerini aktif edin.", "Enable Weight, Body Fat, Steps, and Workout sharing."),
                                str("Tartıya çıktığınızda ölçümünüz otomatik olarak $platformName üzerinden HealthTrack'e akar.", "Whenever you weigh in, readings sync automatically via $platformName.")
                            )
                        }
                    )
                }

                // 2: Huawei Watch
                if (selectedFilter == DeviceGuideFilter.ALL || selectedFilter == DeviceGuideFilter.HUAWEI) {
                    GuideStepCard(
                        stepNumber = "2",
                        title = str("Huawei Watch (Tenis, Yoga, Adım & Nabız)", "Huawei Watch (Tennis, Yoga, Steps & HR)"),
                        badge = if (isAppleHealth) str("Huawei Sağlık → Apple Sağlık", "Huawei Health → Apple Health") else str("Huawei Sağlık veya Health Sync", "Huawei Health or Health Sync"),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Rounded.DirectionsRun,
                        steps = if (isAppleHealth) {
                            listOf(
                                str("iPhone'da Huawei Sağlık uygulamasını açıp saat verilerinizin güncellendiğinden emin olun.", "Open Huawei Health on iPhone and make sure your watch has synced."),
                                str("Ben → Gizlilik Yönetimi → Veri Paylaşımı ve Yetkilendirme → \"Apple Sağlık\" yolunu açıp Tüm Kategorileri (Adım, Egzersiz, Nabız, Kalori) etkinleştirin.", "Go to Me → Privacy Management → Data Sharing and Authorization → \"Apple Health\" and enable Steps, Workouts, Heart Rate & Calories."),
                                str("Tenis, Yoga, Koşu antrenmanlarınız ile günlük adım, nabız ve aktif kalori verileriniz Apple Sağlık üzerinden HealthTrack'e otomatik gelir (ek uygulama gerekmez).", "Your Tennis, Yoga, Running workouts plus daily steps, heart rate & calories sync into HealthTrack via Apple Health automatically (no extra bridge app needed)."),
                                str("Veri görünmezse iPhone Ayarlar → Sağlık → Veri Erişimi ve Cihazlar → HealthTrack bölümünden okuma izinlerinin açık olduğunu kontrol edin.", "If data is missing, check iPhone Settings → Health → Data Access & Devices → HealthTrack and verify read toggles are ON.")
                            )
                        } else {
                            listOf(
                                str("Doğrudan Bağlantı: Huawei Sağlık → Ben → Gizlilik Yönetimi → Veri Paylaşımı → \"$platformName\" menüsünden Adım ve Egzersiz paylaşımını açın.", "Direct: Huawei Health → Me → Privacy Management → Data Sharing → \"$platformName\", enable Steps and Workouts."),
                                str("Geçmiş 30 Günü Aktarmak İçin: Play Store'dan \"Health Sync\" yükleyip Kaynak: Huawei Health Kit, Hedef: \"$platformName\" seçin.", "Past 30 Days: Install \"Health Sync\" from Play Store, pick Source: Huawei Health Kit, Destination: \"$platformName\"."),
                                str("Saatten antrenman başlatmasanız bile günlük adım ve tahmini kalori otomatik çekilir.", "Daily steps and active burn are imported automatically even without manual watch workouts.")
                            )
                        }
                    )
                }

                // 3: Samsung Health (Galaxy Watch & Scale)
                if (selectedFilter == DeviceGuideFilter.ALL || selectedFilter == DeviceGuideFilter.SAMSUNG) {
                    GuideStepCard(
                        stepNumber = "3",
                        title = str("Samsung Health (Galaxy Watch & Tartı)", "Samsung Health (Galaxy Watch & Scale)"),
                        badge = "Samsung Health → $platformName",
                        accentColor = Color(0xFF38BDF8),
                        icon = Icons.Rounded.Watch,
                        steps = listOf(
                            str("Samsung Health uygulamasını açın ve sağ üstten Menü → Ayarlar'a dokunun.", "Open Samsung Health and tap Menu → Settings in the top corner."),
                            str("\"$platformName\" seçeneğine girip Adım, Egzersiz, Kalp Atışı, Kilo ve Vücut Yağ izinlerini onaylayın.", "Tap \"$platformName\" and allow Steps, Exercise, Heart Rate, Weight, and Body Fat."),
                            str("Galaxy Watch antrenmanlarınız ve Bluetooth tartı ölçümleriniz otomatik eşitlenir.", "Your Galaxy Watch sessions and paired smart scale readings will sync automatically.")
                        )
                    )
                }

                // 4: Garmin, Amazfit & Polar
                if (selectedFilter == DeviceGuideFilter.ALL || selectedFilter == DeviceGuideFilter.GARMIN_ZEPP) {
                    GuideStepCard(
                        stepNumber = "4",
                        title = str("Garmin / Amazfit / Polar Saatler", "Garmin / Amazfit / Polar Watches"),
                        badge = "Zepp / Garmin Connect / Polar Flow",
                        accentColor = Color(0xFFF59E0B),
                        icon = Icons.Rounded.FitnessCenter,
                        steps = listOf(
                            str("Amazfit (Zepp): Profil → Hesap Ekle / Üçüncü Taraf Paylaşımından \"$platformName\" (veya Apple Sağlık) yetkisi verin.", "Amazfit (Zepp): Profile → Add Accounts / 3rd-Party Sharing → enable \"$platformName\" (or Apple Health)."),
                            str("Garmin Connect: Ayarlar → Bağlı Uygulamalar üzerinden \"$platformName\" / Apple Health entegrasyonunu açın (Garmin Android senkronu için gerekirse Health Sync köprüsünü kullanın).", "Garmin Connect: Settings → Connected Apps → enable \"$platformName\" / Apple Health (use Health Sync on Android if needed)."),
                            str("Polar Flow: Genel Ayarlar → \"$platformName\" / Apple Health bağlantısını aktif edin.", "Polar Flow: General Settings → activate \"$platformName\" / Apple Health sync.")
                        )
                    )
                }

                // 5: WearOS / Google Fit / Apple Watch
                if (selectedFilter == DeviceGuideFilter.ALL || selectedFilter == DeviceGuideFilter.WEAR_APPLE) {
                    GuideStepCard(
                        stepNumber = "5",
                        title = str("WearOS / Google Fit & Apple Watch", "WearOS / Google Fit & Apple Watch"),
                        badge = str("Doğrudan Sistem Köprüsü", "Native OS Bridge"),
                        accentColor = Color(0xFF10B981),
                        icon = Icons.Rounded.HealthAndSafety,
                        steps = listOf(
                            str("Google Pixel Watch / WearOS: Google Fit → Profil → Ayarlar → \"Fit'i $platformName ile senkronize et\" seçeneğini açın.", "Pixel Watch / WearOS: Google Fit → Profile → Settings → enable \"Sync Fit with $platformName\"."),
                            str("Apple Watch (iOS): Saat antrenman ve tartı verilerini otomatik Apple Sağlık'a yazar; iOS cihazda Sağlık izinlerini açmanız yeterlidir.", "Apple Watch (iOS): Watch automatically logs to Apple Health; simply allow HealthKit read permissions on iOS.")
                        )
                    )
                }

                // 6: Conflict-free Merge
                GuideStepCard(
                    stepNumber = if (selectedFilter == DeviceGuideFilter.ALL) "6" else "✓",
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

