package io.lunosfer.dreamap.ui.components.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Doğrudan hedeflenen sosyal platformlar. Yalnızca cihazda yüklü olanlar
 * gösterilir; ikon, platformun kendi uygulama ikonudur (marka logosu
 * gömülmez). Paket görünürlüğü için AndroidManifest <queries> listesi bu
 * listeyle aynı tutulmalı.
 */
data class SharePlatform(
    /** record_share RPC'sindeki kanal adı (sunucudaki izin listesiyle aynı). */
    val channel: String,
    val label: String,
    val packages: List<String>,
    /** Hikâye paylaşımı gibi belirli bir hedef ekran (varsa). */
    val component: String? = null
)

data class InstalledPlatform(
    val platform: SharePlatform,
    val packageName: String,
    val icon: ImageBitmap?
)

object SharePlatforms {
    const val INSTAGRAM_PACKAGE = "com.instagram.android"
    private const val INSTAGRAM_STORY_COMPONENT = "com.instagram.share.handleractivity.StoryShareHandlerActivity"

    /** Instagram Hikâye düğmesinin etiketi dile göre değiştiği için ayrı tutulur. */
    fun instagramStory(label: String) =
        SharePlatform("instagram_story", label, listOf(INSTAGRAM_PACKAGE), INSTAGRAM_STORY_COMPONENT)

    private val platforms = listOf(
        SharePlatform("whatsapp", "WhatsApp", listOf("com.whatsapp", "com.whatsapp.w4b")),
        SharePlatform("instagram", "Instagram", listOf(INSTAGRAM_PACKAGE)),
        SharePlatform("x", "X", listOf("com.twitter.android")),
        SharePlatform("facebook", "Facebook", listOf("com.facebook.katana", "com.facebook.lite")),
        SharePlatform("messenger", "Messenger", listOf("com.facebook.orca", "com.facebook.mlite")),
        SharePlatform("telegram", "Telegram", listOf("org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram")),
        SharePlatform("snapchat", "Snapchat", listOf("com.snapchat.android")),
        SharePlatform("tiktok", "TikTok", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")),
        SharePlatform("threads", "Threads", listOf("com.instagram.barcelona")),
        SharePlatform("linkedin", "LinkedIn", listOf("com.linkedin.android")),
        SharePlatform("reddit", "Reddit", listOf("com.reddit.frontpage")),
        SharePlatform("pinterest", "Pinterest", listOf("com.pinterest")),
        SharePlatform("discord", "Discord", listOf("com.discord"))
    )

    /** Yüklü platformlar; Instagram Hikâye hedefi varsa Instagram'ın hemen önüne eklenir. */
    fun installed(context: Context, instagramStoryLabel: String): List<InstalledPlatform> {
        val pm = context.packageManager
        val result = mutableListOf<InstalledPlatform>()
        for (platform in platforms) {
            val pkg = platform.packages.firstOrNull { isInstalled(pm, it) } ?: continue
            val icon = runCatching { pm.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap() }.getOrNull()
            if (pkg == INSTAGRAM_PACKAGE) {
                storyTarget(context, instagramStoryLabel)?.let { result += it.copy(icon = icon) }
            }
            result += InstalledPlatform(platform, pkg, icon)
        }
        return result
    }

    /** Instagram'ın hikâye ekranı bu cihazda çözülebiliyorsa hedefi döner. */
    fun storyTarget(context: Context, label: String): InstalledPlatform? {
        val pm = context.packageManager
        if (!isInstalled(pm, INSTAGRAM_PACKAGE)) return null
        val probe = Intent(Intent.ACTION_SEND).setType("image/*").setClassName(INSTAGRAM_PACKAGE, INSTAGRAM_STORY_COMPONENT)
        if (probe.resolveActivity(pm) == null) return null
        val icon = runCatching { pm.getApplicationIcon(INSTAGRAM_PACKAGE).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        return InstalledPlatform(instagramStory(label), INSTAGRAM_PACKAGE, icon)
    }

    fun isInstagramInstalled(context: Context) = isInstalled(context.packageManager, INSTAGRAM_PACKAGE)

    /**
     * Görseli (ve varsa metni) doğrudan platforma gönderir. Belirli hedef ekran
     * açılamazsa aynı paketin genel paylaşım ekranına düşer.
     */
    fun launch(context: Context, target: InstalledPlatform, imageUri: Uri, text: String?): Boolean {
        val base = sendIntent(imageUri, text)
        val attempts = listOfNotNull(
            target.platform.component?.let { Intent(base).setClassName(target.packageName, it) },
            Intent(base).setPackage(target.packageName)
        )
        return attempts.any { intent -> runCatching { context.startActivity(intent) }.isSuccess }
    }

    /** Sistem paylaşım menüsü ("Diğer"). */
    fun launchChooser(context: Context, imageUri: Uri, text: String?, title: String): Boolean =
        runCatching { context.startActivity(Intent.createChooser(sendIntent(imageUri, text), title)) }.isSuccess

    private fun sendIntent(imageUri: Uri, text: String?) = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        if (!text.isNullOrBlank()) putExtra(Intent.EXTRA_TEXT, text)
        // Android 10+ paylaşım menüsü önizlemesi ve URI izni ClipData üzerinden taşınır.
        clipData = ClipData.newRawUri("", imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    @Suppress("DEPRECATION")
    private fun isInstalled(pm: PackageManager, pkg: String): Boolean =
        runCatching { pm.getPackageInfo(pkg, 0); true }.getOrDefault(false)
}
