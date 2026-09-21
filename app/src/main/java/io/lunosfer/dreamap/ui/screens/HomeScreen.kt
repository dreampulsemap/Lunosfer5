package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.FeedItem
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.HomeViewModel
import io.lunosfer.dreamap.ui.viewmodel.UiState
import io.lunosfer.dreamap.util.AppLanguage
import java.text.SimpleDateFormat
import java.util.Locale

import io.lunosfer.dreamap.ui.components.DiaryRingsBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    compassViewModel: io.lunosfer.dreamap.ui.viewmodel.DailyCompassViewModel = viewModel(),
    onOpenDreamReels: (List<Dream>, Int) -> Unit = { _, _ -> },
    onOpenComposer: () -> Unit = {},
    onOpenViewer: (String) -> Unit = {},
    onOpenReels: (List<Goal>, Int) -> Unit = { _, _ -> },
    onUserClick: (String) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val compassState by compassViewModel.state.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val headerCounts by viewModel.headerCounts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val canLoadMore by viewModel.canLoadMore.collectAsState()
    val likedDreamIds by viewModel.likedDreamIds.collectAsState()
    val likeCountOverrides by viewModel.likeCountOverrides.collectAsState()
    val actionError by viewModel.actionError.collectAsState()
    val context = LocalContext.current
    val currentUserId = supabaseClient.auth.currentUserOrNull()?.id

    LaunchedEffect(actionError) {
        if (actionError != null) {
            Toast.makeText(context, actionError, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

    // Akisi yenilemenin hicbir yolu yoktu: yeni bir ruya/vizyon paylasildiginda
    // kullanici uygulamayi kapatip acmadan goremiyordu.
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize().background(Void950)
    ) {
        when (val current = state) {
            is UiState.Loading -> HomeLoading()
            is UiState.Error -> HomeError(message = current.message, onRetry = viewModel::retry)
            is UiState.Success -> HomeFeedList(
                items = current.data,
                likedDreamIds = likedDreamIds,
                likeCountOverrides = likeCountOverrides,
                onToggleLike = { dream -> viewModel.toggleDreamLike(dream, currentUserId) },
                onOpenDreamReels = onOpenDreamReels,
                onOpenComposer = onOpenComposer,
                onOpenViewer = onOpenViewer,
                onOpenReels = onOpenReels,
                compassState = compassState,
                onDrawCompass = compassViewModel::draw,
                streak = streak,
                headerCounts = headerCounts,
                isLoadingMore = isLoadingMore,
                canLoadMore = canLoadMore,
                onLoadMore = viewModel::loadMore,
                onUserClick = onUserClick
            )
        }
    }
}

@Composable
private fun HomeLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = AstralGold)
    }
}

@Composable
private fun HomeError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_feed),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFF94A3B8),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRetry,
            // buttonColors() (dolu buton renkleri) bir OutlinedButton'a verilince
            // zemin de altin, yazi da altin oluyordu: "Tekrar Dene" yazisi
            // gorunmuyordu (canli ekran goruntusunde bos bir altin hap).
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
            border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.retry))
        }
    }
}

