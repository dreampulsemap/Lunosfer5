package io.lunosfer.dreamap.util

import android.net.Uri
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.DreamDetail
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.model.SharedRef
import io.lunosfer.dreamap.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val SHARE_BASE_URL = "https://lunosfer.com"

enum class ShareKind(val path: String) {
    DREAM("dream"), DIARY("diary"), VISION("vision");

    companion object {
        fun from(path: String?): ShareKind? = entries.firstOrNull { it.path == path }
    }
}

/** Paylaşım sayfasında gösterilen ve karta çizilen içerik. */
data class ShareContent(
    val kind: ShareKind,
    val id: String,
    val title: String,
    val body: String?,
    val imageUrl: String?,
    val ownerId: String?,
    val ownerName: String?,
    val visibility: String?,
    val isOwner: Boolean
) {
    val isPublic: Boolean get() = visibility == "public"

    /** Sunucudaki kuralın aynısı (lib/shareSnapshot.js): sahibi ya da herkese açık. */
    val canShare: Boolean get() = isOwner || isPublic

    /** Önizlemeli bağlantı yalnızca herkese açık içerikte işe yarar; gizlide sayfa "gizli" der. */
    val webUrl: String get() = "$SHARE_BASE_URL/share/${kind.path}/$id"
}

/** Herhangi bir ekrandan paylaşım sayfasını açar; MainScreen tek bir ShareSheetHost çizer. */
object ShareController {
    private val _request = MutableStateFlow<ShareContent?>(null)
    val request: StateFlow<ShareContent?> = _request.asStateFlow()

    fun open(content: ShareContent) {
        _request.value = content
    }

    fun close() {
        _request.value = null
    }
}

private val UUID_RE = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

private fun firstLine(text: String?, max: Int): String =
    text.orEmpty().lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.let {
        if (it.length > max) it.take(max - 1).trimEnd() + "…" else it
    }.orEmpty()

fun DreamDetail.toShareContent(currentUserId: String?) = ShareContent(
    kind = ShareKind.DREAM,
    id = id.toString(),
    title = aiTitle?.takeIf { it.isNotBlank() } ?: firstLine(content, 80),
    body = content,
    imageUrl = displayImageUrl,
    ownerId = userId,
    ownerName = owner?.nameOrFallback,
    visibility = visibility,
    isOwner = currentUserId != null && currentUserId == userId
)

fun Dream.toShareContent(currentUserId: String?) = ShareContent(
    kind = ShareKind.DREAM,
    id = id.toString(),
    title = aiTitle?.takeIf { it.isNotBlank() } ?: firstLine(content, 80),
    body = content,
    imageUrl = aiImageUrl?.takeIf { it.isNotBlank() && imageStatus != "broken" },
    ownerId = userId,
    ownerName = owner?.nameOrFallback,
    visibility = visibility ?: "public",
    isOwner = currentUserId != null && currentUserId == userId
)

fun Goal.toShareContent(currentUserId: String?) = ShareContent(
    kind = ShareKind.VISION,
    id = id,
    title = title,
    body = description,
    imageUrl = coverImageUrl?.takeIf { it.isNotBlank() },
    ownerId = userId,
    ownerName = owner?.nameOrFallback,
    visibility = visibility,
    isOwner = currentUserId != null && currentUserId == userId
)

fun DiaryEntry.toShareContent(owner: UserProfile?, ownerId: String, currentUserId: String?) = ShareContent(
    kind = ShareKind.DIARY,
    id = id,
    title = firstLine(caption, 80),
    body = caption,
    imageUrl = if (mediaType == "photo") mediaUrl else posterUrl,
    ownerId = ownerId,
    ownerName = owner?.nameOrFallback,
    visibility = visibility,
    isOwner = currentUserId != null && currentUserId == ownerId
)

/** Günce girdisinin sahibini bilmeden açılan bağlantılar için çözümleyici rota. */
const val SHARED_DIARY_ROUTE = "shared_diary/{entryId}"

/**
 * https://lunosfer.com/share/{tip}/{id} ya da io.lunosfer.dreamap://share/{tip}/{id}
 * bağlantısını uygulama içi rotaya çevirir; tanınmazsa null.
 */
fun shareRouteFromUri(uri: Uri?): String? {
    uri ?: return null
    val segments = when {
        uri.scheme == "io.lunosfer.dreamap" && uri.host == "share" -> uri.pathSegments
        (uri.scheme == "https" || uri.scheme == "http") &&
            (uri.host == "lunosfer.com" || uri.host == "www.lunosfer.com") &&
            uri.pathSegments.firstOrNull() == "share" -> uri.pathSegments.drop(1)
        else -> return null
    }
    if (segments.size < 2) return null
    return routeFor(segments[0], segments[1], ownerId = null)
}

/** DM'deki paylaşım kartına dokununca gidilecek ekran. */
fun SharedRef.route(): String? = routeFor(type, id, ownerId)

private fun routeFor(type: String, id: String, ownerId: String?): String? = when (ShareKind.from(type)) {
    ShareKind.DREAM -> id.toLongOrNull()?.let { "dream/$it" }
    ShareKind.VISION -> id.takeIf { UUID_RE.matches(it) }?.let { "goal/$it" }
    ShareKind.DIARY -> id.takeIf { UUID_RE.matches(it) }?.let { entryId ->
        if (ownerId != null) "diary_viewer/$ownerId?entry=$entryId" else "shared_diary/$entryId"
    }
    null -> null
}
