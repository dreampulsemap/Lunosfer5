package io.lunosfer.dreamap.util

import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max

/**
 * Bildirim/mesaj listelerinde ham "2026-09-13" yerine "3 saat once" gibi
 * okunabilir zaman. Sunucu tarihleri UTC geldigi icin once UTC olarak
 * cozumlenip cihazin saatiyle karsilastiriliyor.
 */
object RelativeTime {

    fun format(isoDate: String?): String {
        if (isoDate.isNullOrBlank()) return ""
        val date = parse(isoDate) ?: return isoDate.take(10)
        val app = DreamapApp.instance
        val rawDiffMs = System.currentTimeMillis() - date.time
        // Cihaz saati geride/ileride olabilir (veya sunucu saatiyle kayabilir):
        // gelecekteki bir tarihi "az once" diye gostermek yerine tam tarihi yaz.
        if (rawDiffMs < -2 * 60_000L) {
            return SimpleDateFormat("d MMM yyyy", AppLanguage.locale()).format(date)
        }
        val diffMs = max(0L, rawDiffMs)
        val minutes = diffMs / 60_000
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1L -> app.getString(R.string.time_just_now)
            minutes < 60L -> app.getString(R.string.time_minutes_ago, minutes.toInt())
            hours < 24L -> app.getString(R.string.time_hours_ago, hours.toInt())
            days < 7L -> app.getString(R.string.time_days_ago, days.toInt())
            else -> SimpleDateFormat("d MMM yyyy", AppLanguage.locale()).format(date)
        }
    }

    private fun parse(isoDate: String): Date? = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(isoDate.take(19))
    }.getOrNull()
}
