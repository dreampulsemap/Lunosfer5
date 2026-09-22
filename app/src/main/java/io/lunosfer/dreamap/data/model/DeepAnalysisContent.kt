package io.lunosfer.dreamap.data.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * dreams.premium_deep_analysis'in cozumlenmis hali.
 *
 * Sunucu (pages/api/generate-deep-analysis.js) duz metin DEGIL, yapilandirilmis
 * bir JSON dondurur — web'deki DreamAnalysisView.jsx bunu bolumlere ayirarak
 * gosteriyor. Android tarafi ise yaniti `String` olarak cozmeye calisiyordu;
 * gelen JSON nesnesi oldugu icin cozumleme patliyor, hata runCatching'e
 * dusuyor ve ekranda "analiz gelmiyor" gibi gorunuyordu.
 *
 * Alanlar iki bicimde gelebiliyor: duz metin ya da {"tr": "...", "en": "..."}
 * dil haritasi (web'deki getVal/getArr ile ayni tolerans).
 */
data class DeepSymbol(
    val symbol: String,
    val meaning: String? = null,
    val intensity: Int? = null
)

data class DeepEmotion(
    val emotion: String,
    val score: Int? = null
)

data class DeepAnalysisContent(
    val title: String? = null,
    val summary: String? = null,
    val shadowFocus: String? = null,
    val coreConflict: String? = null,
    val symbolicReading: String? = null,
    val individuationPath: String? = null,
    val symbols: List<DeepSymbol> = emptyList(),
    val emotions: List<DeepEmotion> = emptyList(),
    val archetypes: List<String> = emptyList(),
    val reflectionQuestions: List<String> = emptyList()
) {
    val hasContent: Boolean
        get() = listOf(title, summary, shadowFocus, coreConflict, symbolicReading, individuationPath)
            .any { !it.isNullOrBlank() } ||
            symbols.isNotEmpty() || emotions.isNotEmpty() ||
            archetypes.isNotEmpty() || reflectionQuestions.isNotEmpty()

    companion object {

        fun from(element: JsonElement?, lang: String): DeepAnalysisContent? {
            if (element == null) return null

            // Eski kayitlar duz metin olarak saklanmisti; ozet gibi gosteriyoruz.
            if (element is JsonPrimitive) {
                val text = element.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
                return DeepAnalysisContent(summary = text)
            }

            val obj = element as? JsonObject ?: return null

            return DeepAnalysisContent(
                title = text(obj["title"], lang),
                summary = text(obj["summary"], lang),
                shadowFocus = text(obj["shadow_focus"], lang),
                coreConflict = text(obj["core_conflict"], lang),
                symbolicReading = text(obj["symbolic_reading"], lang),
                individuationPath = text(obj["individuation_path"], lang),
                symbols = array(obj["symbols"], lang).mapNotNull { symbol(it, lang) },
                emotions = array(obj["emotions"], lang).mapNotNull { emotion(it, lang) },
                archetypes = array(obj["archetypes"], lang).mapNotNull { text(it, lang) },
                reflectionQuestions = array(obj["reflection_questions"], lang).mapNotNull { text(it, lang) }
            ).takeIf { it.hasContent }
        }

        /** Duz metin ya da dil haritasindan istenen dildeki metni cikarir. */
        private fun text(element: JsonElement?, lang: String): String? = when (element) {
            null -> null
            is JsonPrimitive -> element.contentOrNull?.takeIf { it.isNotBlank() }
            is JsonObject -> {
                val picked = element[lang] ?: element["en"] ?: element.values.firstOrNull()
                (picked as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
            }
            else -> null
        }

        /** Dizi ya da dil haritasi altindaki diziyi dondurur. */
        private fun array(element: JsonElement?, lang: String): List<JsonElement> = when (element) {
            is JsonArray -> element
            is JsonObject -> {
                val picked = element[lang] ?: element["en"] ?: element.values.firstOrNull()
                (picked as? JsonArray)?.toList() ?: emptyList()
            }
            else -> emptyList()
        }

        private fun symbol(element: JsonElement, lang: String): DeepSymbol? {
            if (element is JsonPrimitive) {
                return element.contentOrNull?.takeIf { it.isNotBlank() }?.let { DeepSymbol(it) }
            }
            val obj = element as? JsonObject ?: return null
            val name = text(obj["symbol"] ?: obj["name"], lang) ?: return null
            return DeepSymbol(
                symbol = name,
                meaning = text(obj["meaning"], lang),
                intensity = (obj["intensity"] as? JsonPrimitive)?.intOrNull
            )
        }

        private fun emotion(element: JsonElement, lang: String): DeepEmotion? {
            if (element is JsonPrimitive) {
                return element.contentOrNull?.takeIf { it.isNotBlank() }?.let { DeepEmotion(it) }
            }
            val obj = element as? JsonObject ?: return null
            val name = text(obj["emotion"] ?: obj["name"], lang) ?: return null
            return DeepEmotion(
                emotion = name,
                score = (obj["score"] as? JsonPrimitive)?.intOrNull
                    ?: (obj["intensity"] as? JsonPrimitive)?.intOrNull
            )
        }
    }
}
