package io.lunosfer.dreamap.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Avatar icin gorsel normalizasyonu.
 *
 * Profil fotografi hicbir sekilde kirpilmiyor/kuculmuyordu: kullanicinin
 * sectigi 1920x1200'luk bir ekran goruntusu oldugu gibi yukleniyor, dairesel
 * avatarda ortasindan rastgele bir serit gorunuyordu (canli hesapta boyle bir
 * avatar bulundu). Ayrica her yerde tam boy indiriliyordu.
 *
 * Burada goruntunun ORTASINDAN kare bir alan kirpilip en fazla [size] pikselde
 * JPEG olarak sikistiriliyor.
 */
object ImageUtils {

    fun toSquareAvatarJpeg(source: ByteArray, size: Int = 512, quality: Int = 88): ByteArray {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return source

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, size)
            }
            val decoded = BitmapFactory.decodeByteArray(source, 0, source.size, decodeOptions) ?: return source

            val edge = min(decoded.width, decoded.height)
            val cropped = Bitmap.createBitmap(
                decoded,
                (decoded.width - edge) / 2,
                (decoded.height - edge) / 2,
                edge,
                edge
            )
            val targetEdge = min(size, edge)
            val scaled = if (targetEdge != edge) {
                Bitmap.createScaledBitmap(cropped, targetEdge, targetEdge, true)
            } else {
                cropped
            }

            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
            val result = output.toByteArray()

            if (scaled != cropped) scaled.recycle()
            if (cropped != decoded) cropped.recycle()
            decoded.recycle()

            if (result.isNotEmpty()) result else source
        }.getOrDefault(source)
    }

    private fun calculateInSampleSize(width: Int, height: Int, target: Int): Int {
        var sample = 1
        var maxEdge = max(width, height)
        while (maxEdge / 2 >= target) {
            maxEdge /= 2
            sample *= 2
        }
        return sample
    }
}
