package io.lunosfer.dreamap.util

import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
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
 */
object ErrorText {

    fun friendly(throwable: Throwable?): String = friendly(throwable?.message)

    fun friendly(message: String?): String {
        val app = DreamapApp.instance
        val raw = (message ?: "").lowercase(Locale.US)
        return when {
            raw.isBlank() -> app.getString(R.string.error_unknown)
            raw.contains("failed to connect") || raw.contains("unable to resolve host") ||
                raw.contains("no address associated") || raw.contains("network is unreachable") ||
                raw.contains("connection refused") || raw.contains("unknownhost") ->
                app.getString(R.string.error_no_connection)
            raw.contains("timeout") || raw.contains("timed out") ->
                app.getString(R.string.error_timeout)
            raw.contains("http 5") || raw.contains(" 500") || raw.contains("502") || raw.contains("503") ->
                app.getString(R.string.error_server)
            raw.contains("401") || raw.contains("unauthorized") ->
                app.getString(R.string.error_session_expired)
            // Sunucunun kendi (okunabilir) mesaji: oldugu gibi goster.
            raw.length <= 120 && !raw.contains("java.") && !raw.contains("exception") && !raw.contains("/") ->
                message ?: app.getString(R.string.error_unknown)
            else -> app.getString(R.string.error_unknown)
        }
    }
}
