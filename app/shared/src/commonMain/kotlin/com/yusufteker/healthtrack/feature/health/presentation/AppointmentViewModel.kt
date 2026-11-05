package com.yusufteker.healthtrack.feature.health.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yusufteker.healthtrack.core.database.AttachmentStorage
import com.yusufteker.healthtrack.core.database.HealthDatabase
import com.yusufteker.healthtrack.core.model.AppointmentEntry
import com.yusufteker.healthtrack.core.model.AttachmentMetadata
import com.yusufteker.healthtrack.core.model.AttachmentType
import com.yusufteker.healthtrack.core.model.SyncStatus
import com.yusufteker.healthtrack.core.network.TokenStorage
import com.yusufteker.healthtrack.core.sync.SyncEngine
import com.yusufteker.healthtrack.core.utils.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AppointmentViewModel(
    private val database: HealthDatabase,
    private val attachmentStorage: AttachmentStorage,
    private val tokenStorage: TokenStorage,
    private val syncEngine: SyncEngine
) : ViewModel() {

    private val _state = MutableStateFlow(AppointmentState())
    val state: StateFlow<AppointmentState> = _state.asStateFlow()

    private val _effects = Channel<AppointmentEffect>(Channel.BUFFERED)
    val effects: Flow<AppointmentEffect> = _effects.receiveAsFlow()

    private var appointmentsJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStorage.sessionFlow.collect { session ->
                if (session.isLoggedIn) {
                    observeAppointments(session.userId)
                }
            }
        }
    }

    fun onIntent(intent: AppointmentIntent) {
        when (intent) {
            is AppointmentIntent.OnOpenAddDialog -> _state.update { it.copy(isAddDialogOpen = true) }
            is AppointmentIntent.OnCloseAddDialog -> _state.update { it.copy(isAddDialogOpen = false) }
            is AppointmentIntent.OnCreateAppointment -> createAppointment(
                intent.doctorName,
                intent.hospitalName,
                intent.department,
                intent.dateTime,
                intent.notes,
                intent.reminderMinutes
            )
            is AppointmentIntent.OnDeleteAppointment -> deleteAppointment(intent.id)
            is AppointmentIntent.OnOpenAttachmentDialog -> _state.update {
                it.copy(isAttachmentDialogOpen = true, selectedAppointmentId = intent.appointmentId)
            }
            is AppointmentIntent.OnCloseAttachmentDialog -> _state.update {
                it.copy(isAttachmentDialogOpen = false, selectedAppointmentId = null)
            }
            is AppointmentIntent.OnAddAttachment -> addAttachment(
                intent.appointmentId,
                intent.fileName,
                intent.type,
                intent.fileBytes
            )
            is AppointmentIntent.OnDeleteAttachment -> deleteAttachment(intent.appointmentId, intent.attachmentId)
            is AppointmentIntent.OnDismissErrorMessage -> _state.update { it.copy(errorMessage = null) }
            is AppointmentIntent.OnDismissInfoMessage -> _state.update { it.copy(infoMessage = null) }
        }
    }

    private fun observeAppointments(userId: Int) {
        appointmentsJob?.cancel()
        appointmentsJob = viewModelScope.launch {
            database.observeAppointments(userId).collect { list ->
                _state.update { it.copy(appointments = list.sortedBy { a -> a.dateTime }) }
            }
        }
    }

    private fun createAppointment(
        doctor: String,
        hospital: String,
        department: String?,
        dateTime: Long,
        notes: String?,
        reminderMinutes: Int
    ) {
        val userId = tokenStorage.getUserId()
        if (doctor.isBlank() || hospital.isBlank()) {
            _state.update { it.copy(errorMessage = "Doktor ve Kurum/Hastane adı zorunludur.") }
            return
        }

        viewModelScope.launch {
            val now = currentTime()
            val entry = AppointmentEntry(
                id = Uuid.random().toString(),
                userId = userId,
                dateTime = dateTime,
                doctorName = doctor.trim(),
                hospitalName = hospital.trim(),
                department = department?.trim()?.ifBlank { null },
                notes = notes?.trim()?.ifBlank { null },
                reminderMinutes = reminderMinutes,
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            database.upsertAppointment(entry)
            _state.update {
                it.copy(
                    isAddDialogOpen = false,
                    infoMessage = "Randevu eklendi ve Planora takvimine senkronize edildi."
                )
            }
            _effects.send(AppointmentEffect.ShowToast("Randevu oluşturuldu."))
            launch { syncEngine.sync() }
        }
    }

    private fun deleteAppointment(id: String) {
        viewModelScope.launch {
            database.deleteAppointment(id)
            _effects.send(AppointmentEffect.ShowToast("Randevu silindi."))
            launch { syncEngine.sync() }
        }
    }

    private fun addAttachment(
        appointmentId: String,
        fileName: String,
        type: AttachmentType,
        bytes: ByteArray
    ) {
        viewModelScope.launch {
            val path = attachmentStorage.saveAttachment(appointmentId, fileName, bytes)
            val appt = _state.value.appointments.find { it.id == appointmentId } ?: return@launch
            val now = currentTime()

            val attachment = AttachmentMetadata(
                id = Uuid.random().toString(),
                appointmentId = appointmentId,
                fileName = fileName,
                fileType = type,
                localFilePath = path,
                fileSizeBytes = bytes.size.toLong(),
                createdAt = now,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            val updated = appt.copy(
                attachments = appt.attachments + attachment,
                updatedAt = now,
                syncStatus = SyncStatus.PENDING
            )

            database.upsertAppointment(updated)
            _state.update { it.copy(isAttachmentDialogOpen = false, selectedAppointmentId = null) }
            _effects.send(AppointmentEffect.ShowToast("Belge yerel olarak eklendi."))
            launch { syncEngine.sync() }
        }
    }

    private fun deleteAttachment(appointmentId: String, attachmentId: String) {
        viewModelScope.launch {
            val appt = _state.value.appointments.find { it.id == appointmentId } ?: return@launch
            val attachment = appt.attachments.find { it.id == attachmentId }
            attachment?.let { attachmentStorage.deleteAttachment(it.localFilePath) }

            val updatedAttachments = appt.attachments.filter { it.id != attachmentId }
            val updated = appt.copy(
                attachments = updatedAttachments,
                updatedAt = currentTime(),
                syncStatus = SyncStatus.PENDING
            )
            database.upsertAppointment(updated)
        }
    }

    private fun currentTime(): Long = currentTimeMillis()
}
