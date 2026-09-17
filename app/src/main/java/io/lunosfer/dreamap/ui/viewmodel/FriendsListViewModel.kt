package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.Friendship
import io.lunosfer.dreamap.data.repository.FriendsRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FriendsListUiState {
    object Loading : FriendsListUiState()
    data class Content(val friendships: List<Friendship> = emptyList()) : FriendsListUiState()
    data class Error(val message: String) : FriendsListUiState()
}

/**
 * /api/friends/list, gizlilik düzeltmesinden sonra query'deki userId'yi
 * yok sayıp DAİMA giriş yapmış kullanıcının kendi listesini döner (bkz.
 * pages/api/friends/list.js). Bu yüzden bu ekran yalnızca "kendi arkadaş
 * listem" için kullanılabilir — başka birinin listesini isteyip alamayız.
 */
class FriendsListViewModel(
    private val repository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    private val _state = MutableStateFlow<FriendsListUiState>(FriendsListUiState.Loading)
    val state: StateFlow<FriendsListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val uid = currentUserId
        if (uid.isNullOrBlank()) {
            _state.value = FriendsListUiState.Error(
                io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.friends_list_error_login_required)
            )
            return
        }
        _state.value = FriendsListUiState.Loading
        viewModelScope.launch {
            repository.getFriendsList(userId = uid, type = "accepted")
                .onSuccess { list -> _state.value = FriendsListUiState.Content(list) }
                .onFailure { err ->
                    _state.value = FriendsListUiState.Error(
                        err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_unknown)
                    )
                }
        }
    }

    /** requester/target'tan hangisi giriş yapmış kullanıcı DEĞİLSE, kartta gösterilecek kişi odur. */
    fun otherPartyOf(friendship: Friendship): io.lunosfer.dreamap.data.model.UserProfile? {
        val uid = currentUserId
        return if (friendship.userId == uid) friendship.target else friendship.requester
    }

    fun otherUserIdOf(friendship: Friendship): String? {
        val uid = currentUserId
        return if (friendship.userId == uid) friendship.friendId else friendship.userId
    }
}
