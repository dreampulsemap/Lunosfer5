package io.lunosfer.dreamap.util

import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

/**
 * Kullanicinin uygulama icinde sectigi dil (Profil > Uygulama Dili,
 * AppCompatDelegate.setApplicationLocales ile uygulanir).
 *
 * Locale.getDefault() burada yeterli DEGIL: per-app dil secimi cihazin
 * sistem diline yansimayabiliyor, bu yuzden uygulama Turkce'yken
 * sunucudan gelen AI metinleri Ingilizce donuyordu (canli testte
 * dogrulandi, bkz. DailyCompassViewModel ve CreateDreamScreen'deki
 * ayni sinif hatalar). Sunucuya "hangi dilde uretilsin/cevrilsin"
 * bilgisi giden HER yerde bu yardimci kullanilmali.
 */
object AppLanguage {

    /** ISO 639-1 dil kodu ("tr", "en", ...). */
    fun code(): String {
        val locale = locale()
        return locale.language.takeIf { it.isNotBlank() } ?: "en"
    }

    /** Tarih/saat bicimlendirmede kullanilacak Locale. */
    fun locale(): Locale {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val selected = if (!appLocales.isEmpty) appLocales[0] else null
        return selected ?: Locale.getDefault()
    }
}
