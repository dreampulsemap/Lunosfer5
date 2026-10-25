package io.lunosfer.dreamap.util

import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
import retrofit2.HttpException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Locale

/**
 * Kullaniciya gosterilen hata metinleri.
 *
 * Liste ekranlari `throwable.message`'i oldugu gibi basiyordu; sonuc, akis
 * yuklenemediginde ekranda su cikiyordu:
 * "failed to connect to www.lunosfer.com/216.198.79.1 (port 443) from
 *  /10.0.2.16 (port 36610) after 15000ms" (canli goruldu).
 * Teknik ayrinti kullaniciya bir sey anlatmiyor; ag/zaman asimi/sunucu
 * hatalarini anlasilir mesaja cevirir.
 *
 * Retrofit 2xx disindaki her yaniti "HTTP 403 " gibi bir mesajla
 * HttpException'a sariyor. ViewModel'lerdeki `err.message ?: yedekMetin`
 * kalibinda bu mesaj hic null olmadigi icin yedek metin hic kullanilmiyor,
 * kullanici "HTTP 403" goruyordu (canli goruldu: gizli bir ruyayi acinca).
 * `friendly(err, yedekMetin)` durum koduna (ve sunucunun `error` koduna)
 * gore anlasilir metin, taniyamadigi hatada ekranin kendi yedek metnini verir.
 */
object ErrorText {

    private val HTTP_STATUS = Regex("^http (\\d{3})\\b")
    private val SNAKE_CASE_CODE = Regex("^[a-z0-9]+(_[a-z0-9]+)+$")
    private val AURA_ERROR_CODES = setOf("insufficient_auras", "no_auras")

    fun friendly(throwable: Throwable?, fallback: String? = null): String {
        // Guvenlik agi: proaktif kapatmayi atladigimiz bir yazma islemi
        // sunucudan "misafir yazamaz" ile donerse, kullaniciya ham hata
        // yerine kayit daveti gosterilsin (bkz. util/GuestMode.kt).
        if (GuestMode.isGuestWriteBlocked(throwable)) {
            GuestPrompt.show()
            return DreamapApp.instance.getString(R.string.error_guest_read_only)
        }
        val app = DreamapApp.instance
        return when (throwable) {
            is InsufficientAuraException -> ApiErrors.message(throwable, fallback ?: app.getString(R.string.error_insufficient_auras))
            is HttpException -> forStatus(throwable.code(), HttpErrorBody.errorCode(throwable), throwable, fallback)
            is UnknownHostException, is ConnectException, is NoRouteToHostException ->
                app.getString(R.string.error_no_connection)
            is SocketTimeoutException -> app.getString(R.string.error_timeout)
            else -> friendly(throwable?.message, fallback)
        }
    }

    /**
     * Hata siniflandirmasi (`contains(...)` kontrolleri) icin metin: istisna
     * mesaji + HttpException ise sunucunun `error` kodu. Retrofit mesaji
     * yalnizca "HTTP 400 " oldugu icin "already_reacted", "no_active_goals"
     * gibi kodlara bakan kontroller hic eslesmiyor, kullanici ozel mesaj
     * yerine genel hata goruyordu.
     */
    fun matchText(throwable: Throwable?): String {
        val code = (throwable as? HttpException)?.let { HttpErrorBody.errorCode(it) }
        return listOfNotNull(throwable?.message, code).joinToString(" ")
    }

    fun friendly(message: String?, fallback: String? = null): String {
        val app = DreamapApp.instance
        val unknown = fallback ?: app.getString(R.string.error_unknown)
        val raw = (message ?: "").trim().lowercase(Locale.US)
        val status = HTTP_STATUS.find(raw)?.groupValues?.get(1)?.toIntOrNull()
        return when {
            raw.isBlank() -> unknown
            status != null -> forStatus(status, null, null, fallback)
            raw.contains("failed to connect") || raw.contains("unable to resolve host") ||
                raw.contains("no address associated") || raw.contains("network is unreachable") ||
                raw.contains("connection refused") || raw.contains("unknownhost") ->
                app.getString(R.string.error_no_connection)
            raw.contains("timeout") || raw.contains("timed out") ->
                app.getString(R.string.error_timeout)
            raw.contains(" 500") || raw.contains("502") || raw.contains("503") ->
                app.getString(R.string.error_server)
            raw.contains("401") || raw.contains("unauthorized") ->
                app.getString(R.string.error_session_expired)
            // Sunucunun kendi (okunabilir) mesaji: oldugu gibi goster.
            isReadable(raw) -> message?.trim() ?: unknown
            else -> unknown
        }
    }

    // Ham hata kodlari ("goal_not_found" gibi), yigin izleri, URL ve header
    // dokumleri okunabilir metin degil.
    private fun isReadable(raw: String): Boolean =
        raw.length <= 160 && !raw.contains('\n') && !raw.contains("java.") &&
            !raw.contains("exception") && !raw.contains("/") && !raw.contains("supabase") &&
            !raw.contains("headers") && !SNAKE_CASE_CODE.matches(raw)

    private fun forStatus(code: Int, errorCode: String?, error: HttpException?, fallback: String?): String {
        val app = DreamapApp.instance
        return when {
            code == 401 -> app.getString(R.string.error_session_expired)
            code == 402 && error != null && errorCode != null && errorCode in AURA_ERROR_CODES ->
                ApiErrors.message(error, fallback ?: app.getString(R.string.error_insufficient_auras))
            code == 403 && errorCode == "not_visible" -> app.getString(R.string.error_content_private)
            code == 403 -> app.getString(R.string.common_error_not_authorized)
            code == 404 -> app.getString(R.string.error_content_not_found)
            code == 408 || code == 504 -> app.getString(R.string.error_timeout)
            code == 429 -> app.getString(R.string.auth_error_rate_limited)
            code >= 500 -> app.getString(R.string.error_server)
            // 400/409/413...: sunucu okunabilir bir cumle donduyse (or.
            // "Bu rüyayı silme yetkiniz yok") onu, kod donduyse ekranin
            // kendi yedek metnini goster.
            else -> errorCode?.trim()?.takeIf { isReadable(it.lowercase(Locale.US)) }
                ?: fallback ?: app.getString(R.string.error_unknown)
        }
    }
}
