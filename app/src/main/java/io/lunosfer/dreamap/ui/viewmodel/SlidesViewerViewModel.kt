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
import io.lunosfer.dreamap.data.model.GoalSlide
import io.lunosfer.dreamap.data.model.UserProfile
import io.lunosfer.dreamap.data.repository.VisionRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * components/SlidesViewer.jsx'in Android karşılığı — "Vizyon Slaytları",
 * oto-oynatan, Instagram/TikTok Stories tarzı tam ekran görüntüleyici.
 *
 * Yorumlar (GoalDetailScreen'deki ile aynı API, bottom sheet olarak) ve
 * bildirme (rapor, pages/api/goals/report.js ile birebir eşleşen şema)
 * akışlarını da içerir.
 */
sealed class SlidesViewerUiState {
    object Loading : SlidesViewerUiState()

    data class Content(
        val goal: Goal?,
        val owner: UserProfile?,
        val slides: List<GoalSlide>,
        val currentIndex: Int = 0,
        val isOwner: Boolean = false,
        val progress: Float = 0f,
        val isPaused: Boolean = false,
        val hasReacted: Boolean = false,
        val believersCount: Int = 0,
        // goal.commentsCount'tan başlatılır (trigger ile güncel tutulan DB
        // kolonu) — panel hiç açılmadan doğru sayıyı göstermek için. Panel
        // açılıp yorumlar gerçekten yüklendikten sonra comments.size daha
        // güncel olabilir (ör. iki cihazdan eşzamanlı yorum), ama ekleme/
        // silme işlemleri her iki sayacı da senkron tutar.
        val commentsCount: Int = 0,
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
        // reportResultToast: null = gösterme, true = "zaten bildirilmişti",
        // false = "bildirimin alındı" — ikisi de success, farklı mesaj.
        val reportResultToast: Boolean? = null,
        // --- Kendi Vizyonlarıma Ekle (klonla) — reportResultToast ile aynı desen ---
        val isCloning: Boolean = false,
        val cloneResultToast: Boolean? = null
    ) : SlidesViewerUiState() {
        val currentSlide: GoalSlide? get() = slides.getOrNull(currentIndex)
    }

    object Closed : SlidesViewerUiState()
    data class Error(val message: String) : SlidesViewerUiState()
}

class SlidesViewerViewModel(
    private val goalId: String,
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<SlidesViewerUiState>(SlidesViewerUiState.Loading)
    val state: StateFlow<SlidesViewerUiState> = _state.asStateFlow()

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    private var timerJob: Job? = null

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = SlidesViewerUiState.Loading

            val goal = runCatching {
                supabaseClient.postgrest["goals"]
                    .select(Columns.raw("*, user_profiles:user_id(*)")) {
                        filter { eq("id", goalId) }
                    }.decodeSingle<Goal>()
            }.getOrNull()

            repository.loadGoalSlides(goalId).onSuccess { res ->
                if (res.slides.isEmpty()) {
                    _state.value = SlidesViewerUiState.Error(
                        io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_slides_not_found)
                    )
                } else {
                    _state.value = SlidesViewerUiState.Content(
                        goal = goal,
                        owner = res.owner,
                        slides = res.slides,
                        currentIndex = 0,
                        isOwner = currentUserId != null && goal?.userId == currentUserId,
                        hasReacted = goal?.hasReacted ?: false,
                        believersCount = goal?.believersCount ?: 0,
                        commentsCount = goal?.commentsCount ?: 0
                    )
                    startTimer()
                }
            }.onFailure { err ->
                _state.value = SlidesViewerUiState.Error(
                    err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_slides_load)
                )
            }
        }
    }

    // Her slayt kendi duration_seconds'ı kadar ekranda kalır, süre dolunca
    // bir sonrakine geçer (SlidesViewer.jsx ile aynı mantık).
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val current0 = _state.value as? SlidesViewerUiState.Content ?: return@launch
            val durationMs = (current0.currentSlide?.durationSeconds ?: 4).coerceAtLeast(1) * 1000L
            val stepMs = 50L
            var elapsed = 0L

            while (elapsed < durationMs) {
                delay(stepMs)
                val current = _state.value as? SlidesViewerUiState.Content ?: return@launch
                if (current.isPaused) continue
                elapsed += stepMs
                val p = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                _state.value = current.copy(progress = p)
            }
            nextSlide()
        }
    }

    fun pauseTimer() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(isPaused = true)
    }

    fun resumeTimer() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(isPaused = false)
    }

    // Son slayttaysa hiçbir şey yapma — web'deki gibi dur, döngüye girme,
    // otomatik kapatma.
    fun nextSlide() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        if (current.currentIndex < current.slides.size - 1) {
            _state.value = current.copy(currentIndex = current.currentIndex + 1, progress = 0f)
            startTimer()
        }
    }

    // İlk slayttaysa hiçbir şey yapma (web'deki gibi).
    fun previousSlide() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        if (current.currentIndex > 0) {
            _state.value = current.copy(currentIndex = current.currentIndex - 1, progress = 0f)
            startTimer()
        }
    }

    fun close() {
        timerJob?.cancel()
        _state.value = SlidesViewerUiState.Closed
    }

    fun toggleMana() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        if (current.isOwner) return
        val wasReacted = current.hasReacted
        val oldCount = current.believersCount

        if (wasReacted) {
            _state.value = current.copy(hasReacted = false, believersCount = maxOf(0, oldCount - 1))
            viewModelScope.launch {
                repository.removeMana(goalId).onFailure {
                    val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(hasReacted = wasReacted, believersCount = oldCount)
                }
            }
        } else {
            _state.value = current.copy(hasReacted = true, believersCount = oldCount + 1)
            viewModelScope.launch {
                repository.giveMana(goalId, 1).onFailure { err ->
                    val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(
                        hasReacted = wasReacted,
                        believersCount = oldCount,
                        actionError = err.message
                    )
                }
            }
        }
    }

    fun toggleSaveSlide() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        val slide = current.currentSlide ?: return
        val index = current.currentIndex
        val wasSaved = slide.hasSaved ?: false
        val oldCount = slide.savesCount ?: 0

        val updatedSlide = slide.copy(
            hasSaved = !wasSaved,
            savesCount = if (wasSaved) maxOf(0, oldCount - 1) else oldCount + 1
        )
        _state.value = current.copy(
            slides = current.slides.toMutableList().also { it[index] = updatedSlide }
        )

        viewModelScope.launch {
            repository.toggleSlideSave(slide.id).onFailure {
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                _state.value = latest.copy(
                    slides = latest.slides.toMutableList().also { it[index] = slide }
                )
            }
        }
    }

    fun deleteCurrentSlide() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        val slide = current.currentSlide ?: return
        timerJob?.cancel()

        viewModelScope.launch {
            repository.deleteGoalSlide(slide.id).onSuccess {
                val remaining = current.slides.filterNot { it.id == slide.id }
                if (remaining.isEmpty()) {
                    _state.value = SlidesViewerUiState.Closed
                } else {
                    val nextIdx = current.currentIndex.coerceAtMost(remaining.size - 1)
                    _state.value = current.copy(slides = remaining, currentIndex = nextIdx, progress = 0f)
                    startTimer()
                }
            }.onFailure { err ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                _state.value = latest.copy(actionError = err.message)
                startTimer()
            }
        }
    }

    fun clearActionError() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(actionError = null)
    }

    // --- Yorumlar ---
    // GoalDetailScreen'deki ile birebir aynı API (getGoalComments /
    // createGoalComment / deleteGoalComment) — burada bottom sheet olarak
    // sunuluyor.

    fun openComments() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        pauseTimer()
        if (current.commentsLoaded) {
            _state.value = current.copy(showComments = true)
            return
        }
        _state.value = current.copy(showComments = true, isLoadingComments = true)
        viewModelScope.launch {
            repository.getGoalComments(goalId).onSuccess { comments ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(comments = comments, isLoadingComments = false, commentsLoaded = true)
            }.onFailure {
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                _state.value = latest.copy(isLoadingComments = false, commentsLoaded = true)
            }
        }
    }

    fun closeComments() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(showComments = false)
        resumeTimer()
    }

    fun addComment(content: String) {
        if (content.isBlank()) return
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(isSubmittingComment = true)
        viewModelScope.launch {
            repository.createGoalComment(goalId, content).onSuccess { comment ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(
                    comments = latest.comments + comment,
                    commentsCount = latest.commentsCount + 1,
                    isSubmittingComment = false
                )
            }.onFailure { err ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                _state.value = latest.copy(isSubmittingComment = false, actionError = err.message)
            }
        }
    }

    fun deleteComment(commentId: String) {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        val removed = current.comments.firstOrNull { it.id == commentId }
        // Optimistic — geri alma gerekirse removed'i ekliyoruz.
        _state.value = current.copy(
            comments = current.comments.filterNot { it.id == commentId },
            commentsCount = maxOf(0, current.commentsCount - 1)
        )
        viewModelScope.launch {
            repository.deleteGoalComment(commentId).onFailure {
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                if (removed == null) return@onFailure
                _state.value = latest.copy(
                    comments = latest.comments + removed,
                    commentsCount = latest.commentsCount + 1
                )
            }
        }
    }

    // --- Bildir (Rapor) ---

    fun openReportSheet() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        pauseTimer()
        _state.value = current.copy(showReportSheet = true)
    }

    fun closeReportSheet() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(showReportSheet = false)
        resumeTimer()
    }

    fun submitReport(reason: GoalReportReason, note: String? = null) {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(isSubmittingReport = true)
        viewModelScope.launch {
            repository.reportGoal(goalId, reason, note).onSuccess { alreadyReported ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(
                    isSubmittingReport = false,
                    showReportSheet = false,
                    reportResultToast = alreadyReported
                )
                resumeTimer()
            }.onFailure { err ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                _state.value = latest.copy(isSubmittingReport = false, actionError = err.message)
            }
        }
    }

    fun consumeReportResultToast() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(reportResultToast = null)
    }

    // --- Kendi Vizyonlarıma Ekle (klonla) ---

    /** Sahibi kendi vizyonunu ekleyemez — toggleMana'daki isOwner koruması ile aynı mantık. */
    fun cloneToMyVisions() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        if (current.isOwner || current.isCloning) return
        // openComments/openReportSheet'teki gibi: işlem sürerken oto-oynatma
        // ilerlemesin diye duraklat, sonuç gelince devam ettir.
        pauseTimer()
        _state.value = current.copy(isCloning = true)
        viewModelScope.launch {
            repository.cloneGoal(goalId).onSuccess { res ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(isCloning = false, cloneResultToast = res.alreadyCloned)
                resumeTimer()
            }.onFailure { err ->
                val latest = _state.value as? SlidesViewerUiState.Content ?: return@onFailure
                _state.value = latest.copy(isCloning = false, actionError = err.message)
                resumeTimer()
            }
        }
    }

    fun consumeCloneResultToast() {
        val current = _state.value as? SlidesViewerUiState.Content ?: return
        _state.value = current.copy(cloneResultToast = null)
    }

    class Factory(private val goalId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SlidesViewerViewModel(goalId) as T
        }
    }
}
