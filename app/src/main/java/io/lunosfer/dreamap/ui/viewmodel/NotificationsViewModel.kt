package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.AppNotification
import io.lunosfer.dreamap.data.repository.FriendsRepository
import io.lunosfer.dreamap.data.repository.NotificationsRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(
        val notifications: List<AppNotification> = emptyList(),
        val unreadCount: Int = 0,
        val isMarkingRead: Boolean = false,
        val actionError: String? = null
    ) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel(
    private val repository: NotificationsRepository = NotificationsRepository(),
    private val friendsRepository: FriendsRepository = FriendsRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        _state.value = NotificationsUiState.Loading
        viewModelScope.launch {
            repository.getNotifications().onSuccess { res ->
                _state.value = NotificationsUiState.Success(
                    notifications = res.notifications,
                    unreadCount = res.unreadCount
                )
            }.onFailure { err ->
                _state.value = NotificationsUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_notifications_load))
            }
        }
    }

    fun markAsRead(notificationId: String? = null, onComplete: () -> Unit = {}) {
        val current = _state.value as? NotificationsUiState.Success ?: return
        _state.value = current.copy(isMarkingRead = true)

        viewModelScope.launch {
            repository.markNotificationsRead(notificationId).onSuccess {
                val latest = _state.value as? NotificationsUiState.Success ?: return@onSuccess
                val updatedNotifications = if (notificationId == null) {
                    latest.notifications.map { it.copy(isRead = true) }
                } else {
                    latest.notifications.map {
                        if (it.id == notificationId) it.copy(isRead = true) else it
                    }
                }
                val newUnread = if (notificationId == null) 0 else maxOf(0, latest.unreadCount - 1)

                _state.value = latest.copy(
                    notifications = updatedNotifications,
                    unreadCount = newUnread,
                    isMarkingRead = false
                )
                onComplete()
            }.onFailure { err ->
                val latest = _state.value as? NotificationsUiState.Success ?: return@onFailure
                _state.value = latest.copy(
                    isMarkingRead = false,
                    actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.notifications_error_mark_read)
                )
                onComplete()
            }
        }
    }

    fun clearActionError() {
        val current = _state.value as? NotificationsUiState.Success ?: return
        _state.value = current.copy(actionError = null)
    }

    // Bug #4: bir "friend_request" bildiriminin üzerindeki Kabul/Reddet
    // butonlarına basınca çağrılır — AddFriendScreen'deki
    // AddFriendViewModel.respondToRequest ile aynı uca gider, tekrar
    // türetmek yerine aynı FriendsRepository çağrısı yeniden kullanılıyor.
    fun respondToFriendRequest(notification: AppNotification, action: String) {
        val friendshipId = notification.referenceId ?: return
        val uid = supabaseClient.auth.currentUserOrNull()?.id ?: return
        val current = _state.value as? NotificationsUiState.Success ?: return

        viewModelScope.launch {
            friendsRepository.respondToFriendRequest(friendshipId = friendshipId, userId = uid, action = action)
                .onSuccess {
                    val latest = _state.value as? NotificationsUiState.Success ?: return@onSuccess
                    _state.value = latest.copy(
                        notifications = latest.notifications.map {
                            if (it.id == notification.id) it.copy(referenceId = null) else it
                        }
                    )
                }.onFailure { err ->
                    val latest = _state.value as? NotificationsUiState.Success ?: return@onFailure
                    _state.value = latest.copy(
                        actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.common_error_action_failed)
                    )
                }
        }
    }
}
