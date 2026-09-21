package io.lunosfer.dreamap.util

import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
import org.json.JSONObject
import retrofit2.HttpException

/**
 * Aura gerektiren uclar (derin analiz 8, parlat 3, derin yorum 10) bakiye
 * yetmediginde HTTP 402 + {"error":"insufficient_auras"|"no_auras","cost":N}
 * donuyor. Retrofit 2xx disinda HttpException firlattigi icin govdedeki bu
 * bilgi hicbir yerde okunmuyordu; kullanici "Zihin duvari olusturulamadi" /
 * "Parlatma islemi basarisiz" gibi genel bir metin goruyor, PARASININ
 * yetmedigini ogrenemiyordu. Burasi o govdeyi cozup tipli hataya ceviriyor.
 */
class InsufficientAuraException(val cost: Int) : Exception("insufficient_auras")

object ApiErrors {

    private val AURA_ERROR_CODES = setOf("insufficient_auras", "no_auras")

    /** Retrofit/HTTP hatasini, anlamliysa tipli bir hataya cevirir; degilse aynen dondurur. */
    fun translate(error: Throwable): Throwable {
        val http = error as? HttpException ?: return error
        if (http.code() != 402) return error

        val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
            ?: return InsufficientAuraException(0)

        val json = runCatching { JSONObject(body) }.getOrNull() ?: return InsufficientAuraException(0)
        val code = json.optString("error")
        if (code !in AURA_ERROR_CODES) return error

        return InsufficientAuraException(json.optInt("cost", 0))
    }

    /**
     * Kullaniciya gosterilecek metin. Aura yetersizligini acikca soyler
     * (maliyet biliniyorsa rakamla), diger hatalarda cagiranin verdigi
     * yedek metne duser.
     */
    fun message(error: Throwable, fallback: String): String {
        val translated = translate(error)
        if (translated is InsufficientAuraException) {
            val app = DreamapApp.instance
            return if (translated.cost > 0) {
                app.getString(R.string.error_insufficient_auras_cost, translated.cost)
            } else {
                app.getString(R.string.error_insufficient_auras)
            }
        }
        return translated.message?.takeIf { it.isNotBlank() && !it.startsWith("HTTP ") } ?: fallback
    }

    /** Arayuzun "Aura al / Premium'a gec" teklifini gosterip gostermeyecegi. */
    fun isInsufficientAura(error: Throwable): Boolean = translate(error) is InsufficientAuraException
}
