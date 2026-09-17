package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.FeedItem
import io.lunosfer.dreamap.data.repository.DreamRepository
import io.lunosfer.dreamap.data.repository.HomeRepository
import io.lunosfer.dreamap.util.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Home ekranindaki "gunluk seri" (streak) bilgisini tutan basit veri sinifi.
 * streakDays: kullanicinin art arda ruya kaydettigi gun sayisi (bugun dahil).
 * streakStartDate: serinin basladigi tarih (kullanicidostu format, ornek: "12 Ocak").
 * hasDreamToday: bugun zaten bir ruya kaydedilmis mi.
 */
data class StreakInfo(
    val streakDays: Int = 0,
    val streakStartDate: String? = null,
    val hasDreamToday: Boolean = false
)

/**
 * Karsilama basligindaki sayilar.
 * todayDreams: BUGUN akista paylasilmis ruya sayisi.
 * activeVisions: kullanicinin KENDI aktif vizyon sayisi.
 *
 * Onceden ikisi de "ilk sayfada kac ogeler var" demekti; API sayfa basina 6+6
 * getirdigi icin metin herkese hep "6 ruya, 6 aktif vizyon" diyordu.
 */
data class HomeHeaderCounts(
    val todayDreams: Int = 0,
    val activeVisions: Int = 0
)

