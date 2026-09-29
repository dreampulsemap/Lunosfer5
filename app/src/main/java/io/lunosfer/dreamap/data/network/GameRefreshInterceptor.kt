package io.lunosfer.dreamap.data.network

import io.lunosfer.dreamap.data.repository.GameRepository
import okhttp3.Interceptor
import okhttp3.Response

/**
 * XP sunucuda trigger'larla veriliyor; istemcinin haberi olmasi icin her
 * basarili yazma cagrisindan sonra ilerleme (debounce'lu) tazelenir. Tek
 * yerden: yorum/begeni/mana/vizyon/gunluk... her ViewModel'e ayri ayri
 * dokunmak yerine hepsi buradan geciyor.
 */
class GameRefreshInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val path = request.url.encodedPath
        if (response.isSuccessful && request.method != "GET" && path.contains("/api/") && IGNORED.none { path.endsWith(it) }) {
            GameRepository.requestRefresh()
        }
        return response
    }

    private companion object {
        // XP ile ilgisi olmayan yazma uclari.
        val IGNORED = listOf(
            "/api/translate",
            "/api/push/subscribe",
            "/api/diary/mark-seen",
            "/api/notifications",
            "/api/summaries/generate"
        )
    }
}
