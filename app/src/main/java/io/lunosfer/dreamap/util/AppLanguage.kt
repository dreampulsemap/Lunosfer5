package io.lunosfer.dreamap.util

import android.content.Context
import android.content.res.Resources
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    private const val PREFS = "app_language"
    private const val KEY_LEGACY_RESET_DONE = "legacy_en_reset_done"

    @Volatile
    private var syncedUserId: String? = null

    /**
     * Uygulama dilini profil ile esitler.
     * - Kullanici dili Profil'den BILEREK sectiyse (language_explicit) o uygulanir.
     * - Secmediyse kullanicinin cihazda yaptigi secime dokunulmaz: uygulama icinde
     *   ya da Android'in "Uygulama dili" ayarindan secilmis bir dil varsa korunur,
     *   yoksa uygulama cihaz dilini izler.
     * - Tek istisna (bir kerelik): eski surumler DB'deki 'en' VARSAYILANINI her
     *   giriste zorla uyguluyordu. Uygulama dili tam o zorlanmis 'en' ise temizlenir.
     *   (1.4.6'da BUTUN yerel secimler temizleniyordu; Turkce secmis kullanicilar
     *   Ingilizceye dustu - bu surumde duzeltildi.)
     * DB'deki language de fiili dile cekilir ki push/AI metinleri ayni dilde olsun.
     * Ayni surecte ayni kullanici icin bir kez calisir (dil degisince Activity
     * yeniden olusur; tekrar calisip secimi ezmesin).
     */
    suspend fun syncWithProfile(userId: String) {
        if (syncedUserId == userId) return
        syncedUserId = userId
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

            val prefs = DreamapApp.instance.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            var appLocale = AppCompatDelegate.getApplicationLocales()
                .takeIf { !it.isEmpty }?.get(0)?.language?.takeIf { it in SUPPORTED }
            if (appLocale == "en" && row.language == "en" && !prefs.getBoolean(KEY_LEGACY_RESET_DONE, false)) {
                withContext(Dispatchers.Main) {
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                }
                appLocale = null
            }
            prefs.edit().putBoolean(KEY_LEGACY_RESET_DONE, true).apply()

            val device = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]?.language
            val effective = appLocale ?: device?.takeIf { it in SUPPORTED } ?: "en"
            // Cihazda bilincli secilmis bir uygulama dili varsa onu kalici secim say.
            val explicitNow = appLocale != null
            if (effective != row.language || explicitNow) {
                supabaseClient.postgrest["user_profiles"].update(
                    buildMap {
                        put("language", JsonPrimitive(effective))
                        if (explicitNow) put("language_explicit", JsonPrimitive(true))
                    }.let { JsonObject(it) }
                ) {
                    filter {
                        eq("id", userId)
                        eq("language_explicit", false)
                    }
                }
            }
        }.onFailure { Log.w("AppLanguage", "Dil esitleme basarisiz", it) }
    }

    /** Oturum kapaninca bir sonraki kullanici icin tekrar calissin. */
    fun resetSync() {
        syncedUserId = null
    }
}