@Composable
private fun HomeFeedList(
    items: List<FeedItem>,
    likedDreamIds: Set<Long>,
    likeCountOverrides: Map<Long, Int>,
    onToggleLike: (Dream) -> Unit,
    onOpenDreamReels: (List<Dream>, Int) -> Unit,
    onOpenComposer: () -> Unit,
    onOpenViewer: (String) -> Unit,
    onOpenReels: (List<Goal>, Int) -> Unit = { _, _ -> },
    compassState: io.lunosfer.dreamap.ui.viewmodel.CompassUiState,
    onDrawCompass: () -> Unit,
    streak: io.lunosfer.dreamap.ui.viewmodel.StreakInfo,
    headerCounts: io.lunosfer.dreamap.ui.viewmodel.HomeHeaderCounts,
    isLoadingMore: Boolean = false,
    canLoadMore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onUserClick: (String) -> Unit = {}
) {
    val listState = rememberLazyListState()

    // Sonsuz kaydirma: son karta yaklasinca sonraki sayfayi iste.
    LaunchedEffect(listState, canLoadMore, items.size) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }.collect { nearEnd ->
            if (nearEnd && canLoadMore) onLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        // Alttaki "+" butonu ve gezinme cubugu son karti kapatiyordu.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Karsilama + gunluk seri basligi. HomeViewModel bu bilgiyi (computeStreak)
        // zaten hesaplayip StateFlow olarak yayinliyordu ve home_welcome_* /
        // home_streak_* string'leri de tanimliydi — ama hicbir ekran okumuyordu,
        // yani ozellik yazilmis ama hic baglanmamisti.
        item {
            WelcomeStreakHeader(
                streak = streak,
                dreamCount = headerCounts.todayDreams,
                visionCount = headerCounts.activeVisions
            )
        }

        item {
            DiaryRingsBar(
                onOpenComposer = onOpenComposer,
                onOpenViewer = onOpenViewer
            )
        }

        // Web'deki ana sayfada oldugu gibi, basili-tut gunluk pusula akisin
        // hemen ustunde duruyor (bkz. components/DailyCompass.jsx).
        item {
            io.lunosfer.dreamap.ui.components.DailyCompassHoldCard(
                state = compassState,
                onDraw = onDrawCompass
            )
        }

        if (items.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_feed_empty_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_feed_empty_desc),
                        color = Color(0xFF94A3B8),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        val visionGoals = items.filterIsInstance<FeedItem.VisionItem>().map { it.goal }
        val dreamsList = items.filterIsInstance<FeedItem.DreamItem>().map { it.dream }

        items(
            items,
            // Onceki anahtar (createdAt + hashCode) ayni ogenin iki kez gelmesi
            // durumunda cakisiyor ve LazyColumn "Key was already used" ile
            // cokuyordu; tur + kimlik her zaman benzersiz.
            key = { item ->
                when (item) {
                    is FeedItem.DreamItem -> "dream-${item.dream.id}"
                    is FeedItem.VisionItem -> "vision-${item.goal.id}"
                }
            }
        ) { feedItem ->
            when (feedItem) {
                is FeedItem.DreamItem -> {
                    DreamFeedCard(
                        dream = feedItem.dream,
                        isLiked = likedDreamIds.contains(feedItem.dream.id),
                        likesCount = likeCountOverrides[feedItem.dream.id] ?: (feedItem.dream.likesCount ?: 0),
                        onToggleLike = { onToggleLike(feedItem.dream) },
                        onDreamClick = { id ->
                            val index = dreamsList.indexOfFirst { it.id == id }.coerceAtLeast(0)
                            onOpenDreamReels(dreamsList, index)
                        },
                        onUserClick = onUserClick
                    )
                }
                is FeedItem.VisionItem -> {
                    VisionFeedCard(
                        goal = feedItem.goal,
                        onClick = {
                            val index = visionGoals.indexOfFirst { it.id == feedItem.goal.id }.coerceAtLeast(0)
                            onOpenReels(visionGoals, index)
                        },
                        onUserClick = onUserClick
                    )
                }
            }
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun WelcomeStreakHeader(
    streak: io.lunosfer.dreamap.ui.viewmodel.StreakInfo,
    dreamCount: Int,
    visionCount: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(R.string.home_welcome_title),
            color = AstralGold,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp
        )

        Text(
            text = stringResource(R.string.home_welcome_summary, dreamCount, visionCount),
            color = Color.White,
            fontSize = 15.sp,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (streak.streakDays > 0) {
                    pluralStringResource(R.plurals.home_streak_days, streak.streakDays, streak.streakDays)
                } else {
                    stringResource(R.string.home_streak_start)
                },
                color = if (streak.streakDays > 0) AstralGold else Color.Gray,
                fontSize = 12.sp,
                fontWeight = if (streak.streakDays > 0) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Gorunurluk rozeti ham veritabani degerini buyuk harfe cevirip basiyordu:
 * uygulama Turkce'yken bile "PUBLIC/FRIENDS/PRIVATE" goruluyordu.
 */
@Composable
fun visibilityLabel(visibility: String): String = when (visibility.lowercase(Locale.US)) {
    "public" -> stringResource(R.string.dream_public)
    "friends" -> stringResource(R.string.dream_friends)
    "private" -> stringResource(R.string.dream_private)
    else -> visibility
}

@Composable
private fun FeedCardOwnerHeader(
    ownerName: String,
    avatarUrl: String?,
    dreamDate: String? = null,
    visibility: String? = null,
    ownerId: String? = null,
    onUserClick: (String) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Avatar + isim tiklanabilir: akistaki bir gonderiden dogrudan o
        // kisinin profiline gidilebiliyor (Explore/Notifications ile ayni
        // onUserClick deseni). ownerId yoksa tiklama devre disi.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = if (ownerId != null) {
                Modifier.clickable { onUserClick(ownerId) }
            } else {
                Modifier
            }
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Void800),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        ownerName.take(1).uppercase(),
                        color = AstralGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column {
                Text(
                    ownerName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (!dreamDate.isNullOrBlank()) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val dateDisplay = try {
                        val date = sdf.parse(dreamDate.take(19))
                        SimpleDateFormat("d MMMM yyyy", AppLanguage.locale()).format(date ?: java.util.Date())
                    } catch (e: Exception) {
                        dreamDate.take(10)
                    }
                    Text(
                        dateDisplay,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (!visibility.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Void800)
                    .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)), shape = RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = visibilityLabel(visibility).uppercase(AppLanguage.locale()),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

@Composable
fun getHomeSlideTitle(pageIndex: Int): String {
    return when (pageIndex) {
        0 -> stringResource(R.string.dream_slide_title_0)
        1 -> stringResource(R.string.dream_slide_title_1)
        2 -> stringResource(R.string.dream_slide_title_2)
        else -> ""
    }
}

/**
 * Ana sayfa kartı - 3 Sayfalı Yana Kaydırılabilir Pager (Görsel + Metin + AI Analiz)
 * Instagram Post formatında sabit 4:5 frame yüksekliği, kaydırırken boyut değişmez.
 */
@Composable
private fun DreamFeedCard(
    dream: Dream,
    isLiked: Boolean,
    likesCount: Int,
    onToggleLike: () -> Unit,
    onDreamClick: (Long) -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 3 })

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Owner, Date, Visibility
            FeedCardOwnerHeader(
                ownerName = dream.owner?.nameOrFallback ?: stringResource(R.string.common_unknown_fallback),
                avatarUrl = dream.owner?.avatarUrl,
                dreamDate = dream.dreamDate ?: dream.createdAt,
                visibility = dream.visibility,
                ownerId = dream.owner?.id,
                onUserClick = onUserClick
            )

            // Top-left page indicator text (e.g., "Rüya Görseli (1/3)") in gray monospace style
            val slideLabel = getHomeSlideTitle(pagerState.currentPage)
            Text(
                text = "$slideLabel (${pagerState.currentPage + 1}/3)",
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp)
            )

            // Sabit 4:5 Aspect Ratio Pager Çerçevesi (Instagram Post Tarzı)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 5f)
            ) { page ->
                when (page) {
                    0 -> DreamImagePage(dream = dream, onDreamClick = onDreamClick)
                    1 -> DreamTextPage(dream = dream)
                    2 -> DreamAnalysisPage(dream = dream)
                }
            }

            // Paylaşım Alt Etkileşim Barı (Beğeni, Yorum, Detay Linki)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        // Dokunma hedefi ~20dp idi; kucuk ekranlarda kalbe isabet
                        // ettirmek zordu (min. 48dp onerilir).
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onToggleLike)
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) SemanticDanger400 else Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("$likesCount", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDreamClick(dream.id) }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Message,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${dream.commentsCount ?: 0}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    }
                }

                Text(
                    text = stringResource(R.string.home_feed_detail_btn),
                    color = AstralGold.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onDreamClick(dream.id) }
                )
            }
        }
    }
}

