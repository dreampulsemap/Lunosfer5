package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.MentalWallResponse
import io.lunosfer.dreamap.data.model.ProphetResponse
import io.lunosfer.dreamap.data.model.PsycheMapResponse
import io.lunosfer.dreamap.data.repository.SpiritualToolsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MentalWallUiState {
    object Idle : MentalWallUiState()
    object Loading : MentalWallUiState()
    data class Success(val response: MentalWallResponse) : MentalWallUiState()
    /** [insufficientAura] true ise arayuz "Aura al / Premium'a gec" teklifini gosterir. */
    data class Error(val message: String, val insufficientAura: Boolean = false) : MentalWallUiState()
}

sealed class PsycheMapUiState {
    object Idle : PsycheMapUiState()
    object Loading : PsycheMapUiState()
    data class Success(val response: PsycheMapResponse) : PsycheMapUiState()
    data class Error(val message: String) : PsycheMapUiState()
}

sealed class ProphetUiState {
    object Idle : ProphetUiState()
    object Loading : ProphetUiState()
    data class Success(val response: ProphetResponse) : ProphetUiState()
    data class Error(val message: String, val insufficientAura: Boolean = false) : ProphetUiState()
}

class SpiritualToolsViewModel(
    private val repository: SpiritualToolsRepository = SpiritualToolsRepository()
) : ViewModel() {

    private val _mentalWallState = MutableStateFlow<MentalWallUiState>(MentalWallUiState.Idle)
    val mentalWallState: StateFlow<MentalWallUiState> = _mentalWallState.asStateFlow()

    private val _psycheMapState = MutableStateFlow<PsycheMapUiState>(PsycheMapUiState.Idle)
    val psycheMapState: StateFlow<PsycheMapUiState> = _psycheMapState.asStateFlow()

    private val _prophetState = MutableStateFlow<ProphetUiState>(ProphetUiState.Idle)
    val prophetState: StateFlow<ProphetUiState> = _prophetState.asStateFlow()

    // "Daha derin yorum" ayni istegi tekrarliyor; son sorulani hatirlamamiz
    // gerekiyor ki kullanici soruyu yeniden yazmak zorunda kalmasin.
    private var lastMode: String = io.lunosfer.dreamap.data.model.ProphetRequest.MODE_GENERAL
    private var lastQuestion: String? = null

    init {
        loadPsycheMap()
    }

    /** [deep] true: "daha derin yorum" — premium uyeye bedava, degilse 10 Aura. */
    fun generateMentalWall(deep: Boolean = false) {
        _mentalWallState.value = MentalWallUiState.Loading
        viewModelScope.launch {
            repository.generateMentalWall(
                // R.string.app_lang_code UYGULAMA context'inden okunuyordu ve o,
                // AppCompatDelegate ile secilen per-app dili takip etmiyor: uygulama
                // Turkce'yken sunucuya "en" gidiyor, Zihin Duvari ve Kahin Ingilizce
                // donuyordu (bkz. AppLanguage dosyasindaki ayni sinif hata notu).
                io.lunosfer.dreamap.util.AppLanguage.code(),
                deep
            )
                .onSuccess { res ->
                    _mentalWallState.value = MentalWallUiState.Success(res)
                }
                .onFailure { err ->
                    val app = io.lunosfer.dreamap.DreamapApp.instance
                    val msg = err.message ?: ""
                    // 400 iki farkli sebep olabiliyor ve ikisinin cozumu ayri:
                    // yeterli ruya yok VS aktif vizyon yok. Eskiden ikisi de
                    // "yeterli veri yok" diye tek metne dusuyordu.
                    val friendly = when {
                        io.lunosfer.dreamap.util.ApiErrors.isInsufficientAura(err) ->
                            io.lunosfer.dreamap.util.ApiErrors.message(err, app.getString(io.lunosfer.dreamap.R.string.error_mental_wall))
                        msg.contains("no_active_goals") -> app.getString(io.lunosfer.dreamap.R.string.error_mental_wall_no_goals)
                        msg.contains("not_enough_dreams") || msg.contains("400") ->
                            app.getString(io.lunosfer.dreamap.R.string.error_mental_wall_not_enough_data)
                        else -> app.getString(io.lunosfer.dreamap.R.string.error_mental_wall)
                    }
                    _mentalWallState.value = MentalWallUiState.Error(
                        friendly,
                        io.lunosfer.dreamap.util.ApiErrors.isInsufficientAura(err)
                    )
                }
        }
    }

    fun loadPsycheMap() {
        _psycheMapState.value = PsycheMapUiState.Loading
        viewModelScope.launch {
            repository.getPsycheMap()
                .onSuccess { res ->
                    _psycheMapState.value = PsycheMapUiState.Success(res)
                }
                .onFailure { err ->
                    _psycheMapState.value = PsycheMapUiState.Error(err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_psyche_map))
                }
        }
    }

    /** Kullanicinin kendi ruya ve vizyonlarindan kehanet uretir. */
    fun generalProphecy(deep: Boolean = false) {
        lastMode = io.lunosfer.dreamap.data.model.ProphetRequest.MODE_GENERAL
        lastQuestion = null
        runProphet(io.lunosfer.dreamap.data.model.ProphetRequest.MODE_GENERAL, null, deep)
    }

    /** Kullanicinin yazdigi soruya cevap verir. */
    fun askProphet(question: String, deep: Boolean = false) {
        if (question.isBlank()) return
        lastMode = io.lunosfer.dreamap.data.model.ProphetRequest.MODE_ASK
        lastQuestion = question
        runProphet(io.lunosfer.dreamap.data.model.ProphetRequest.MODE_ASK, question, deep)
    }

    /** "Daha derin bir yorum ister misin?" — ayni istegi derin surumle tekrarlar. */
    fun deepenLastProphecy() {
        runProphet(lastMode, lastQuestion, deep = true)
    }

    private fun runProphet(mode: String, question: String?, deep: Boolean = false) {
        _prophetState.value = ProphetUiState.Loading
        viewModelScope.launch {
            repository.consultProphet(
                mode,
                question,
                // R.string.app_lang_code UYGULAMA context'inden okunuyordu ve o,
                // AppCompatDelegate ile secilen per-app dili takip etmiyor: uygulama
                // Turkce'yken sunucuya "en" gidiyor, Zihin Duvari ve Kahin Ingilizce
                // donuyordu (bkz. AppLanguage dosyasindaki ayni sinif hata notu).
                io.lunosfer.dreamap.util.AppLanguage.code(),
                deep
            )
                .onSuccess { res ->
                    _prophetState.value = ProphetUiState.Success(res)
                }
                .onFailure { err ->
                    val fallback = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_prophet_failed)
                    _prophetState.value = ProphetUiState.Error(
                        io.lunosfer.dreamap.util.ApiErrors.message(err, fallback),
                        io.lunosfer.dreamap.util.ApiErrors.isInsufficientAura(err)
                    )
                }
        }
    }
}
