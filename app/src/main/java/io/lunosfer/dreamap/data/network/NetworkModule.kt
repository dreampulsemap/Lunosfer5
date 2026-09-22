package io.lunosfer.dreamap.data.network

import io.lunosfer.dreamap.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Base URL: lunosfer.com web app'inin kendisi (Next.js API route'ları burada
 * yaşıyor), Supabase URL'i DEĞİL. BuildConfig.APP_URL .env / .env.example
 * üzerinden Secrets Gradle Plugin ile enjekte edilir — web tarafındaki
 * NEXT_PUBLIC_APP_URL ile aynı konvansiyon (bkz. .env.example'a eklenen satır).
 */
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        // Varsayilan degerli alanlar aksi halde govdeye HIC yazilmiyor:
        // DailyCompassRequest.lang varsayilani "tr" oldugu icin Turkce
        // kullanicida alan tamamen dusuyor, sunucu kendi 'en' varsayilanina
        // donuyor ve pusula/tohum metinleri Ingilizce geliyordu.
        encodeDefaults = true
        // encodeDefaults ile birlikte sart: null alanlar yazilmazsa sunucudaki
        // `const { x = 'varsayilan' } = req.body` ifadeleri calismaya devam
        // eder (acik null gonderilseydi varsayilan devreye girmezdi).
        explicitNulls = false
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor())
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        // Derin analiz ucu sunucuda OpenAI cagrisi + gorsel uretimi yapiyor ve
        // 30-60 sn surebiliyor. 15 sn'lik okuma zaman asimi istegi sunucu daha
        // yanit vermeden kesiyordu; ekranda bu, butona basildiginda sonucun
        // hic gelmemesi olarak gorunuyordu.
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val rawUrl = if (BuildConfig.APP_URL.isNotBlank()) BuildConfig.APP_URL else "https://www.lunosfer.com/"
    private val normalizedUrl = if (rawUrl.endsWith("/")) rawUrl else "$rawUrl/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(normalizedUrl.replace("https://lunosfer.com", "https://www.lunosfer.com"))
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val api: LunosferApi by lazy { retrofit.create(LunosferApi::class.java) }
}
