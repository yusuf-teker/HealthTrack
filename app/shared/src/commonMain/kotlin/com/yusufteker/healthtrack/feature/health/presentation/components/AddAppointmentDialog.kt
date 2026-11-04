package com.yusufteker.healthtrack.feature.health.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onSave: (doctor: String, hospital: String, department: String?, dateTime: Long, notes: String?, reminder: Int) -> Unit
) {
    var doctorName by remember { mutableStateOf("") }
    var hospitalName by remember { mutableStateOf("") }
    var department by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var reminderMinutes by remember { mutableStateOf(60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Doktor Randevusu", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doktor Adı *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = hospitalName,
                    onValueChange = { hospitalName = it },
                    label = { Text("Kurum / Hastane *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Bölüm / Branş (Örn: Dahiliye)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notlar / Şikayetler") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3
                )

                Text(
                    text = "Hatırlatıcı: $reminderMinutes dakika önce",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (doctorName.isNotBlank() && hospitalName.isNotBlank()) {
                        // Default to tomorrow 10:00 AM epoch millis for demonstration
                        val targetTime = 1720000000000L + (24 * 3600 * 1000L)
                        onSave(doctorName, hospitalName, department, targetTime, notes, reminderMinutes)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
