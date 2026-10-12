package com.medsurgery.kiruplus.core.auth

import com.medsurgery.kiruplus.domain.privacy.AccountDeletionReceipt
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de autenticación KIRU+.
 * Implementación en data/repository/AuthRepositoryImpl.kt usa supabase-kt v3.
 *
 * Equivalente conceptual al SupabaseAuthService de iOS.
 */
interface AuthRepository {

    /** Estado de sesión observable. Emite en login, logout y refresh. */
    val sessionState: Flow<SessionState>

    suspend fun signIn(email: String, password: String): Result<Unit>

    suspend fun signUp(email: String, password: String): Result<Unit>

    suspend fun signOut(): Result<Unit>

    suspend fun resetPassword(email: String): Result<Unit>

    /**
     * Agenda la baja con el RPC `kiru_request_account_deletion` (gracia 48 h).
     * No llama a `process_account_deletions`: esa función solo acepta
     * credencial de máquina.
     */
    suspend fun requestAccountDeletion(reason: String? = null): Result<AccountDeletionReceipt>

    /** Revoca una baja programada. RPC `kiru_cancel_account_deletion`. */
    suspend fun cancelAccountDeletion(): Result<String>

    /**
     * Lee el estado. RPC `kiru_account_deletion_status`.
     * Sin sesión devuelve [com.medsurgery.kiruplus.domain.privacy.PrivacyError.NotAuthenticated],
     * nunca un estado "sin solicitud".
     */
    suspend fun accountDeletionStatus(): Result<AccountDeletionStatus>

    /**
     * Solicita exportación de datos personales del usuario (GDPR Art. 15 /
     * LFPDPPP Art. 23). Llama a Edge Function `process_data_export`, que
     * agrega los datos en un payload y lo deja en storage `gdpr-exports`.
     * El usuario recibe por email un signed URL para descarga.
     */
    suspend fun requestDataExport(): Result<Unit>
}

sealed interface SessionState {
    data object Unknown : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val userId: String, val email: String?) : SessionState
}
