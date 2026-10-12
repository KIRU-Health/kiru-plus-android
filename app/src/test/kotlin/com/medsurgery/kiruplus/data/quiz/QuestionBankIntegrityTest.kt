package com.medsurgery.kiruplus.data.quiz

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class QuestionBankIntegrityTest {
    @Test
    fun `bundled bank matches the iOS Questions json byte for byte`() {
        val file = locateBank()
        val digest = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        val hex = digest.joinToString("") { byte -> "%02x".format(byte) }
        assertEquals(IOS_QUESTIONS_SHA256, hex)
    }

    @Test
    fun `parser keeps the published stem and correct option`() {
        val bank =
            Json {
                ignoreUnknownKeys = true
            }.decodeFromString<QuestionBankFile>(
                locateBank().readText(),
            )
        assertEquals(PUBLISHED_COUNT, bank.questionCount)
        assertEquals(PUBLISHED_COUNT, bank.questions.size)
        val first = bank.questions.first()
        assertEquals("Q-0001", first.number)
        assertEquals(FIRST_STEM, first.text)
        val quiz = first.toQuizQuestion()
        assertTrue(quiz != null)
        if (quiz != null) {
            assertEquals(first.options[first.correctIndex], quiz.correctAnswer)
            assertEquals(FIRST_STEM, quiz.questionText)
        }
    }

    private fun locateBank(): File {
        val relative = "src/main/assets/preguntas/Questions.json"
        val candidates =
            listOf(
                File(relative),
                File("app/$relative"),
            )
        return candidates.firstOrNull { it.isFile }
            ?: error("Questions.json asset not found from ${File("").absolutePath}")
    }

    private companion object {
        const val PUBLISHED_COUNT = 1450
        const val IOS_QUESTIONS_SHA256 =
            "57263515457585b903a5426ffe9265dabfdecdc1f1173d219acf6a8b27a66de8"
        const val FIRST_STEM = "¿Qué define el síndrome de intestino corto en el adulto?"
    }
}
