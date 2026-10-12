package com.medsurgery.kiruplus.feature.quiz

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.domain.quiz.QuizRepository
import com.medsurgery.kiruplus.domain.quiz.QuizSpecialty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── ViewModel ──────────────────────────────────────────────────────────────

@HiltViewModel
class QuizSpecialtiesViewModel @Inject constructor(
    private val repository: QuizRepository,
) : androidx.lifecycle.ViewModel() {

    private val _state = MutableStateFlow(QuizSpecialtiesUiState())
    val state: StateFlow<QuizSpecialtiesUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            repository.fetchSpecialties()
                .onSuccess { list ->
                    _state.update { it.copy(isLoading = false, specialties = list) }
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false, errorRes = R.string.quiz_error_load) }
                }
        }
    }
}

data class QuizSpecialtiesUiState(
    val isLoading: Boolean = true,
    val specialties: List<QuizSpecialty> = emptyList(),
    @StringRes val errorRes: Int? = null,
)

// ── Screen ─────────────────────────────────────────────────────────────────

@Composable
fun QuizSpecialtiesScreen(
    onOpenQuiz: (String) -> Unit,
    viewModel: QuizSpecialtiesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.errorRes != null -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        state.errorRes?.let { stringResource(it) }.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = viewModel::load) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            state.specialties.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.quiz_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.specialties, key = { it.name }) { specialty ->
                    SpecialtyCard(specialty = specialty, onClick = { onOpenQuiz(specialty.name) })
                }
            }
        }
    }
}

@Composable
private fun SpecialtyCard(specialty: QuizSpecialty, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = specialty.name.substringBefore("(").trim(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pluralStringResource(R.plurals.quiz_question_count, specialty.questionCount, specialty.questionCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
