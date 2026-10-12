package com.medsurgery.kiruplus.feature.pearls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.domain.pearls.Pearl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PearlDetailScreen(
    onBack: () -> Unit,
    viewModel: PearlDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.pearl?.title ?: stringResource(R.string.pearls_title),
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
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
                state.errorRes != null -> Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = state.errorRes?.let { stringResource(it) }.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        TextButton(onClick = viewModel::load) {
                            Text(stringResource(R.string.action_retry))
                        }
                    }
                }
                state.pearl != null -> state.pearl?.let { PearlBody(pearl = it) }
            }
        }
    }
}

@Composable
private fun PearlBody(pearl: Pearl) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CategoryChip(category = pearl.category)
        Text(
            text = pearl.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = pearl.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // Nota: `content_blocks` (jsonb) y `content` (text[]) están en la tabla pero
        // el render rico se difiere a v1; los slugs iOS "cat.xxx" se limpian arriba.
    }
}

@Composable
private fun CategoryChip(category: String) {
    val cleaned = category.removePrefix("cat.").replaceFirstChar { it.uppercase() }
    val readable = Regex("([a-z])([A-Z])").replace(cleaned) { match ->
        "${match.groupValues[1]} ${match.groupValues[2].lowercase()}"
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
    ) {
        Text(
            text = readable,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier
                .fillMaxWidth(0f)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
