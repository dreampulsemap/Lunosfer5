package io.lunosfer.dreamap.data.repository

import android.app.Activity
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.data.model.BillingProductIds
import io.lunosfer.dreamap.data.model.VerifyGooglePlayPurchaseRequest
import io.lunosfer.dreamap.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

// Satın alınabilir bir Aura paketinin UI'a sunulan hali — ham ProductDetails
// bu katmanın dışına sızmaz (diğer repository'lerin ViewModel'e yalnızca
// domain modeli döndürmesiyle aynı desen, bkz. ProfileRepository).
data class AuraPackOffer(
    val productId: String,
    val auraCount: Int,
    val formattedPrice: String
)

data class PremiumPlanOffer(
    val basePlanId: String,
    val formattedPrice: String,
    // ISO 8601 süre (ör. "P1M", "P1Y") — ekranda gösterilecek etiket
    // (Aylık/Yıllık) UI katmanında basePlanId'den string resource ile
    // üretiliyor, bkz. BillingSheet.kt.
    val billingPeriodIso: String
)

sealed class PurchaseFlowState {
    object Idle : PurchaseFlowState()
    object Processing : PurchaseFlowState()
    data class Success(val status: String, val aurasAdded: Int = 0) : PurchaseFlowState()
    data class Error(val message: String) : PurchaseFlowState()
}

/**
 * Magazanin durumu.
 *
 * ONCEDEN boyle bir state YOKTU: ekran yalnizca teklif listesinin bos olup
 * olmadigina bakiyordu, bu yuzden "henuz yukleniyor", "cihazda Play Billing
 * yok" ve "Play Console'da urun tanimli degil" durumlarinin ucu de ayni
 * sonsuz "Urunler yukleniyor..." spinner'i olarak gorunuyordu (canli
 * raporlandi: hem emulatorde hem gercek cihazda hic acilmiyor). Baglanti
 * kurulamadiginda da hicbir sey yapilmadigi icin spinner asla bitmiyordu.
 */
sealed class StoreState {
    /** Baglaniliyor ya da urunler sorgulaniyor. */
    object Loading : StoreState()

    /** Baglanti kuruldu ve en az bir teklif var. */
    object Ready : StoreState()

    /**
     * Cihazda Play Billing kullanilamiyor (Play Store yok/eski, emulator
     * imaji Play'siz, kullanici profili desteklemiyor). Yeniden denemek
     * genelde ise yaramaz.
     */
    data class Unavailable(val debugMessage: String) : StoreState()

    /**
     * Baglanti kuruldu ama Play hicbir urun dondurmedi. Neredeyse her zaman
     * yapilandirma hatasidir: urunler Play Console'da olusturulmamis/aktif
     * degil, uygulama hicbir track'e yuklenmemis, ya da imza/paket adi
     * Play Console'daki ile eslesmiyor.
     */
    object NoProducts : StoreState()

    /** Gecici hata (ag, servis kesintisi) - tekrar denenebilir. */
    data class Error(val debugMessage: String) : StoreState()
}

/**
 * Play BillingClient bağlantısının uygulama boyunca TEK olması önerildiği
 * için (bkz. Play Billing dokümantasyonu) singleton olarak tutuluyor —
 * NetworkModule ile aynı `object` deseni.
 *
 * Akış: connectAndLoadProducts() -> queryProducts() ile ProductDetails'leri
 * çek -> launchAuraPurchase/launchPremiumPurchase ile satın alma başlat ->
 * onPurchasesUpdated -> handlePurchase(): ÖNCE backend'e doğrulat (api.
 * verifyGooglePlayPurchase — yetkiyi/aura'yı Supabase'e o yazar), SONRA
 * Google'a acknowledge/consume ile "tamamlandı" de. Bu sıralama önemli:
 * client hiçbir zaman kendi kendine yetki vermez.
 */
