package com.medsurgery.kiruplus.data.quiz

import android.content.Context
import com.medsurgery.kiruplus.domain.quiz.LearningEvaluationConsentKeys
import com.medsurgery.kiruplus.domain.quiz.LearningEvaluationConsentReading
import com.medsurgery.kiruplus.domain.quiz.LearningEvaluationReporter
import com.medsurgery.kiruplus.domain.quiz.LearningEvaluationReporting
import com.medsurgery.kiruplus.domain.quiz.LearningEvaluationTransport
import com.medsurgery.kiruplus.domain.quiz.LearningEvaluationTransportError
import com.medsurgery.kiruplus.domain.quiz.QUIZ_EVALUATION_RPC
import com.medsurgery.kiruplus.domain.quiz.RecordQuizEvaluationParams
import com.medsurgery.kiruplus.domain.quiz.grantedConsentSubject
import com.medsurgery.kiruplus.domain.quiz.normalizeConsentSubject
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lee el almacén en cada llamada. Mismas claves que
 * `LearningEvaluationConsentService` en iOS. Sin hoja de consentimiento
 * todavía: el productor permanece apagado hasta una concesión `granted`.
 */
@Singleton
class PersistedLearningEvaluationConsent
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : LearningEvaluationConsentReading {
        private val preferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

        override fun grantedSubject(): String? =
            grantedConsentSubject(
                decision = preferences.getString(LearningEvaluationConsentKeys.DECISION, null),
                acceptedVersion = preferences.getInt(LearningEvaluationConsentKeys.VERSION, 0),
                subject = preferences.getString(LearningEvaluationConsentKeys.SUBJECT, null),
            )

        private companion object {
            const val PREFS_FILE = "kiruplus_learning_evaluation_consent"
        }
    }

@Singleton
class SupabaseLearningEvaluationTransport
    @Inject
    constructor(
        private val supabase: SupabaseClient,
    ) : LearningEvaluationTransport {
        override suspend fun adjudicatedSubject(): String? =
            normalizeConsentSubject(
                supabase.auth.currentUserOrNull()?.id,
            )

        override suspend fun record(
            params: RecordQuizEvaluationParams,
            subject: String,
        ) {
            val live = adjudicatedSubject()
            if (live == null || live != subject) {
                throw LearningEvaluationTransportError.SubjectMismatch()
            }
            supabase.postgrest.rpc(
                function = QUIZ_EVALUATION_RPC,
                parameters = params.toRpcJson(),
            )
        }
    }

@Singleton
class AppLearningEvaluationReporter
    @Inject
    constructor(
        consent: LearningEvaluationConsentReading,
        transport: LearningEvaluationTransport,
    ) : LearningEvaluationReporting by LearningEvaluationReporter(
            consent = consent,
            transport = transport,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            onSendFailure = { attemptPrefix, errorType ->
                Timber.d("KCC-11/5.7 send not applied (attempt %s): %s", attemptPrefix, errorType)
            },
        )

@Module
@InstallIn(SingletonComponent::class)
abstract class LearningEvaluationBindings {
    @Binds
    abstract fun bindConsent(impl: PersistedLearningEvaluationConsent): LearningEvaluationConsentReading

    @Binds
    abstract fun bindTransport(impl: SupabaseLearningEvaluationTransport): LearningEvaluationTransport

    @Binds
    abstract fun bindReporter(impl: AppLearningEvaluationReporter): LearningEvaluationReporting
}
