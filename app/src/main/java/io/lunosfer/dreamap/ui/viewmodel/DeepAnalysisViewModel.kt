package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.DeepAnalysis
import io.lunosfer.dreamap.data.repository.BillingRepository
import io.lunosfer.dreamap.data.repository.DeepAnalysisError
import io.lunosfer.dreamap.data.repository.DeepAnalysisRepository
import io.lunosfer.dreamap.util.AppLanguage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Uretim 5-15 sn surebiliyor; bos bir spinner yerine nerede oldugunu soyluyoruz. */
val DEEP_ANALYSIS_PROGRESS_STEPS = listOf(
    R.string.deep_analysis_step_reading,
    R.string.deep_analysis_step_patterns,
    R.string.deep_analysis_step_fears,
    R.string.deep_analysis_step_card,
)

data class DeepAnalysisUiState(
    val loading: Boolean = false,
    /** [DEEP_ANALYSIS_PROGRESS_STEPS] icindeki konum. */
    val progressStep: Int = 0,
    val result: DeepAnalysis? = null,
    val history: List<DeepAnalysis> = emptyList(),
    val historyLoading: Boolean = true,
    val error: String? = null,
    /** Hata Aura yetersizligindense satin alma teklifi gosterilir. */
    val needsAuras: Boolean = false,
    val auraCost: Int = 10,
    val auraBalance: Int = 0
)

class DeepAnalysisViewModel(
    private val repository: DeepAnalysisRepository = DeepAnalysisRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(DeepAnalysisUiState())
    val state: StateFlow<DeepAnalysisUiState> = _state.asStateFlow()

    init {
        loadHistory()
        refreshBalance()
    }

    fun refreshBalance() {
        viewModelScope.launch {
            BillingRepository.refreshAuraBalance()
            _state.value = _state.value.copy(auraBalance = BillingRepository.auraBalance.value)
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            repository.history()
                .onSuccess { list ->
                    _state.value = _state.value.copy(history = list, historyLoading = false)
                }
                .onFailure {
                    // Gecmis yuklenememesi yeni analiz uretmeyi engellemez.
                    _state.value = _state.value.copy(historyLoading = false)
                }
        }
    }

    fun openFromHistory(analysis: DeepAnalysis) {
        _state.value = _state.value.copy(result = analysis, error = null, needsAuras = false)
    }

    fun dismissResult() {
        _state.value = _state.value.copy(result = null)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null, needsAuras = false)
    }

    fun generate() {
        if (_state.value.loading) return
        _state.value = _state.value.copy(
            loading = true,
            progressStep = 0,
            error = null,
            needsAuras = false,
            result = null
        )

        // Adim gostergesi: gercek ilerlemeyi sunucudan alamiyoruz (tek
        // istek), ama kullaniciyi 15 saniye bos spinner'la birakmak da
        // "dondu mu?" hissi veriyor.
        val ticker = viewModelScope.launch {
            var step = 0
            while (isActive && step < DEEP_ANALYSIS_PROGRESS_STEPS.lastIndex) {
                delay(4_000)
                step++
                _state.value = _state.value.copy(progressStep = step)
            }
        }

        viewModelScope.launch {
            val app = DreamapApp.instance
            repository.generate(AppLanguage.code())
                .onSuccess { analysis ->
                    ticker.cancel()
                    _state.value = _state.value.copy(
                        loading = false,
                        result = analysis,
                        history = listOf(analysis) + _state.value.history
                    )
                    refreshBalance()
                }
                .onFailure { err ->
                    ticker.cancel()
                    val (message, needsAuras) = when (err) {
                        is DeepAnalysisError.InsufficientAuras ->
                            app.getString(R.string.error_insufficient_auras_cost, err.cost) to true
                        is DeepAnalysisError.NotEnoughDreams ->
                            app.getString(R.string.deep_analysis_error_not_enough_dreams, err.minimum) to false
                        is DeepAnalysisError.RateLimited ->
                            app.getString(R.string.deep_analysis_error_rate_limited) to false
                        is DeepAnalysisError.GenerationFailed ->
                            (if (err.refunded) {
                                app.getString(R.string.deep_analysis_error_failed_refunded)
                            } else {
                                app.getString(R.string.deep_analysis_error_failed)
                            }) to false
                        else -> app.getString(R.string.deep_analysis_error_failed) to false
                    }
                    _state.value = _state.value.copy(
                        loading = false,
                        error = message,
                        needsAuras = needsAuras
                    )
                    refreshBalance()
                }
        }
    }
}
