package io.lunosfer.dreamap.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.repository.VisionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Ana sayfadaki basili-tut pusulasinin durumu. Web'deki DailyCompass.jsx
 * localStorage'a yazdigi icin gun icinde sayfa yenilense de okuma ekranda
 * kaliyordu; burada ayni davranis SharedPreferences ile saglaniyor —
 * aksi halde uygulama yeniden acildiginda kullanici okumasini bir daha
 * goremiyor, sunucu da 429 dondugu icin sadece "bugun zaten baktin" yaziyordu.
 */
class DailyCompassViewModel(
    private val repository: VisionRepository = VisionRepository()
) : ViewModel() {

    private val prefs = DreamapApp.instance.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow<CompassUiState>(CompassUiState.Idle)
    val state: StateFlow<CompassUiState> = _state.asStateFlow()

    init {
        restoreTodaysReading()
    }

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun currentLanguage(): String =
        androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: Locale.getDefault().language

    private fun restoreTodaysReading() {
        if (prefs.getString(KEY_DATE, null) != today()) return
        val reading = prefs.getString(KEY_READING, null) ?: return
        _state.value = CompassUiState.Success(
            reading = reading,
            archetype = prefs.getString(KEY_ARCHETYPE, null),
            color = prefs.getString(KEY_COLOR, null)
        )
    }

    private fun persist(reading: String, archetype: String?, color: String?) {
        prefs.edit()
            .putString(KEY_DATE, today())
            .putString(KEY_READING, reading)
            .putString(KEY_ARCHETYPE, archetype)
            .putString(KEY_COLOR, color)
            .apply()
    }

    fun draw() {
        if (_state.value is CompassUiState.Loading) return
        _state.value = CompassUiState.Loading
        viewModelScope.launch {
            // Okumanin dili kullanicinin sectigi UYGULAMA diline gore isteniyor.
            // Locale.getDefault() burada yeterli degil: canli testte uygulama
            // Turkce'yken okuma Ingilizce geliyordu, cunku per-app dil secimi
            // (AppCompatDelegate) sistem varsayilan Locale'ine yansimayabiliyor.
            repository.getDailyCompass(currentLanguage())
                .onSuccess { res ->
                    val data = res.data
                    when {
                        res.error == "already_used_today" -> _state.value = CompassUiState.AlreadyUsedToday
                        res.error != null -> _state.value = CompassUiState.Error(
                            DreamapApp.instance.getString(R.string.error_compass_failed)
                        )
                        data?.reading != null -> {
                            persist(data.reading, data.archetype, data.color)
                            _state.value = CompassUiState.Success(data.reading, data.archetype, data.color)
                        }
                        else -> _state.value = CompassUiState.AlreadyUsedToday
                    }
                }
                .onFailure { err ->
                    val msg = err.message ?: ""
                    _state.value = if (msg.contains("429") || msg.contains("already_used")) {
                        CompassUiState.AlreadyUsedToday
                    } else {
                        CompassUiState.Error(DreamapApp.instance.getString(R.string.error_daily_compass_failed))
                    }
                }
        }
    }

    private companion object {
        const val PREFS_NAME = "lunosfer_daily_compass"
        const val KEY_DATE = "date"
        const val KEY_READING = "reading"
        const val KEY_ARCHETYPE = "archetype"
        const val KEY_COLOR = "color"
    }
}
