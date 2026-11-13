package io.lunosfer.dreamap.util

/**
 * Kullaniciya gosterilebilecek hata metni. Supabase/HTTP istisnalari ham
 * mesajlarinda istek URL'sini (xxx.supabase.co/storage/...), header dokumunu
 * ve cok satirli teknik ayrintiyi tasiyor ve bunlar dogrudan Toast'a
 * basiliyordu. Teknik gorunen mesajda null doner; cagiran kendi
 * yerellestirilmis yedek metnini gosterir.
 */
private val HTTP_STATUS_MESSAGE = Regex("^HTTP \\d{3}\\b")

fun safeMessage(t: Throwable?): String? {
    val msg = t?.message?.trim().orEmpty()
    if (msg.isEmpty()) return null
    val technical = msg.contains("://") ||
        // Retrofit'in "HTTP 403 " mesaji: durum koduna gore anlasilir metin
        // icin ErrorText.friendly kullanilmali; burada yedek metne dussun.
        HTTP_STATUS_MESSAGE.containsMatchIn(msg) ||
        msg.contains("supabase", ignoreCase = true) ||
        msg.contains('\n') ||
        msg.contains("Headers", ignoreCase = true) ||
        msg.length > 160
    return if (technical) null else msg
}

/** Uygulamanin kendi depolamasina yuklenmis dosya mi (adresi kullaniciya gosterilmez). */
fun isInternalMediaUrl(url: String?): Boolean =
    url != null && url.contains(".supabase.co/", ignoreCase = true)
