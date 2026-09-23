package io.lunosfer.dreamap.util

/**
 * Kullaniciya gosterilebilecek hata metni. Supabase/HTTP istisnalari ham
 * mesajlarinda istek URL'sini (xxx.supabase.co/storage/...), header dokumunu
 * ve cok satirli teknik ayrintiyi tasiyor ve bunlar dogrudan Toast'a
 * basiliyordu. Teknik gorunen mesajda null doner; cagiran kendi
 * yerellestirilmis yedek metnini gosterir.
 */
fun safeMessage(t: Throwable?): String? {
    val msg = t?.message?.trim().orEmpty()
    if (msg.isEmpty()) return null
    val technical = msg.contains("://") ||
        msg.contains("supabase", ignoreCase = true) ||
        msg.contains('\n') ||
        msg.contains("Headers", ignoreCase = true) ||
        msg.length > 160
    return if (technical) null else msg
}

/** Uygulamanin kendi depolamasina yuklenmis dosya mi (adresi kullaniciya gosterilmez). */
fun isInternalMediaUrl(url: String?): Boolean =
    url != null && url.contains(".supabase.co/", ignoreCase = true)
