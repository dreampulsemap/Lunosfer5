package io.lunosfer.dreamap.util

import android.util.Base64
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

/**
 * Misafir (anonim) oturum tespiti ve sunucunun "misafirler salt okunur"
 * cevabinin taninmasi.
 *
 * Neden JWT claim'i: sunucu tarafindaki kisitlamayi uygulayan
 * `public.block_guest_writes()` trigger'i TAM OLARAK
 * `auth.jwt() ->> 'is_anonymous'` degerine bakiyor. Istemci de ayni claim'i
 * okursa istemci ile sunucu asla farkli karar veremez. `email` bos mu diye
 * bakmak yaklasik bir yontemdi (ProfileScreen sifre degistirmeyi bununla
 * gizliyordu) ve e-postasini henuz dogrulamamis gercek kullanicilari da
 * misafir sayabilirdi; bu yuzden sadece claim okunamazsa yedek olarak
 * kullaniliyor.
 */
object GuestMode {

    private val json = Json { ignoreUnknownKeys = true }

    /** Oturum anonim mi? Oturum yoksa false. */
    fun isGuest(): Boolean {
        val token = supabaseClient.auth.currentSessionOrNull()?.accessToken
        val claim = token?.let { isAnonymousClaim(it) }
        if (claim != null) return claim
        // Yedek: JWT cozulemezse (beklenmez) e-postasiz oturumu misafir say.
        return supabaseClient.auth.currentUserOrNull()?.email.isNullOrBlank()
    }

    /** JWT payload'undaki `is_anonymous`; cozulemezse null. */
    private fun isAnonymousClaim(accessToken: String): Boolean? = runCatching {
        val payload = accessToken.split('.').getOrNull(1) ?: return@runCatching null
        val decoded = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val obj = json.parseToJsonElement(decoded.toString(Charsets.UTF_8)) as? JsonObject
        obj?.get("is_anonymous")?.jsonPrimitive?.booleanOrNull
    }.getOrNull()

    /**
     * Sunucu bu hatayi "misafir yazamaz" diye mi dondu?
     *
     * Iki farkli yoldan gelebiliyor, ikisi de taninmali:
     *  - Next.js API uclari: HTTP 403 + `{"error":"guest_read_only"}`
     *  - Dogrudan PostgREST cagrilari: trigger `errcode 42501` ile
     *    "Guest accounts are read-only" firlatiyor, PostgREST bunu 403'e
     *    ceviriyor.
     */
    fun isGuestWriteBlocked(error: Throwable?): Boolean {
        if (error == null) return false
        val bodyText = (error as? HttpException)?.let { http ->
            if (http.code() != 403) return false
            runCatching { http.response()?.errorBody()?.string() }.getOrNull()
        }
        val haystack = listOfNotNull(bodyText, error.message).joinToString(" ").lowercase()
        return haystack.contains("guest_read_only") ||
            haystack.contains("guest accounts are read-only") ||
            (haystack.contains("42501") && haystack.contains("guest"))
    }
}

/**
 * "Ucretsiz hesap olustur" alt sayfasinin gorunurlugu.
 *
 * GlobalContentPicker ile ayni gerekce: tetikleyen yerler (ViewModel'ler,
 * repository hata yollari, dialog icindeki butonlar) navController'a sahip
 * degil; tek bir gozlemlenebilir singleton, alt sayfayi MainScreen'de bir
 * kez cizip her yerden acilabilir kiliyor.
 */
object GuestPrompt {
    private val _visible = MutableStateFlow(false)
    val visibleFlow: StateFlow<Boolean> = _visible.asStateFlow()

    fun show() { _visible.value = true }
    fun dismiss() { _visible.value = false }

    /**
     * Guvenlik agi: proaktif olarak kapatmayi atladigimiz bir yazma islemi
     * sunucudan "misafir yazamaz" ile donerse, kullaniciya ham hata yerine
     * yine kayit daveti gosterilir.
     */
    fun showIfGuestBlocked(error: Throwable?) {
        if (GuestMode.isGuestWriteBlocked(error)) show()
    }
}

/**
 * Yazma islemini yalnizca kayitli kullanici icin calistirir; misafirse
 * eylemi hic denemeden kayit davetini acar.
 */
inline fun requireAccount(action: () -> Unit) {
    if (GuestMode.isGuest()) GuestPrompt.show() else action()
}
