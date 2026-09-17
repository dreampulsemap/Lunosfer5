package io.lunosfer.dreamap.data.repository

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/**
 * Kullanicinin mana bakiyesi (user_profiles.mana_balance).
 *
 * Ust bardaki mana rozeti sabit "0" yaziyordu — gercek bakiye hicbir zaman
 * okunmuyordu (canli testte hesabin 18 manasi varken rozet 0 gosteriyordu).
 * Aura icin BillingRepository'nin yaptigi isin mana karsiligi: tekil bir
 * StateFlow, mana harcayan/kazandiran her akis (bkz. GoalDetailViewModel'deki
 * mana ver/kaldir) sonucu buraya yaziyor.
 */
object UserWallet {

    @Serializable
    private data class ManaRow(@kotlinx.serialization.SerialName("mana_balance") val manaBalance: Int = 0)

    private val _mana = MutableStateFlow(0)
    val mana: StateFlow<Int> = _mana.asStateFlow()

    /** Sunucudan gelen guncel bakiyeyi yazar ( or. give-mana yanitindaki manaBalance). */
    fun set(balance: Int?) {
        if (balance != null && balance >= 0) _mana.value = balance
    }

    fun clear() {
        _mana.value = 0
    }

    suspend fun refresh() {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return
        // Yavas baglantida ilk istek zaman asimina ugrayabiliyor; bakiye rozeti
        // sessizce 0'da kalmasin diye bir kez daha deniyoruz.
        repeat(2) { attempt ->
            val row = runCatching {
                supabaseClient.postgrest["user_profiles"]
                    .select(columns = Columns.list("mana_balance")) { filter { eq("id", userId) } }
                    .decodeList<ManaRow>()
                    .firstOrNull()
            }.getOrNull()
            if (row != null) {
                _mana.value = row.manaBalance
                return
            }
            if (attempt == 0) kotlinx.coroutines.delay(2_000)
        }
    }
}