class HomeViewModel(
    private val repository: HomeRepository = HomeRepository(),
    private val dreamRepository: DreamRepository = DreamRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<FeedItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<FeedItem>>> = _state.asStateFlow()

    private val _streak = MutableStateFlow(StreakInfo())
    val streak: StateFlow<StreakInfo> = _streak.asStateFlow()

    private val _headerCounts = MutableStateFlow(HomeHeaderCounts())
    val headerCounts: StateFlow<HomeHeaderCounts> = _headerCounts.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _canLoadMore = MutableStateFlow(false)
    val canLoadMore: StateFlow<Boolean> = _canLoadMore.asStateFlow()

    private var nextDreamsBefore: String? = null
    private var nextVisionsBefore: String? = null
    private var hasMoreDreams = false
    private var hasMoreVisions = false

    // Ana akış kartındaki kalp ikonuna dokunarak beğenme durumu — sadece bu oturum
    // için lokal tutulur, çünkü /api/home-feed liste yanıtı is_liked döndürmüyor
    // (bu alan sadece tekil rüya detay endpoint'inde var, bkz. DreamDetail.isLiked).
    // DreamRepository.likeDream() sunucudaki "Already liked" hatasını sessizce
    // başarı sayarak yuttuğu için ilk dokunuşta da doğru sonuca yakınsıyor.
    private val _likedDreamIds = MutableStateFlow<Set<Long>>(emptySet())
    val likedDreamIds: StateFlow<Set<Long>> = _likedDreamIds.asStateFlow()

    private val _likeCountOverrides = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val likeCountOverrides: StateFlow<Map<Long, Int>> = _likeCountOverrides.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    fun clearActionError() {
        _actionError.value = null
    }

    fun toggleDreamLike(dream: Dream, userId: String?) {
        if (userId.isNullOrBlank()) {
            _actionError.value = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.dream_detail_error_login_required_like)
            return
        }
        val dreamId = dream.id
        val wasLiked = _likedDreamIds.value.contains(dreamId)
        val baseCount = _likeCountOverrides.value[dreamId] ?: (dream.likesCount ?: 0)
        val newLiked = !wasLiked
        val newCount = if (newLiked) baseCount + 1 else maxOf(0, baseCount - 1)

        // Optimistic güncelleme
        _likedDreamIds.value = if (newLiked) _likedDreamIds.value + dreamId else _likedDreamIds.value - dreamId
        _likeCountOverrides.value = _likeCountOverrides.value + (dreamId to newCount)

        viewModelScope.launch {
            val result = if (newLiked) {
                dreamRepository.likeDream(dreamId, userId)
            } else {
                dreamRepository.unlikeDream(dreamId, userId)
            }

            result.onSuccess { response ->
                val confirmedLiked = response.liked || response.isLiked
                _likedDreamIds.value = if (confirmedLiked) _likedDreamIds.value + dreamId else _likedDreamIds.value - dreamId
                val confirmedCount = response.likesCount ?: response.count
                if (confirmedCount != null) {
                    _likeCountOverrides.value = _likeCountOverrides.value + (dreamId to confirmedCount)
                }
            }.onFailure { err ->
                val msg = err.message ?: ""
                if (msg.contains("Already liked", ignoreCase = true)) {
                    // Sessizce yut, optimistic state zaten doğru
                    return@onFailure
                }
                // Hata durumunda rollback
                _likedDreamIds.value = if (wasLiked) _likedDreamIds.value + dreamId else _likedDreamIds.value - dreamId
                _likeCountOverrides.value = _likeCountOverrides.value + (dreamId to baseCount)
                _actionError.value = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.dream_detail_error_like_failed)
            }
        }
    }

    init {
        load()
    }

    fun retry() = load()

    /** Aşağı çekip yenileme — akışı sıfırdan yükler ama ekranı boşaltmaz. */
    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            repository.loadFirstPage()
                .onSuccess { page ->
                    applyFirstPage(page)
                }
                .onFailure {
                    if (_state.value !is UiState.Success) {
                        _state.value = UiState.Error(io.lunosfer.dreamap.util.ErrorText.friendly(it))
                    }
                }
            _isRefreshing.value = false
        }
    }

    /** Sonsuz kaydırma: listenin sonuna gelindiğinde bir sonraki sayfa. */
    fun loadMore() {
        if (_isLoadingMore.value || _isRefreshing.value) return
        if (!hasMoreDreams && !hasMoreVisions) return
        val current = (_state.value as? UiState.Success)?.data ?: return

        _isLoadingMore.value = true
        viewModelScope.launch {
            repository.loadPage(
                dreamsBefore = nextDreamsBefore,
                visionsBefore = nextVisionsBefore,
                includeDreams = hasMoreDreams && nextDreamsBefore != null,
                includeVisions = hasMoreVisions && nextVisionsBefore != null
            ).onSuccess { page ->
                if (page.items.isNotEmpty()) {
                    val merged = (current + page.items)
                        .distinctBy { feedItemKey(it) }
                        .sortedByDescending { it.createdAt }
                    _state.value = UiState.Success(merged)
                }
                if (page.nextDreamsBefore != null) nextDreamsBefore = page.nextDreamsBefore
                if (page.nextVisionsBefore != null) nextVisionsBefore = page.nextVisionsBefore
                hasMoreDreams = page.hasMoreDreams && page.nextDreamsBefore != null
                hasMoreVisions = page.hasMoreVisions && page.nextVisionsBefore != null
                _canLoadMore.value = hasMoreDreams || hasMoreVisions
            }
            _isLoadingMore.value = false
        }
    }

    private fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            repository.loadFirstPage()
                .onSuccess { page -> applyFirstPage(page) }
                .onFailure { _state.value = UiState.Error(io.lunosfer.dreamap.util.ErrorText.friendly(it)) }
        }
    }

    private suspend fun applyFirstPage(page: HomeRepository.HomePage) {
        _state.value = UiState.Success(page.items)
        nextDreamsBefore = page.nextDreamsBefore
        nextVisionsBefore = page.nextVisionsBefore
        hasMoreDreams = page.hasMoreDreams && page.nextDreamsBefore != null
        hasMoreVisions = page.hasMoreVisions && page.nextVisionsBefore != null
        _canLoadMore.value = hasMoreDreams || hasMoreVisions

        val ownDreamDates = repository.loadOwnDreamDates().getOrDefault(emptyList())
        _streak.value = computeStreak(ownDreamDates)

        val todayKey = dayKeyFor(Calendar.getInstance())
        _headerCounts.value = HomeHeaderCounts(
            todayDreams = page.items.count { it is FeedItem.DreamItem && parseDayKey(it.createdAt) == todayKey },
            activeVisions = repository.loadOwnActiveVisionCount().getOrDefault(0)
        )
    }

    /** Aynı öğenin iki sayfada birden gelmesi durumunda listeyi tekilleştirmek için. */
    private fun feedItemKey(item: FeedItem): String = when (item) {
        is FeedItem.DreamItem -> "dream-${item.dream.id}"
        is FeedItem.VisionItem -> "vision-${item.goal.id}"
    }

    /**
     * Kullanicinin KENDI ruyalarindan art arda kac gun kayit girdigini hesaplar.
     * Liste created_at'e gore azalan sirada gelir (bkz. HomeRepository.loadOwnDreamDates).
     */
    private fun computeStreak(ownDreamDates: List<String>): StreakInfo {
        val dreamDates = ownDreamDates
            .mapNotNull { parseDayKey(it) }
            .distinct()
            .sortedDescending()

        if (dreamDates.isEmpty()) return StreakInfo()

        val todayKey = dayKeyFor(Calendar.getInstance())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayKey = dayKeyFor(yesterdayCal)

        val hasToday = dreamDates.first() == todayKey
        // Seri, bugunku veya dunku kayittan baslamiyorsa kirilmis demektir.
        val startingKey = when (dreamDates.first()) {
            todayKey -> todayKey
            yesterdayKey -> yesterdayKey
            else -> return StreakInfo(streakDays = 0, hasDreamToday = false)
        }

        var streak = 1
        val cursor = Calendar.getInstance().apply {
            timeInMillis = startingKey
        }
        for (i in 1 until dreamDates.size) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
            val expectedKey = dayKeyFor(cursor)
            if (dreamDates[i] == expectedKey) {
                streak++
            } else {
                break
            }
        }

        val streakStartCal = Calendar.getInstance().apply {
            timeInMillis = startingKey
            add(Calendar.DAY_OF_YEAR, -(streak - 1))
        }
        val displayFormat = SimpleDateFormat("d MMMM", AppLanguage.locale())
        val startDateDisplay = displayFormat.format(streakStartCal.time)

        return StreakInfo(
            streakDays = streak,
            streakStartDate = startDateDisplay,
            hasDreamToday = hasToday
        )
    }

    /**
     * ISO tarih string'ini gun bazinda karsilastirilabilir bir zaman damgasina cevirir.
     * Sunucu tarihleri UTC; cihazin yerel gunune gore gruplayabilmek icin once UTC
     * olarak cozumleniyor (aksi halde gece yarisina yakin kayitlar bir gun kayabiliyordu).
     */
    private fun parseDayKey(isoDate: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(isoDate.take(19)) ?: return null
            val cal = Calendar.getInstance()
            cal.time = date
            dayKeyFor(cal)
        } catch (e: Exception) {
            null
        }
    }

    private fun dayKeyFor(cal: Calendar): Long {
        val clone = cal.clone() as Calendar
        clone.set(Calendar.HOUR_OF_DAY, 0)
        clone.set(Calendar.MINUTE, 0)
        clone.set(Calendar.SECOND, 0)
        clone.set(Calendar.MILLISECOND, 0)
        return clone.timeInMillis
    }
}
