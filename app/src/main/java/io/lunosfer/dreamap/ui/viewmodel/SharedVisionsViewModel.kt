package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.GoalCollaborator
import io.lunosfer.dreamap.data.repository.VisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SharedVisionsUiState {
    object Loading : SharedVisionsUiState()
    data class Content(
        val invites: List<GoalCollaborator> = emptyList(),
        val accepted: List<GoalCollaborator> = emptyList(),
        val error: String? = null
    ) : SharedVisionsUiState()
}

/** "Ortak Vizyonlarım" — başkasının vizyonuna işbirlikçi olarak davet edildiğim
 * veya katıldığım vizyonların listesi. Bunlar kendi vizyonlarım olmadığı için
 * normal "own" feed'inde görünmezler; bkz. goal_collaborators tablosu. */
class SharedVisionsViewModel(
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<SharedVisionsUiState>(SharedVisionsUiState.Loading)
    val state: StateFlow<SharedVisionsUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = SharedVisionsUiState.Loading
        viewModelScope.launch {
            val invitesResult = repository.getMyPendingCollaboratorInvites()
            val acceptedResult = repository.getMyAcceptedCollaborations()
            _state.value = SharedVisionsUiState.Content(
                invites = invitesResult.getOrDefault(emptyList()),
                accepted = acceptedResult.getOrDefault(emptyList()),
                error = invitesResult.exceptionOrNull()?.message ?: acceptedResult.exceptionOrNull()?.message
            )
        }
    }

    fun respond(collaboratorId: Long, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToCollaboratorInvite(collaboratorId, accept).onSuccess {
                load()
            }
        }
    }
}
