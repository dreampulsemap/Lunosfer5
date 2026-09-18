package io.lunosfer.dreamap.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.SerifFontFamily
import io.lunosfer.dreamap.ui.theme.Void950
import java.util.Locale

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
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobeScreen(onBack: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    val deviceLang = io.lunosfer.dreamap.util.AppLanguage.code().lowercase()
    val lang = if (deviceLang in SUPPORTED_GLOBE_LANGS) deviceLang else "en"
    val url = "$GLOBE_URL?lang=$lang"

    Scaffold(
        containerColor = Void950,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.globe_title), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(Void950)) {
            if (loadError) {
                GlobeFallback(onBack = onBack)
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                                    isLoading = false
                                }
                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame != false) {
                                        loadError = true
                                    }
                                }
                            }
                            loadUrl(url)
                        }
                    }
                )
            }

            if (isLoading && !loadError) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold)
                }
            }
        }
    }
}

@Composable
private fun GlobeFallback(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    listOf(Void950, Color.Black)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = AstralGold.copy(alpha = 0.8f),
                modifier = Modifier.size(80.dp)
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.globe_load_error_title),
                color = AstralGold,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SerifFontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.globe_load_error_desc),
                color = Color.Gray,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
            ) {
                Text(stringResource(R.string.generic_back), color = Void950, fontWeight = FontWeight.Bold)
            }
        }
    }
}
