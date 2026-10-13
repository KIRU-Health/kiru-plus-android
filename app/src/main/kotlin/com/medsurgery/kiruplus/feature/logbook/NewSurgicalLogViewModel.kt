package com.medsurgery.kiruplus.feature.logbook

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.domain.logbook.LogbookRepository
import com.medsurgery.kiruplus.domain.logbook.LogbookSaveOutcome
import com.medsurgery.kiruplus.domain.logbook.NewLogInput
import com.medsurgery.kiruplus.domain.logbook.SurgicalRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class NewSurgicalLogViewModel @Inject constructor(
    private val repository: LogbookRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        NewLogUiState(
            procedureDate = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).date,
        ),
    )
    val state: StateFlow<NewLogUiState> = _state.asStateFlow()

    fun setProcedureName(value: String) {
        _state.update { it.copy(procedureName = value, errorRes = null) }
    }

    fun setProcedureDate(date: LocalDate) {
        _state.update { it.copy(procedureDate = date) }
    }

    fun setPatientInitials(value: String) {
        _state.update { it.copy(patientInitials = value, errorRes = null) }
    }

    fun setMedicalRecordNumber(value: String) {
        _state.update { it.copy(medicalRecordNumber = value) }
    }

    fun setRole(role: SurgicalRole) {
        _state.update { it.copy(role = role) }
    }

    fun setSupervisorName(value: String) {
        _state.update { it.copy(supervisorName = value, errorRes = null) }
    }

    fun setNotes(value: String) {
        _state.update { it.copy(notes = value) }
    }

    fun setDurationMinutes(value: String) {
        _state.update { it.copy(durationMinutes = value.filter { it.isDigit() }, errorRes = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting || current.saved) return
        val duration = current.durationMinutes.toIntOrNull() ?: 0
        val textMissing =
            listOf(
                current.procedureName,
                current.patientInitials,
                current.supervisorName,
            ).any { it.isBlank() }
        if (textMissing || duration <= 0) {
            _state.update { it.copy(errorRes = R.string.logbook_error_incomplete) }
            return
        }
        _state.update { it.copy(isSubmitting = true, errorRes = null, capacityMessage = null) }
        viewModelScope.launch {
            val input =
                NewLogInput(
                procedureName = current.procedureName,
                procedureDate = current.procedureDate,
                patientInitials = current.patientInitials,
                medicalRecordNumber = current.medicalRecordNumber.ifBlank { null },
                role = current.role,
                supervisorName = current.supervisorName,
                notes = current.notes,
                durationMinutes = duration,
                )
            repository.createLog(input)
                .onSuccess { outcome ->
                    when (outcome) {
                        is LogbookSaveOutcome.Created,
                        LogbookSaveOutcome.Updated,
                        -> _state.update { it.copy(isSubmitting = false, saved = true) }
                        is LogbookSaveOutcome.DeniedAtCapacity ->
                            _state.update {
                                it.copy(
                                    isSubmitting = false,
                                    capacityMessage = outcome.capacity.reachedMessage,
                                )
                            }
                    }
                }
                .onFailure {
                    _state.update { it.copy(isSubmitting = false, errorRes = R.string.logbook_error_save) }
                }
        }
    }
}

data class NewLogUiState(
    val procedureName: String = "",
    val procedureDate: LocalDate,
    val patientInitials: String = "",
    val medicalRecordNumber: String = "",
    val role: SurgicalRole = SurgicalRole.Cirujano,
    val supervisorName: String = "",
    val notes: String = "",
    val durationMinutes: String = "",
    val isSubmitting: Boolean = false,
    val saved: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val capacityMessage: String? = null,
)
