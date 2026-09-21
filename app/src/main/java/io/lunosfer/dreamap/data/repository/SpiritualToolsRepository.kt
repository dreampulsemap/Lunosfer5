package io.lunosfer.dreamap.data.repository

import io.lunosfer.dreamap.data.model.*
import io.lunosfer.dreamap.data.network.LunosferApi
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.util.ApiErrors

class SpiritualToolsRepository(
    private val api: LunosferApi = NetworkModule.api
) {
    // Aura yetersizligi 402 ile geliyor ve Retrofit bunu HttpException olarak
    // firlatiyor; ApiErrors.translate govdedeki {"error","cost"} bilgisini
    // okuyup InsufficientAuraException'a ceviriyor ki arayuz "Aura al /
    // Premium" teklifini gosterebilsin.
    suspend fun generateMentalWall(lang: String = "tr", deep: Boolean = false): Result<MentalWallResponse> =
        runCatching {
            val res = api.generateMentalWall(MentalWallRequest(lang, deep))
            if (res.ok == false && res.error != null) {
                throw Exception(res.error)
            }
            res
        }.recoverCatching { throw ApiErrors.translate(it) }

    suspend fun getPsycheMap(): Result<PsycheMapResponse> = runCatching {
        val res = api.getPsycheMap()
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }

    suspend fun consultProphet(
        mode: String = ProphetRequest.MODE_GENERAL,
        question: String? = null,
        lang: String = "tr",
        deep: Boolean = false
    ): Result<ProphetResponse> = runCatching {
        val res = api.consultProphet(ProphetRequest(mode, question, lang, deep))
        if (res.ok == false && res.error != null) {
            throw Exception(res.error)
        }
        res
    }.recoverCatching { throw ApiErrors.translate(it) }
}
