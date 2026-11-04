package com.yusufteker.healthtrack.feature.health.presentation

import com.yusufteker.healthtrack.core.model.AppointmentEntry
import com.yusufteker.healthtrack.core.model.AttachmentMetadata
import com.yusufteker.healthtrack.core.model.AttachmentType

data class AppointmentState(
    val appointments: List<AppointmentEntry> = emptyList(),
    val isAddDialogOpen: Boolean = false,
    val isAttachmentDialogOpen: Boolean = false,
    val selectedAppointmentId: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null
)

sealed interface AppointmentIntent {
    data object OnOpenAddDialog : AppointmentIntent
    data object OnCloseAddDialog : AppointmentIntent
    data class OnCreateAppointment(
        val doctorName: String,
        val hospitalName: String,
        val department: String?,
        val dateTime: Long,
        val notes: String?,
        val reminderMinutes: Int
    ) : AppointmentIntent
    data class OnDeleteAppointment(val id: String) : AppointmentIntent
    data class OnOpenAttachmentDialog(val appointmentId: String) : AppointmentIntent
    data object OnCloseAttachmentDialog : AppointmentIntent
    data class OnAddAttachment(
        val appointmentId: String,
        val fileName: String,
        val type: AttachmentType,
        val fileBytes: ByteArray
    ) : AppointmentIntent
    data class OnDeleteAttachment(val appointmentId: String, val attachmentId: String) : AppointmentIntent
    data object OnDismissErrorMessage : AppointmentIntent
    data object OnDismissInfoMessage : AppointmentIntent
}

sealed interface AppointmentEffect {
    data class ShowToast(val message: String) : AppointmentEffect
}
