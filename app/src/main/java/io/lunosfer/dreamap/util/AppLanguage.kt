package io.lunosfer.dreamap.util

import android.content.res.Resources
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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

    /** user_profiles_language_check ve res/values-* ile birebir ayni liste. */
    val SUPPORTED = listOf("en", "tr", "es", "fr", "de", "pt", "ru", "ar", "hi", "zh", "ja", "fi", "ro", "uk")

    @Serializable
    private data class LanguageRow(
        val language: String? = null,
        @SerialName("language_explicit") val languageExplicit: Boolean = false
    )

    /**
     * Dili profil secimine gore esitler. Kullanici dili Profil'den BILEREK
     * sectiyse (language_explicit) o uygulanir; secmediyse uygulama cihaz dilini
     * izler ve DB'deki language cihaz diline cekilir (push/AI metinleri icin).
     * Onceden kolon varsayilani 'en' her giriste zorla uygulandigi icin, dilini
     * hic secmemis herkes cihaz dilinden bagimsiz Ingilizce goruyordu.
     */
    suspend fun syncWithProfile(userId: String) {
        runCatching {
            val row = supabaseClient.postgrest["user_profiles"]
                .select(columns = Columns.list("language", "language_explicit")) { filter { eq("id", userId) } }
                .decodeList<LanguageRow>()
                .firstOrNull() ?: return

            val saved = row.language?.takeIf { it in SUPPORTED }
            if (row.languageExplicit && saved != null) {
                if (code() != saved) {
                    withContext(Dispatchers.Main) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(saved))
                    }
                }
                return
            }

            if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
                withContext(Dispatchers.Main) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
            }
            val device = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]?.language
            val target = device?.takeIf { it in SUPPORTED } ?: "en"
            if (target != row.language) {
                supabaseClient.postgrest["user_profiles"]
                    .update(mapOf("language" to target)) { filter { eq("id", userId) } }
            }
        }.onFailure { Log.w("AppLanguage", "Dil esitleme basarisiz", it) }
    }
}
