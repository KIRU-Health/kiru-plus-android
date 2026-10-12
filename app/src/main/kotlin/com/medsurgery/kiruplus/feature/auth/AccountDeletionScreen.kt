package com.medsurgery.kiruplus.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsurgery.kiruplus.R

/**
 * Pantalla de eliminación de cuenta.
 *
 * Obligatoria por política de Google Play (Account Deletion policy)
 * y GDPR Art. 17. El estado sale de los RPC de iOS
 * (`kiru_account_deletion_status`, `kiru_request_account_deletion`,
 * `kiru_cancel_account_deletion`), con gracia de 48 h.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDeletionScreen(
    onBack: () -> Unit,
    onCompleted: () -> Unit,
    viewModel: AccountDeletionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.completed) {
        if (state.completed) onCompleted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.delete_account_title)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
        ) {
            when (val phase = state.phase) {
                AccountDeletionPhase.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
                    Text(
                        text = stringResource(R.string.delete_account_status_loading),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                AccountDeletionPhase.NotAuthenticated -> {
                    Text(
                        text = stringResource(R.string.delete_account_not_authenticated),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                AccountDeletionPhase.None -> RequestForm(state, viewModel)
                is AccountDeletionPhase.Scheduled -> ScheduledBody(phase, state.isSubmitting, viewModel)
                AccountDeletionPhase.Cancelled -> StatusText(R.string.delete_account_status_cancelled)
                AccountDeletionPhase.FailedStatus -> StatusText(R.string.delete_account_status_failed)
                AccountDeletionPhase.Unknown -> StatusText(R.string.delete_account_status_unknown)
                AccountDeletionPhase.TransportError -> {
                    StatusText(R.string.delete_account_status_error)
                    TextButton(onClick = viewModel::loadStatus) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
            state.errorRes?.let { res ->
                Text(
                    text = stringResource(res),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun StatusText(@androidx.annotation.StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun RequestForm(state: AccountDeletionUiState, viewModel: AccountDeletionViewModel) {
    Text(
        text = stringResource(R.string.delete_account_body),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        text = stringResource(R.string.delete_account_warning),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        text = stringResource(R.string.delete_account_legal),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 16.dp),
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) {
        Checkbox(checked = state.acknowledged, onCheckedChange = viewModel::onAcknowledgeChange)
        Text(text = stringResource(R.string.delete_account_acknowledge), style = MaterialTheme.typography.bodyMedium)
    }
    Button(
        onClick = viewModel::submit,
        enabled = state.acknowledged && !state.isSubmitting,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
    ) {
        if (state.isSubmitting) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        } else {
            Text(stringResource(R.string.delete_account_confirm))
        }
    }
}

@Composable
private fun ScheduledBody(
    phase: AccountDeletionPhase.Scheduled,
    isSubmitting: Boolean,
    viewModel: AccountDeletionViewModel,
) {
    Text(
        text = stringResource(R.string.delete_account_status_scheduled_title),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp),
    )
    Text(
        text = stringResource(R.string.delete_account_status_scheduled_body, phase.dateText),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    Text(
        text = stringResource(R.string.delete_account_status_grace, phase.graceHours),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 8.dp),
    )
    if (phase.canCancel) {
        Text(
            text = stringResource(R.string.delete_account_status_cancelable),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = viewModel::cancelScheduledDeletion,
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.delete_account_status_cancel_action))
        }
    }
    TextButton(
        onClick = viewModel::confirmTeardown,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        Text(stringResource(R.string.delete_account_understood))
    }
}