object BillingRepository : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val api = NetworkModule.api

    private val billingClient: BillingClient by lazy {
        BillingClient.newBuilder(DreamapApp.instance)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .enableAutoServiceReconnection()
            .build()
    }

    private var premiumProductDetails: ProductDetails? = null
    private val auraProductDetailsByProductId = mutableMapOf<String, ProductDetails>()

    private val _auraOffers = MutableStateFlow<List<AuraPackOffer>>(emptyList())
    val auraOffers: StateFlow<List<AuraPackOffer>> = _auraOffers.asStateFlow()

    private val _premiumOffers = MutableStateFlow<List<PremiumPlanOffer>>(emptyList())
    val premiumOffers: StateFlow<List<PremiumPlanOffer>> = _premiumOffers.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseFlowState>(PurchaseFlowState.Idle)
    val purchaseState: StateFlow<PurchaseFlowState> = _purchaseState.asStateFlow()

    private val _storeState = MutableStateFlow<StoreState>(StoreState.Loading)
    val storeState: StateFlow<StoreState> = _storeState.asStateFlow()

    // TopBar'daki Aura pill'i ve ProfileScreen'in ayrı ayrı istek atmasını
    // önlemek için bakiyeyi burada, tek yerde tutuyoruz.
    private val _auraBalance = MutableStateFlow(0)
    val auraBalance: StateFlow<Int> = _auraBalance.asStateFlow()

    suspend fun refreshAuraBalance() {
        runCatching { api.getPremiumStatus() }.onSuccess { _auraBalance.value = it.auraBalance }
    }

    // Aura satın alımı onaylandığında eklenen miktarı bir kerelik yayınlar —
    // TopBar'daki Aura pill'in yerel bir "+N" animasyonu göstermesi ya da
    // ProfileViewModel'in bakiyeyi yeniden çekmesi için tetikleyici.
    private val _lastAurasAdded = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val lastAurasAdded: SharedFlow<Int> = _lastAurasAdded

    fun connectAndLoadProducts() {
        scope.launch { refreshAuraBalance() }
        if (billingClient.isReady) {
            _storeState.value = StoreState.Loading
            scope.launch { loadProductsAndPurchasesSafely() }
            return
        }
        _storeState.value = StoreState.Loading
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> scope.launch {
                        loadProductsAndPurchasesSafely()
                    }

                    // Cihaz/hesap Play Billing'i hic desteklemiyor. Emulatorde
                    // gorunen durum bu: "In-app billing API version 3 is not
                    // supported on this device".
                    BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                    BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED -> {
                        android.util.Log.w("BillingRepository", "Cihaz Play Billing desteklemiyor: ${billingResult.responseCode} ${billingResult.debugMessage}")
                        _storeState.value = StoreState.Unavailable(billingResult.debugMessage)
                    }

                    // Gecici: ag yok, servis mesgul vs.
                    else -> {
                        android.util.Log.w("BillingRepository", "onBillingSetupFinished hata: ${billingResult.responseCode} ${billingResult.debugMessage}")
                        _storeState.value = StoreState.Error(billingResult.debugMessage)
                    }
                }
            }

            // enableAutoServiceReconnection() otomatik yeniden dener; ekrana
            // yine de bir sey soylemeliyiz, yoksa spinner asili kalir.
            override fun onBillingServiceDisconnected() {
                if (_storeState.value !is StoreState.Ready) {
                    _storeState.value = StoreState.Error("service_disconnected")
                }
            }
        })
    }

    /** Ekrandaki "Tekrar dene" butonu icin. */
    fun retryLoadProducts() = connectAndLoadProducts()

    // queryProducts() (ya da onun cagirdigi processExistingPurchases())
    // beklenmedik bir istisna firlatirsa - ag katmaninda tuhaf bir durum,
    // BillingClient'in kendi kutuphanesinde bir kenar durum vs. - ONCEDEN
    // _storeState hicbir zaman Loading'den cikmiyordu ve UI sonsuza kadar
    // "yukleniyor" gosteriyordu. Coroutine'i burada sariyoruz ki HANGI
    // sebeple olursa olsun _storeState mutlaka bir sonuca (Ready/NoProducts/
    // Error) ulassin.
    private suspend fun loadProductsAndPurchasesSafely() {
        try {
            queryProducts()
            processExistingPurchases()
        } catch (e: Exception) {
            android.util.Log.e("BillingRepository", "Urun/satin alma yukleme hatasi", e)
            _storeState.value = StoreState.Error(e.message ?: "load_failed")
        }
    }

    private suspend fun queryProducts() {
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingProductIds.PREMIUM_SUBSCRIPTION)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        queryProductDetails(subsParams)?.productDetailsList?.firstOrNull()?.let { pd ->
            premiumProductDetails = pd
            _premiumOffers.value = pd.subscriptionOfferDetails.orEmpty().mapNotNull { offer ->
                val phase = offer.pricingPhases.pricingPhaseList.firstOrNull() ?: return@mapNotNull null
                PremiumPlanOffer(
                    basePlanId = offer.basePlanId,
                    formattedPrice = phase.formattedPrice,
                    billingPeriodIso = phase.billingPeriod
                )
            }
        }

        val auraParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                BillingProductIds.AURA_PACKS.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        val auraResult = queryProductDetails(auraParams)
        auraProductDetailsByProductId.clear()
        _auraOffers.value = auraResult?.productDetailsList.orEmpty().mapNotNull { pd ->
            val offer = pd.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: return@mapNotNull null
            val auraCount = BillingProductIds.auraCountForProductId(pd.productId) ?: return@mapNotNull null
            auraProductDetailsByProductId[pd.productId] = pd
            AuraPackOffer(pd.productId, auraCount, offer.formattedPrice)
        }.sortedBy { it.auraCount }

        // Baglanti kuruldu ama Play hicbir sey dondurmediyse bu bir
        // yapilandirma hatasidir; bos listeyi "yukleniyor" gibi gostermek
        // kullaniciyi sonsuza kadar bekletiyordu.
        _storeState.value =
            if (_auraOffers.value.isEmpty() && _premiumOffers.value.isEmpty()) StoreState.NoProducts
            else StoreState.Ready
    }

    private suspend fun queryProductDetails(
        params: QueryProductDetailsParams
    ) = suspendCancellableCoroutine { cont ->
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (cont.isActive) cont.resumeWith(Result.success(result))
            } else {
                if (cont.isActive) cont.resumeWith(Result.success(null))
            }
        }
    }

    // --- Satın alma başlatma ---

    fun launchAuraPurchase(activity: Activity, productId: String) {
        val productDetails = auraProductDetailsByProductId[productId] ?: return
        val offerToken = productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken ?: return
        launchFlow(activity, productDetails, offerToken)
    }

    fun launchPremiumPurchase(activity: Activity, basePlanId: String) {
        val productDetails = premiumProductDetails ?: return
        val offerToken = productDetails.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == basePlanId }
            ?.offerToken ?: return
        launchFlow(activity, productDetails, offerToken)
    }

    private fun launchFlow(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        _purchaseState.value = PurchaseFlowState.Processing
        // launchBillingFlow senkron bir BillingResult DONDURUR; onPurchasesUpdated
        // yalnizca odeme sayfasi GERCEKTEN acildiysa cagrilir. Bu donus degeri
        // ONCEDEN yok sayiliyordu, o yuzden ITEM_ALREADY_OWNED (onceki test
        // satin almasi consume edilmemisse), DEVELOPER_ERROR (yanlis/eksik
        // urun-teklif eslesmesi) ya da ITEM_UNAVAILABLE (Play Console'da urun
        // yoksa/aktif degilse) gibi senkron hatalarda odeme sayfasi HIC
        // ACILMIYOR ve onPurchasesUpdated de asla tetiklenmiyordu - sonuc,
        // "Islemde" spinner'inin sonsuza kadar asili kalmasiydi (canli
        // raporlandi). Simdi bu donus degeri kontrol ediliyor.
        val launchResult = billingClient.launchBillingFlow(activity, params)
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            if (launchResult.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                // Onceki bir test/basarisiz akistan consume/acknowledge
                // edilmemis bir satin alma kalmis olabilir - kullaniciyi
                // sonsuza kadar kilitli birakmamak icin mevcut satin almalari
                // yeniden isleyip (consume dahil) durumu duzeltmeyi dene.
                scope.launch { processExistingPurchases() }
            }
            _purchaseState.value = PurchaseFlowState.Error(
                "${launchResult.responseCode}: ${launchResult.debugMessage}"
            )
        }
    }

    // --- PurchasesUpdatedListener ---

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase -> scope.launch { handlePurchase(purchase) } }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseFlowState.Idle
            }
            else -> {
                android.util.Log.w("BillingRepository", "onPurchasesUpdated hata: ${billingResult.responseCode} ${billingResult.debugMessage}")
                _purchaseState.value = PurchaseFlowState.Error(billingResult.debugMessage)
            }
        }
    }

    // Uygulama arka plandayken/kapalıyken tamamlanmış ama henüz işlenmemiş
    // (acknowledge/consume edilmemiş) satın almaları yakalamak için —
    // özellikle banka onayı gecikmeli ödeme yöntemlerinde önemli.
    private suspend fun processExistingPurchases() {
        listOf(BillingClient.ProductType.SUBS, BillingClient.ProductType.INAPP).forEach { type ->
            val params = QueryPurchasesParams.newBuilder().setProductType(type).build()
            suspendCancellableCoroutine<Unit> { cont ->
                billingClient.queryPurchasesAsync(params) { result, purchases ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        purchases.forEach { purchase ->
                            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                                scope.launch { handlePurchase(purchase) }
                            }
                        }
                    }
                    if (cont.isActive) cont.resumeWith(Result.success(Unit))
                }
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val productId = purchase.products.firstOrNull() ?: run {
            _purchaseState.value = PurchaseFlowState.Error("unknown_product")
            return
        }
        val isSubscription = productId == BillingProductIds.PREMIUM_SUBSCRIPTION
        val purchaseType = if (isSubscription) "subscription" else "aura_pack"

        _purchaseState.value = PurchaseFlowState.Processing
        try {
            val response = api.verifyGooglePlayPurchase(
                VerifyGooglePlayPurchaseRequest(
                    purchaseToken = purchase.purchaseToken,
                    productId = productId,
                    purchaseType = purchaseType
                )
            )

            if (!response.ok) {
                _purchaseState.value = PurchaseFlowState.Error(response.status)
                return
            }

            // Backend zaten yetkiyi verdi/aura'yı ekledi — şimdi Google'a
            // işlemi kapatıyoruz. Aksi halde Google, acknowledge edilmemiş
            // satın almaları birkaç gün içinde otomatik iade eder.
            if (isSubscription) {
                if (!purchase.isAcknowledged) acknowledgePurchase(purchase.purchaseToken)
            } else {
                consumePurchase(purchase.purchaseToken)
                if (response.aurasAdded > 0) {
                    _lastAurasAdded.tryEmit(response.aurasAdded)
                    _auraBalance.value += response.aurasAdded
                }
            }

            _purchaseState.value = PurchaseFlowState.Success(response.status, response.aurasAdded)
        } catch (e: Exception) {
            android.util.Log.e("BillingRepository", "Satin alma dogrulama hatasi", e)
            _purchaseState.value = PurchaseFlowState.Error(e.message ?: "verify_failed")
        }
    }

    private suspend fun acknowledgePurchase(purchaseToken: String) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken).build()
        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.acknowledgePurchase(params) { if (cont.isActive) cont.resumeWith(Result.success(Unit)) }
        }
    }

    private suspend fun consumePurchase(purchaseToken: String) {
        val params = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
        suspendCancellableCoroutine<Unit> { cont ->
            billingClient.consumeAsync(params) { _, _ -> if (cont.isActive) cont.resumeWith(Result.success(Unit)) }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = PurchaseFlowState.Idle
    }
}
