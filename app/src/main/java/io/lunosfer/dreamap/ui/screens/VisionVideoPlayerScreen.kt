package io.lunosfer.dreamap.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.GoalReportReason
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.VisionVideoPlayerUiState
import io.lunosfer.dreamap.ui.viewmodel.VisionVideoPlayerViewModel
import kotlinx.coroutines.delay

/**
 * components/VisionVideoPlayer.jsx'in Android karşılığı. Tam ekran,
 * edge-to-edge, döngülü oynatan tek video; dokununca oynat/duraklat, çift
 * dokununca beğen, paylaşan profiline gitme, yorumlar, bildirme.
 */
@Composable
fun VisionVideoPlayerScreen(
    goalId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    val factory = remember(goalId) { VisionVideoPlayerViewModel.Factory(goalId) }
    val viewModel: VisionVideoPlayerViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val s = state) {
            is VisionVideoPlayerUiState.Loading -> {
                CircularProgressIndicator(color = AstralGold, modifier = Modifier.align(Alignment.Center))
            }
            is VisionVideoPlayerUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = AstralGold)) {
                        Text(text = stringResource(R.string.generic_back), color = Void950)
                    }
                }
            }
            is VisionVideoPlayerUiState.Content -> {
                VisionVideoPlayerContent(
                    state = s,
                    onClose = onBack,
                    onEdit = { onEdit(goalId) },
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
                    onCloneToMyVisions = viewModel::cloneToMyVisions,
                    onConsumeCloneToast = viewModel::consumeCloneResultToast
                )
            }
        }
    }
}

@Composable
internal fun VisionVideoPlayerContent(
    state: VisionVideoPlayerUiState.Content,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onUserClick: () -> Unit = {},
    onToggleMana: () -> Unit,
    onDoubleTapLike: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenComments: () -> Unit = {},
    onCloseComments: () -> Unit = {},
    onAddComment: (String) -> Unit = {},
    onDeleteComment: (String) -> Unit = {},
    onOpenReport: () -> Unit = {},
    onCloseReport: () -> Unit = {},
    onSubmitReport: (GoalReportReason, String?) -> Unit = { _, _ -> },
    onCloneToMyVisions: () -> Unit = {},
    onConsumeCloneToast: () -> Unit = {},
    isActive: Boolean = true
) {
    val context = LocalContext.current
    val videoUrl = state.goal.visionVideoUrl ?: return

    var isPlaying by remember { mutableStateOf(true) }
    var showPauseIcon by remember { mutableStateOf(false) }
    var showHeartBurst by remember { mutableStateOf(false) }

    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            volume = 1f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(videoUrl) {
        onDispose { exoPlayer.release() }
    }
    // isActive: Reels pager'da bu sayfa görünür DEĞİLKEN video sesi/oynatması
    // arka planda devam etmesin diye eklendi. Tek-vizyon ekranında (isActive
    // varsayılan true) davranış eskisiyle birebir aynı kalır.
    LaunchedEffect(isPlaying, isActive) {
        if (isPlaying && isActive) exoPlayer.play() else exoPlayer.pause()
    }

    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(650)
            showHeartBurst = false
        }
    }
    LaunchedEffect(showPauseIcon) {
        if (showPauseIcon) {
            delay(400)
            showPauseIcon = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        isPlaying = !isPlaying
                        showPauseIcon = true
                    },
                    onDoubleTap = {
                        onDoubleTapLike()
                        showHeartBurst = true
                        if (!isPlaying) isPlaying = true
                    }
                )
            }
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        )

        AnimatedVisibility(
            visible = showPauseIcon,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Black.copy(alpha = 0.4f), shape = androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showHeartBurst,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = SemanticDanger500,
                modifier = Modifier.size(96.dp)
            )
        }

        // --- Üst gradyan: paylaşan (tıklanınca profiline gider) + kapat (+ sahipse düzenle, değilse bildir) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f).padding(end = 8.dp).clickable(onClick = onUserClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val avatarUrl = state.goal.owner?.avatarUrl
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(36.dp).clip(CircleShape).border(1.dp, AstralGold, CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(AstralGold.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.goal.owner?.nameOrFallback?.take(1)?.uppercase() ?: "?",
                            color = AstralGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column {
                    Text(
                        text = state.goal.owner?.nameOrFallback ?: "",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = state.goal.title,
                        color = AstralGold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (state.isOwner) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.generic_edit_cd), tint = Color.White)
                    }
                } else {
                    VisionMoreMenuButton(isOwner = false, onReportClick = onOpenReport)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.generic_close_cd), tint = Color.White)
                }
            }
        }

        // --- Sağ alt aksiyon şeridi ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (!state.isOwner) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onToggleMana) {
                        Icon(
                            imageVector = if (state.hasReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = if (state.hasReacted) SemanticDanger500 else Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "${state.believersCount}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onOpenComments) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Comment,
                        contentDescription = stringResource(R.string.goal_detail_comments_count, state.goal.commentsCount ?: 0),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = "${state.goal.commentsCount ?: 0}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(onClick = onToggleSave) {
                Icon(
                    imageVector = if (state.hasSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    tint = if (state.hasSaved) BrandSecondary400 else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            if (!state.isOwner) {
                IconButton(onClick = onCloneToMyVisions, enabled = !state.isCloning) {
                    if (state.isCloning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.vision_action_add_to_my_visions_cd),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // Klonlama sonucu için kısa ömürlü, sessiz bir bilgi kartı (Reels
        // deneyimini bölmeyen bir Toast yerine bu tam ekran akışın kendi
        // görsel diline uygun inline pill).
        state.cloneResultToast?.let { alreadyCloned ->
            LaunchedEffect(alreadyCloned) {
                delay(2200)
                onConsumeCloneToast()
            }
            Text(
                text = stringResource(
                    if (alreadyCloned) R.string.vision_clone_msg_already_added else R.string.vision_clone_msg_added
                ),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 100.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (state.showComments) {
        VisionCommentsSheet(
            comments = state.comments,
            isLoading = state.isLoadingComments,
            isSubmitting = state.isSubmittingComment,
            currentUserId = supabaseClient.auth.currentUserOrNull()?.id,
            onDismiss = onCloseComments,
            onAddComment = onAddComment,
            onDeleteComment = onDeleteComment
        )
    }

    if (state.showReportSheet) {
        VisionReportSheet(
            isSubmitting = state.isSubmittingReport,
            onDismiss = onCloseReport,
            onSubmit = onSubmitReport
        )
    }
}
