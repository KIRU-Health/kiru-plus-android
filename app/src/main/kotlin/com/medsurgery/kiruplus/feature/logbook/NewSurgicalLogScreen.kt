package com.medsurgery.kiruplus.feature.logbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.domain.logbook.SurgicalRole
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSurgicalLogScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: NewSurgicalLogViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logbook_new_title)) },
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
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.procedureName,
                onValueChange = viewModel::setProcedureName,
                label = { Text(stringResource(R.string.logbook_field_procedure)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            dateField(
                date = state.procedureDate,
                onChange = viewModel::setProcedureDate,
            )

            OutlinedTextField(
                value = state.patientInitials,
                onValueChange = viewModel::setPatientInitials,
                label = { Text(stringResource(R.string.logbook_field_initials)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.medicalRecordNumber,
                onValueChange = viewModel::setMedicalRecordNumber,
                label = { Text(stringResource(R.string.logbook_field_record)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            roleDropdown(selected = state.role, onSelect = viewModel::setRole)

            OutlinedTextField(
                value = state.supervisorName,
                onValueChange = viewModel::setSupervisorName,
                label = { Text(stringResource(R.string.logbook_field_supervisor)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.durationMinutes,
                onValueChange = viewModel::setDurationMinutes,
                label = { Text(stringResource(R.string.logbook_field_duration)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                label = { Text(stringResource(R.string.logbook_field_notes)) },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )

            state.capacityMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            state.errorRes?.let { res ->
                Text(
                    text = stringResource(res),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = viewModel::submit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                if (state.isSubmitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.logbook_save))
                }
            }
        }
    }
}

@Composable
private fun roleDropdown(
    selected: SurgicalRole,
    onSelect: (SurgicalRole) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label =
        when (selected) {
            SurgicalRole.Cirujano -> R.string.logbook_role_surgeon
            SurgicalRole.Ayudante1 -> R.string.logbook_role_assistant1
            SurgicalRole.Ayudante2 -> R.string.logbook_role_assistant2
            SurgicalRole.Observador -> R.string.logbook_role_observer
        }
    Box {
        OutlinedTextField(
            value = stringResource(label),
            onValueChange = {},
            label = { Text(stringResource(R.string.logbook_field_role)) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                }
            },
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SurgicalRole.entries.forEach { role ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                when (role) {
                                    SurgicalRole.Cirujano -> R.string.logbook_role_surgeon
                                    SurgicalRole.Ayudante1 -> R.string.logbook_role_assistant1
                                    SurgicalRole.Ayudante2 -> R.string.logbook_role_assistant2
                                    SurgicalRole.Observador -> R.string.logbook_role_observer
                                },
                            ),
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(role)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dateField(
    date: LocalDate,
    onChange: (LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val initialMillis =
        remember(date) {
            date.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
        }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    OutlinedTextField(
        value = date.toString(),
        onValueChange = { /* read-only */ },
        label = { Text(stringResource(R.string.logbook_field_date)) },
        readOnly = true,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { showPicker = true },
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val localDate =
                            Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC).date
                        onChange(localDate)
                    }
                    showPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
