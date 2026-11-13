package io.lunosfer.dreamap.util

import org.json.JSONObject
import retrofit2.HttpException
import java.util.Collections
import java.util.WeakHashMap

/**
 * Retrofit'in hata govdesi (errorBody) yalnizca BIR KEZ okunabiliyor: ikinci
 * `.string()` bos donuyor. GuestMode, ApiErrors, ErrorText ve repository'ler
 * ayni istisnanin govdesine bakiyor; ilk okuyan govdeyi tukettigi icin
 * sonrakiler sunucunun hata kodunu (or. "not_visible", "guest_read_only")
 * hic goremiyordu. Govde istisna basina bir kez okunup burada saklanir
 * (WeakHashMap: istisna toplaninca kayit da gider).
 */
object HttpErrorBody {

    private val cache: MutableMap<HttpException, String> =
        Collections.synchronizedMap(WeakHashMap())

    fun read(error: HttpException): String? {
        cache[error]?.let { return it }
        val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull() ?: return null
        cache[error] = body
        return body
    }

    /** Sunucunun `{"error": "..."}` alani; yoksa null. */
    fun errorCode(error: HttpException): String? =
        read(error)
            ?.let { runCatching { JSONObject(it).optString("error") }.getOrNull() }
            ?.takeIf { it.isNotBlank() }
}
