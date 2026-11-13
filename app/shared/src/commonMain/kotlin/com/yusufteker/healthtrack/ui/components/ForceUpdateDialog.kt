package com.yusufteker.healthtrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yusufteker.healthtrack.core.model.AppVersionConfigDto
import com.yusufteker.healthtrack.getPlatform
import com.yusufteker.healthtrack.theme.HealthColors

@Composable
fun ForceUpdateDialog(
    config: AppVersionConfigDto,
    isForceUpdate: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current
    val isIos = getPlatform().name.contains("iOS", ignoreCase = true)
    val targetStoreUrl = if (isIos) config.storeUrlIos else config.storeUrlAndroid

    val titleText = config.forceUpdateTitleTr?.takeIf { it.isNotBlank() }
        ?: if (isForceUpdate) "Zorunlu Güncelleme" else "Yeni Güncelleme Mevcut"
    val messageText = config.forceUpdateMessageTr?.takeIf { it.isNotBlank() }
        ?: if (isForceUpdate) {
            "HealthTrack'i kullanmaya devam edebilmek için lütfen uygulamayı son sürüme güncelleyin."
        } else {
            "HealthTrack'in yeni bir sürümü yayınlandı. En iyi deneyim için güncelleyebilirsiniz."
        }

    Dialog(
        onDismissRequest = {
            if (!isForceUpdate) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForceUpdate,
            dismissOnClickOutside = !isForceUpdate
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    HealthColors.GradientPrimary.first().copy(alpha = 0.2f),
                                    HealthColors.GradientPrimary.last().copy(alpha = 0.15f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = HealthColors.GradientPrimary.first(),
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (targetStoreUrl.isNotBlank()) {
                            uriHandler.openUri(targetStoreUrl)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HealthColors.GradientPrimary.first(),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = "Şimdi Güncelle",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (!isForceUpdate) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        Text(text = "Daha Sonra")
                    }
                }
            }
        }
    }
}

