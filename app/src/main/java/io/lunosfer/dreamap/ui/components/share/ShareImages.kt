package io.lunosfer.dreamap.ui.components.share

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.util.ShareContent
import io.lunosfer.dreamap.util.ShareKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date
import java.util.Random

/**
 * Paylaşım görselleri tamamen cihazda çizilir (sunucu / görsel-üretim çağrısı
 * yok). Boyut Instagram / WhatsApp hikâyesi için 9:16 (1080×1920); aynı görsel
 * sohbet uygulamalarında da düzgün görünür.
 *
 * [watermark] = true: alta logo + lunosfer.com şeridi (dış paylaşım / indirme).
 * false: Lunosfer'in kendi güncesine eklenen sade sürüm.
 */
object ShareImages {
    const val WIDTH = 1080
    const val HEIGHT = 1920

    private const val GOLD = 0xFFE6C687.toInt()
    private const val VOID = 0xFF04060E.toInt()
    private const val VOID_900 = 0xFF090D1A.toInt()
    private const val VIOLET_DEEP = 0xFF1B1440.toInt()
    private const val VIOLET = 0xFFA855F7.toInt()
    private const val CYAN = 0xFF38BDF8.toInt()
    private const val SILVER = 0xFF94A3B8.toInt()
    private const val PANEL = 0xD9121826.toInt()

    private const val CACHE_DIR = "shared_cards"
    private const val CACHE_MAX_AGE_MS = 24L * 60 * 60 * 1000

