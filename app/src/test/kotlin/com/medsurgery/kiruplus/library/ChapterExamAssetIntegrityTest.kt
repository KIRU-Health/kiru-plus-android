package com.medsurgery.kiruplus.library

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChapterExamAssetIntegrityTest {

    private val appRoot = File(
        javaClass.classLoader?.getResource(".")?.path.orEmpty(),
    ).let { testOutputDir ->
        generateSequence(testOutputDir.parentFile) { it.parentFile }
            .firstOrNull { File(it, "src/main/AndroidManifest.xml").exists() }
    }

    private val chapterExamsFile: File?
        get() = appRoot?.let { File(it, "src/main/assets/curriculum/ChapterExams.json") }

    private val json = Json { ignoreUnknownKeys = true }

    private fun loadExams(): JsonArray? {
        val file = chapterExamsFile ?: return null
        if (!file.exists()) return null
        return json.parseToJsonElement(file.readText()).jsonArray
    }

    @Test
    fun `ChapterExams asset exists`() {
        val file = chapterExamsFile ?: return
        assertTrue("ChapterExams.json missing from assets/curriculum/", file.exists())
    }

    @Test
    fun `ChapterExams is parseable JSON array`() {
        val exams = loadExams() ?: return
        assertTrue("Expected JSON array of exams, got empty", exams.size() > 0)
    }

    @Test
    fun `ChapterExams has exactly 100 chapters`() {
        val exams = loadExams() ?: return
        assertEquals("Expected 100 chapter exams", 100, exams.size())
    }

    @Test
    fun `all chapter exams have exactly 10 questions`() {
        val exams = loadExams() ?: return
        exams.forEachIndexed { idx, element ->
            val exam = element.jsonObject
            val id = exam["id"]?.jsonPrimitive?.content ?: "exam[$idx]"
            val questions = exam["questions"]?.jsonArray
                ?: error("Exam $id missing 'questions' field")
            assertEquals("Exam $id has ${questions.size()} questions, expected 10", 10, questions.size())
        }
    }

    @Test
    fun `all questions have non-empty prompt`() {
        val exams = loadExams() ?: return
        exams.forEach { element ->
            val exam = element.jsonObject
            val id = exam["id"]?.jsonPrimitive?.content ?: "?"
            (exam["questions"]?.jsonArray ?: error("Exam $id missing questions")).forEachIndexed { qIdx, q ->
                val prompt = q.jsonObject["prompt"]?.jsonPrimitive?.content ?: ""
                assertTrue("Exam $id question[$qIdx] has empty prompt", prompt.isNotBlank())
            }
        }
    }

    @Test
    fun `all questions have exactly 4 options`() {
        val exams = loadExams() ?: return
        exams.forEach { element ->
            val exam = element.jsonObject
            val id = exam["id"]?.jsonPrimitive?.content ?: "?"
            (exam["questions"]?.jsonArray ?: error("Exam $id missing questions")).forEachIndexed { qIdx, q ->
                val options = q.jsonObject["options"]?.jsonArray
                    ?: error("Exam $id question[$qIdx] missing 'options'")
                assertEquals("Exam $id question[$qIdx] has ${options.size()} options, expected 4", 4, options.size())
            }
        }
    }

    @Test
    fun `all questions have correctIndex in 0-3 range`() {
        val exams = loadExams() ?: return
        exams.forEach { element ->
            val exam = element.jsonObject
            val id = exam["id"]?.jsonPrimitive?.content ?: "?"
            (exam["questions"]?.jsonArray ?: error("Exam $id missing questions")).forEachIndexed { qIdx, q ->
                val correctIndex = q.jsonObject["correctIndex"]?.jsonPrimitive?.int
                    ?: error("Exam $id question[$qIdx] missing 'correctIndex'")
                assertTrue(
                    "Exam $id question[$qIdx] correctIndex=$correctIndex out of range [0,3]",
                    correctIndex in 0..3,
                )
            }
        }
    }

    @Test
    fun `all exams have non-empty id and title`() {
        val exams = loadExams() ?: return
        exams.forEachIndexed { idx, element ->
            val exam = element.jsonObject
            val id = exam["id"]?.jsonPrimitive?.content ?: ""
            val title = exam["title"]?.jsonPrimitive?.content ?: ""
            assertTrue("Exam[$idx] has empty id", id.isNotBlank())
            assertTrue("Exam $id has empty title", title.isNotBlank())
        }
    }

    private fun JsonArray.size() = this.size
    private fun JsonObject.size() = this.size
}
