package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiritualToolsScreen(
    onBack: () -> Unit = {},
    onUpgrade: () -> Unit = {},
    onOpenDeepAnalysis: () -> Unit = {},
    viewModel: SpiritualToolsViewModel = viewModel()
) {
    val mentalWallState by viewModel.mentalWallState.collectAsState()
    val psycheMapState by viewModel.psycheMapState.collectAsState()
    val prophetState by viewModel.prophetState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.spiritual_tab_mental_wall),
        stringResource(R.string.spiritual_tab_psyche_map),
        stringResource(R.string.spiritual_tab_prophet)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("✨", fontSize = 20.sp)
                        Text(
                            stringResource(R.string.spiritual_title),
                            color = AstralGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SerifFontFamily
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button_desc), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Void900,
                contentColor = AstralGold,
                divider = { HorizontalDivider(color = Color.White.copy(alpha = 0.1f)) }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) AstralGold else Color.Gray
                            )
                        }
                    )
                }
            }

            // Derin Analiz yalnizca ust bardaki tasma menusunde duruyordu ve
            // kimse bulamiyordu. Diger AI okumalarinin yaninda, gorunur bir
            // giris.
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable(onClick = onOpenDeepAnalysis),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Void900),
                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("🧠", fontSize = 20.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.deep_analysis_title),
                            color = AstralGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SerifFontFamily
                        )
                        Text(
                            stringResource(R.string.deep_analysis_intro_title),
                            color = Color(0xFFCBD5E1),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = AstralGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> MentalWallSection(
                        state = mentalWallState,
                        onGenerate = { deep -> viewModel.generateMentalWall(deep) },
                        onUpgrade = onUpgrade
                    )
                    1 -> PsycheMapSection(state = psycheMapState, onRefresh = viewModel::loadPsycheMap)
                    2 -> ProphetSection(
                        state = prophetState,
                        onGeneral = { viewModel.generalProphecy() },
                        onAsk = { question -> viewModel.askProphet(question) },
                        onUpgrade = onUpgrade,
                        onDeepen = viewModel::deepenLastProphecy
                    )
                }
            }
        }
    }
}

// Sunucu deepCost bildirmezse kullanilan varsayilan fiyat; backend'deki
// DEEP_AURA_COST ile ayni (pages/api/mental-wall/generate.js, prophet.js).
private const val DEFAULT_DEEP_AURA_COST = 10

// --- 1) Zihin Duvarı (Mental Wall) ---