    private val serifBold: Typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    private val sans: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    private val sansBold: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    private val sansItalic: Typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)

    // ------------------------------------------------------------------
    // Rüya / günce / vizyon kartı
    // ------------------------------------------------------------------

    suspend fun renderContent(context: Context, content: ShareContent, watermark: Boolean = true): Bitmap {
        val photo = content.imageUrl?.let { loadBitmap(context, it) }
        return withContext(Dispatchers.Default) {
            val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            drawBackground(c, seed = content.id.hashCode(), accent = VIOLET)
            drawHeader(c, context)

            val typeLabel = context.getString(
                when (content.kind) {
                    ShareKind.DREAM -> R.string.share_card_dream
                    ShareKind.DIARY -> R.string.share_card_diary
                    ShareKind.VISION -> R.string.share_card_vision
                }
            )
            val emoji = when (content.kind) {
                ShareKind.DREAM -> "🌙"
                ShareKind.DIARY -> "📖"
                ShareKind.VISION -> "✨"
            }
            drawPill(c, "$emoji  ${typeLabel.uppercase()}", centerY = 262f, color = GOLD)

            val panelTop = 330f
            val panelBottom = if (watermark) 1610f else 1800f
            val panel = RectF(70f, panelTop, WIDTH - 70f, panelBottom)
            drawPanel(c, panel, GOLD)

            val innerLeft = panel.left + 50f
            val innerWidth = (panel.width() - 100f).toInt()
            var y = panel.top + 50f

            if (photo != null) {
                val imageHeight = if (content.body.isNullOrBlank() && content.title.isBlank()) 1000f else 660f
                val dst = RectF(innerLeft, y, innerLeft + innerWidth, y + imageHeight)
                drawCenterCrop(c, photo, dst, radius = 36f)
                y = dst.bottom + 56f
            } else {
                val moonPaint = textPaint(serifBold, 150f, GOLD).apply { textAlign = Paint.Align.CENTER }
                c.drawText(if (content.kind == ShareKind.VISION) "✦" else "☾", WIDTH / 2f, y + 150f, moonPaint)
                y += 220f
            }

            val ownerLine = content.ownerName?.takeIf { it.isNotBlank() }?.let { "— $it" }
            val ownerPaint = textPaint(sans, 34f, SILVER)
            val contentBottom = panel.bottom - (if (ownerLine != null) 120f else 60f)

            if (content.title.isNotBlank()) {
                val titleLayout = layout(content.title, textPaint(serifBold, 62f, GOLD), innerWidth, maxLines = 3, align = Layout.Alignment.ALIGN_NORMAL, spacing = 1.12f)
                c.save(); c.translate(innerLeft, y); titleLayout.draw(c); c.restore()
                y += titleLayout.height + 34f
            }

            val body = content.body?.trim()?.takeIf { it.isNotBlank() && it != content.title }
            if (body != null) {
                val bodyPaint = textPaint(sans, 40f, 0xE0FFFFFF.toInt())
                val lineHeight = bodyPaint.fontSpacing * 1.3f
                val lines = ((contentBottom - y) / lineHeight).toInt().coerceIn(0, 22)
                if (lines > 0) {
                    val bodyLayout = layout(body, bodyPaint, innerWidth, maxLines = lines, align = Layout.Alignment.ALIGN_NORMAL, spacing = 1.3f)
                    c.save(); c.translate(innerLeft, y); bodyLayout.draw(c); c.restore()
                }
            }

            if (ownerLine != null) {
                val ownerLayout = layout(ownerLine, ownerPaint, innerWidth, maxLines = 1, align = Layout.Alignment.ALIGN_NORMAL, spacing = 1f)
                c.save(); c.translate(innerLeft, panel.bottom - 50f - ownerLayout.height); ownerLayout.draw(c); c.restore()
            }

            if (watermark) drawWatermark(c, context)
            bmp
        }
    }

    // ------------------------------------------------------------------
    // Günün pusulası hikâyesi
    // ------------------------------------------------------------------

    suspend fun renderCompass(
        context: Context,
        archetype: String?,
        reading: String,
        accentHex: String?,
        watermark: Boolean
    ): Bitmap = withContext(Dispatchers.Default) {
        val accent = runCatching { android.graphics.Color.parseColor(accentHex) }.getOrDefault(GOLD)
        val bmp = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        drawBackground(c, seed = (reading.hashCode() * 31) + (archetype?.hashCode() ?: 0), accent = accent)
        drawHeader(c, context)

        drawPill(c, "🧭  " + context.getString(R.string.compass_card_title).uppercase(), centerY = 262f, color = GOLD)
        val date = DateFormat.getDateInstance(DateFormat.LONG, io.lunosfer.dreamap.util.AppLanguage.locale()).format(Date())
        c.drawText(date, WIDTH / 2f, 350f, textPaint(sans, 32f, SILVER).apply { textAlign = Paint.Align.CENTER })

        drawCompassRose(c, cx = WIDTH / 2f, cy = 640f, radius = 200f, accent = accent)

        var y = 930f
        if (!archetype.isNullOrBlank()) {
            val label = context.getString(R.string.compass_card_archetype).uppercase()
            c.drawText(label, WIDTH / 2f, y, textPaint(sansBold, 30f, SILVER).apply {
                textAlign = Paint.Align.CENTER
                letterSpacing = 0.25f
            })
            y += 40f
            val archLayout = layout(archetype.uppercase(), textPaint(serifBold, 84f, accent), WIDTH - 180, maxLines = 2, align = Layout.Alignment.ALIGN_CENTER, spacing = 1.05f)
            c.save(); c.translate(90f, y); archLayout.draw(c); c.restore()
            y += archLayout.height + 50f
        }

        val readingBottom = if (watermark) 1620f else 1820f
        val quotePaint = textPaint(sansItalic, 46f, 0xF0FFFFFF.toInt())
        val lineHeight = quotePaint.fontSpacing * 1.32f
        val lines = ((readingBottom - y) / lineHeight).toInt().coerceIn(1, 14)
        val quoteLayout = layout("“${reading.trim()}”", quotePaint, WIDTH - 180, maxLines = lines, align = Layout.Alignment.ALIGN_CENTER, spacing = 1.32f)
        c.save(); c.translate(90f, y); quoteLayout.draw(c); c.restore()

        if (watermark) drawWatermark(c, context)
        bmp
    }

    // ------------------------------------------------------------------
    // Kaydetme / paylaşım dosyası
    // ------------------------------------------------------------------

    /** Önbelleğe JPEG yazar ve FileProvider URI'si döner (dış uygulamalara verilecek). */
    suspend fun writeShareFile(context: Context, bitmap: Bitmap, prefix: String): Uri = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { if (now - it.lastModified() > CACHE_MAX_AGE_MS) it.delete() }
        val file = File(dir, "${prefix}_$now.jpg")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun toJpegBytes(bitmap: Bitmap, quality: Int = 90): ByteArray =
        java.io.ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }

    /** Galeriye (Pictures/Lunosfer) kaydeder. Android 9 ve altında depolama izni gerekir. */
    suspend fun saveToGallery(context: Context, bitmap: Bitmap, prefix: String): Boolean = withContext(Dispatchers.IO) {
        val name = "${prefix}_${System.currentTimeMillis()}.jpg"
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, name)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Lunosfer")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values)
                    ?: return@runCatching false
                resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                    ?: run { resolver.delete(uri, null, null); return@runCatching false }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Lunosfer").apply { mkdirs() }
                val file = File(dir, name)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg"), null)
                true
            }
        }.getOrDefault(false)
    }

    // ------------------------------------------------------------------
    // Çizim yardımcıları
    // ------------------------------------------------------------------

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(1600)
            .build()
        (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
    }.getOrNull()

    private fun textPaint(typeface: Typeface, size: Float, color: Int) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
        this.color = color
    }

    private fun layout(
        text: CharSequence,
        paint: TextPaint,
        width: Int,
        maxLines: Int,
        align: Layout.Alignment,
        spacing: Float
    ): StaticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(align)
        .setLineSpacing(0f, spacing)
        .setIncludePad(false)
        .setMaxLines(maxLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .build()

    private fun drawBackground(c: Canvas, seed: Int, accent: Int) {
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, HEIGHT.toFloat(), intArrayOf(VOID, VIOLET_DEEP, VOID_900, VOID), floatArrayOf(0f, 0.38f, 0.72f, 1f), Shader.TileMode.CLAMP)
        }
        c.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), bg)
        glow(c, 170f, 430f, 620f, withAlpha(accent, 0x55))
        glow(c, 930f, 260f, 420f, withAlpha(CYAN, 0x22))
        glow(c, 940f, 1640f, 540f, withAlpha(GOLD, 0x2E))

        val random = Random(seed.toLong())
        val star = Paint(Paint.ANTI_ALIAS_FLAG)
        repeat(150) {
            star.color = withAlpha(0xFFFFFFFF.toInt(), 40 + random.nextInt(170))
            c.drawCircle(random.nextFloat() * WIDTH, random.nextFloat() * HEIGHT, 1f + random.nextFloat() * 2.6f, star)
        }
    }

    private fun glow(c: Canvas, x: Float, y: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(x, y, r, color, color and 0x00FFFFFF, Shader.TileMode.CLAMP)
        }
        c.drawCircle(x, y, r, p)
    }

    private fun withAlpha(color: Int, alpha: Int) = (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    private fun logo(context: Context, size: Int): Bitmap? = runCatching {
        ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)?.toBitmap(size, size)
    }.getOrNull()

    private fun drawCircleBitmap(c: Canvas, bmp: Bitmap, cx: Float, cy: Float, size: Float) {
        val dst = RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        c.save()
        c.clipPath(Path().apply { addOval(dst, Path.Direction.CW) })
        c.drawBitmap(bmp, null, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        c.restore()
    }

    private fun drawHeader(c: Canvas, context: Context) {
        val word = "LUNOSFER"
        val paint = textPaint(sansBold, 44f, GOLD).apply { letterSpacing = 0.3f }
        val logoSize = 76f
        val gap = 22f
        val total = logoSize + gap + paint.measureText(word)
        val startX = (WIDTH - total) / 2f
        logo(context, logoSize.toInt())?.let { drawCircleBitmap(c, it, startX + logoSize / 2, 150f, logoSize) }
        c.drawText(word, startX + logoSize + gap, 150f - (paint.descent() + paint.ascent()) / 2, paint)
    }

    private fun drawPill(c: Canvas, text: String, centerY: Float, color: Int) {
        val paint = textPaint(sansBold, 30f, color).apply { letterSpacing = 0.18f }
        val w = paint.measureText(text) + 64f
        val h = 64f
        val rect = RectF((WIDTH - w) / 2f, centerY - h / 2, (WIDTH + w) / 2f, centerY + h / 2)
        c.drawRoundRect(rect, h / 2, h / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = withAlpha(color, 0x1F) })
        c.drawRoundRect(rect, h / 2, h / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            this.color = withAlpha(color, 0x8C)
        })
        c.drawText(text, rect.left + 32f, centerY - (paint.descent() + paint.ascent()) / 2, paint)
    }

    private fun drawPanel(c: Canvas, rect: RectF, accent: Int) {
        c.drawRoundRect(rect, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PANEL })
        c.drawRoundRect(rect, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = withAlpha(accent, 0x44)
        })
    }

    private fun drawCenterCrop(c: Canvas, bmp: Bitmap, dst: RectF, radius: Float) {
        val dstRatio = dst.width() / dst.height()
        val srcRatio = bmp.width.toFloat() / bmp.height
        val src = if (srcRatio > dstRatio) {
            val w = (bmp.height * dstRatio).toInt()
            val x = (bmp.width - w) / 2
            Rect(x, 0, x + w, bmp.height)
        } else {
            val h = (bmp.width / dstRatio).toInt()
            val y = (bmp.height - h) / 2
            Rect(0, y, bmp.width, y + h)
        }
        c.save()
        c.clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
        c.drawBitmap(bmp, src, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        c.restore()
    }

    private fun drawCompassRose(c: Canvas, cx: Float, cy: Float, radius: Float, accent: Int) {
        glow(c, cx, cy, radius * 1.6f, withAlpha(accent, 0x40))
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = accent
        }
        c.drawCircle(cx, cy, radius, ring)
        ring.strokeWidth = 2f
        ring.color = withAlpha(accent, 0x80)
        c.drawCircle(cx, cy, radius * 0.78f, ring)

        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = withAlpha(accent, 0xB0); strokeWidth = 4f }
        for (i in 0 until 24) {
            val a = Math.toRadians(i * 15.0)
            val inner = if (i % 6 == 0) radius * 0.86f else radius * 0.92f
            c.drawLine(
                cx + (inner * Math.sin(a)).toFloat(), cy - (inner * Math.cos(a)).toFloat(),
                cx + (radius * 0.98f * Math.sin(a)).toFloat(), cy - (radius * 0.98f * Math.cos(a)).toFloat(),
                tick
            )
        }

        // Dört köşeli yıldız (pusula gülü)
        fun star(r: Float, w: Float, rotation: Double, color: Int) {
            val path = Path()
            for (i in 0 until 8) {
                val a = rotation + Math.PI / 4 * i
                val len = if (i % 2 == 0) r else w
                val x = cx + (len * Math.sin(a)).toFloat()
                val y = cy - (len * Math.cos(a)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            c.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(cx, cy - r, cx, cy + r, intArrayOf(GOLD, color), null, Shader.TileMode.CLAMP)
            })
        }
        star(radius * 0.52f, radius * 0.14f, Math.PI / 4, withAlpha(accent, 0x99))
        star(radius * 0.74f, radius * 0.16f, 0.0, accent)
        c.drawCircle(cx, cy, radius * 0.07f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = VOID })
        c.drawCircle(cx, cy, radius * 0.04f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD })
    }

    private fun drawWatermark(c: Canvas, context: Context) {
        val top = 1668f
        c.drawLine(WIDTH * 0.32f, top, WIDTH * 0.68f, top, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(GOLD, 0x55)
            strokeWidth = 2f
        })
        val site = "lunosfer.com"
        val sitePaint = textPaint(sansBold, 46f, GOLD).apply { letterSpacing = 0.08f }
        val logoSize = 70f
        val gap = 18f
        val total = logoSize + gap + sitePaint.measureText(site)
        val startX = (WIDTH - total) / 2f
        val rowCenter = top + 78f
        logo(context, logoSize.toInt())?.let { drawCircleBitmap(c, it, startX + logoSize / 2, rowCenter, logoSize) }
        c.drawText(site, startX + logoSize + gap, rowCenter - (sitePaint.descent() + sitePaint.ascent()) / 2, sitePaint)

        val tagline = context.getString(R.string.share_card_tagline)
        val tagPaint = textPaint(sans, 30f, SILVER)
        val tagLayout = layout(tagline, tagPaint, WIDTH - 160, maxLines = 2, align = Layout.Alignment.ALIGN_CENTER, spacing = 1.1f)
        c.save(); c.translate(80f, rowCenter + 58f); tagLayout.draw(c); c.restore()
    }
}
