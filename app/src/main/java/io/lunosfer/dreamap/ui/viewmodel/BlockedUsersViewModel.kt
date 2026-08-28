package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.BlockedUserEntry
import io.lunosfer.dreamap.data.repository.BlockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class BlockedUsersUiState {
    object Loading : BlockedUsersUiState()
    data class Success(
        val users: List<BlockedUserEntry>,
        val unblockingUserId: String? = null,
        val error: String? = null
    ) : BlockedUsersUiState()
    data class Error(val message: String) : BlockedUsersUiState()
}

class BlockedUsersViewModel(
    private val repository: BlockRepository = BlockRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<BlockedUsersUiState>(BlockedUsersUiState.Loading)
    val state: StateFlow<BlockedUsersUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = BlockedUsersUiState.Loading
        viewModelScope.launch {
            repository.getBlockedUsers()
                .onSuccess { users -> _state.value = BlockedUsersUiState.Success(users = users) }
                .onFailure { err ->
                    _state.value = BlockedUsersUiState.Error(
                        err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.blocked_users_load_error)
                    )
                }
        }
    }

    fun unblock(userId: String) {
        val current = _state.value as? BlockedUsersUiState.Success ?: return
        if (current.unblockingUserId != null) return

        _state.value = current.copy(unblockingUserId = userId, error = null)
        viewModelScope.launch {
            repository.unblockUser(userId)
                .onSuccess {
                    val latest = _state.value as? BlockedUsersUiState.Success ?: return@onSuccess
                    _state.value = latest.copy(
                        users = latest.users.filterNot { it.userId == userId },
                        unblockingUserId = null
                    )
                }
                .onFailure { err ->
                    val latest = _state.value as? BlockedUsersUiState.Success ?: return@onFailure
                    _state.value = latest.copy(
                        unblockingUserId = null,
                        error = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.unblock_user_error)
                    )
                }
        }
    }
}
