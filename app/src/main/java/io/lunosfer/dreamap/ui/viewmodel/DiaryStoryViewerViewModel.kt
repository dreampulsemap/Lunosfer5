package io.lunosfer.dreamap.ui.viewmodel

import io.lunosfer.dreamap.util.GuestPrompt
import io.lunosfer.dreamap.util.GuestMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.DiaryComment
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.data.model.UserProfile
import io.lunosfer.dreamap.data.repository.DiaryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DiaryStoryViewerUiState {
    object Loading : DiaryStoryViewerUiState()
    data class Content(
        val owner: UserProfile?,
        val entries: List<DiaryEntry>,
        val currentIndex: Int = 0,
        val isSelf: Boolean = false,
        val progress: Float = 0f,
        val isPaused: Boolean = false,
        val actionError: String? = null,
        val commentsSheetVisible: Boolean = false,
        val comments: List<DiaryComment> = emptyList(),
        val isLoadingComments: Boolean = false,
        val isPostingComment: Boolean = false
    ) : DiaryStoryViewerUiState() {
        val currentEntry: DiaryEntry? get() = entries.getOrNull(currentIndex)
    }
    object Closed : DiaryStoryViewerUiState()
    data class Error(val message: String) : DiaryStoryViewerUiState()
}

class DiaryStoryViewerViewModel(
    private val userId: String,
    private val repository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<DiaryStoryViewerUiState>(DiaryStoryViewerUiState.Loading)
    val state: StateFlow<DiaryStoryViewerUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadData()
        markSeen()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = DiaryStoryViewerUiState.Loading
            repository.getEntriesForUser(userId).onSuccess { res ->
                if (res.entries.isEmpty()) {
                    _state.value = DiaryStoryViewerUiState.Error(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_diary_not_found))
                } else {
                    _state.value = DiaryStoryViewerUiState.Content(
                        owner = res.owner,
                        entries = res.entries,
                        currentIndex = 0,
                        isSelf = res.isSelf
                    )
                    startTimer()
                }
            }.onFailure { err ->
                _state.value = DiaryStoryViewerUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_stories_load))
            }
        }
    }

    private fun markSeen() {
        viewModelScope.launch {
            repository.markSeen(userId)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        val entry = (_state.value as? DiaryStoryViewerUiState.Content)?.currentEntry
        // Video girdileri kendi ilerlemesini gerçek oynatma konumundan sürer
        // (bkz. DiaryStoryViewerScreen -> ExoPlayer dinleyicisi) ve bittiğinde
        // kendisi nextStory() çağırır — burada sabit süreli bir zamanlayıcı
        // çalıştırmak videoyla yarışıp erken ileri sarardı.
        if (entry?.mediaType == "video") return

        timerJob = viewModelScope.launch {
            val durationMs = 4000L
            val stepMs = 50L
            var elapsed = 0L

            while (elapsed < durationMs) {
                delay(stepMs)
                val current = _state.value as? DiaryStoryViewerUiState.Content ?: break
                if (current.isPaused) continue

                elapsed += stepMs
                val p = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                _state.value = current.copy(progress = p)
            }

            // Time finished -> go next
            nextStory()
        }
    }

    /** Video oynatma konumundan sürülen ilerleme — bkz. startTimer() içindeki not. */
    fun setVideoProgress(p: Float) {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        _state.value = current.copy(progress = p.coerceIn(0f, 1f))
    }

    fun pauseTimer() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        _state.value = current.copy(isPaused = true)
    }

    fun resumeTimer() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        _state.value = current.copy(isPaused = false)
    }

    fun nextStory() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        if (current.currentIndex < current.entries.size - 1) {
            _state.value = current.copy(
                currentIndex = current.currentIndex + 1,
                progress = 0f
            )
            startTimer()
        } else {
            // End of stories -> close
            _state.value = DiaryStoryViewerUiState.Closed
        }
    }

    fun previousStory() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        if (current.currentIndex > 0) {
            _state.value = current.copy(
                currentIndex = current.currentIndex - 1,
                progress = 0f
            )
            startTimer()
        } else {
            _state.value = current.copy(progress = 0f)
            startTimer()
        }
    }

    fun deleteCurrentEntry() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        val entryToDelete = current.currentEntry ?: return

        timerJob?.cancel()
        viewModelScope.launch {
            repository.deleteEntry(entryToDelete.id).onSuccess {
                val updatedList = current.entries.filter { it.id != entryToDelete.id }
                if (updatedList.isEmpty()) {
                    _state.value = DiaryStoryViewerUiState.Closed
                } else {
                    val nextIdx = current.currentIndex.coerceAtMost(updatedList.size - 1)
                    _state.value = current.copy(
                        entries = updatedList,
                        currentIndex = nextIdx,
                        progress = 0f
                    )
                    startTimer()
                }
            }.onFailure { err ->
                _state.value = current.copy(actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_delete_failed))
                startTimer()
            }
        }
    }

    // Bug #13: Günlük/Hikaye gönderilerine beğeni + yorum eklendi. Dream
    // like/comment akışıyla aynı desen: iyimser (optimistic) güncelleme,
    // hata olursa geri al.
    fun toggleLike() {
        if (GuestMode.isGuest()) { GuestPrompt.show(); return }
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        val entry = current.currentEntry ?: return
        val newLiked = !entry.isLiked
        val newCount = (entry.likesCount + if (newLiked) 1 else -1).coerceAtLeast(0)
        val optimisticEntry = entry.copy(isLiked = newLiked, likesCount = newCount)
        applyEntryUpdate(optimisticEntry)

        viewModelScope.launch {
            repository.setLiked(entry.id, newLiked)
                .onSuccess { res ->
                    applyEntryUpdate(optimisticEntry.copy(isLiked = res.liked, likesCount = res.count))
                }
                .onFailure {
                    // Başarısız olursa optimistic güncellemeyi geri al.
                    applyEntryUpdate(entry)
                }
        }
    }

    private fun applyEntryUpdate(updated: DiaryEntry) {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        val updatedEntries = current.entries.map { if (it.id == updated.id) updated else it }
        _state.value = current.copy(entries = updatedEntries)
    }

    fun openComments() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        val entry = current.currentEntry ?: return
        pauseTimer()
        _state.value = current.copy(commentsSheetVisible = true, isLoadingComments = true)
        viewModelScope.launch {
            repository.getComments(entry.id)
                .onSuccess { list ->
                    val latest = _state.value as? DiaryStoryViewerUiState.Content ?: return@onSuccess
                    _state.value = latest.copy(comments = list, isLoadingComments = false)
                }
                .onFailure {
                    val latest = _state.value as? DiaryStoryViewerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(isLoadingComments = false)
                }
        }
    }

    fun closeComments() {
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        _state.value = current.copy(commentsSheetVisible = false, comments = emptyList())
        resumeTimer()
    }

    fun addComment(content: String) {
        if (GuestMode.isGuest()) { GuestPrompt.show(); return }
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return
        val current = _state.value as? DiaryStoryViewerUiState.Content ?: return
        val entry = current.currentEntry ?: return

        _state.value = current.copy(isPostingComment = true)
        viewModelScope.launch {
            repository.addComment(entry.id, trimmed)
                .onSuccess { comment ->
                    val latest = _state.value as? DiaryStoryViewerUiState.Content ?: return@onSuccess
                    _state.value = latest.copy(
                        comments = listOf(comment) + latest.comments,
                        isPostingComment = false
                    )
                    applyEntryUpdate(entry.copy(commentsCount = entry.commentsCount + 1))
                }
                .onFailure {
                    val latest = _state.value as? DiaryStoryViewerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(
                        isPostingComment = false,
                        actionError = it.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.common_error_action_failed)
                    )
                }
        }
    }

    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiaryStoryViewerViewModel(userId) as T
        }
    }
}
