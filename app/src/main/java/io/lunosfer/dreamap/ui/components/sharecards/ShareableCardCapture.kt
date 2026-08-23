package io.lunosfer.dreamap.ui.components.sharecards

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Makes any card composable capturable + shareable, rendered entirely on
 * device (Compose draws it, no image-gen API call, no server round trip).
 *
 * Reuses the exact FileProvider authority already declared in
 * AndroidManifest.xml ("${applicationId}.fileprovider", the same one
 * VideoEditorScreen.kt's launchCamera() uses) — only res/xml/file_paths.xml
 * needed a new <cache-path> entry ("shared_cards"), which has been added
 * alongside the existing "reel_recordings" one. No new provider, no new
 * manifest entry.
 *
 * Usage:
 *   val (captureModifier, cardHost) = rememberShareableCardHost()
 *   DreamArchetypeCard(data = ..., modifier = captureModifier)
 *   // on the Share button's onClick, inside a coroutine scope:
 *   cardHost.share(context, cardHost.captureBitmap())
 *
 * NOTE: written outside an Android build environment (no Gradle/Android SDK
 * available where this was generated), so it hasn't actually been compiled.
 * Checked rememberGraphicsLayer specifically against this project's pinned
 * compose-bom (2024.09.00, gradle/libs.versions.toml) — that BOM lines up
 * with Compose UI 1.7.0, which is the release that introduced this API, so
 * it should resolve, but it's right at the introduction boundary rather
 * than several versions past it. If Android Studio can't resolve the
 * import, bumping compose-bom to a later 2024.09.0x/2024.10.00 patch is a
 * safe, small fix.
 */
class ShareableCardHost(private val graphicsLayer: GraphicsLayer) {

    suspend fun captureBitmap(): Bitmap =
        graphicsLayer.toImageBitmap().asAndroidBitmap()

    /** Saves the bitmap to cache and opens the system share sheet. */
    fun share(context: Context, bitmap: Bitmap, shareText: String = "") {
        val dir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(dir, "lunosfer_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (shareText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}

@Composable
fun rememberShareableCardHost(): Pair<Modifier, ShareableCardHost> {
    val graphicsLayer = rememberGraphicsLayer()
    val captureModifier = Modifier.drawWithContent {
        graphicsLayer.record { this@drawWithContent.drawContent() }
        drawLayer(graphicsLayer)
    }
    val host = remember(graphicsLayer) { ShareableCardHost(graphicsLayer) }
    return captureModifier to host
}
