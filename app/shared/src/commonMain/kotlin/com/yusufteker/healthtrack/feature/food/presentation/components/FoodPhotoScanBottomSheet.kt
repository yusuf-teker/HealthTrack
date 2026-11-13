package com.yusufteker.healthtrack.feature.food.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.i18n.trEn
import com.yusufteker.healthtrack.core.utils.CapturedPhoto
import com.yusufteker.healthtrack.core.utils.CropRect
import com.yusufteker.healthtrack.core.utils.rememberCameraPhotoLauncher
import com.yusufteker.healthtrack.feature.food.presentation.FoodScanTarget
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.appLang
import com.yusufteker.healthtrack.theme.str
import com.yusufteker.healthtrack.ui.components.ImageCropDialog
import com.yusufteker.healthtrack.ui.components.bounceClick
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPhotoScanBottomSheet(
    isAnalyzing: Boolean,
    initialTarget: FoodScanTarget = FoodScanTarget.DAILY_MEAL,
    onAnalyze: (textOrPrompt: String, imageBase64: String?, target: FoodScanTarget) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val lang = appLang()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cameraLauncher = rememberCameraPhotoLauncher()

    var selectedTarget by remember { mutableStateOf(initialTarget) }
    var capturedPhoto by remember { mutableStateOf<CapturedPhoto?>(null) }
    var isCropDialogOpen by remember { mutableStateOf(false) }

    var labelInputText by remember { mutableStateOf("") }
    var labelPortionInfo by remember { mutableStateOf(trEn("1 paket / 1 porsiyon", "1 pack / 1 serving", lang)) }
    var ocrFailedNoText by remember { mutableStateOf(false) }

    // Interactive Crop Dialog
    if (isCropDialogOpen && capturedPhoto?.imageBitmap != null) {
        ImageCropDialog(
            photo = capturedPhoto!!.imageBitmap!!,
            isProcessing = cameraLauncher.isScanning,
            onConfirmCrop = { cropRect: CropRect, rotation: Int ->
                cameraLauncher.cropAndScan(cropRect, rotation) { cropped ->
                    capturedPhoto = cropped
                    isCropDialogOpen = false
                    val text = cropped.extractedText
                    if (!text.isNullOrBlank()) {
                        labelInputText = text
                        ocrFailedNoText = false
                    } else {
                        if (labelInputText.isBlank()) {
                            labelInputText = trEn(
                                "Fotoğraftaki besin/ürün tablosu analizi",
                                "Nutrition/product table analysis from photo",
                                lang
                            )
                        }
                        ocrFailedNoText = false
                    }
                }
            },
            onScanFull = { rotation: Int ->
                cameraLauncher.cropAndScan(CropRect(0f, 0f, 1f, 1f), rotation) { fullPhoto ->
                    capturedPhoto = fullPhoto
                    isCropDialogOpen = false
                    val text = fullPhoto.extractedText
                    if (!text.isNullOrBlank()) {
                        labelInputText = text
                        ocrFailedNoText = false
                    } else {
                        if (labelInputText.isBlank()) {
                            labelInputText = trEn(
                                "Fotoğraftaki besin/ürün tablosu analizi",
                                "Nutrition/product table analysis from photo",
                                lang
                            )
                        }
                        ocrFailedNoText = false
                    }
                }
            },
            onDismiss = { isCropDialogOpen = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        containerColor = if (isDark) HealthColors.SurfaceContainerHigh else MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .clearFocusOnTap()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Top Header ──
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
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = HealthColors.Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (selectedTarget == FoodScanTarget.INVENTORY) {
                                str("Envanter & Etiket Okuyucu 🏷️", "Inventory & Label Scanner 🏷️")
                            } else {
                                str("Besin Etiketi / Fiş Okuyucu 📸", "Nutrition Label / Receipt Scanner 📸")
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedTarget == FoodScanTarget.INVENTORY) {
                                str(
                                    "Etiketi kırparak tarayın, envanterinize tam besin değerleriyle ekleyin",
                                    "Crop & scan the label to save it to your inventory with full nutrition values"
                                )
                            } else {
                                str(
                                    "Besin tablosu veya fişi kırparak tarayın",
                                    "Crop & scan a nutrition facts table or receipt"
                                )
                            },
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Target Switcher: Günlük Öğün vs Envanterim ──
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDark) HealthColors.SurfaceContainerLow else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TargetSegmentTab(
                        title = str("📅 Günlük Öğün", "📅 Daily Meal"),
                        subtitle = str("Bugünün günlüğüne ekle", "Add to today's log"),
                        isSelected = selectedTarget == FoodScanTarget.DAILY_MEAL,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTarget = FoodScanTarget.DAILY_MEAL }
                    )
                    TargetSegmentTab(
                        title = str("🏷️ Envanterim", "🏷️ Inventory"),
                        subtitle = str("Kalıcı ürün olarak ekle", "Save as reusable item"),
                        isSelected = selectedTarget == FoodScanTarget.INVENTORY,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTarget = FoodScanTarget.INVENTORY }
                    )
                }
            }

            // ── Ongoing Analysis Loading State Card ──
            AnimatedVisibility(
                visible = isAnalyzing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = HealthColors.Primary.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HealthColors.Primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = HealthColors.Primary
                        )
                        Column {
                            Text(
                                text = str("Yapay Zeka Analiz Ediyor...", "AI Is Analyzing..."),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (selectedTarget == FoodScanTarget.INVENTORY) {
                                    str(
                                        "Ürün isimleri, paket birimleri ve tüm makrolar envanter formatına dönüştürülüyor...",
                                        "Converting product names, package units, and macros into inventory format..."
                                    )
                                } else {
                                    str(
                                        "Etiketteki besin değerleri günlüğünüze aktarılıyor...",
                                        "Extracting nutrition values from the label into your log..."
                                    )
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Action Buttons: Camera & Gallery ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        cameraLauncher.launchCamera { photo ->
                            capturedPhoto = photo
                            if (photo.imageBitmap != null) {
                                isCropDialogOpen = true
                            }
                            if (!photo.extractedText.isNullOrBlank()) {
                                labelInputText = photo.extractedText
                                ocrFailedNoText = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.Primary,
                        contentColor = Color.White
                    ),
                    enabled = !cameraLauncher.isScanning
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        str("Fotoğraf Çek", "Take Photo"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        cameraLauncher.launchGallery { photo ->
                            capturedPhoto = photo
                            if (photo.imageBitmap != null) {
                                isCropDialogOpen = true
                            }
                            if (!photo.extractedText.isNullOrBlank()) {
                                labelInputText = photo.extractedText
                                ocrFailedNoText = false
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .bounceClick(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = HealthColors.Primary
                    ),
                    enabled = !cameraLauncher.isScanning
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        str("Galeriden Seç", "Choose from Gallery"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // ── Scanning OCR Progress Banner ──
            AnimatedVisibility(
                visible = cameraLauncher.isScanning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HealthColors.Primary.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = HealthColors.Primary
                        )
                        Text(
                            text = str(
                                "Besin tablosu yüksek çözünürlükle taranıyor (ML Kit OCR)...",
                                "Scanning nutrition table in high resolution (ML Kit OCR)..."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ── Captured Photo Preview (With Crop / Re-Crop Action) ──
            if (capturedPhoto?.imageBitmap != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDark) Color(0xFF141A28) else Color(0xFFF3F5FA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            bitmap = capturedPhoto!!.imageBitmap!!,
                            contentDescription = str("Çekilen Etiket", "Captured Label"),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            if (!ocrFailedNoText && labelInputText.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (capturedPhoto?.isCropped == true) {
                                            str("Kırpılmış Alan Okundu ✓", "Cropped Area Read ✓")
                                        } else {
                                            str("Besin Tablosu Okundu ✓", "Nutrition Table Read ✓")
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                Text(
                                    text = str(
                                        "Değerler aşağıdaki alana aktarıldı. Kontrol edip onaylayabilirsiniz.",
                                        "Values transferred below. Review and confirm."
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = str("Tablo Metni Bulunamadı", "No Table Text Found"),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Text(
                                    text = str(
                                        "Görsel net okunamadı. 'Alanı Kırp' butonuna basarak sadece tabloyu çerçeveleyin.",
                                        "Image couldn't be read clearly. Tap 'Crop Area' to frame only the table."
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Re-Crop Button
                        IconButton(
                            onClick = { isCropDialogOpen = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(HealthColors.Primary.copy(alpha = 0.12f))
                                .bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = str("Alanı Kırp", "Crop Area"),
                                tint = HealthColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── Quick Samples Chips for Labels ──
            Text(
                text = str("Hızlı Örnekler:", "Quick Examples:"),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SampleChip(str("🍫 Fellas Protein Bar (45g)", "🍫 Protein Bar (45g)")) {
                    labelInputText = trEn(
                        "Fellas Protein Bar (45g): 100g'da 375 kcal, 33g protein, 24g karb, 11g yağ, 16g lif, magnezyum 90mg",
                        "Protein Bar (45g): per 100g 375 kcal, 33g protein, 24g carbs, 11g fat, 16g fiber, magnesium 90mg",
                        lang
                    )
                    labelPortionInfo = trEn("1 adet (45g)", "1 bar (45g)", lang)
                    ocrFailedNoText = false
                }
                SampleChip(str("🥣 Yulaf Ezmesi (50g)", "🥣 Rolled Oats (50g)")) {
                    labelInputText = trEn(
                        "Eti Lifalif Yulaf: 100g için 356 kcal, 12.5g protein, 60g karb, 7g yağ, 10g lif, 120mg magnezyum",
                        "Rolled Oats: per 100g 356 kcal, 12.5g protein, 60g carbs, 7g fat, 10g fiber, 120mg magnesium",
                        lang
                    )
                    labelPortionInfo = trEn("50 gram", "50g", lang)
                    ocrFailedNoText = false
                }
                SampleChip(str("🥛 Proteinli Süt (330ml)", "🥛 Protein Milk (330ml)")) {
                    labelInputText = trEn(
                        "Pınar Protein Süt: 100ml'de 52 kcal, 7.8g protein, 4.8g karb, 0.1g yağ, 150mg kalsiyum, 1.2ug B12",
                        "Protein Milk: per 100ml 52 kcal, 7.8g protein, 4.8g carbs, 0.1g fat, 150mg calcium, 1.2ug B12",
                        lang
                    )
                    labelPortionInfo = trEn("1 şişe (330ml)", "1 bottle (330ml)", lang)
                    ocrFailedNoText = false
                }
                SampleChip(str("🧾 Market Fişi (3 Kalem)", "🧾 Grocery Receipt (3 Items)")) {
                    labelInputText = trEn(
                        "MİGROS TİC. A.Ş.\n1x Pınar Protein Süt 500ml\n1x Eti Lifalif Yulaf 500g\n1x Muz İthal 1kg",
                        "FRESH MARKET RECEIPT\n1x High-Protein Milk 500ml\n1x Rolled Oats 500g\n1x Bananas 1kg",
                        lang
                    )
                    labelPortionInfo = trEn("1 paket / adet", "1 pack / unit", lang)
                    ocrFailedNoText = false
                }
            }

            // ── Editable Label Text Field ──
            OutlinedTextField(
                value = labelInputText,
                onValueChange = {
                    labelInputText = it
                    if (it.isNotBlank()) ocrFailedNoText = false
                },
                label = {
                    Text(
                        if (selectedTarget == FoodScanTarget.INVENTORY) {
                            str("Taranan Envanter / Ürün Metni", "Scanned Inventory / Product Text")
                        } else {
                            str("Taranan / Girilen Besin Tablosu Metni", "Scanned / Entered Nutrition Label Text")
                        }
                    )
                },
                placeholder = {
                    Text(
                        str(
                            "Kameradan taranan veya elle girilen metin: 100g için 280 kcal, 22g protein, 18g karb, 9g yağ...",
                            "Text scanned from camera or entered manually: per 100g 280 kcal, 22g protein, 18g carbs, 9g fat..."
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                minLines = 3,
                maxLines = 6
            )

            // ── Consumed Portion Field (Relevant for Daily Meal) ──
            if (selectedTarget == FoodScanTarget.DAILY_MEAL) {
                OutlinedTextField(
                    value = labelPortionInfo,
                    onValueChange = { labelPortionInfo = it },
                    label = { Text(str("Tükettiğiniz Miktar / Porsiyon", "Consumed Amount / Serving")) },
                    placeholder = {
                        Text(
                            str(
                                "Örn: 1 paket (45g), yarım kutu veya 1 porsiyon",
                                "E.g.: 1 pack (45g), half carton, or 1 serving"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Primary Action Button: Extract & Calculate with AI ──
            Button(
                onClick = {
                    val promptBase = labelInputText.ifBlank {
                        trEn(
                            "Görseldeki ürün/besin değerleri ve kalori analizi",
                            "Product/nutrition values and calorie analysis from image",
                            lang
                        )
                    }
                    val fullPrompt = if (selectedTarget == FoodScanTarget.DAILY_MEAL) {
                        trEn(
                            "$promptBase (Tüketilen porsiyon: $labelPortionInfo)",
                            "$promptBase (Consumed serving: $labelPortionInfo)",
                            lang
                        )
                    } else {
                        promptBase
                    }
                    onAnalyze(fullPrompt, capturedPhoto?.base64, selectedTarget)
                },
                enabled = (labelInputText.isNotBlank() || capturedPhoto?.base64 != null) && !isAnalyzing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .bounceClick(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HealthColors.Primary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedTarget == FoodScanTarget.INVENTORY) {
                        str("AI ile Envanterime Çıkar ve Kaydet", "Extract & Save to Inventory with AI")
                    } else {
                        str("AI ile Besin Değerlerini Çıkar ve Ekle", "Extract & Add Nutrition with AI")
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TargetSegmentTab(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) HealthColors.Primary else Color.Transparent
    )
    val titleColor by animateColorAsState(
        if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
    )
    val subtitleColor by animateColorAsState(
        if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    )

    Surface(
        shape = RoundedCornerShape(11.dp),
        color = backgroundColor,
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .bounceClick()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Normal,
                color = subtitleColor
            )
        }
    }
}

@Composable
private fun SampleChip(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
