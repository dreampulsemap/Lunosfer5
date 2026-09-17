package io.lunosfer.dreamap.util

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Navigation Compose bazi ekranlarda (orn. ThreadScreen) donus sirasinda
// composable'i sifirdan yeniden olusturuyor ve o SIRADA `rememberLauncherForActivityResult`
// ile kaydedilmis bir ActivityResultLauncher'in callback'i HICBIR ZAMAN
// TETIKLENMIYOR — muhtemelen eski composable'in DisposableEffect temizligi,
// ActivityResultRegistry'deki kaydi, sonuc gelmeden once (veya gelirken)
// siliyor (bkz. DiaryComposerScreen'deki PendingCameraCapture ve
// ThreadScreen'deki ayni sorunun notlari). Bunu asmak icin GetContent
// launcher'ini composable'a degil, Activity'nin kendisine (hic yeniden
// olusturulmayan tek instance) baglayan bu paylasilan singleton'i kullaniyoruz.
object GlobalContentPicker {
    private var launcher: ActivityResultLauncher<String>? = null

    // Sonuc bir StateFlow'da tutuluyor: cagiran ekran bir DIALOG ise (orn.
    // ProfileScreen'deki EditProfileDialog) secici acilip kapandiginda
    // composable hic composition'dan cikmadigi icin `LaunchedEffect(Unit)`
    // BIR DAHA calismiyordu ve secilen foto sessizce yok sayiliyordu (canli
    // testte dogrulandi: avatar hic degismiyordu). Gozlemlenebilir bir state
    // ile, sonuc geldigi anda dinleyen her composable tetikleniyor.
    private val _pendingUri = MutableStateFlow<Uri?>(null)
    val pendingUriFlow: StateFlow<Uri?> = _pendingUri.asStateFlow()

    val pendingUri: Uri? get() = _pendingUri.value

    fun register(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri -> _pendingUri.value = uri }
    }

    fun launch(mimeType: String) {
        launcher?.launch(mimeType)
    }

    fun consumePendingUri(): Uri? {
        val uri = _pendingUri.value
        _pendingUri.value = null
        return uri
    }
}

// GlobalContentPicker ile ayni gerekce: TakePicture/CaptureVideo'nun
// composable'a bagli callback'i, kameradan donuste Navigation Compose
// composable'i yeniden olusturunca hicbir zaman tetiklenmiyordu (bkz.
// DiaryComposerScreen'in kendi ic PendingCameraCapture cozumu — orada URI
// launch'tan once bilindigi icin composable-ici bir singleton yeterliydi;
// burada da ayni riskten kacinmak icin ayni Activity-seviyesi deseni
// kullaniyoruz, boylece bu launcher'i kullanan HERHANGI bir ekran icin
// guvenilir olur).
object GlobalCameraCapture {
    private var photoLauncher: ActivityResultLauncher<Uri>? = null
    private var videoLauncher: ActivityResultLauncher<Uri>? = null
    private var pendingUri: Uri? = null
    private var pendingFilePath: String? = null

    // GlobalContentPicker ile ayni gerekce: sonuc gozlemlenebilir olmazsa,
    // cagiran ekran composition'dan hic cikmadiginda `LaunchedEffect(Unit)`
    // bir daha calismiyor ve cekilen foto/video sessizce kayboluyor.
    private val _completedUri = MutableStateFlow<Uri?>(null)
    val completedUriFlow: StateFlow<Uri?> = _completedUri.asStateFlow()

    fun register(activity: ComponentActivity) {
        photoLauncher = activity.registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success -> publishResult(success) }
        videoLauncher = activity.registerForActivityResult(
            ActivityResultContracts.CaptureVideo()
        ) { success -> publishResult(success) }
    }

    /** success bayragina ek olarak dosyanin gercekten yazilip yazilmadigini da kontrol eder (bkz. DiaryComposerScreen notu). */
    private fun publishResult(success: Boolean) {
        val uri = pendingUri
        val filePath = pendingFilePath
        pendingUri = null
        pendingFilePath = null
        if (uri == null) return
        val hasContent = success || (filePath != null && java.io.File(filePath).let { it.exists() && it.length() > 0L })
        if (hasContent) _completedUri.value = uri
    }

    fun launchPhoto(uri: Uri, filePath: String) {
        pendingUri = uri
        pendingFilePath = filePath
        _completedUri.value = null
        photoLauncher?.launch(uri)
    }

    fun launchVideo(uri: Uri, filePath: String) {
        pendingUri = uri
        pendingFilePath = filePath
        _completedUri.value = null
        videoLauncher?.launch(uri)
    }

    fun consumePendingResult(): Uri? {
        val uri = _completedUri.value
        _completedUri.value = null
        return uri
    }
}
