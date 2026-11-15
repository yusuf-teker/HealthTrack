package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yusufteker.healthtrack.theme.HealthColors
import com.yusufteker.healthtrack.theme.LocalIsDarkTheme
import com.yusufteker.healthtrack.theme.str

/**
 * Periodic rationale popup explaining why HealthTrack requests notification permissions
 * (13:30 meal reminder, evening workout & weigh-in nudges, instant friend activity pushes).
 */
@Composable
fun NotificationPermissionDialog(
    onAllowClick: () -> Unit,
    onLaterClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current

    AlertDialog(
        onDismissRequest = onLaterClick,
        containerColor = if (isDark) HealthColors.SurfaceDark else MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NotificationsActive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                text = str(
                    "Bildirimler Açık Kalırsa Neler Kazanırsın?",
                    "Why Turn On Notifications?"
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = str(
                        "Şu anda cihaz bildirim izni kapalı olduğu için hatırlatıcılar ve anlık arkadaş bildirimleri ulaşmıyor:",
                        "Notification permission is currently off, so reminders and live friend alerts cannot reach you:"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PermissionBenefitRow(
                    icon = Icons.Rounded.Restaurant,
                    tint = HealthColors.NutrientProtein,
                    title = str("13:30 Akıllı Öğün Kontrolü", "1:30 PM Smart Meal Nudge"),
                    subtitle = str(
                        "Öğle vaktine kadar kayıt girmediysen nazikçe hatırlatır.",
                        "Gently reminds you if you haven't logged a meal by midday."
                    )
                )

                PermissionBenefitRow(
                    icon = Icons.Rounded.FitnessCenter,
                    tint = HealthColors.TertiaryVariant,
                    title = str("Spor & Tartı Disiplini", "Workout & Weigh-in Rhythm"),
                    subtitle = str(
                        "Serini bozmaman için akşam sporu ve sabah tartı saatinde uyarır.",
                        "Alerts you at workout & morning scale times to protect your streak."
                    )
                )

                PermissionBenefitRow(
                    icon = Icons.Rounded.Group,
                    tint = Color(0xFF38BDF8),
                    title = str("Anlık Arkadaş Bildirimleri", "Instant Friend Activity"),
                    subtitle = str(
                        "Arkadaşların antrenman veya öğün eklediğinde anında haberin olur.",
                        "Get instant alerts when friends log workouts or meals."
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAllowClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.bounceClick()
            ) {
                Text(
                    text = str("Bildirimlere İzin Ver", "Allow Notifications"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onLaterClick) {
                Text(text = str("Şimdi Değil", "Not Now"))
            }
        }
    )
}

@Composable
private fun PermissionBenefitRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = tint.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