@Composable
private fun MentalWallSection(
    state: MentalWallUiState,
    onGenerate: (Boolean) -> Unit,
    onUpgrade: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Void900),
                border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🖼️", fontSize = 22.sp)
                        Text(
                            stringResource(R.string.spiritual_mental_wall_header),
                            color = AstralGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SerifFontFamily
                        )
                    }

                    Text(
                        text = stringResource(R.string.spiritual_mental_wall_desc),
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Button(
                        onClick = { onGenerate(false) },
                        enabled = state !is MentalWallUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state is MentalWallUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.spiritual_synthesizing), color = Color.White, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AstralGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.spiritual_synthesize_btn), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        when (state) {
            is MentalWallUiState.Idle -> {
                item {
                    Text(
                        text = stringResource(R.string.spiritual_mental_wall_idle),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                }
            }
            is MentalWallUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = AstralGold)
                            Text(stringResource(R.string.spiritual_mental_wall_loading_status), color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
            }
            is MentalWallUiState.Success -> {
                val res = state.response
                if (res.limitReached) {
                    item {
                        ProphetUpsellCard(
                            title = stringResource(R.string.spiritual_prophet_limit_title),
                            body = stringResource(R.string.spiritual_prophet_limit_body, res.dailyLimit ?: 3),
                            onUpgrade = onUpgrade,
                            auraCost = res.deepCost ?: DEFAULT_DEEP_AURA_COST,
                            onPayWithAura = { onGenerate(true) }
                        )
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Void900),
                        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (!res.displayImage.isNullOrBlank()) {
                                AsyncImage(
                                    model = res.displayImage,
                                    contentDescription = stringResource(R.string.mental_wall_image_desc),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }

                            if (!res.displayText.isNullOrBlank()) {
                                Text(
                                    text = res.displayText!!,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }

                            if (!res.archetypes.isNullOrEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    res.archetypes.forEach { arch ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(AetherViolet.copy(alpha = 0.3f))
                                                .border(0.5.dp, AstralGold.copy(alpha = 0.5f), RoundedCornerShape(50))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Text(text = arch, color = AstralGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }

                            res.remaining?.let { left ->
                                if (!res.isPremium) {
                                    Text(
                                        text = stringResource(R.string.spiritual_mental_wall_remaining, left),
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
                // Kisa (ucretsiz) rapor alindiysa derin surumu teklif et:
                // Premium sinirsiz, degilse tek seferlik Aura.
                if (!res.isPremium && !res.detailed && !res.limitReached) {
                    item {
                        ProphetUpsellCard(
                            title = stringResource(R.string.spiritual_deep_prompt_title),
                            body = stringResource(
                                R.string.spiritual_deep_prompt_body,
                                res.deepCost ?: DEFAULT_DEEP_AURA_COST
                            ),
                            onUpgrade = onUpgrade,
                            auraCost = res.deepCost ?: DEFAULT_DEEP_AURA_COST,
                            onPayWithAura = { onGenerate(true) }
                        )
                    }
                }
            }
            is MentalWallUiState.Error -> {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Void900),
                        border = BorderStroke(1.dp, SemanticDanger400.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = state.message,
                            color = SemanticDanger400,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                // Aura yetmedigi icin basarisiz olduysa cikis yolunu goster;
                // eskiden sadece "olusturulamadi" yaziyor, kullanici neden
                // olmadigini hicbir sekilde ogrenemiyordu.
                if (state.insufficientAura) {
                    item {
                        ProphetUpsellCard(
                            title = stringResource(R.string.spiritual_deep_prompt_title),
                            body = stringResource(R.string.spiritual_deep_prompt_body, DEFAULT_DEEP_AURA_COST),
                            onUpgrade = onUpgrade
                        )
                    }
                }
            }
        }
    }
}

// --- 2) Psyche Map ---

@Composable
private fun PsycheMapSection(
    state: PsycheMapUiState,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Void900),
                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🌌", fontSize = 22.sp)
                            Text(
                                stringResource(R.string.spiritual_psyche_map_title),
                                color = AstralGold,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SerifFontFamily
                            )
                        }

                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_button_desc), tint = AstralGold)
                        }
                    }

                    Text(
                        text = stringResource(R.string.spiritual_psyche_map_desc),
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )
                }
            }
        }

        when (state) {
            is PsycheMapUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AstralGold)
                    }
                }
            }
            is PsycheMapUiState.Success -> {
                val data = state.response
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (data.psychicScore != null) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Void900),
                                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(stringResource(R.string.spiritual_psychic_score), color = Color.Gray, fontSize = 11.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text("${data.psychicScore}" + stringResource(R.string.spiritual_score_suffix), color = AstralGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (!data.dominantArchetype.isNullOrBlank()) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Void900),
                                border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(stringResource(R.string.spiritual_dominant_archetype), color = Color.Gray, fontSize = 11.sp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(data.dominantArchetype!!, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }
                }

                if (!data.summary.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Void900),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(stringResource(R.string.spiritual_synthesis_summary), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(data.summary!!, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }

                if (!data.archetypes.isNullOrEmpty()) {
                    item {
                        Text(stringResource(R.string.spiritual_archetype_distribution), color = AstralGold, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = SerifFontFamily)
                    }

                    items(data.archetypes) { arch ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Void900),
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(arch.name, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(stringResource(R.string.spiritual_percentage_format, arch.percentage ?: 0), color = AstralGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { ((arch.percentage ?: 0) / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)),
                                    color = AstralGold,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                                if (!arch.description.isNullOrBlank()) {
                                    Text(arch.description!!, color = Color.LightGray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            is PsycheMapUiState.Error -> {
                item {
                    Text(state.message, color = SemanticDanger400, fontSize = 13.sp)
                }
            }
            is PsycheMapUiState.Idle -> {}
        }
    }
}

// --- 3) Kahin (Prophet) ---

@Composable
private fun ProphetSection(
    state: ProphetUiState,
    onGeneral: () -> Unit,
    onAsk: (String) -> Unit,
    onUpgrade: () -> Unit,
    onDeepen: () -> Unit
) {
    var questionText by remember { mutableStateOf("") }
    val busy = state is ProphetUiState.Loading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Void900),
                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔮", fontSize = 22.sp)
                        Text(
                            stringResource(R.string.spiritual_prophet_header),
                            color = AstralGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = SerifFontFamily
                        )
                    }

                    Text(
                        text = stringResource(R.string.spiritual_prophet_desc),
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    )

                    // 1) Genel kehanet — soru gerektirmez, kullanicinin kendi
                    //    ruya ve vizyonlarindan uretilir.
                    Button(
                        onClick = onGeneral,
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AstralGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.spiritual_prophet_general_btn),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = stringResource(R.string.spiritual_prophet_or),
                        color = Color.Gray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2) Kahine sor — yazilan soruya cevap.
                    OutlinedTextField(
                        value = questionText,
                        onValueChange = { questionText = it },
                        placeholder = { Text(stringResource(R.string.spiritual_prophet_placeholder), color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstralGold,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = {
                            if (questionText.isNotBlank()) onAsk(questionText)
                        },
                        enabled = !busy && questionText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = AetherViolet),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.spiritual_prophet_loading), color = Color.White, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = AstralGold, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.spiritual_prophet_btn), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        when (state) {
            is ProphetUiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AstralGold)
                    }
                }
            }
            is ProphetUiState.Success -> {
                val res = state.response
                when {
                    // Ucretsiz gunluk hak bitti -> premium teklifi.
                    res.limitReached -> item {
                        ProphetUpsellCard(
                            title = stringResource(R.string.spiritual_prophet_limit_title),
                            body = stringResource(
                                R.string.spiritual_prophet_limit_body,
                                res.dailyLimit ?: 3
                            ),
                            onUpgrade = onUpgrade,
                            auraCost = res.deepCost ?: DEFAULT_DEEP_AURA_COST,
                            onPayWithAura = onDeepen,
                            busy = busy
                        )
                    }

                    // Hic ruya/vizyon yok -> once icerik eklemesi gerekiyor.
                    res.needsContent -> item {
                        Text(
                            stringResource(R.string.spiritual_prophet_needs_content),
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                    }

                    else -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Void900),
                                border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.6f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (!res.card.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(AetherViolet.copy(alpha = 0.3f))
                                                .border(0.5.dp, AstralGold, RoundedCornerShape(50))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = stringResource(R.string.spiritual_prophet_card_label, res.card),
                                                color = AstralGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (!res.resultText.isNullOrBlank()) {
                                        Text(
                                            text = res.resultText!!,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp
                                        )
                                    }

                                    res.remaining?.let { left ->
                                        if (!res.isPremium) {
                                            Text(
                                                text = stringResource(R.string.spiritual_prophet_remaining, left),
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Kisa (ucretsiz) cevap aldiysa daha detaylisini teklif et.
                        if (!res.isPremium && !res.detailed) {
                            item {
                                ProphetUpsellCard(
                                    title = stringResource(R.string.spiritual_deep_prompt_title),
                                    body = stringResource(
                                        R.string.spiritual_deep_prompt_body,
                                        res.deepCost ?: DEFAULT_DEEP_AURA_COST
                                    ),
                                    onUpgrade = onUpgrade,
                                    auraCost = res.deepCost ?: DEFAULT_DEEP_AURA_COST,
                                    onPayWithAura = onDeepen,
                                    busy = busy
                                )
                            }
                        }
                    }
                }
            }
            is ProphetUiState.Error -> {
                item {
                    Text(state.message, color = SemanticDanger400, fontSize = 13.sp)
                }
                if (state.insufficientAura) {
                    item {
                        ProphetUpsellCard(
                            title = stringResource(R.string.spiritual_deep_prompt_title),
                            body = stringResource(R.string.spiritual_deep_prompt_body, DEFAULT_DEEP_AURA_COST),
                            onUpgrade = onUpgrade
                        )
                    }
                }
            }
            is ProphetUiState.Idle -> {}
        }
    }
}

/**
 * "Daha derin bir yorum ister misin?" teklifi. Iki cikis yolu sunar:
 * Premium abonelik (sinirsiz) veya tek seferlik Aura odemesi. Aura yolu
 * [onPayWithAura] verilmediginde hic gosterilmez.
 */
@Composable
private fun ProphetUpsellCard(
    title: String,
    body: String,
    onUpgrade: () -> Unit,
    auraCost: Int? = null,
    onPayWithAura: (() -> Unit)? = null,
    busy: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, AstralGold)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = AstralGold, modifier = Modifier.size(18.dp))
                Text(title, color = AstralGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text(body, color = Color(0xFFCBD5E1), fontSize = 13.sp, lineHeight = 19.sp)
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.billing_upgrade_cta),
                    color = Void950,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            if (onPayWithAura != null && auraCost != null) {
                OutlinedButton(
                    onClick = onPayWithAura,
                    enabled = !busy,
                    border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.spiritual_deep_btn_aura, auraCost),
                        color = AstralGold,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
