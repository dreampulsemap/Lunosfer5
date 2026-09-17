package io.lunosfer.dreamap.ui.screens

import io.lunosfer.dreamap.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.DiaryComment
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.DiaryStoryViewerUiState
import io.lunosfer.dreamap.ui.viewmodel.DiaryStoryViewerViewModel
import kotlinx.coroutines.delay

@Composable
private fun DiaryStoryVideoPlayer(
    url: String,
    posterUrl: String?,
    isPaused: Boolean,
    onProgress: (Float) -> Unit,
    onEnded: () -> Unit
) {
    val context = LocalContext.current
    var isReady by remember(url) { mutableStateOf(false) }

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            volume = 1f
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(url) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) isReady = true
                if (playbackState == Player.STATE_ENDED) onEnded()
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPaused) {
        if (isPaused) exoPlayer.pause() else exoPlayer.play()
    }

    // İlerleme çubuğunu videonun gerçek oynatma konumundan sürükle (sabit
    // süreli zamanlayıcı yerine) — bkz. ViewModel.startTimer() içindeki not.
    LaunchedEffect(url) {
        while (true) {
            delay(50)
            val duration = exoPlayer.duration
            if (duration > 0) {
                onProgress(exoPlayer.currentPosition.toFloat() / duration.toFloat())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Video hazır olana kadar poster'ı göster ki ekran boş/siyah kalmasın.
        if (!posterUrl.isNullOrBlank() && !isReady) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
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
    }
}

@Composable
fun DiaryStoryViewerScreen(
    userId: String,
    onBack: () -> Unit,
    onGoalClick: ((String) -> Unit)? = null
) {
    val factory = remember(userId) { DiaryStoryViewerViewModel.Factory(userId) }
    val viewModel: DiaryStoryViewerViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is DiaryStoryViewerUiState.Closed) {
            onBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val s = state) {
            is DiaryStoryViewerUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold)
                }
            }
            is DiaryStoryViewerUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
                    ) {
                        Text(stringResource(R.string.common_go_back), color = Void950)
                    }
                }
            }
            is DiaryStoryViewerUiState.Content -> {
                val currentEntry = s.currentEntry

                if (currentEntry != null) {
                    // Story Content View
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(s.currentIndex) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.pauseTimer()
                                        tryAwaitRelease()
                                        viewModel.resumeTimer()
                                    },
                                    onTap = { offset ->
                                        if (offset.x < size.width * 0.35f) {
                                            viewModel.previousStory()
                                        } else {
                                            viewModel.nextStory()
                                        }
                                    }
                                )
                            }
                    ) {
                        // Media or Text
                        if (currentEntry.mediaType == "photo" && !currentEntry.mediaUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = currentEntry.mediaUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentEntry.mediaType == "video" && !currentEntry.mediaUrl.isNullOrBlank()) {
                            // ÖNCEDEN: burada sadece poster'ın durağan bir görüntüsü
                            // gösteriliyor, video hiç oynatılmıyordu — sabit 4sn'lik
                            // zamanlayıcı de zaten dönüyordu, bu yüzden video story
                            // hep "yükleniyor gibi takılı kalıp" bir sonrakine geçiyordu.
                            // Artık gerçek bir ExoPlayer ile oynatılıyor ve ilerleme
                            // çubuğu videonun gerçek konumundan sürülüyor.
                            DiaryStoryVideoPlayer(
                                url = currentEntry.mediaUrl,
                                posterUrl = currentEntry.posterUrl,
                                isPaused = s.isPaused,
                                onProgress = { viewModel.setVideoProgress(it) },
                                onEnded = { viewModel.nextStory() }
                            )
                        } else {
                            // Text Entry
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Void950,
                                                Void900,
                                                AetherViolet.copy(alpha = 0.3f),
                                                Void950
                                            )
                                        )
                                    )
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = currentEntry.caption ?: "",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = SerifFontFamily),
                                    lineHeight = 32.sp
                                )
                            }
                        }

                        // Caption Overlay for Photo/Video
                        if (currentEntry.mediaType != "text" && !currentEntry.caption.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                        )
                                    )
                                    .padding(start = 20.dp, end = 20.dp, bottom = 40.dp, top = 40.dp)
                            ) {
                                Text(
                                    text = currentEntry.caption,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Bug #13: beğeni + yorum aksiyon sütunu (Instagram
                    // hikaye görüntüleyicisindeki gibi sağ altta).
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 100.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { viewModel.toggleLike() }
                        ) {
                            Icon(
                                imageVector = if (currentEntry.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.diary_like_cd),
                                tint = if (currentEntry.isLiked) ShadowWorkRose else Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                            if (currentEntry.likesCount > 0) {
                                Text(
                                    text = "${currentEntry.likesCount}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { viewModel.openComments() }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = stringResource(R.string.diary_comment_cd),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            if (currentEntry.commentsCount > 0) {
                                Text(
                                    text = "${currentEntry.commentsCount}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Top Bar Controls Overlay
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        // Progress Bars Segmented
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            s.entries.forEachIndexed { index, _ ->
                                val progressVal = when {
                                    index < s.currentIndex -> 1f
                                    index == s.currentIndex -> s.progress
                                    else -> 0f
                                }
                                LinearProgressIndicator(
                                    progress = { progressVal },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = Color.White,
                                    trackColor = Color.White.copy(alpha = 0.3f)
                                )
                            }
                        }

                        // Header Info Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (!s.owner?.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = s.owner?.avatarUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .border(1.dp, AstralGold, CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AstralGold.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s.owner?.nameOrFallback?.take(1)?.uppercase() ?: "K",
                                            color = AstralGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = s.owner?.nameOrFallback ?: stringResource(R.string.common_user_fallback),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    if (!currentEntry.goalTitle.isNullOrBlank()) {
                                        Surface(
                                            onClick = {
                                                currentEntry.goalId?.let { gid -> onGoalClick?.invoke(gid) }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = AstralGold.copy(alpha = 0.25f),
                                            border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.TrackChanges,
                                                    contentDescription = null,
                                                    tint = AstralGold,
                                                    modifier = Modifier.size(11.dp)
                                                )
                                                Text(
                                                    text = currentEntry.goalTitle,
                                                    color = AstralGold,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Actions: Delete (if self) & Close
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (s.isSelf) {
                                    IconButton(
                                        onClick = { viewModel.deleteCurrentEntry() }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.common_delete_action),
                                            tint = SemanticDanger400
                                        )
                                    }
                                }

                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.common_close_action),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                if (s.commentsSheetVisible) {
                    DiaryCommentsDialog(
                        comments = s.comments,
                        isLoading = s.isLoadingComments,
                        isPosting = s.isPostingComment,
                        onDismiss = { viewModel.closeComments() },
                        onSend = { viewModel.addComment(it) }
                    )
                }
            }
            else -> {}
        }
    }
}

@Composable
private fun DiaryCommentsDialog(
    comments: List<DiaryComment>,
    isLoading: Boolean,
    isPosting: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Void900,
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.diary_comments_title, comments.size),
                    color = AstralGold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f, fill = false)) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = AstralGold,
                            modifier = Modifier.size(24.dp).align(Alignment.Center)
                        )
                    } else if (comments.isEmpty()) {
                        Text(
                            text = stringResource(R.string.diary_no_comments),
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    } else {
                        LazyColumnComments(comments)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commentText,
                        onValueChange = { if (it.length <= 1000) commentText = it },
                        placeholder = { Text(stringResource(R.string.diary_comment_placeholder), color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (commentText.isNotBlank()) {
                                onSend(commentText)
                                commentText = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstralGold,
                            unfocusedBorderColor = Void800,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    IconButton(
                        onClick = {
                            if (commentText.isNotBlank()) {
                                onSend(commentText)
                                commentText = ""
                            }
                        },
                        enabled = !isPosting && commentText.isNotBlank(),
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (commentText.isNotBlank()) AstralGold else Void800)
                    ) {
                        if (isPosting) {
                            CircularProgressIndicator(color = Void950, modifier = Modifier.size(18.dp))
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Void950,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LazyColumnComments(comments: List<DiaryComment>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(comments, key = { it.id }) { comment ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Void800),
                    contentAlignment = Alignment.Center
                ) {
                    if (!comment.userProfile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = comment.userProfile?.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(
                            (comment.userProfile?.nameOrFallback ?: "?").take(1).uppercase(),
                            color = AstralGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        comment.userProfile?.nameOrFallback ?: "?",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        comment.content,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
