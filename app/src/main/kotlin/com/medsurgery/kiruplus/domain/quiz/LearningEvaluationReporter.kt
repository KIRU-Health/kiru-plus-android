package com.medsurgery.kiruplus.domain.quiz

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Lectura del almacén en cada envío. La concesión vigente devuelve el uid
 * en minúsculas; si no hay concesión, null. Apagado hasta consentimiento.
 */
interface LearningEvaluationConsentReading {
    fun grantedSubject(): String?
}

sealed class LearningEvaluationTransportError(message: String) : Exception(message) {
    class Unavailable : LearningEvaluationTransportError("unavailable")

    class SubjectMismatch : LearningEvaluationTransportError("subject_mismatch")
}

/**
 * Frontera de red. [adjudicatedSubject] es el uid de la sesión de ahora.
 * [record] vuelve a adjudicar y solo escribe si esa sesión es [subject].
 */
interface LearningEvaluationTransport {
    suspend fun adjudicatedSubject(): String?

    suspend fun record(
        params: RecordQuizEvaluationParams,
        subject: String,
    )
}

interface LearningEvaluationReporting {
    fun report(attempt: QuizEvaluationAttempt): Job?
}

/**
 * Productor de `record_quiz_evaluation`. No bloquea el quiz: el envío va
 * en una corrutina y un fallo solo se registra por tipo, sin contenido.
 */
class LearningEvaluationReporter(
    private val consent: LearningEvaluationConsentReading,
    private val transport: LearningEvaluationTransport,
    private val scope: CoroutineScope,
    private val onSendFailure: (attemptPrefix: String, errorType: String) -> Unit = { _, _ -> },
) : LearningEvaluationReporting {
    override fun report(attempt: QuizEvaluationAttempt): Job? {
        val subject = consent.grantedSubject()?.takeIf { isReportableContentId(attempt.contentId) }
        return subject?.let { bound -> launchRecord(attempt, bound) }
    }

    private fun launchRecord(
        attempt: QuizEvaluationAttempt,
        subject: String,
    ): Job {
        val params = RecordQuizEvaluationParams.from(attempt)
        val attemptPrefix = attempt.attemptId.take(ATTEMPT_LOG_PREFIX)
        return scope.launch {
            val live = transport.adjudicatedSubject()
            if (live != null && live == subject) {
                recordQuietly(params, subject, attemptPrefix)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun recordQuietly(
        params: RecordQuizEvaluationParams,
        subject: String,
        attemptPrefix: String,
    ) {
        try {
            transport.record(params, subject)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            onSendFailure(attemptPrefix, error.javaClass.simpleName)
        }
    }

    private companion object {
        const val ATTEMPT_LOG_PREFIX = 8
    }
}

/** Misma normalización que `LearningEvaluationConsentService.normalizedSubject`. */
fun normalizeConsentSubject(raw: String?): String? {
    val trimmed = raw?.trim()?.lowercase().orEmpty()
    return trimmed.ifEmpty { null }
}

/**
 * Concesión vigente: decisión `granted`, versión aceptada >= vigente, uid.
 * Versión actual del texto: 1, igual que iOS.
 */
fun grantedConsentSubject(
    decision: String?,
    acceptedVersion: Int,
    subject: String?,
    currentVersion: Int = LEARNING_EVALUATION_CONSENT_VERSION,
): String? {
    val accepted = decision == CONSENT_GRANTED && acceptedVersion >= currentVersion
    return if (accepted) normalizeConsentSubject(subject) else null
}

const val LEARNING_EVALUATION_CONSENT_VERSION = 1
const val CONSENT_GRANTED = "granted"

object LearningEvaluationConsentKeys {
    const val DECISION = "kiruplus.consent.learningEvaluation.decision"
    const val DECIDED_AT = "kiruplus.consent.learningEvaluation.decidedAt"
    const val VERSION = "kiruplus.consent.learningEvaluation.version"
    const val SUBJECT = "kiruplus.consent.learningEvaluation.subject"
}
