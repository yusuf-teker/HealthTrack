package com.yusufteker.healthtrack.feature.premium.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.core.model.PaymentMethod
import com.yusufteker.healthtrack.core.model.SubscriptionPlan
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.ui.components.GlassCard
import com.yusufteker.healthtrack.ui.components.GradientText
import com.yusufteker.healthtrack.ui.components.clearFocusOnTap

private val PremiumGold = Color(0xFFF59E0B)
private val PremiumAmber = Color(0xFFD97706)
private val PremiumGradient = listOf(Color(0xFFF59E0B), Color(0xFFEC4899))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    state: PremiumState,
    onIntent: (PremiumIntent) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    GradientText(
                        text = "HealthTrack Premium",
                        colors = PremiumGradient,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onIntent(PremiumIntent.OnBackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onIntent(PremiumIntent.OnRefreshStatus) },
                        enabled = !state.isSyncing
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Durumu Yenile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .clearFocusOnTap()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(PremiumGradient.map { it.copy(alpha = 0.2f) })),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = PremiumGold,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (state.isPremium) "HealthTrack Premium Aktif" else "Bio-Telemetri Deneyimini Yükselt",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (state.isPremium) {
                            val untilText = state.premiumUntil?.take(10)
                            if (untilText != null) "Bitiş Tarihi: $untilText" else "Süresiz / Aktif Üyelik"
                        } else {
                            "AI destekli öğün planlama, sınırsız yemek fotoğrafı analizi ve haftalık bio-telemetri raporları."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Info / Error banners
            state.infoMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HealthColors.GradientPrimary.first().copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, HealthColors.GradientPrimary.first().copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onIntent(PremiumIntent.OnDismissMessages) }) {
                            Text("Tamam")
                        }
                    }
                }
            }

            state.errorMessage?.let { err ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Features List
            Text(
                text = "Premium Ayrıcalıkları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    state.features.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PremiumGold.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = mapFeatureIcon(feature.iconName),
                                    contentDescription = null,
                                    tint = PremiumGold,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = feature.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (!state.isPremium) {
                Text(
                    text = "Abonelik Planını Seç",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                state.availablePlans.forEach { plan ->
                    PlanOptionCard(
                        plan = plan,
                        isSelected = plan.id == state.selectedPlan.id,
                        onClick = { onIntent(PremiumIntent.OnPlanSelected(plan)) }
                    )
                }

                Button(
                    onClick = { onIntent(PremiumIntent.OnStartPurchaseClicked) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumAmber,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text(
                        text = "${state.selectedPlan.priceText} ile Başla",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = { onIntent(PremiumIntent.OnRestorePurchasesClicked) },
                    enabled = !state.isRestoring,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    if (state.isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Satın Alımları Geri Yükle")
                }
            }
        }
    }

    if (state.showPaymentSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onIntent(PremiumIntent.OnDismissPaymentSheet) },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Ödeme Yöntemi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.selectedPlan.name} • ${state.selectedPlan.priceText}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val methods = if (state.isIosPlatform) {
                    listOf(PaymentMethod.APPLE_PAY to "Apple App Store / Apple Pay", PaymentMethod.CREDIT_CARD to "Kredi / Banka Kartı")
                } else {
                    listOf(PaymentMethod.GOOGLE_PLAY to "Google Play Billing", PaymentMethod.CREDIT_CARD to "Kredi / Banka Kartı")
                }

                methods.forEach { (method, label) ->
                    val selected = state.selectedPaymentMethod == method
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) PremiumGold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntent(PremiumIntent.OnPaymentMethodSelected(method)) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = PremiumGold)
                                Text(text = label, fontWeight = FontWeight.Medium)
                            }
                            if (selected) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = PremiumGold)
                            }
                        }
                    }
                }

                Button(
                    onClick = { onIntent(PremiumIntent.OnConfirmPurchaseClicked) },
                    enabled = !state.isPurchasing,
                    colors = ButtonDefaults.buttonColors(containerColor = PremiumAmber, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (state.isPurchasing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Aboneliği Onayla", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PlanOptionCard(
    plan: SubscriptionPlan,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PremiumGold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) PremiumGold.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (plan.isPopular) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PremiumGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "%${plan.discountPercentage} Tasarruf",
                                style = MaterialTheme.typography.labelSmall,
                                color = PremiumGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = plan.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = plan.priceText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) PremiumGold else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun mapFeatureIcon(name: String): ImageVector {
    return when (name) {
        "auto_awesome" -> Icons.Default.AutoAwesome
        "restaurant" -> Icons.Default.Restaurant
        "insights" -> Icons.Default.Insights
        "cloud_done" -> Icons.Default.CloudDone
        "palette" -> Icons.Default.Palette
        else -> Icons.Default.WorkspacePremium
    }
}

