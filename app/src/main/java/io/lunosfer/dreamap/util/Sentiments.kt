package io.lunosfer.dreamap.util

import io.lunosfer.dreamap.R

/**
 * ai_sentiment sunucudan kucuk harfli tek Ingilizce kelime geliyor
 * (analyze-dream.js: hopeful, anxious, mysterious...). Ekranda uygulama
 * diline cevrilir; bilinmeyen kelime icin null -> cagiran ham degeri gosterir.
 */
fun sentimentResId(raw: String): Int? = when (raw.trim().lowercase(java.util.Locale.ROOT)) {
    "mysterious", "mystical", "mystic", "eerie" -> R.string.sentiment_mysterious
    "tender", "gentle", "warm" -> R.string.sentiment_tender
    "restless", "uneasy", "agitated" -> R.string.sentiment_restless
    "heavy", "somber", "sombre", "gloomy" -> R.string.sentiment_heavy
    "luminous", "bright", "radiant" -> R.string.sentiment_luminous
    "hopeful", "hope" -> R.string.dream_emotion_hope
    "anxious", "anxiety", "tense", "nervous" -> R.string.dream_emotion_anxiety
    "joyful", "joy", "happy", "playful" -> R.string.dream_emotion_joy
    "peaceful", "peace", "calm", "serene" -> R.string.dream_emotion_peace
    "loving", "love" -> R.string.dream_emotion_love
    "awe", "awed", "awestruck", "wonder" -> R.string.dream_emotion_awe
    "surprised", "surprise" -> R.string.dream_emotion_surprise
    "curious", "curiosity" -> R.string.dream_emotion_curiosity
    "confused", "confusion", "disoriented" -> R.string.dream_emotion_confusion
    "fearful", "fear", "afraid", "scared", "frightened", "terrified" -> R.string.dream_emotion_fear
    "sad", "sadness", "melancholic", "melancholy", "sorrowful" -> R.string.dream_emotion_sadness
    "lonely", "loneliness", "isolated" -> R.string.dream_emotion_loneliness
    "angry", "anger", "frustrated" -> R.string.dream_emotion_anger
    "ashamed", "shame", "guilty" -> R.string.dream_emotion_shame
    "disgusted", "disgust" -> R.string.dream_emotion_disgust
    "relieved", "relief" -> R.string.dream_emotion_relief
    else -> null
}
