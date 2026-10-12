package com.medsurgery.kiruplus.domain.logbook

import kotlinx.datetime.LocalDate

/**
 * Bitácora local. No habla con Supabase: iOS persiste
 * `surgical_logbook.json` y `surgical_logs` no admite INSERT de cliente.
 */
interface LogbookRepository {
    suspend fun fetchLogs(): Result<List<SurgicalLog>>

    suspend fun createLog(input: NewLogInput): Result<Unit>

    suspend fun deleteLog(id: String): Result<Unit>
}

data class NewLogInput(
    val procedureName: String,
    val procedureDate: LocalDate,
    val patientInitials: String,
    val medicalRecordNumber: String?,
    val role: SurgicalRole,
    val supervisorName: String,
    val notes: String,
    val durationMinutes: Int,
)
