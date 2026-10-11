package com.medsurgery.kiruplus.feature.chapterexam

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsurgery.kiruplus.domain.chapterexam.ChapterExam
import com.medsurgery.kiruplus.domain.chapterexam.ChapterExamQuestion
import com.medsurgery.kiruplus.domain.chapterexam.ChapterExamRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ChapterExamUiState(
    val isLoading: Boolean = true,
    val exam: ChapterExam? = null,
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val correctCount: Int = 0,
    val isCompleted: Boolean = false,
    val error: String? = null,
) {
    val currentQuestion: ChapterExamQuestion? get() = exam?.questions?.getOrNull(currentIndex)
    val isAnswered: Boolean get() = selectedOptionIndex != null
    val totalQuestions: Int get() = exam?.questionCount ?: 0
    val scorePct: Int get() = if (totalQuestions > 0) (correctCount * 100) / totalQuestions else 0
    val isEmpty: Boolean get() = !isLoading && exam == null && error == null
}

@HiltViewModel
class ChapterExamViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChapterExamRepository,
) : ViewModel() {

    private val examId: String = savedStateHandle["examId"] ?: ""

    private val _state = MutableStateFlow(ChapterExamUiState())
    val state: StateFlow<ChapterExamUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            repository.getChapterExam(examId).collect { result ->
                result.fold(
                    onSuccess = { exam ->
                        _state.update {
                            if (exam == null) {
                                Timber.w("ChapterExam not found: %s", examId)
                                it.copy(isLoading = false, error = "chapter_exam_not_found")
                            } else {
                                it.copy(isLoading = false, exam = exam)
                            }
                        }
                    },
                    onFailure = { err ->
                        Timber.w(err, "ChapterExam load failed: %s", examId)
                        _state.update { it.copy(isLoading = false, error = err.message) }
                    },
                )
            }
        }
    }

    fun selectOption(index: Int) {
        val current = _state.value
        val question = current.currentQuestion
        if (current.isAnswered || question == null) return
        val isCorrect = index == question.correctIndex
        _state.update {
            it.copy(
                selectedOptionIndex = index,
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
            )
        }
    }

    fun nextQuestion() {
        if (!_state.value.isAnswered) return
        val nextIndex = _state.value.currentIndex + 1
        if (nextIndex >= _state.value.totalQuestions) {
            _state.update { it.copy(isCompleted = true) }
        } else {
            _state.update { it.copy(currentIndex = nextIndex, selectedOptionIndex = null) }
        }
    }

    fun restart() {
        _state.update {
            it.copy(
                currentIndex = 0,
                selectedOptionIndex = null,
                correctCount = 0,
                isCompleted = false,
            )
        }
    }
}
