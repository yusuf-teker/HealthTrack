package com.yusufteker.healthtrack.feature.health.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yusufteker.healthtrack.core.model.AttachmentType

@Composable
fun AddAttachmentDialog(
    onDismiss: () -> Unit,
    onSave: (fileName: String, type: AttachmentType, bytes: ByteArray) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AttachmentType.PDF) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reçete veya Rapor Belgesi Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Belgeler güvenliğiniz için yalnızca yerel cihazınızda saklanır ve sunucuya yüklenmez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Belge Adı (Örn: Kan Tahlili.pdf)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = selectedType == AttachmentType.PDF,
                        onClick = { selectedType = AttachmentType.PDF },
                        label = { Text("PDF Belgesi") }
                    )
                    FilterChip(
                        selected = selectedType == AttachmentType.IMAGE,
                        onClick = { selectedType = AttachmentType.IMAGE },
                        label = { Text("Reçete Resmi") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        // Dummy mock payload for local storage representation
                        val dummyBytes = "HealthTrack_Mock_Attachment_Data".encodeToByteArray()
                        onSave(fileName, selectedType, dummyBytes)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Yerel Olarak Ekle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
