package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.model.GoalComment
import io.lunosfer.dreamap.data.model.GoalReportReason
import io.lunosfer.dreamap.data.repository.VisionRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * components/VisionVideoPlayer.jsx'in Android karşılığı. Tek vizyonun
 * videosu, oynat/duraklat, çift dokununca beğen, mana ver/kaldır, kaydet,
 * (sahipse) düzenlemeye dön, yorumlar (bottom sheet) ve bildirme (rapor).
 * GoalDetailScreen'deki "Vizyonu İzle" butonundan, goal.visionVideoUrl
 * doluysa buraya gelinir.
 */
sealed class VisionVideoPlayerUiState {
    object Loading : VisionVideoPlayerUiState()
    data class Content(
        val goal: Goal,
        val isOwner: Boolean = false,
        val hasReacted: Boolean = false,
        val believersCount: Int = 0,
        val hasSaved: Boolean = false,
        val actionError: String? = null,
        // --- Yorumlar (bottom sheet) ---
        val showComments: Boolean = false,
        val comments: List<GoalComment> = emptyList(),
        val isLoadingComments: Boolean = false,
        val isSubmittingComment: Boolean = false,
        val commentsLoaded: Boolean = false,
        // --- Bildir (rapor) ---
        val showReportSheet: Boolean = false,
        val isSubmittingReport: Boolean = false,
        val reportResultToast: Boolean? = null
    ) : VisionVideoPlayerUiState()
    data class Error(val message: String) : VisionVideoPlayerUiState()
}

class VisionVideoPlayerViewModel(
    private val goalId: String,
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<VisionVideoPlayerUiState>(VisionVideoPlayerUiState.Loading)
    val state: StateFlow<VisionVideoPlayerUiState> = _state.asStateFlow()

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    init {
        loadGoal()
    }

    fun loadGoal() {
        viewModelScope.launch {
            _state.value = VisionVideoPlayerUiState.Loading
            runCatching {
                supabaseClient.postgrest["goals"]
                    .select(Columns.raw("*, user_profiles:user_id(*)")) {
                        filter { eq("id", goalId) }
                    }.decodeSingle<Goal>()
            }.onSuccess { goal ->
                if (goal.visionVideoUrl.isNullOrBlank()) {
                    _state.value = VisionVideoPlayerUiState.Error(
                        io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_video_not_found)
                    )
                } else {
                    _state.value = VisionVideoPlayerUiState.Content(
                        goal = goal,
                        isOwner = currentUserId != null && goal.userId == currentUserId,
                        hasReacted = goal.hasReacted ?: false,
                        believersCount = goal.believersCount ?: 0,
                        hasSaved = goal.hasSaved ?: false
                    )                }
            }.onFailure { err ->
                _state.value = VisionVideoPlayerUiState.Error(
                    err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_slides_load)
                )
            }
        }
    }

    fun toggleMana() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        if (current.isOwner) return
        val wasReacted = current.hasReacted
        val oldCount = current.believersCount

        if (wasReacted) {
            _state.value = current.copy(hasReacted = false, believersCount = maxOf(0, oldCount - 1))
            viewModelScope.launch {
                repository.removeMana(goalId).onFailure {
                    val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(hasReacted = wasReacted, believersCount = oldCount)
                }
            }
        } else {
            _state.value = current.copy(hasReacted = true, believersCount = oldCount + 1)
            viewModelScope.launch {
                repository.giveMana(goalId, 1).onFailure { err ->
                    val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(
                        hasReacted = wasReacted,
                        believersCount = oldCount,
                        actionError = err.message
                    )
                }
            }
        }
    }

    /** Çift dokununca beğen — zaten reacted ise tekrar tetiklemeye gerek yok. */
    fun likeOnDoubleTap() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        if (!current.hasReacted) toggleMana()
    }

    fun toggleSave() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        val wasSaved = current.hasSaved
        _state.value = current.copy(hasSaved = !wasSaved)

        viewModelScope.launch {
            repository.saveGoal(goalId).onSuccess { isSaved ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(hasSaved = isSaved)
            }.onFailure { err ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                _state.value = latest.copy(hasSaved = wasSaved, actionError = err.message)
            }
        }
    }

    // --- Yorumlar ---

    fun openComments() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        if (current.commentsLoaded) {
            _state.value = current.copy(showComments = true)
            return
        }
        _state.value = current.copy(showComments = true, isLoadingComments = true)
        viewModelScope.launch {
            repository.getGoalComments(goalId).onSuccess { comments ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(comments = comments, isLoadingComments = false, commentsLoaded = true)
            }.onFailure {
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                _state.value = latest.copy(isLoadingComments = false, commentsLoaded = true)
            }
        }
    }

    fun closeComments() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        _state.value = current.copy(showComments = false)
    }

    fun addComment(content: String) {
        if (content.isBlank()) return
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        _state.value = current.copy(isSubmittingComment = true)
        viewModelScope.launch {
            repository.createGoalComment(goalId, content).onSuccess { comment ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(
                    comments = latest.comments + comment,
                    goal = latest.goal.copy(commentsCount = (latest.goal.commentsCount ?: 0) + 1),
                    isSubmittingComment = false
                )
            }.onFailure { err ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                _state.value = latest.copy(isSubmittingComment = false, actionError = err.message)
            }
        }
    }

    fun deleteComment(commentId: String) {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        val removed = current.comments.firstOrNull { it.id == commentId }
        _state.value = current.copy(
            comments = current.comments.filterNot { it.id == commentId },
            goal = current.goal.copy(commentsCount = maxOf(0, (current.goal.commentsCount ?: 0) - 1))
        )
        viewModelScope.launch {
            repository.deleteGoalComment(commentId).onFailure {
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                if (removed == null) return@onFailure
                _state.value = latest.copy(
                    comments = latest.comments + removed,
                    goal = latest.goal.copy(commentsCount = (latest.goal.commentsCount ?: 0) + 1)
                )
            }
        }
    }

    // --- Bildir (Rapor) ---

    fun openReportSheet() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        _state.value = current.copy(showReportSheet = true)
    }

    fun closeReportSheet() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        _state.value = current.copy(showReportSheet = false)
    }

    fun submitReport(reason: GoalReportReason, note: String? = null) {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        _state.value = current.copy(isSubmittingReport = true)
        viewModelScope.launch {
            repository.reportGoal(goalId, reason, note).onSuccess { alreadyReported ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(
                    isSubmittingReport = false,
                    showReportSheet = false,
                    reportResultToast = alreadyReported
                )
            }.onFailure { err ->
                val latest = _state.value as? VisionVideoPlayerUiState.Content ?: return@onFailure
                _state.value = latest.copy(isSubmittingReport = false, actionError = err.message)
            }
        }
    }

    fun consumeReportResultToast() {
        val current = _state.value as? VisionVideoPlayerUiState.Content ?: return
        _state.value = current.copy(reportResultToast = null)
    }

    class Factory(private val goalId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VisionVideoPlayerViewModel(goalId) as T
        }
    }
}
