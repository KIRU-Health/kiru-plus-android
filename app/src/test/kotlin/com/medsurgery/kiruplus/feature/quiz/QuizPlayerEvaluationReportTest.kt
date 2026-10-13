package com.medsurgery.kiruplus.feature.quiz

import androidx.lifecycle.SavedStateHandle
import com.medsurgery.kiruplus.MainDispatcherRule
import com.medsurgery.kiruplus.domain.quiz.LearningEvaluationReporting
import com.medsurgery.kiruplus.domain.quiz.QuizContentFingerprint
import com.medsurgery.kiruplus.domain.quiz.QuizEvaluationAttempt
import com.medsurgery.kiruplus.domain.quiz.QuizQuestion
import com.medsurgery.kiruplus.domain.quiz.QuizRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuizPlayerEvaluationReportTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `selectAnswer reports fingerprints and ignores a second tap or a foreign id`() =
        runTest {
            val specialty = "Cirugía general (General Surgery)"
            val repo: QuizRepository = mockk()
            val stem = "¿Cuál es el signo de Murphy?"
            val options = listOf("b opción", "a opción", "á opción", "Z opción")
            val bank =
                QuizQuestion(
                    id = "2",
                    questionText = stem,
                    options = options,
                    correctAnswer = "a opción",
                    explanation = "",
                    topic = "",
                    specialty = specialty,
                )
            coEvery { repo.fetchQuestions(specialty) } returns Result.success(listOf(bank))
            val reporter = RecordingReporter()
            val viewModel = QuizPlayerViewModel(repo, SavedStateHandle(mapOf("specialty" to specialty)), reporter)

            viewModel.selectAnswer("Z opción")
            viewModel.selectAnswer("a opción")

            assertEquals(1, reporter.attempts.size)
            assertEquals("2", reporter.attempts[0].contentId)
            assertEquals("97b39f120f01bdd0", reporter.attempts[0].contentVersion)
            assertEquals(QuizContentFingerprint.optionId("Z opción"), reporter.attempts[0].selectedOptionId)
            assertFalse(reporter.attempts[0].selectedOptionId?.contains("opción") ?: true)

            coEvery { repo.fetchQuestions(specialty) } returns Result.success(listOf(bank.copy(id = "question-7")))
            val skipped = RecordingReporter()
            val foreign =
                QuizPlayerViewModel(
                    repo,
                    SavedStateHandle(mapOf("specialty" to specialty)),
                    skipped,
                )
            foreign.selectAnswer("b opción")
            assertTrue(skipped.attempts.isEmpty())
        }

    private class RecordingReporter : LearningEvaluationReporting {
        val attempts = mutableListOf<QuizEvaluationAttempt>()

        override fun report(attempt: QuizEvaluationAttempt): Job? {
            attempts += attempt
            return null
        }
    }
}
