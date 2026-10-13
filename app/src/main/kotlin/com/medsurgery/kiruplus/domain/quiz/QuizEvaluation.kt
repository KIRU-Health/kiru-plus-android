package com.medsurgery.kiruplus.domain.quiz

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Vocabulario cerrado de la RPC `record_quiz_evaluation`:
 * `answered`, `omitted`, `timed_out`. El quiz de hoy solo produce `answered`.
 */
enum class QuizResponseStatus(val wire: String) {
    Answered("answered"),
    Omitted("omitted"),
    TimedOut("timed_out"),
}

/** Intento mínimo: identificadores y huellas. Sin texto del ítem. */
data class QuizEvaluationAttempt(
    val attemptId: String,
    val contentId: String,
    val contentVersion: String,
    val selectedOptionId: String?,
    val responseStatus: QuizResponseStatus,
    val occurredAtEpochMillis: Long,
)

/**
 * Firma SQL de `record_quiz_evaluation`. Los nombres no se renombran.
 * `p_selected_option_id` viaja siempre, con null si no hubo opción.
 */
data class RecordQuizEvaluationParams(
    val attemptId: String,
    val contentId: String,
    val contentVersion: String,
    val selectedOptionId: String?,
    val responseStatus: String,
    val occurredAt: String,
) {
    fun toRpcJson(): JsonObject =
        buildJsonObject {
            put(ATTEMPT_ID, attemptId)
            put(CONTENT_ID, contentId)
            put(CONTENT_VERSION, contentVersion)
            if (selectedOptionId == null) {
                put(SELECTED_OPTION_ID, JsonNull)
            } else {
                put(SELECTED_OPTION_ID, selectedOptionId)
            }
            put(RESPONSE_STATUS, responseStatus)
            put(OCCURRED_AT, occurredAt)
        }

    companion object {
        const val ATTEMPT_ID = "p_attempt_id"
        const val CONTENT_ID = "p_content_id"
        const val CONTENT_VERSION = "p_content_version"
        const val SELECTED_OPTION_ID = "p_selected_option_id"
        const val RESPONSE_STATUS = "p_response_status"
        const val OCCURRED_AT = "p_occurred_at"

        private val iso8601: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)

        fun from(attempt: QuizEvaluationAttempt): RecordQuizEvaluationParams =
            RecordQuizEvaluationParams(
                attemptId = attempt.attemptId.lowercase(),
                contentId = attempt.contentId,
                contentVersion = attempt.contentVersion,
                selectedOptionId = attempt.selectedOptionId,
                responseStatus = attempt.responseStatus.wire,
                occurredAt = iso8601.format(Instant.ofEpochMilli(attempt.occurredAtEpochMillis)),
            )
    }
}

const val QUIZ_EVALUATION_RPC = "record_quiz_evaluation"
private const val CANONICAL_PREFIX = "Q-"

/**
 * Solo el banco de KIRU+ tiene clave en el servidor: `Q-` + dígitos ASCII,
 * o un id numérico ASCII legado (`2`…`327`).
 */
fun isReportableContentId(contentId: String): Boolean {
    val body = if (contentId.startsWith(CANONICAL_PREFIX)) contentId.drop(CANONICAL_PREFIX.length) else contentId
    return body.isNotEmpty() && body.all { digit -> digit in '0'..'9' }
}

/**
 * Mismo armado que `QuizManager.reportLearningEvaluation`: huellas, no texto.
 * Devuelve null si el id no es del banco o la correcta no está entre las opciones.
 */
fun quizEvaluationAttempt(
    question: QuizQuestion,
    selectedAnswer: String?,
    status: QuizResponseStatus,
    attemptId: String,
    occurredAtEpochMillis: Long,
): QuizEvaluationAttempt? {
    val reportable = isReportableContentId(question.id) && question.correctAnswer in question.options
    if (!reportable) return null
    return QuizEvaluationAttempt(
        attemptId = attemptId,
        contentId = question.id,
        contentVersion =
            QuizContentFingerprint.contentVersion(
                stem = question.questionText,
                options = question.options,
                correct = question.correctAnswer,
            ),
        selectedOptionId = selectedAnswer?.let(QuizContentFingerprint::optionId),
        responseStatus = status,
        occurredAtEpochMillis = occurredAtEpochMillis,
    )
}
