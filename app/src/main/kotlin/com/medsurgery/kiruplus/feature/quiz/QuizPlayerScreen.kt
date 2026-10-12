package com.medsurgery.kiruplus.feature.quiz

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsurgery.kiruplus.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPlayerScreen(
    onBack: () -> Unit,
    viewModel: QuizPlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.specialty.substringBefore("(").trim(),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
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

                state.finished -> ScoreScreen(
                    correct = state.correctCount,
                    total = state.totalCount,
                    onRestart = viewModel::restart,
                    onBack = onBack,
                )

                state.currentQuestion != null -> QuestionPage(
                    state = state,
                    onSelectAnswer = viewModel::selectAnswer,
                    onNext = viewModel::next,
                )
            }
        }
    }
}

@Composable
private fun QuestionPage(
    state: QuizPlayerUiState,
    onSelectAnswer: (String) -> Unit,
    onNext: () -> Unit,
) {
    val question = state.currentQuestion ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.quiz_progress, state.currentIndex + 1, state.totalCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )

        Text(
            text = question.questionText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        Spacer(Modifier.height(8.dp))

        AnimatedContent(targetState = question, label = "options") { currentQuestion ->
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                currentQuestion.options.forEach { option ->
                    OptionButton(
                        text = option,
                        selectedAnswer = state.selectedAnswer,
                        correctAnswer = currentQuestion.correctAnswer,
                        onClick = { onSelectAnswer(option) },
                    )
                }
            }
        }

        if (state.showExplanation) {
            ExplanationCard(
                explanation = question.explanation,
                modifier = Modifier.padding(16.dp),
            )

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                val isLast = state.currentIndex + 1 >= state.totalCount
                Text(stringResource(if (isLast) R.string.quiz_finish else R.string.quiz_next))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OptionButton(
    text: String,
    selectedAnswer: String?,
    correctAnswer: String,
    onClick: () -> Unit,
) {
    val answered = selectedAnswer != null
    val isSelected = text == selectedAnswer
    val isCorrect = text == correctAnswer

    val (borderColor, containerColor) = when {
        !answered -> MaterialTheme.colorScheme.outline to Color.Transparent
        isCorrect -> MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        isSelected -> MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) to Color.Transparent
    }

    val answerStateDesc = when {
        !answered -> ""
        isCorrect -> stringResource(R.string.quiz_answer_correct)
        isSelected -> stringResource(R.string.quiz_answer_incorrect)
        else -> ""
    }

    OutlinedButton(
        onClick = { if (!answered) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = answerStateDesc },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(2.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isCorrect -> MaterialTheme.colorScheme.tertiary
                isSelected && !isCorrect -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ExplanationCard(explanation: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.quiz_explanation_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ScoreScreen(
    correct: Int,
    total: Int,
    onRestart: () -> Unit,
    onBack: () -> Unit,
) {
    val pct = if (total > 0) (correct * 100f / total).toInt() else 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.quiz_score_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$correct / $total",
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.quiz_score_pct, pct),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.quiz_restart))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_back))
        }
    }
}
