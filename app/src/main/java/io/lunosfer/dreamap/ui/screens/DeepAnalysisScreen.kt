package io.lunosfer.dreamap.ui.screens

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.DeepAnalysis
import io.lunosfer.dreamap.data.model.FearMapItem
import io.lunosfer.dreamap.ui.components.sharecards.CardHeight
import io.lunosfer.dreamap.ui.components.sharecards.CardWidth
import io.lunosfer.dreamap.ui.components.sharecards.rememberShareableCardHost
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.DEEP_ANALYSIS_PROGRESS_STEPS
import io.lunosfer.dreamap.ui.viewmodel.DeepAnalysisViewModel
import io.lunosfer.dreamap.util.requireAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * "Derin Analiz" — kullanicinin son ruyalari + vizyonlari birlikte okunup
 * Opus 5 ile tek bir rapor uretiliyor: kisilik analizi, korku haritasi,
 * yuzlesme onerisi ve paylasilabilir bir kart.
 *
 * Fiyat 10 Aura; premium uyede bedava. Bakiye yetmezse Aura paketi
 * satin alma sayfasi aciliyor (aura_pack_10 ~ $0.99).
 *
 * Gecmis analizler ucretsiz tekrar okunabiliyor — kullanici ayni parayi
 * ikinci kez odemesin diye.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepAnalysisScreen(
    onBack: () -> Unit,
    onBuyAuras: () -> Unit,
    viewModel: DeepAnalysisViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.deep_analysis_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Bakiye hep gorunur olsun: kullanici butona basmadan
                    // once yetip yetmedigini bilsin.
                    Text(
                        text = stringResource(R.string.deep_analysis_balance, state.auraBalance),
                        color = AstralGold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 14.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { padding ->
        val result = state.result
        if (result != null) {
            DeepAnalysisResultView(
                analysis = result,
                modifier = Modifier.padding(padding),
                onClose = viewModel::dismissResult
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { DeepAnalysisIntroCard(state.auraCost) }

            item {
                Button(
                    onClick = { requireAccount { viewModel.generate() } },
                    enabled = !state.loading,
                    colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(DEEP_ANALYSIS_PROGRESS_STEPS[state.progressStep]),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    } else {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AstralGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.deep_analysis_cta, state.auraCost),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (state.loading) {
                item {
                    Text(
                        stringResource(R.string.deep_analysis_loading_hint),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            state.error?.let { message ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Void900),
                        border = BorderStroke(1.dp, SemanticDanger400.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(message, color = SemanticDanger400, fontSize = 13.sp)
                            if (state.needsAuras) {
                                Button(
                                    onClick = onBuyAuras,
                                    colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        stringResource(R.string.deep_analysis_buy_auras),
                                        color = Void950,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (state.history.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.deep_analysis_history_title),
                        color = AstralGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                items(state.history, key = { it.id }) { past ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openFromHistory(past) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Void900),
                        border = BorderStroke(1.dp, Void800)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!past.cardImageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = past.cardImageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    past.cardHeadline ?: stringResource(R.string.deep_analysis_title),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    formatAnalysisDate(past.createdAt),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.deep_analysis_disclaimer),
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun DeepAnalysisIntroCard(auraCost: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(R.string.deep_analysis_intro_title),
                color = AstralGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SerifFontFamily
            )
            Text(
                stringResource(R.string.deep_analysis_intro_body),
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Text(
                stringResource(R.string.deep_analysis_price_note, auraCost),
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DeepAnalysisResultView(
    analysis: DeepAnalysis,
    modifier: Modifier = Modifier,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val (captureModifier, cardHost) = rememberShareableCardHost()
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val savedOk = stringResource(R.string.deep_analysis_card_saved)
    val savedFail = stringResource(R.string.deep_analysis_card_save_failed)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Yuzlesme karti (9:16, paylasilabilir) ---
        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            ConfrontationCard(analysis = analysis, modifier = captureModifier)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val bitmap = cardHost.captureBitmap()
                        val ok = withContext(Dispatchers.IO) { saveToGallery(context, bitmap) }
                        savedMessage = if (ok) savedOk else savedFail
                    }
                },
                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = AstralGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.deep_analysis_download), color = AstralGold, fontSize = 13.sp)
            }
            Button(
                onClick = {
                    scope.launch {
                        val bitmap = cardHost.captureBitmap()
                        cardHost.share(context, bitmap)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Void950, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.deep_analysis_share),
                    color = Void950,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        savedMessage?.let {
            Text(it, color = AetherCyan, fontSize = 12.sp)
        }

        // --- Kisilik analizi ---
        SectionCard(title = stringResource(R.string.deep_analysis_section_personality)) {
            Text(
                analysis.personalityAnalysis.orEmpty(),
                color = Color.White,
                fontSize = 14.sp,
                lineHeight = 22.sp
            )
        }

        // --- Korku haritasi ---
        if (!analysis.fearMap.isNullOrEmpty()) {
            SectionCard(title = stringResource(R.string.deep_analysis_section_fears)) {
                analysis.fearMap.forEach { item -> FearRow(item) }
            }
        }

        // --- Yuzlesme onerisi ---
        if (!analysis.confrontationSolution.isNullOrBlank()) {
            SectionCard(title = stringResource(R.string.deep_analysis_section_solution)) {
                Text(
                    analysis.confrontationSolution,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Text(
            stringResource(R.string.deep_analysis_disclaimer),
            color = Color(0xFF6B7280),
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        OutlinedButton(
            onClick = onClose,
            border = BorderStroke(1.dp, Color.Gray),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.deep_analysis_back_to_list), color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                color = AstralGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SerifFontFamily
            )
            content()
        }
    }
}

@Composable
private fun FearRow(item: FearMapItem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(item.symbol, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${item.weight}%", color = AstralGold, fontSize = 12.sp)
        }
        LinearProgressIndicator(
            progress = { item.weight / 100f },
            color = AetherViolet,
            trackColor = Void800,
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
        if (!item.evidence.isNullOrBlank()) {
            Text(item.evidence, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

/** Instagram hikaye orani (9:16). Paylasim/indirme bu composable'i rasterize eder. */
@Composable
private fun ConfrontationCard(analysis: DeepAnalysis, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(width = CardWidth, height = CardHeight)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Void950, Void900, Void950)))
    ) {
        if (!analysis.cardImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = analysis.cardImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Metnin gorsel uzerinde okunabilir kalmasi icin alttan koyulasan
            // bir perde.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Void950.copy(alpha = 0.45f), Void950.copy(alpha = 0.92f))
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                analysis.cardHeadline.orEmpty(),
                color = AstralGold,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = SerifFontFamily
            )
            Text(
                analysis.cardAffirmation.orEmpty(),
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
            Text(
                "lunosfer",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 11.sp,
                letterSpacing = 3.sp
            )
        }
    }
}

/** MediaStore'a kaydeder; API 29+ scoped storage, altinda WRITE izni gerekmiyor cunku
 *  uygulamanin kendi olusturdugu medyayi ekliyoruz (RELATIVE_PATH yalnizca Q+). */
private fun saveToGallery(context: Context, bitmap: Bitmap): Boolean = runCatching {
    val name = "lunosfer_${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lunosfer")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
    resolver.openOutputStream(uri)?.use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        ?: return false

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }
    true
}.getOrElse { false }

private fun formatAnalysisDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(iso.take(19))
        SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(parsed!!)
    }.getOrElse { iso.take(10) }
}