@Composable
private fun DreamImagePage(dream: Dream, onDreamClick: (Long) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDreamClick(dream.id) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void800),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!dream.aiImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = dream.aiImageUrl,
                    contentDescription = dream.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    AstralGold.copy(alpha = 0.18f),
                                    AetherViolet.copy(alpha = 0.28f),
                                    Void800
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = AstralGold.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.home_brand_watermark),
                            color = AstralGold.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            // Dark gradient scrim ONLY inside this image card at the bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Void950.copy(alpha = 0.75f),
                                Void950.copy(alpha = 0.95f)
                            )
                        )
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = dream.displayTitle,
                        color = AstralGold,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = SerifFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!dream.aiArchetypes.isNullOrEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(dream.aiArchetypes) { arch ->
                                ChipView(text = arch, isSelected = true)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamTextPage(dream: Dream) {
    val titleLabel = getHomeSlideTitle(1)

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void800),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "📖", fontSize = 16.sp)
                Text(
                    text = titleLabel,
                    color = AstralGold,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            if (!dream.aiTitle.isNullOrBlank()) {
                Text(
                    text = dream.aiTitle,
                    color = AstralGold,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SerifFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Text(
                text = dream.content,
                color = Color(0xFFE2E8F0),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                fontSize = 14.sp
            )

            if (!dream.userSelectedSentiment.isNullOrBlank()) {
                val emotions = dream.userSelectedSentiment.split(",").map { it.trim() }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(emotions) { emotion ->
                        ChipView(text = emotion)
                    }
                }
            }
        }
    }
}

