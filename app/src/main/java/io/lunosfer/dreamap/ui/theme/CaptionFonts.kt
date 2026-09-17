package io.lunosfer.dreamap.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Slayt altyazıları için 6 hazır stil. Google Fonts (indirilebilir font)
 * KULLANMIYORUZ — sertifika dosyası (font_certs.xml) tam olarak doğrulanamadan
 * eklenirse fontlar sessizce hiç yüklenmez, riskli. Bunun yerine Android'in
 * yerleşik 4 jenerik ailesini (Default/Serif/Monospace/Cursive) ağırlık ve
 * stil varyasyonlarıyla birleştirip 6 görsel olarak belirgin, %100 güvenilir
 * seçenek elde ediyoruz — hiçbir ek indirme/sertifika gerekmez.
 *
 * ÖNEMLİ backend notu: pages/api/goals/slides/update.js şu an
 * ALLOWED_FONTS = ['sans','serif','mono'] ile sınırlı. Aşağıdaki 3 yeni
 * anahtar (elegant/display/handwritten) TAM kalıcı olması için o dizinin
 * backend'de genişletilmesi gerekiyor — genişletilmezse bu 3 seçenek
 * arayüzde çalışır ama kaydedilen değer güncellenmez (sessizce yok sayılır,
 * hata vermez). Diğer tüm stil alanları (renk/boyut/konum/süre) bu
 * kısıtlamadan etkilenmiyor.
 */
data class CaptionFontStyle(
    val key: String,
    val family: FontFamily,
    val weight: FontWeight,
    val style: FontStyle = FontStyle.Normal,
    val letterSpacingEm: Float = 0f
)

val CAPTION_FONT_STYLES = listOf(
    CaptionFontStyle("elegant", FontFamily.Serif, FontWeight.Light, FontStyle.Italic),
    CaptionFontStyle("serif", FontFamily.Serif, FontWeight.SemiBold),
    CaptionFontStyle("display", FontFamily.SansSerif, FontWeight.ExtraBold),
    CaptionFontStyle("sans", FontFamily.SansSerif, FontWeight.Medium),
    CaptionFontStyle("handwritten", FontFamily.Cursive, FontWeight.Normal),
    CaptionFontStyle("mono", FontFamily.Monospace, FontWeight.Normal)
)

/** "En göze hoş görünen premium" varsayılan: zarif, ince italik serif + altın renk. */
const val DEFAULT_CAPTION_FONT_KEY = "elegant"
const val DEFAULT_CAPTION_COLOR_HEX = "#FBBF24"

fun captionFontStyleFor(key: String?): CaptionFontStyle =
    CAPTION_FONT_STYLES.find { it.key == key } ?: CAPTION_FONT_STYLES.first { it.key == "sans" }
