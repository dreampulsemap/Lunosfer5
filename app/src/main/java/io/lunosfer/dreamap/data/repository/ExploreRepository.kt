package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.ExploreFeedResponse
import io.lunosfer.dreamap.data.network.NetworkModule

class ExploreRepository {
    private val api = NetworkModule.api

    /**
     * Kesfet akisi sayfalanabilir (API page + rankToken + hasMore donuyor) ama
     * uygulama yalnizca ilk sayfayi cekiyordu: grid 15 ruyada bitip devami
     * hicbir sekilde gorunmuyordu. rankToken siralamanin sayfalar arasinda
     * tutarli kalmasi icin geri gonderiliyor.
     */
    suspend fun loadPage(page: Int = 0, rankToken: String? = null): Result<ExploreFeedResponse> = runCatching {
        api.getExploreFeed(page = page, rankToken = rankToken, asOf = null)
    }
}
