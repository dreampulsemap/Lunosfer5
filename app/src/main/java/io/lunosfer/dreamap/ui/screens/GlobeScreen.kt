package io.lunosfer.dreamap.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.ExperimentalMaterial3Api
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.Void950

private const val GLOBE_URL = "https://www.lunosfer.com/globe"

// globe.jsx'in desteklediği diller — components/DreamGlobe.jsx'teki
// supportedLanguages ile birebir aynı, kapsam dışındaysa 'en'e düşer.
private val SUPPORTED_GLOBE_LANGS = setOf("en", "tr", "de", "fr", "es", "pt", "ru", "ja")

/**
 * "Rüya Küresi" — pages/globe.js'in Android karşılığı. Bilinçli olarak
 * NATIVE bir 3B render motoru yazmadık (globe.gl/three.js'i Android'de
 * birebir yeniden yazmak günler süren ayrı bir iş olurdu) — web'deki
 * gerçek, tam işlevli küreyi WebView ile olduğu gibi gömüyoruz. Bu sayfa
 * herkese açık ('in_feed' rüyalar), giriş gerektirmiyor — o yüzden
 * Android'in oturumunu WebView'e taşımaya gerek yok.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GlobeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableIntStateOf(0) }

    // WebView kendi geçmişinde ileri gitmişse (örn. rüya detayına tıklamış
    // olabilir) önce ORADA geri git; değilse ekranı tamamen kapat.
    BackHandler {
        val wv = webViewRef
        if (wv != null && wv.canGoBack()) wv.goBack() else onBack()
    }

    Scaffold(
        containerColor = Void950,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.globe_title), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { reloadToken++; loadError = false; isLoading = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.globe_refresh_cd), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Color.Black)) {
            if (loadError) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.globe_load_error),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { reloadToken++; loadError = false; isLoading = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                    ) {
                        Text(stringResource(R.string.retry), color = Void950)
                    }
                }
            } else {
                key(reloadToken) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                // Küre dönüşü/yakınlaştırma parmakla oluyor —
                                // sayfanın scroll'una izin vermiyoruz, jestleri
                                // WebView'in kendi JS tuvaline bırakıyoruz.
                                isVerticalScrollBarEnabled = false
                                isHorizontalScrollBarEnabled = false

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        isLoading = true
                                    }
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isLoading = false
                                        // Cihazın dilini küreye bildir — sayfa
                                        // localStorage'dan okuyor (lunosfer_lang).
                                        val deviceLang = java.util.Locale.getDefault().language
                                        val lang = if (deviceLang in SUPPORTED_GLOBE_LANGS) deviceLang else "en"
                                        view?.evaluateJavascript(
                                            "try { localStorage.setItem('lunosfer_lang','$lang'); } catch(e) {}", null
                                        )
                                    }
                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: android.webkit.WebResourceRequest?,
                                        error: android.webkit.WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            loadError = true
                                        }
                                    }
                                }

                                loadUrl(GLOBE_URL)
                                webViewRef = this
                            }
                        },
                        onRelease = { webViewRef = null }
                    )
                }
            }

            if (isLoading && !loadError) {
                Box(modifier = Modifier.fillMaxSize().background(Void950), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AstralGold)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.globe_loading), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
