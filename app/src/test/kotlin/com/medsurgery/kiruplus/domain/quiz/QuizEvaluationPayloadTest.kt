package com.medsurgery.kiruplus.domain.quiz

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizEvaluationPayloadTest {
    @Test
    fun `params use rpc names lowercase uuid fractional utc and explicit null`() =
        runTest {
            val (reporter, _, spy) = reporter(consent = USER_A, live = USER_A)
            val job = reporter.report(attempt())
            assertTrue(job != null)
            job?.join()

            assertEquals(1, spy.recorded.size)
            val (params, subject) = spy.recorded[0]
            assertEquals(USER_A, subject)
            assertEquals("0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9", params.attemptId)
            assertEquals("2", params.contentId)
            assertEquals("5c70a69b9dea2c2d", params.contentVersion)
            assertEquals("3bb15e76b4a72eea", params.selectedOptionId)
            assertEquals("answered", params.responseStatus)
            assertEquals("2026-09-07T16:53:20.250Z", params.occurredAt)

            val json = params.toRpcJson()
            assertEquals(
                setOf(
                    "p_attempt_id",
                    "p_content_id",
                    "p_content_version",
                    "p_selected_option_id",
                    "p_response_status",
                    "p_occurred_at",
                ),
                json.keys,
            )
            val option = json["p_selected_option_id"]
            assertEquals("3bb15e76b4a72eea", option?.jsonPrimitive?.content)
        }

    @Test
    fun `missing option stays in the json as null`() {
        val params = RecordQuizEvaluationParams.from(attempt(option = null, status = QuizResponseStatus.Omitted))
        val json = params.toRpcJson()
        assertEquals(
            setOf(
                "p_attempt_id",
                "p_content_id",
                "p_content_version",
                "p_selected_option_id",
                "p_response_status",
                "p_occurred_at",
            ),
            json.keys,
        )
        assertTrue(json["p_selected_option_id"] is JsonNull)
        assertEquals("omitted", params.responseStatus)
        assertEquals("timed_out", QuizResponseStatus.TimedOut.wire)
        assertEquals("answered", QuizResponseStatus.Answered.wire)
    }

    @Test
    fun `timed out sends no option and the server vocabulary`() =
        runTest {
            val (reporter, _, spy) = reporter(consent = USER_A, live = USER_A)
            reporter.report(attempt(option = null, status = QuizResponseStatus.TimedOut))?.join()
            assertNull(spy.recorded.first().first.selectedOptionId)
            assertEquals("timed_out", spy.recorded.first().first.responseStatus)
        }

    @Test
    fun `canonical bank id is sent`() =
        runTest {
            val (reporter, _, spy) = reporter(consent = USER_A, live = USER_A)
            val job = reporter.report(attempt(id = "Q-0001"))
            assertTrue(job != null)
            job?.join()
            assertEquals("Q-0001", spy.recorded.single().first.contentId)
            assertEquals(USER_A, spy.recorded.single().second)
        }

    @Test
    fun `without consent nothing is built and the session is not read`() =
        runTest {
            val (reporter, consent, spy) = reporter(consent = null, live = USER_A)
            assertNull(reporter.report(attempt()))
            assertEquals(0, spy.recorded.size)
            assertEquals(0, spy.adjudications)
            assertEquals(1, consent.reads)
        }

    @Test
    fun `consent bound to another account sends nothing`() =
        runTest {
            val (reporter, _, spy) = reporter(consent = USER_A, live = USER_B)
            val job = reporter.report(attempt())
            assertTrue(job != null)
            job?.join()
            assertEquals(1, spy.adjudications)
            assertEquals(0, spy.recorded.size)
        }

    @Test
    fun `without a session nothing is sent`() =
        runTest {
            val (reporter, _, spy) = reporter(consent = USER_A, live = null)
            reporter.report(attempt())?.join()
            assertEquals(0, spy.recorded.size)
        }

    @Test
    fun `consent is reread on every call`() =
        runTest {
            val (reporter, consent, spy) = reporter(consent = USER_A, live = USER_A)
            reporter.report(attempt())?.join()
            consent.subject = null
            assertNull(reporter.report(attempt()))
            assertEquals(1, spy.recorded.size)
            assertEquals(2, consent.reads)
        }

    @Test
    fun `non bank ids are rejected even with consent`() =
        runTest {
            val (reporter, _, spy) = reporter(consent = USER_A, live = USER_A)
            val rejected =
                listOf(
                    "",
                    "question-1",
                    "12a",
                    "kirupardy:abc",
                    " 2",
                    "Q-",
                    "Q-abc",
                    "Q-12a",
                    "Q- 1",
                    "remote:abc-123",
                )
            rejected.forEach { id ->
                assertNull("accepted: $id", reporter.report(attempt(id = id)))
            }
            assertEquals(0, spy.recorded.size)
            assertTrue(isReportableContentId("2"))
            assertTrue(isReportableContentId("310"))
            assertTrue(isReportableContentId("Q-0001"))
            assertTrue(isReportableContentId("Q-1450"))
            assertFalse(isReportableContentId("٢"))
            assertFalse(isReportableContentId("Q-٢"))
        }

    @Test
    fun `transport failure does not escape the quiz`() =
        runTest {
            val (reporter, _, spy) = reporter(consent = USER_A, live = USER_A)
            spy.failure = IllegalStateException("boom")
            reporter.report(attempt())?.join()
            assertEquals(1, spy.recorded.size)
            spy.failure = LearningEvaluationTransportError.SubjectMismatch()
            reporter.report(attempt())?.join()
            assertEquals(2, spy.recorded.size)
        }

    @Test
    fun `select answer payload is fingerprints only`() {
        val stem = "¿Cuál es el signo de Murphy?"
        val options = listOf("b opción", "a opción", "á opción", "Z opción")
        val question =
            QuizQuestion(
                id = "2",
                questionText = stem,
                options = options,
                correctAnswer = "a opción",
                explanation = "",
                topic = "",
                specialty = "",
            )
        val attempt =
            quizEvaluationAttempt(
                question = question,
                selectedAnswer = "Z opción",
                status = QuizResponseStatus.Answered,
                attemptId = "0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9",
                occurredAtEpochMillis = EPOCH_MILLIS,
            )
        val params = RecordQuizEvaluationParams.from(checkNotNull(attempt))
        assertEquals("2", params.contentId)
        assertEquals("97b39f120f01bdd0", params.contentVersion)
        assertEquals(QuizContentFingerprint.optionId("Z opción"), params.selectedOptionId)
        assertEquals("answered", params.responseStatus)
        assertFalse(params.selectedOptionId?.contains("opción") ?: true)
        assertFalse(params.contentVersion == stem)
        assertNull(
            quizEvaluationAttempt(
                question = question.copy(id = "question-7"),
                selectedAnswer = "b opción",
                status = QuizResponseStatus.Answered,
                attemptId = "0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9",
                occurredAtEpochMillis = EPOCH_MILLIS,
            ),
        )
    }

    @Test
    fun `granted consent requires the current version and a subject`() {
        assertNull(grantedConsentSubject(decision = null, acceptedVersion = 0, subject = null))
        assertNull(grantedConsentSubject(decision = "denied", acceptedVersion = 1, subject = USER_A))
        assertNull(grantedConsentSubject(decision = "granted", acceptedVersion = 0, subject = USER_A))
        assertNull(grantedConsentSubject(decision = "granted", acceptedVersion = 1, subject = "   "))
        assertEquals(
            USER_A,
            grantedConsentSubject(decision = "granted", acceptedVersion = 1, subject = "  $USER_A  "),
        )
    }

    private fun attempt(
        id: String = "2",
        option: String? = "3bb15e76b4a72eea",
        status: QuizResponseStatus = QuizResponseStatus.Answered,
    ) = QuizEvaluationAttempt(
        attemptId = "0A1B2C3D-4E5F-6071-8293-A4B5C6D7E8F9",
        contentId = id,
        contentVersion = "5c70a69b9dea2c2d",
        selectedOptionId = option,
        responseStatus = status,
        occurredAtEpochMillis = EPOCH_MILLIS,
    )

    private fun reporter(
        consent: String?,
        live: String?,
    ): Triple<LearningEvaluationReporter, ConsentStub, TransportSpy> {
        val consentStub = ConsentStub(consent)
        val spy = TransportSpy(live)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        return Triple(LearningEvaluationReporter(consentStub, spy, scope), consentStub, spy)
    }

    private class ConsentStub(var subject: String?) : LearningEvaluationConsentReading {
        var reads: Int = 0

        override fun grantedSubject(): String? {
            reads += 1
            return subject
        }
    }

    private class TransportSpy(var liveSubject: String?) : LearningEvaluationTransport {
        val recorded = mutableListOf<Pair<RecordQuizEvaluationParams, String>>()
        var adjudications: Int = 0
        var failure: Exception? = null

        override suspend fun adjudicatedSubject(): String? {
            adjudications += 1
            return liveSubject
        }

        override suspend fun record(
            params: RecordQuizEvaluationParams,
            subject: String,
        ) {
            recorded += params to subject
            val error = failure
            if (error != null) throw error
        }
    }

    private companion object {
        const val USER_A = "0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9"
        const val USER_B = "ffffffff-0000-4000-8000-000000000002"
        const val EPOCH_MILLIS = 1_788_800_000_250L
    }
}
