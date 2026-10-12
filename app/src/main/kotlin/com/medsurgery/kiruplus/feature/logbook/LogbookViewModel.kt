package com.medsurgery.kiruplus.feature.logbook

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.domain.logbook.LogbookRepository
import com.medsurgery.kiruplus.domain.logbook.SurgicalLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogbookViewModel
    @Inject
    constructor(
        private val repository: LogbookRepository,
    ) : ViewModel() {
    private val _state = MutableStateFlow(LogbookUiState())
    val state: StateFlow<LogbookUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, errorRes = null) }
        viewModelScope.launch {
            repository.fetchLogs()
                .onSuccess { logs ->
                    _state.update { it.copy(isLoading = false, logs = logs) }
                }
                .onFailure {
                    _state.update {
                        it.copy(isLoading = false, errorRes = R.string.logbook_error_load)
                    }
                }
        }
    }

    fun deleteLog(id: String) {
        val snapshot = _state.value.logs
        _state.update {
            it.copy(logs = it.logs.filter { log -> log.id != id }, deleteErrorRes = null)
        }
        viewModelScope.launch {
            repository.deleteLog(id)
                .onFailure {
                    _state.update {
                        it.copy(logs = snapshot, deleteErrorRes = R.string.logbook_error_delete)
                    }
                }
        }
    }

    fun clearDeleteError() {
        _state.update { it.copy(deleteErrorRes = null) }
    }
}

data class LogbookUiState(
    val isLoading: Boolean = true,
    val logs: List<SurgicalLog> = emptyList(),
    @StringRes val errorRes: Int? = null,
    @StringRes val deleteErrorRes: Int? = null,
)
