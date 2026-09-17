package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.Conversation
import io.lunosfer.dreamap.data.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val repository: MessagesRepository = MessagesRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Conversation>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Conversation>>> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.loadConversations()
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(io.lunosfer.dreamap.util.ErrorText.friendly(it)) }
        }
    }
}
