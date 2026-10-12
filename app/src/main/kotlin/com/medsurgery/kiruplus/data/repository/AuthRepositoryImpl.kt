package com.medsurgery.kiruplus.data.repository

import com.medsurgery.kiruplus.core.auth.AuthRepository
import com.medsurgery.kiruplus.core.auth.SessionState
import com.medsurgery.kiruplus.core.auth.toAuthError
import com.medsurgery.kiruplus.domain.privacy.ACCOUNT_DELETION_GRACE_HOURS
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionReceipt
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionStatus
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionStatusKind
import com.medsurgery.kiruplus.domain.privacy.PrivacyError
import com.medsurgery.kiruplus.domain.privacy.validateAccountDeletionStatus
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AuthRepository sobre supabase-kt v3.
 *
 * Errores se mapean a `AuthError` para que la UI muestre mensajes localizados
 * (ver core/auth/AuthError.kt). Endpoint Supabase: `tttxmupjteqpljtfgmgo.supabase.co`.
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override val sessionState: Flow<SessionState> =
        supabase.auth.sessionStatus.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    SessionState.SignedIn(
                        userId = user?.id.orEmpty(),
                        email = user?.email,
                    )
                }
                is SessionStatus.NotAuthenticated -> SessionState.SignedOut
                else -> SessionState.Unknown
            }
        }

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runAuth("signIn $email") {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runAuth("signUp $email") {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            Unit
        }

    override suspend fun signOut(): Result<Unit> =
        runAuth("signOut") { supabase.auth.signOut() }

    override suspend fun resetPassword(email: String): Result<Unit> =
        runAuth("resetPassword $email") {
            supabase.auth.resetPasswordForEmail(email)
        }

    override suspend fun requestAccountDeletion(reason: String?): Result<AccountDeletionReceipt> =
        runPrivacy("requestAccountDeletion") {
            val response = supabase.postgrest.rpc(
                function = "kiru_request_account_deletion",
                parameters = rpcJson.encodeToJsonElement(AccountDeletionParams(pReason = reason)).jsonObject,
            ).decodeAs<AccountDeletionRpcResponse>()
            AccountDeletionReceipt(
                status = response.status,
                scheduledForIso = response.scheduledFor,
                canCancel = response.canCancel,
                graceHours = response.graceHours ?: ACCOUNT_DELETION_GRACE_HOURS,
            )
        }

    override suspend fun cancelAccountDeletion(): Result<String> =
        runPrivacy("cancelAccountDeletion") {
            supabase.postgrest.rpc(
                function = "kiru_cancel_account_deletion",
                parameters = buildJsonObject { },
            ).decodeAs<CancelDeletionRpcResponse>().status
        }

    override suspend fun accountDeletionStatus(): Result<AccountDeletionStatus> =
        runPrivacy("accountDeletionStatus") {
            val response = supabase.postgrest.rpc(
                function = "kiru_account_deletion_status",
                parameters = buildJsonObject { },
            ).decodeAs<AccountDeletionStatusRpcResponse>()
            validateAccountDeletionStatus(
                kind = AccountDeletionStatusKind.fromRaw(response.status),
                scheduledForIso = response.scheduledFor,
                canCancel = response.canCancel,
                graceHours = response.graceHours,
            ) ?: throw PrivacyError.Contract
        }

    override suspend fun requestDataExport(): Result<Unit> =
        runAuth("requestDataExport") {
            supabase.functions.invoke("process_data_export")
            Unit
        }

    private suspend fun <T> runPrivacy(label: String, block: suspend () -> T): Result<T> {
        if (supabase.auth.currentUserOrNull() == null) {
            return Result.failure(PrivacyError.NotAuthenticated)
        }
        return try {
            Result.success(block())
        } catch (error: PrivacyError) {
            Result.failure(error)
        } catch (error: Throwable) {
            Timber.w(error, "%s failed", label)
            Result.failure(PrivacyError.Server(error.message ?: "privacy_rpc_failed"))
        }
    }

    private val rpcJson = Json { encodeDefaults = false }

    @Serializable
    private data class AccountDeletionParams(
        @SerialName("p_reason") val pReason: String? = null,
    )

    @Serializable
    private data class AccountDeletionRpcResponse(
        val status: String,
        @SerialName("scheduled_for") val scheduledFor: String,
        @SerialName("can_cancel") val canCancel: Boolean,
        @SerialName("grace_hours") val graceHours: Int? = null,
    )

    @Serializable
    private data class CancelDeletionRpcResponse(
        val status: String,
    )

    @Serializable
    private data class AccountDeletionStatusRpcResponse(
        val status: String,
        @SerialName("scheduled_for") val scheduledFor: String? = null,
        @SerialName("can_cancel") val canCancel: Boolean,
        @SerialName("grace_hours") val graceHours: Int,
        @SerialName("error_code") val errorCode: String? = null,
    )

    private inline fun runAuth(label: String, block: () -> Unit): Result<Unit> =
        try {
            block()
            Result.success(Unit)
        } catch (t: Throwable) {
            val mapped = t.toAuthError()
            Timber.w(t, "%s failed → %s", label, mapped::class.simpleName)
            Result.failure(mapped)
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryBindings {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
