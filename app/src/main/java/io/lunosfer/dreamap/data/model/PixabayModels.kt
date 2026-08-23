package io.lunosfer.dreamap.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

object FlexibleTagsSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val input = decoder as? JsonDecoder ?: return emptyList()
        return try {
            when (val element = input.decodeJsonElement()) {
                is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }.filter { it.isNotEmpty() }
                is JsonPrimitive -> element.contentOrNull?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeSerializableValue(ListSerializer(String.serializer()), value)
    }
}

@Serializable
data class PixabaySearchResponse(
    val total: Int = 0,
    val totalHits: Int = 0,
    val hits: List<PixabayHit> = emptyList()
)

@Serializable
data class PixabayHit(
    val id: Long = 0,
    @Serializable(with = FlexibleTagsSerializer::class)
    val tags: List<String> = emptyList(),
    val webformatURL: String = "",
    val previewURL: String? = null,
    val largeImageURL: String? = null,
    val fullHDURL: String? = null,
    val imageURL: String? = null,
    val user: String = "",
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class PixabayImageRequest(
    val pixabayId: Long,
    val imageUrl: String,
    val tags: String = "",
    val pixabayUser: String = "",
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class PixabayImageResponse(
    val url: String? = null,
    val ok: Boolean? = true,
    val error: String? = null
)

@Serializable
data class PixabayVideoImportRequest(
    val pixabayId: Long,
    val videoUrl: String,
    val tags: String = "",
    val user: String = ""
)

@Serializable
data class PixabayVideoImportResponse(
    val url: String? = null,
    val ok: Boolean? = true,
    val error: String? = null
)

@Serializable
data class PixabayVideoSearchResponse(
    val total: Int = 0,
    val totalHits: Int = 0,
    val hits: List<PixabayVideoHit> = emptyList()
)

@Serializable
data class PixabayVideoHit(
    val id: Long = 0,
    @Serializable(with = FlexibleTagsSerializer::class)
    val tags: List<String> = emptyList(),
    val pageURL: String? = null,
    val duration: Int = 0,
    val user: String = "",
    val picture_id: String? = null,
    val videos: PixabayVideoDetailsMap? = null
)

@Serializable
data class PixabayVideoDetailsMap(
    val large: PixabayVideoFormat? = null,
    val medium: PixabayVideoFormat? = null,
    val small: PixabayVideoFormat? = null,
    val tiny: PixabayVideoFormat? = null
)

@Serializable
data class PixabayVideoFormat(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val size: Long = 0,
    val thumbnail: String? = null
)

sealed class PixabaySelectedMedia {
    abstract val id: Long
    abstract val tags: String
    abstract val user: String
    abstract val previewUrl: String

    data class Image(
        override val id: Long,
        val imageUrl: String,
        override val tags: String,
        override val user: String,
        override val previewUrl: String = imageUrl
    ) : PixabaySelectedMedia()

    data class Video(
        override val id: Long,
        val videoUrl: String,
        override val tags: String,
        override val user: String,
        val durationSeconds: Int,
        override val previewUrl: String = ""
    ) : PixabaySelectedMedia()
}


