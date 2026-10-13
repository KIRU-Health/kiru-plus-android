package com.medsurgery.kiruplus.feature.quiz

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.domain.quiz.LearningEvaluationReporting
import com.medsurgery.kiruplus.domain.quiz.QuizEvaluationAttempt
import com.medsurgery.kiruplus.domain.quiz.QuizQuestion
import com.medsurgery.kiruplus.domain.quiz.QuizRepository
import com.medsurgery.kiruplus.domain.quiz.QuizResponseStatus
import com.medsurgery.kiruplus.domain.quiz.quizEvaluationAttempt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class QuizPlayerViewModel @Inject constructor(
    private val repository: QuizRepository,
    savedStateHandle: SavedStateHandle,
    private val learningEvaluation: LearningEvaluationReporting = QuietLearningEvaluation,
) : ViewModel() {

    // Read directly from SavedStateHandle to avoid Bundle serialization in unit tests.
    private val specialty: String = checkNotNull(savedStateHandle["specialty"])

    private val _state = MutableStateFlow(QuizPlayerUiState(specialty = specialty))
    val state: StateFlow<QuizPlayerUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            repository.fetchQuestions(specialty)
                .onSuccess { questions ->
                    if (questions.isEmpty()) {
                        _state.update { it.copy(isLoading = false, errorRes = R.string.quiz_empty) }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                questions = questions,
                                currentIndex = 0,
                                selectedAnswer = null,
                                showExplanation = false,
                                correctCount = 0,
                                finished = false,
                            )
                        }
                    }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, errorRes = R.string.quiz_error_load) }
                }
        }
    }

    fun selectAnswer(answer: String) {
        val s = _state.value
        if (s.selectedAnswer != null || s.finished) return
        val question = s.currentQuestion
        val isCorrect = answer == question?.correctAnswer
        if (question != null) {
            val attempt =
                quizEvaluationAttempt(
                    question = question,
                    selectedAnswer = answer,
                    status = QuizResponseStatus.Answered,
                    attemptId = UUID.randomUUID().toString(),
                    occurredAtEpochMillis = System.currentTimeMillis(),
                )
            if (attempt != null) learningEvaluation.report(attempt)
        }
        _state.update {
            it.copy(
                selectedAnswer = answer,
                showExplanation = true,
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
            )
        }
    }

    fun next() {
        val s = _state.value
        val nextIndex = s.currentIndex + 1
        if (nextIndex >= s.questions.size) {
            _state.update { it.copy(finished = true) }
        } else {
            _state.update {
                it.copy(
                    currentIndex = nextIndex,
                    selectedAnswer = null,
                    showExplanation = false,
                )
            }
        }
    }

    fun restart() {
        load()
    }
}

data class QuizPlayerUiState(
    val specialty: String,
    val isLoading: Boolean = true,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val showExplanation: Boolean = false,
    val correctCount: Int = 0,
    val finished: Boolean = false,
    @StringRes val errorRes: Int? = null,
) {
    val currentQuestion: QuizQuestion? get() = questions.getOrNull(currentIndex)
    val totalCount: Int get() = questions.size
    val progress: Float get() = if (totalCount == 0) 0f else (currentIndex + 1).toFloat() / totalCount
}

private object QuietLearningEvaluation : LearningEvaluationReporting {
    override fun report(attempt: QuizEvaluationAttempt): Job? = null
}
