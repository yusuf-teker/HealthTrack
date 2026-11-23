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
                        badge = "Doğrudan Bluetooth (BLE) / Zepp Life / Mi Fitness",
                        accentColor = HealthColors.Primary,
                        icon = Icons.Rounded.MonitorWeight,
                        steps = if (isAppleHealth) {
                            listOf(
                                str("Doğrudan Bluetooth (Önerilen — Mi Scale 1 & 2): Kilo ekranındaki \"Tartıl\" butonuna basıp çıplak ayakla tartıya çıkın. Kilo, vücut yağ ve kas oranı aracı uygulama olmadan doğrudan kaydedilir.", "Direct Bluetooth (Recommended — Mi Scale 1 & 2): Tap the \"Scale\" button on the Weight screen and step onto the scale barefoot. Weight, body fat & muscle % capture directly with no intermediary app."),
                                str("Apple Sağlık Köprüsü (Zepp Life / Mi Fitness): iPhone'da Profil → Bağlı Uygulamalar → \"Apple Sağlık\" seçeneğini aktif edip Kilo, Vücut Yağ Oranı ve Egzersiz yazma izinlerini açın.", "Apple Health Bridge (Zepp Life / Mi Fitness): Open Profile → Connected Apps → enable \"Apple Health\" and allow Weight, Body Fat & Workout write access."),
                                str("Tartıya çıktığınızda veya saatiniz eşitlendiğinde veriler HealthTrack'e otomatik akar.", "Readings from your scale or watch sync into HealthTrack automatically via Apple Health.")
                            )
                        } else {
                            listOf(
                                str("Doğrudan Bluetooth (Önerilen — Mi Scale 1 & 2): Zepp Life veya Google Fit köprüsü beklemeden Kilo ekranındaki \"Tartıl\" butonuna basıp çıplak ayakla tartıya çıkın. Kilo, yağ ve kas oranı anında okunur.", "Direct Bluetooth (Recommended — Mi Scale 1 & 2): Skip Zepp Life/Google Fit delays by tapping \"Scale\" on the Weight screen and stepping onto the scale barefoot. Weight, fat & muscle % are captured instantly."),
                                str("Mi Fitness / Zepp (Yeni Nesil): Profil → Üçüncü Taraf Verileri → \"$platformName\" seçeneğini doğrudan aktif edin.", "Mi Fitness / Zepp: Profile → Third-Party Data → enable \"$platformName\" directly."),
                                str("Zepp Life (Eski Mi Fit): Profil → Hesap Ekle → \"Google Fit\" bağlantısını açın. Ardından Google Fit Ayarlar → \"Fit'i $platformName ile senkronize et\" (veya Health Sync) üzerinden aktarın.", "Zepp Life: Profile → Add Accounts → connect \"Google Fit\", then enable \"Sync Fit with $platformName\" in Google Fit Settings (or Health Sync).")
                            )
                        }
                    )
                }

                // 2: Huawei Watch
                if (selectedFilter == DeviceGuideFilter.ALL || selectedFilter == DeviceGuideFilter.HUAWEI) {
                    val directCloudStep = str(
                        "Doğrudan Huawei Sağlık Bağlantısı (Önerilen — Health Sync Gerekmez): Profil → Cihaz & Hesap → Akıllı Saat & Tartı → \"Huawei Sağlık ile Bağlan\" butonuna basıp Huawei kimliğinizle izin verin; adım, antrenman (Tenis, Koşu, Yoga vb.), nabız, kalori ve tartı verileriniz doğrudan aktarılır.",
                        "Direct Huawei Health Connection (Recommended — No Health Sync Needed): Go to Profile → Devices & Account → Smart Watch & Scale → \"Connect Huawei Health\" and authorize with your HUAWEI ID; steps, workouts (Tennis, Running, Yoga, etc.), heart rate, calories & weight sync directly."
                    )
                    GuideStepCard(
                        stepNumber = "2",
                        title = str("Huawei Watch (Tenis, Yoga, Adım & Nabız)", "Huawei Watch (Tennis, Yoga, Steps & HR)"),
                        badge = str("Doğrudan Huawei Bulut (Önerilen)", "Direct Huawei Cloud (Recommended)"),
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        icon = Icons.Rounded.DirectionsRun,
                        steps = if (isAppleHealth) {
                            listOf(
                                directCloudStep,
                                str("Alternatif Apple Sağlık Köprüsü: Huawei Sağlık'ta Ben → Gizlilik Yönetimi → Veri Paylaşımı → \"Apple Sağlık\" yolunu açıp Tüm Kategorileri (Adım, Egzersiz, Nabız, Kalori) etkinleştirin.", "Alternative Apple Health Bridge: In Huawei Health, open Me → Privacy Management → Data Sharing → \"Apple Health\" and enable Steps, Workouts, Heart Rate & Calories."),
                                str("Çakışmasız akıllı birleştirme sayesinde aynı antrenman iki kez sayılmaz.", "Conflict-free smart merging ensures the same workout is never double-counted.")
                            )
                        } else {
                            listOf(
                                directCloudStep,
                                str("Alternatif Yerel Köprü (Opsiyonel): İsterseniz Huawei Sağlık → Ben → Gizlilik Yönetimi → Veri Paylaşımı → \"$platformName\" veya Health Sync ile de veri aktarabilirsiniz.", "Alternative Local Bridge (Optional): You can also sync via Huawei Health → Me → Privacy Management → Data Sharing → \"$platformName\" or Health Sync."),
                                str("Saatten antrenman başlatmasanız bile günlük adım ve tahmini kalori otomatik çekilir; çakışmasız birleştirme ile çift sayım olmaz.", "Daily steps and active burn are imported automatically even without manual watch workouts, with conflict-free deduplication.")
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

