package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.repository.ExploreRepository
import io.lunosfer.dreamap.data.repository.VisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ExploreTab {
    DREAMSCAPE,
    VISION,
    VICTORY,
    PHOENIX
}

class ExploreViewModel(
    private val repository: ExploreRepository = ExploreRepository(),
    private val visionRepository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val _activeTab = MutableStateFlow(ExploreTab.DREAMSCAPE)
    val activeTab: StateFlow<ExploreTab> = _activeTab.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<Dream>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Dream>>> = _state.asStateFlow()

    private val _visionState = MutableStateFlow<UiState<List<Goal>>>(UiState.Loading)
    val visionState: StateFlow<UiState<List<Goal>>> = _visionState.asStateFlow()

    private val _victoryState = MutableStateFlow<UiState<List<Goal>>>(UiState.Loading)
    val victoryState: StateFlow<UiState<List<Goal>>> = _victoryState.asStateFlow()

    private val _phoenixState = MutableStateFlow<UiState<List<Goal>>>(UiState.Loading)
    val phoenixState: StateFlow<UiState<List<Goal>>> = _phoenixState.asStateFlow()

    // Sayfalama durumu (her sekme kendi sayfasini takip eder). Onceden sadece
    // ilk sayfa cekiliyordu: Kesfet 15 ruyada, vizyon sekmeleri de bir sayfada
    // bitiyordu ve devamina ulasmanin yolu yoktu.
    private val _loadingMore = MutableStateFlow<Set<ExploreTab>>(emptySet())
    val loadingMore: StateFlow<Set<ExploreTab>> = _loadingMore.asStateFlow()

    private val pageOf = mutableMapOf<ExploreTab, Int>()
    private val hasMoreOf = mutableMapOf<ExploreTab, Boolean>()
    private var dreamsRankToken: String? = null

    fun hasMore(tab: ExploreTab): Boolean = hasMoreOf[tab] == true

    init {
        loadDreams()
    }

    fun selectTab(tab: ExploreTab) {
        _activeTab.value = tab
        when (tab) {
            ExploreTab.DREAMSCAPE -> {
                if (_state.value is UiState.Loading) {
                    loadDreams()
                }
            }
            ExploreTab.VISION -> {
                if (_visionState.value is UiState.Loading) {
                    loadGoals(ExploreTab.VISION, "active")
                }
            }
            ExploreTab.VICTORY -> {
                if (_victoryState.value is UiState.Loading) {
                    loadGoals(ExploreTab.VICTORY, "completed")
                }
            }
            ExploreTab.PHOENIX -> {
                if (_phoenixState.value is UiState.Loading) {
                    loadGoals(ExploreTab.PHOENIX, "abandoned")
                }
            }
        }
    }

    fun retry() {
        retry(_activeTab.value)
    }

    fun retry(tab: ExploreTab) {
        when (tab) {
            ExploreTab.DREAMSCAPE -> loadDreams()
            ExploreTab.VISION -> loadGoals(ExploreTab.VISION, "active")
            ExploreTab.VICTORY -> loadGoals(ExploreTab.VICTORY, "completed")
            ExploreTab.PHOENIX -> loadGoals(ExploreTab.PHOENIX, "abandoned")
        }
    }

    private fun loadDreams() {
        _state.value = UiState.Loading
        pageOf[ExploreTab.DREAMSCAPE] = 0
        dreamsRankToken = null
        viewModelScope.launch {
            repository.loadPage(page = 0, rankToken = null)
                .onSuccess { res ->
                    _state.value = UiState.Success(res.dreams)
                    dreamsRankToken = res.rankToken
                    hasMoreOf[ExploreTab.DREAMSCAPE] = res.hasMore
                }
                .onFailure { _state.value = UiState.Error(io.lunosfer.dreamap.util.ErrorText.friendly(it)) }
        }
    }

    /** Grid'in sonuna gelindiginde bir sonraki sayfa. */
    fun loadMore(tab: ExploreTab) {
        if (tab in _loadingMore.value || hasMoreOf[tab] != true) return
        _loadingMore.value = _loadingMore.value + tab
        val nextPage = (pageOf[tab] ?: 0) + 1

        viewModelScope.launch {
            when (tab) {
                ExploreTab.DREAMSCAPE -> {
                    val current = (_state.value as? UiState.Success)?.data.orEmpty()
                    repository.loadPage(page = nextPage, rankToken = dreamsRankToken)
                        .onSuccess { res ->
                            if (res.dreams.isNotEmpty()) {
                                _state.value = UiState.Success((current + res.dreams).distinctBy { it.id })
                            }
                            pageOf[tab] = nextPage
                            hasMoreOf[tab] = res.hasMore
                            if (res.rankToken != null) dreamsRankToken = res.rankToken
                        }
                }
                else -> {
                    val stateFlow = goalStateFlow(tab) ?: return@launch
                    val status = goalStatus(tab) ?: return@launch
                    val current = (stateFlow.value as? UiState.Success)?.data.orEmpty()
                    visionRepository.loadHubGoalsPage(status, nextPage)
                        .onSuccess { res ->
                            if (res.goals.isNotEmpty()) {
                                stateFlow.value = UiState.Success((current + res.goals).distinctBy { it.id })
                            }
                            pageOf[tab] = nextPage
                            hasMoreOf[tab] = res.hasMore
                        }
                }
            }
            _loadingMore.value = _loadingMore.value - tab
        }
    }

    private fun goalStateFlow(tab: ExploreTab) = when (tab) {
        ExploreTab.VISION -> _visionState
        ExploreTab.VICTORY -> _victoryState
        ExploreTab.PHOENIX -> _phoenixState
        else -> null
    }

    private fun goalStatus(tab: ExploreTab) = when (tab) {
        ExploreTab.VISION -> "active"
        ExploreTab.VICTORY -> "completed"
        ExploreTab.PHOENIX -> "abandoned"
        else -> null
    }

    private fun loadGoals(tab: ExploreTab, status: String) {
        val stateFlow = when (tab) {
            ExploreTab.VISION -> _visionState
            ExploreTab.VICTORY -> _victoryState
            ExploreTab.PHOENIX -> _phoenixState
            else -> return
        }
        stateFlow.value = UiState.Loading
        pageOf[tab] = 0
        viewModelScope.launch {
            visionRepository.loadHubGoalsPage(status, 0)
                .onSuccess {
                    stateFlow.value = UiState.Success(it.goals)
                    hasMoreOf[tab] = it.hasMore
                }
                .onFailure { stateFlow.value = UiState.Error(io.lunosfer.dreamap.util.ErrorText.friendly(it)) }
        }
    }
}
