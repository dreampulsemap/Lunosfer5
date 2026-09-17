package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.CollectiveStatsResponse
import io.lunosfer.dreamap.data.network.LunosferApi
import io.lunosfer.dreamap.data.network.NetworkModule

class CollectiveStatsRepository(
    private val api: LunosferApi = NetworkModule.api
) {
    suspend fun getCollectiveStats(): Result<CollectiveStatsResponse> = runCatching {
        val res = api.getCollectiveStats()
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }
}
