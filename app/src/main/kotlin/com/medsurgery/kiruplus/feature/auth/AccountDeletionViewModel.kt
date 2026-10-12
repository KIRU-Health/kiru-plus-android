package com.medsurgery.kiruplus.feature.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.core.auth.AuthRepository
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionReceipt
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionStatus
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionStatusKind
import com.medsurgery.kiruplus.domain.privacy.PrivacyError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Estados de presentación alineados con `AccountDeletionStatusViewModel` (iOS):
 * programado, cancelable (solo si el servidor manda `canCancel`) y no autenticado.
 * Un fallo de auth no se muestra como "sin solicitud".
 */
@HiltViewModel
class AccountDeletionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountDeletionUiState())
    val state: StateFlow<AccountDeletionUiState> = _state.asStateFlow()
    private var didTeardown = false

    init {
        loadStatus()
    }

    fun loadStatus() {
        _state.update { it.copy(phase = AccountDeletionPhase.Loading, isSubmitting = false, errorRes = null) }
        viewModelScope.launch {
            authRepository.accountDeletionStatus()
                .onSuccess { status -> _state.update { it.copy(phase = status.toPhase()) } }
                .onFailure { error -> _state.update { it.copy(phase = error.toPhase()) } }
        }
    }

    fun onAcknowledgeChange(acknowledged: Boolean) {
        _state.update { it.copy(acknowledged = acknowledged, errorRes = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.phase !is AccountDeletionPhase.None || !current.acknowledged || current.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, errorRes = null) }
        viewModelScope.launch {
            authRepository.requestAccountDeletion(reason = null)
                .onSuccess { receipt ->
                    _state.update {
                        it.copy(isSubmitting = false, phase = receipt.toScheduledPhase())
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isSubmitting = false, errorRes = error.toMessageRes())
                    }
                }
        }
    }

    fun cancelScheduledDeletion() {
        val scheduled = _state.value.phase as? AccountDeletionPhase.Scheduled ?: return
        if (!scheduled.canCancel || _state.value.isSubmitting) return
        _state.update { it.copy(isSubmitting = true, errorRes = null) }
        viewModelScope.launch {
            authRepository.cancelAccountDeletion()
                .onSuccess { loadStatus() }
                .onFailure { error ->
                    _state.update { it.copy(isSubmitting = false, errorRes = error.toMessageRes()) }
                }
        }
    }

    /** Cierra sesión una sola vez, y solo después de un agendado confirmado. */
    fun confirmTeardown() {
        if (didTeardown) return
        if (_state.value.phase !is AccountDeletionPhase.Scheduled) return
        didTeardown = true
        viewModelScope.launch {
            authRepository.signOut()
            _state.update { it.copy(completed = true) }
        }
    }
}

sealed interface AccountDeletionPhase {
    data object Loading : AccountDeletionPhase
    data object NotAuthenticated : AccountDeletionPhase
    data object None : AccountDeletionPhase
    data class Scheduled(
        val dateText: String,
        val graceHours: Int,
        val canCancel: Boolean,
    ) : AccountDeletionPhase
    data object Cancelled : AccountDeletionPhase
    data object FailedStatus : AccountDeletionPhase
    data object Unknown : AccountDeletionPhase
    data object TransportError : AccountDeletionPhase
}

data class AccountDeletionUiState(
    val phase: AccountDeletionPhase = AccountDeletionPhase.Loading,
    val acknowledged: Boolean = false,
    val isSubmitting: Boolean = false,
    val completed: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

private fun AccountDeletionStatus.toPhase(): AccountDeletionPhase = when (kind) {
    AccountDeletionStatusKind.None -> AccountDeletionPhase.None
    AccountDeletionStatusKind.Cancelled -> AccountDeletionPhase.Cancelled
    AccountDeletionStatusKind.Failed -> AccountDeletionPhase.FailedStatus
    AccountDeletionStatusKind.Unknown -> AccountDeletionPhase.Unknown
    AccountDeletionStatusKind.Scheduled -> AccountDeletionPhase.Scheduled(
        dateText = scheduledForIso?.let(::formatScheduledInstant).orEmpty(),
        graceHours = graceHours,
        canCancel = canCancel,
    )
}

private fun AccountDeletionReceipt.toScheduledPhase(): AccountDeletionPhase.Scheduled =
    AccountDeletionPhase.Scheduled(
        dateText = formatScheduledInstant(scheduledForIso),
        graceHours = graceHours,
        canCancel = canCancel,
    )

private fun Throwable.toPhase(): AccountDeletionPhase = when (this) {
    is PrivacyError.NotAuthenticated -> AccountDeletionPhase.NotAuthenticated
    else -> AccountDeletionPhase.TransportError
}

@StringRes
private fun Throwable.toMessageRes(): Int = when (this) {
    is PrivacyError.NotAuthenticated -> R.string.delete_account_not_authenticated
    else -> R.string.delete_account_status_error
}

private fun formatScheduledInstant(raw: String): String {
    val parsed = runCatching { OffsetDateTime.parse(raw) }.getOrNull() ?: return raw
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())
    return formatter.format(parsed)
}