@Composable
private fun DreamAnalysisPage(dream: Dream) {
    val titleLabel = getHomeSlideTitle(2)
    val analysis = dream.aiJungianAnalysis
    // Cok dilli analiz metni, cihaz dili degil kullanicinin sectigi UYGULAMA dili.
    val locale = AppLanguage.code()

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Void800),
        border = BorderStroke(1.dp, AetherViolet.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "✨", fontSize = 16.sp)
                    Text(
                        text = titleLabel,
                        color = AstralGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AetherViolet.copy(alpha = 0.2f))
                        .border(BorderStroke(0.5.dp, AetherViolet.copy(alpha = 0.4f)), shape = RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(stringResource(R.string.home_ai_jung_badge), color = AstralGold, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            val titleText = analysis?.title?.get(locale)
                ?: analysis?.title?.get("en")
                ?: dream.displayTitle

            Text(
                text = titleText,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SerifFontFamily,
                    fontWeight = FontWeight.Bold
                )
            )

            val summaryText = analysis?.summary?.get(locale)
                ?: analysis?.summary?.get("en")

            if (!summaryText.isNullOrBlank()) {
                Text(
                    text = summaryText,
                    color = Color(0xFFE2E8F0),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    fontSize = 13.sp
                )
            }

            val motivText = analysis?.motiv?.get(locale)
                ?: analysis?.motiv?.get("en")

            if (!motivText.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Void900.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = "\"$motivText\"",
                        color = AstralGold,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (!dream.userSelectedSentiment.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Void900)
                        .border(BorderStroke(0.5.dp, AstralGold.copy(alpha = 0.3f)), shape = RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_feed_sentiment_label, dream.userSelectedSentiment),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            val archetypes = analysis?.archetypes ?: dream.aiArchetypes
            if (!archetypes.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.home_feed_archetypes_label),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(archetypes) { arch ->
                        ChipView(text = arch, isSelected = true)
                    }
                }
            }
        }
    }
}

/** goals tablosundan bir kart — GoalCard.jsx'in ön yüzüyle aynı alanlar (title, cover, completion). */
@Composable
private fun VisionFeedCard(goal: Goal, onClick: () -> Unit, onUserClick: (String) -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FeedCardOwnerHeader(
                ownerName = goal.owner?.nameOrFallback ?: stringResource(R.string.common_unknown_fallback),
                avatarUrl = goal.owner?.avatarUrl,
                visibility = stringResource(R.string.home_feed_vision_badge),
                ownerId = goal.owner?.id,
                onUserClick = onUserClick
            )

            Box {
                if (goal.coverImageUrl != null) {
                    AsyncImage(
                        model = goal.coverImageUrl,
                        contentDescription = goal.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Void800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = AstralGold.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                    }
                }
            }

            Text(
                text = goal.title,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(((goal.completionPercentage?.toFloat() ?: 0f) / 100f).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(AstralGold)
                    )
                }
                Text(stringResource(R.string.home_feed_completion_text, goal.completionPercentage?.toInt() ?: 0), color = Color(0xFF94A3B8), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ChipView(text: String, isSelected: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) AstralGold.copy(alpha = 0.15f) else Void800)
            .border(
                BorderStroke(
                    0.5.dp,
                    if (isSelected) AstralGold.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) AstralGold else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
