package com.medsurgery.kiruplus.data.quiz

import com.medsurgery.kiruplus.core.assets.AssetReader
import com.medsurgery.kiruplus.domain.quiz.QuizQuestion
import com.medsurgery.kiruplus.domain.quiz.QuizRepository
import com.medsurgery.kiruplus.domain.quiz.QuizSpecialty
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class QuestionBankFile(
    @SerialName("question_count") val questionCount: Int = 0,
    val questions: List<BankQuestion> = emptyList(),
)

@Serializable
internal data class BankQuestion(
    val number: String,
    val text: String,
    val options: List<String>,
    val correctIndex: Int,
    val specialtyIds: List<String> = emptyList(),
    val topicTags: List<String> = emptyList(),
    val explanation: String? = null,
)

internal fun BankQuestion.toQuizQuestion(): QuizQuestion? {
    val answer = options.getOrNull(correctIndex) ?: return null
    return QuizQuestion(
        id = number,
        questionText = text,
        options = options,
        correctAnswer = answer,
        explanation = explanation.orEmpty(),
        topic = topicTags.firstOrNull().orEmpty(),
        specialty = specialtyIds.firstOrNull().orEmpty(),
    )
}

internal fun QuestionBankFile.questionsFor(specialty: String): List<QuizQuestion> =
    questions.mapNotNull { row ->
        val quiz = row.toQuizQuestion() ?: return@mapNotNull null
        if (specialty.isNotEmpty() && specialty !in row.specialtyIds) return@mapNotNull null
        quiz
    }

internal fun QuestionBankFile.specialties(): List<QuizSpecialty> =
    questions
        .flatMap { row -> row.specialtyIds.map { id -> id to row.number } }
        .groupBy({ it.first }, { it.second })
        .map { (id, numbers) -> QuizSpecialty(name = id, questionCount = numbers.distinct().size) }
        .sortedBy { it.name }

@Singleton
class QuizRepositoryImpl
    @Inject
    constructor(
        private val assetReader: AssetReader,
    ) : QuizRepository {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    override suspend fun fetchSpecialties(): Result<List<QuizSpecialty>> =
        load().map { it.specialties() }

    override suspend fun fetchQuestions(specialty: String): Result<List<QuizQuestion>> =
        load().map { bank -> bank.questionsFor(specialty).shuffled() }

    private suspend fun load(): Result<QuestionBankFile> =
        runCatching {
        withContext(Dispatchers.IO) {
            val stream = assetReader.open(ASSET_PATH) ?: error("question bank asset missing")
            stream.use { json.decodeFromString<QuestionBankFile>(it.bufferedReader().readText()) }
        }
    }

    private companion object {
        const val ASSET_PATH = "preguntas/Questions.json"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class QuizRepositoryBindings {
    @Binds
    abstract fun bindQuizRepository(impl: QuizRepositoryImpl): QuizRepository
}
