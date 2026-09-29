package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.LeaderboardEntry
import io.lunosfer.dreamap.data.repository.GameRepository
import io.lunosfer.dreamap.util.safeMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JourneyViewModel : ViewModel() {

    private val _period = MutableStateFlow(PERIOD_WEEK)
    val period: StateFlow<String> = _period.asStateFlow()

    private val _board = MutableStateFlow<UiState<List<LeaderboardEntry>>>(UiState.Loading)
    val board: StateFlow<UiState<List<LeaderboardEntry>>> = _board.asStateFlow()

    private var boardJob: Job? = null

    init {
        viewModelScope.launch { GameRepository.refresh() }
        loadBoard()
    }

    fun setPeriod(value: String) {
        if (value == _period.value) return
        _period.value = value
        loadBoard()
    }

    fun loadBoard() {
        boardJob?.cancel()
        boardJob = viewModelScope.launch {
            _board.value = UiState.Loading
            _board.value = runCatching { GameRepository.leaderboard(_period.value) }.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(safeMessage(it) ?: "") }
            )
        }
    }

    companion object {
        const val PERIOD_WEEK = "week"
        const val PERIOD_ALL = "all"
    }
}
