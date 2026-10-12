package com.medsurgery.kiruplus.library

import com.medsurgery.kiruplus.core.assets.AssetReader
import com.medsurgery.kiruplus.data.chapterexam.AssetChapterExamRepositoryImpl
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ChapterExamRepositoryTest {

    private val sampleJson = """
        [
          {
            "id": "Chapter-001",
            "chapterNumber": 1,
            "title": "Historia y fundamentos",
            "questions": [
              {
                "id": "q001",
                "prompt": "¿Cuál es el principio del método científico?",
                "options": ["Observación", "Autoridad", "Tradición", "Intuición"],
                "correctIndex": 0,
                "rationale": "La ciencia comienza con la observación sistemática."
              }
            ]
          },
          {
            "id": "Chapter-002",
            "chapterNumber": 2,
            "title": "Ética médica",
            "questions": [
              {
                "id": "q002",
                "prompt": "¿Cuál es el principio de no maleficencia?",
                "options": ["Primum non nocere", "Beneficencia", "Justicia", "Autonomía"],
                "correctIndex": 0,
                "rationale": "Primum non nocere significa primero no hacer daño."
              }
            ]
          }
        ]
    """.trimIndent()

    private fun makeRepo(json: String? = sampleJson): AssetChapterExamRepositoryImpl {
        val assetReader: AssetReader = mockk()
        every { assetReader.open(any()) } answers {
            if (json != null) ByteArrayInputStream(json.toByteArray()) else null
        }
        return AssetChapterExamRepositoryImpl(assetReader)
    }

    @Test
    fun `getAvailableExamIds returns all ids from JSON`() = runTest {
        val repo = makeRepo()
        val result = repo.getAvailableExamIds().first()
        assertTrue(result.isSuccess)
        val ids = result.getOrThrow()
        assertEquals(setOf("Chapter-001", "Chapter-002"), ids)
    }

    @Test
    fun `getChapterExam returns correct exam by id`() = runTest {
        val repo = makeRepo()
        val result = repo.getChapterExam("Chapter-001").first()
        assertTrue(result.isSuccess)
        val exam = result.getOrThrow()
        val loaded = exam ?: error("expected chapter exam")
        assertEquals("Chapter-001", loaded.id)
        assertEquals("Historia y fundamentos", loaded.title)
        assertEquals(1, loaded.questions.size)
    }

    @Test
    fun `getChapterExam returns null for unknown id`() = runTest {
        val repo = makeRepo()
        val result = repo.getChapterExam("Chapter-999").first()
        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `getChapterExam returns failure when asset is missing`() = runTest {
        val repo = makeRepo(json = null)
        val result = repo.getChapterExam("Chapter-001").first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `getAvailableExamIds returns failure when asset is missing`() = runTest {
        val repo = makeRepo(json = null)
        val result = repo.getAvailableExamIds().first()
        assertTrue(result.isFailure)
    }

    @Test
    fun `caching prevents re-parsing on repeated calls`() = runTest {
        val assetReader: AssetReader = mockk()
        var openCallCount = 0
        every { assetReader.open(any()) } answers {
            openCallCount++
            ByteArrayInputStream(sampleJson.toByteArray())
        }
        val repo = AssetChapterExamRepositoryImpl(assetReader)

        repo.getChapterExam("Chapter-001").first()
        repo.getChapterExam("Chapter-002").first()
        repo.getAvailableExamIds().first()

        assertEquals("Asset should only be opened once due to caching", 1, openCallCount)
    }

    @Test
    fun `question fields are mapped correctly`() = runTest {
        val repo = makeRepo()
        val exam = repo.getChapterExam("Chapter-001").first().getOrThrow()
            ?: error("expected chapter exam")
        val question = exam.questions.first()
        assertEquals("q001", question.id)
        assertEquals("¿Cuál es el principio del método científico?", question.prompt)
        assertEquals(4, question.options.size)
        assertEquals(0, question.correctIndex)
        assertEquals("Observación", question.correctAnswer)
        assertTrue(question.rationale.isNotBlank())
    }
}
