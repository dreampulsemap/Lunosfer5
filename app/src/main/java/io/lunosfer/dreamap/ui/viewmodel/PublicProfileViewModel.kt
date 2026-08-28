package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.GoalReportReason
import io.lunosfer.dreamap.data.model.PublicProfileData
import io.lunosfer.dreamap.data.repository.BlockRepository
import io.lunosfer.dreamap.data.repository.FriendsRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PublicProfileUiState {
    object Loading : PublicProfileUiState()
    data class Success(
        val profile: PublicProfileData,
        val dreams: List<Dream> = emptyList(),
        val currentPage: Int = 0,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val friendshipStatus: String? = null, // null | "pending" | "accepted"
        val followsViewer: Boolean = false,
        val isSelf: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null,
        // Google Play UGC politikası: engelleme + şikayet durumu.
        val blockedByMe: Boolean = false,
        val isTogglingBlock: Boolean = false,
        val showReportSheet: Boolean = false,
        val isSubmittingReport: Boolean = false
    ) : PublicProfileUiState()
    data class Error(val message: String) : PublicProfileUiState()
}

class PublicProfileViewModel(
    private val userId: String,
    private val repository: FriendsRepository = FriendsRepository(),
    private val blockRepository: BlockRepository = BlockRepository()
) : ViewModel() {

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    private val _state = MutableStateFlow<PublicProfileUiState>(PublicProfileUiState.Loading)
    val state: StateFlow<PublicProfileUiState> = _state.asStateFlow()

    init {
        loadProfile(page = 0)
    }

    fun loadProfile(page: Int = 0) {
        if (page == 0) {
            _state.value = PublicProfileUiState.Loading
        } else {
            val current = _state.value as? PublicProfileUiState.Success ?: return
            _state.value = current.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            repository.getPublicProfile(userId = userId, page = page).onSuccess { res ->
                val prof = res.profile ?: PublicProfileData(id = userId)
                val current = _state.value as? PublicProfileUiState.Success

                val combinedDreams = if (page == 0 || current == null) {
                    res.dreams
                } else {
                    current.dreams + res.dreams
                }

                val isSelfProfile = res.isSelf || (currentUserId != null && currentUserId == userId)
                _state.value = PublicProfileUiState.Success(
                    profile = prof,
                    dreams = combinedDreams,
                    currentPage = page,
                    hasMore = res.hasMore,
                    isLoadingMore = false,
                    friendshipStatus = res.friendshipStatus,
                    followsViewer = res.followsViewer,
                    isSelf = isSelfProfile,
                    // Önceki durumdan blockedByMe'yi koru (sayfalama sırasında sıfırlanmasın).
                    blockedByMe = (current as? PublicProfileUiState.Success)?.blockedByMe ?: false
                )
                if (page == 0 && !isSelfProfile) {
                    loadBlockStatus()
                }
            }.onFailure { err ->
                if (page == 0) {
                    _state.value = PublicProfileUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_profile_load))
                } else {
                    val current = _state.value as? PublicProfileUiState.Success
                    if (current != null) {
                        _state.value = current.copy(isLoadingMore = false, actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.public_profile_error_load_more))
                    }
                }
            }
        }
    }

    fun loadNextPage() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        if (current.hasMore && !current.isLoadingMore) {
            loadProfile(page = current.currentPage + 1)
        }
    }

    fun sendFollowRequest() {
        val uid = currentUserId ?: return
        val current = _state.value as? PublicProfileUiState.Success ?: return

        viewModelScope.launch {
            repository.sendFriendRequest(userId = uid, friendId = userId).onSuccess { res ->
                val latest = _state.value as? PublicProfileUiState.Success ?: return@onSuccess
                val newStatus = res.status ?: "pending"
                val msg = if (newStatus == "accepted") io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.friend_msg_followed) else io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.friend_msg_follow_requested)
                _state.value = latest.copy(
                    friendshipStatus = newStatus,
                    actionMessage = msg
                )
            }.onFailure { err ->
                val latest = _state.value as? PublicProfileUiState.Success ?: return@onFailure
                _state.value = latest.copy(actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.friend_error_follow_request_failed))
            }
        }
    }

    fun clearActionMessage() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        _state.value = current.copy(actionMessage = null)
    }

    fun clearActionError() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        _state.value = current.copy(actionError = null)
    }

    // --- Engelleme (Google Play UGC politikası) ---

    private fun loadBlockStatus() {
        viewModelScope.launch {
            blockRepository.getBlockStatus(userId).onSuccess { status ->
                val latest = _state.value as? PublicProfileUiState.Success ?: return@onSuccess
                _state.value = latest.copy(blockedByMe = status.blockedByMe)
            }
            // Hata sessizce yutulur: durum bilinmiyorsa buton "Engelle"
            // varsayılanında kalır, ekranı bozmaya değmez.
        }
    }

    fun toggleBlock() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        if (current.isTogglingBlock) return
        val willBlock = !current.blockedByMe

        _state.value = current.copy(isTogglingBlock = true)
        viewModelScope.launch {
            val result = if (willBlock) blockRepository.blockUser(userId) else blockRepository.unblockUser(userId)
            val latest = _state.value as? PublicProfileUiState.Success ?: return@launch
            result.onSuccess {
                val msg = if (willBlock) {
                    io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.block_user_success)
                } else {
                    io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.unblock_user_success)
                }
                _state.value = latest.copy(
                    isTogglingBlock = false,
                    blockedByMe = willBlock,
                    actionMessage = msg
                )
            }.onFailure { err ->
                val fallback = if (willBlock) {
                    io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.block_user_error)
                } else {
                    io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.unblock_user_error)
                }
                _state.value = latest.copy(isTogglingBlock = false, actionError = err.message ?: fallback)
            }
        }
    }

    // --- Kullanıcı Şikayeti (Google Play UGC politikası) ---

    fun openReportSheet() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        _state.value = current.copy(showReportSheet = true)
    }

    fun closeReportSheet() {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        _state.value = current.copy(showReportSheet = false)
    }

    fun submitReport(reason: GoalReportReason, note: String?) {
        val current = _state.value as? PublicProfileUiState.Success ?: return
        if (current.isSubmittingReport) return

        _state.value = current.copy(isSubmittingReport = true)
        viewModelScope.launch {
            repository.reportUser(userId = userId, reason = reason.apiValue, note = note).onSuccess {
                val latest = _state.value as? PublicProfileUiState.Success ?: return@onSuccess
                _state.value = latest.copy(
                    isSubmittingReport = false,
                    showReportSheet = false,
                    actionMessage = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.report_submitted_success)
                )
            }.onFailure { err ->
                val latest = _state.value as? PublicProfileUiState.Success ?: return@onFailure
                _state.value = latest.copy(
                    isSubmittingReport = false,
                    actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.report_submitted_error)
                )
            }
        }
    }

    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PublicProfileViewModel(userId) as T
        }
    }
}
