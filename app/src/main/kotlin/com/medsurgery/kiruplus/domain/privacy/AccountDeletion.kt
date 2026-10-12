package com.medsurgery.kiruplus.domain.privacy

/**
 * Espejo de `AccountDeletionReceipt` y `AccountDeletionStatus` en
 * `Core/Compliance/PrivacyService.swift`. La gracia por defecto es 48 h
 * cuando el RPC de solicitud omite `grace_hours`.
 */
const val ACCOUNT_DELETION_GRACE_HOURS = 48

data class AccountDeletionReceipt(
    val status: String,
    val scheduledForIso: String,
    val canCancel: Boolean,
    val graceHours: Int,
)

enum class AccountDeletionStatusKind {
    None,
    Scheduled,
    Cancelled,
    Failed,
    Unknown,
    ;

    companion object {
        fun fromRaw(raw: String): AccountDeletionStatusKind = when (raw) {
            "none" -> None
            "scheduled" -> Scheduled
            "cancelled" -> Cancelled
            "failed" -> Failed
            else -> Unknown
        }
    }
}

data class AccountDeletionStatus(
    val kind: AccountDeletionStatusKind,
    val scheduledForIso: String?,
    val canCancel: Boolean,
    val graceHours: Int,
)

sealed class PrivacyError : Exception() {
    data object NotAuthenticated : PrivacyError()
    data object Contract : PrivacyError()
    data class Server(val detail: String) : PrivacyError()
}

fun validateAccountDeletionStatus(
    kind: AccountDeletionStatusKind,
    scheduledForIso: String?,
    canCancel: Boolean,
    graceHours: Int,
): AccountDeletionStatus? {
    if (graceHours <= 0) return null
    if (kind == AccountDeletionStatusKind.Scheduled && scheduledForIso.isNullOrBlank()) return null
    if (canCancel && kind != AccountDeletionStatusKind.Scheduled) return null
    if (canCancel && scheduledForIso.isNullOrBlank()) return null
    return AccountDeletionStatus(
        kind = kind,
        scheduledForIso = scheduledForIso,
        canCancel = canCancel,
        graceHours = graceHours,
    )
}
