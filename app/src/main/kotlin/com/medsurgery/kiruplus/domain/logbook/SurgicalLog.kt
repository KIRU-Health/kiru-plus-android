package com.medsurgery.kiruplus.domain.logbook

/**
 * Mismos campos que `SurgicalLogEntry` en
 * `Features/Logbook/SurgicalLogEntry.swift`. Vive solo en el dispositivo.
 */
data class SurgicalLog(
    val id: String,
    val dateIso: String,
    val procedureName: String,
    val patientInitials: String,
    val medicalRecordNumber: String?,
    val role: SurgicalRole,
    val supervisorName: String,
    val notes: String,
    val durationMinutes: Int,
)

enum class SurgicalRole(val wire: String) {
    Cirujano("cirujano"),
    Ayudante1("ayudante1"),
    Ayudante2("ayudante2"),
    Observador("observador"),
    ;

    companion object {
        fun fromWire(raw: String): SurgicalRole = entries.firstOrNull { it.wire == raw } ?: Cirujano
    }
}

fun sanitizedInitials(value: String): String =
    value.trim()
        .uppercase()
        .filter { it.isLetter() }
        .take(INITIALS_MAX_LETTERS)

private const val INITIALS_MAX_LETTERS = 4
