package io.lunosfer.dreamap.util

import android.content.Context
import io.lunosfer.dreamap.DreamapApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tanitim turunun cihaz tarafi durumu.
 *
 * deviceSeen: bu cihazda tur bir kez gosterildi mi (ilk indirme / guncelleme
 *   sonrasi ilk acilis). Girissiz kullaniciya da gosterilir.
 * pendingResult: tur giris yapmadan (ya da misafirken) bitirildiyse sonucu;
 *   ilk gercek hesap girisinde sunucuya islenir (+XP odulu o zaman verilir).
 */
object OnboardingPrefs {
    private const val PREFS = "lunosfer_onboarding"
    private const val KEY_SEEN = "device_seen"
    private const val KEY_PENDING = "pending_result"

    const val COMPLETED = "completed"
    const val SKIPPED = "skipped"

    private fun prefs() = DreamapApp.instance.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var deviceSeen: Boolean
        get() = prefs().getBoolean(KEY_SEEN, false)
        set(value) = prefs().edit().putBoolean(KEY_SEEN, value).apply()

    var pendingResult: String?
        get() = prefs().getString(KEY_PENDING, null)
        set(value) {
            prefs().edit().apply { if (value == null) remove(KEY_PENDING) else putString(KEY_PENDING, value) }.apply()
        }
}

/**
 * Turu nav grafiginin disindan (Yolculugum ekranindaki "Turu tekrar oyna")
 * acabilmek icin; GuestPrompt ile ayni desen.
 */
object OnboardingController {
    private val _visible = MutableStateFlow(false)
    val visible: StateFlow<Boolean> = _visible.asStateFlow()

    fun show() { _visible.value = true }
    fun hide() { _visible.value = false }
}
