package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.Void950
import io.lunosfer.dreamap.ui.viewmodel.ReelsQueueHolder
import io.lunosfer.dreamap.ui.viewmodel.SlidesViewerUiState
import io.lunosfer.dreamap.ui.viewmodel.SlidesViewerViewModel
import io.lunosfer.dreamap.ui.viewmodel.VisionVideoPlayerUiState
import io.lunosfer.dreamap.ui.viewmodel.VisionVideoPlayerViewModel

/**
 * Uygulamadaki HER vizyon kartının (Ana Sayfa akışı, Keşfet, Vizyon sekmesi,
 * Profil) tek giriş noktası. Tek tıkla açılır, dikey kaydırmayla (Reels/TikTok
 * tarzı) ReelsQueueHolder'daki listede bir sonraki/önceki vizyona geçilir.
 *
 * Var olan tek-vizyon ekranlarını (VisionVideoPlayerScreen, SlidesViewerScreen)
 * YENİDEN YAZMAK yerine, onların iç içerik composable'larını (artık internal)
 * burada sayfa başına yeniden kullanıyoruz — mana/kaydet/düzenle/Ken-Burns
 * efekti gibi tüm davranış BİREBİR aynı kalıyor, sadece dışarıdan "bu sayfa şu
 * an ekranda mı" (isActive) bilgisiyle oynatma/zamanlayıcı duraklatılıyor.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VisionReelsScreen(
    onClose: () -> Unit,
    onOpenGoalDetail: (String) -> Unit,
    onEditVideo: (String) -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    val goals = remember { ReelsQueueHolder.goals }
    val startIndex = remember { ReelsQueueHolder.startIndex }

    if (goals.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val pagerState = rememberPagerState(initialPage = startIndex) { goals.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val goal = goals[page]
            VisionReelPage(
                goal = goal,
                isActive = pagerState.currentPage == page,
                onBack = onClose,
                onEditVideo = onEditVideo,
                onOpenGoalDetail = onOpenGoalDetail,
                onUserClick = onUserClick
            )
        }

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.generic_close_cd), tint = Color.White)
        }
    }
}

@Composable
private fun VisionReelPage(
    goal: Goal,
    isActive: Boolean,
    onBack: () -> Unit,
    onEditVideo: (String) -> Unit,
    onOpenGoalDetail: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val hasVideo = !goal.visionVideoUrl.isNullOrBlank()
    val hasSlides = (goal.slideCount ?: 0) > 0

    when {
        hasVideo -> VisionReelVideoPage(goal = goal, isActive = isActive, onBack = onBack, onEditVideo = onEditVideo, onUserClick = onUserClick)
        hasSlides -> VisionReelSlidesPage(goal = goal, isActive = isActive, onBack = onBack, onOpenGoalDetail = onOpenGoalDetail, onUserClick = onUserClick)
        else -> VisionReelCoverFallbackPage(goal = goal, onBack = onBack, onOpenGoalDetail = onOpenGoalDetail, onUserClick = onUserClick)
    }
}

@Composable
private fun VisionReelVideoPage(
    goal: Goal,
    isActive: Boolean,
    onBack: () -> Unit,
    onEditVideo: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val factory = remember(goal.id) { VisionVideoPlayerViewModel.Factory(goal.id) }
    val viewModel: VisionVideoPlayerViewModel = viewModel(factory = factory, key = "reel_video_${goal.id}")
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            is VisionVideoPlayerUiState.Loading -> CircularProgressIndicator(color = AstralGold, modifier = Modifier.align(Alignment.Center))
            is VisionVideoPlayerUiState.Error -> ReelErrorMessage(message = s.message, onBack = onBack)
            is VisionVideoPlayerUiState.Content -> VisionVideoPlayerContent(
                state = s,
                onClose = onBack,
                onEdit = { onEditVideo(goal.id) },
                onUserClick = { onUserClick(s.goal.owner?.id ?: s.goal.userId) },
                onToggleMana = viewModel::toggleMana,
                onDoubleTapLike = viewModel::likeOnDoubleTap,
                onToggleSave = viewModel::toggleSave,
                onOpenComments = viewModel::openComments,
                onCloseComments = viewModel::closeComments,
                onAddComment = viewModel::addComment,
                onDeleteComment = viewModel::deleteComment,
                onOpenReport = viewModel::openReportSheet,
                onCloseReport = viewModel::closeReportSheet,
                onSubmitReport = viewModel::submitReport,
                isActive = isActive
            )
        }
    }
}

@Composable
private fun VisionReelSlidesPage(
    goal: Goal,
    isActive: Boolean,
    onBack: () -> Unit,
    onOpenGoalDetail: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    val factory = remember(goal.id) { SlidesViewerViewModel.Factory(goal.id) }
    val viewModel: SlidesViewerViewModel = viewModel(factory = factory, key = "reel_slides_${goal.id}")
    val state by viewModel.state.collectAsState()

    // isActive: Reels pager'da bu sayfa görünür DEĞİLKEN oto-oynatma
    // zamanlayıcısı arka planda ilerlemesin diye (aksi halde geri
    // kaydırınca slaytlar beklenmedik bir index'te bulunur).
    LaunchedEffect(isActive) {
        if (isActive) viewModel.resumeTimer() else viewModel.pauseTimer()
    }

    LaunchedEffect(state) {
        if (state is SlidesViewerUiState.Closed) onBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        when (val s = state) {
            is SlidesViewerUiState.Loading -> CircularProgressIndicator(color = AstralGold, modifier = Modifier.align(Alignment.Center))
            is SlidesViewerUiState.Error -> ReelErrorMessage(message = s.message, onBack = onBack)
            is SlidesViewerUiState.Content -> SlidesViewerContent(
                state = s,
                onPause = viewModel::pauseTimer,
                onResume = { if (isActive) viewModel.resumeTimer() },
                onNext = viewModel::nextSlide,
                onPrevious = viewModel::previousSlide,
                onClose = onBack,
                onGoalClick = { s.goal?.let { onOpenGoalDetail(it.id) } },
                onUserClick = { onUserClick(s.owner?.id ?: s.goal?.userId ?: goal.userId) },
                onToggleMana = viewModel::toggleMana,
                onToggleSave = viewModel::toggleSaveSlide,
                onDelete = viewModel::deleteCurrentSlide,
                onOpenComments = viewModel::openComments,
                onCloseComments = viewModel::closeComments,
                onAddComment = viewModel::addComment,
                onDeleteComment = viewModel::deleteComment,
                onOpenReport = viewModel::openReportSheet,
                onCloseReport = viewModel::closeReportSheet,
                onSubmitReport = viewModel::submitReport
            )
            is SlidesViewerUiState.Closed -> {}
        }
    }
}

/** Ne video ne slayt varsa (henüz medya eklenmemiş vizyon) — sadece kapak. */
@Composable
private fun VisionReelCoverFallbackPage(
    goal: Goal,
    onBack: () -> Unit,
    onOpenGoalDetail: (String) -> Unit,
    onUserClick: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (!goal.coverImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = goal.coverImageUrl,
                contentDescription = goal.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Void950),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = AstralGold.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))))
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.clickable { onUserClick(goal.owner?.id ?: goal.userId) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val avatarUrl = goal.owner?.avatarUrl
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(28.dp).clip(CircleShape).border(1.dp, AstralGold, CircleShape)
                        )
                    }
                    Text(
                        text = goal.owner?.nameOrFallback ?: "",
                        color = AstralGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(goal.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                OutlinedButton(
                    onClick = { onOpenGoalDetail(goal.id) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.55f))
                ) {
                    Text(stringResource(R.string.goal_detail_watch_vision))
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
        ) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.generic_close_cd), tint = Color.White)
        }
    }
}

@Composable
private fun ReelErrorMessage(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = AstralGold)) {
            Text(text = stringResource(R.string.generic_back), color = Void950)
        }
    }
}
