package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.GoalReportReason
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.viewmodel.DreamDetailUiState
import io.lunosfer.dreamap.ui.viewmodel.DreamDetailViewModel
import io.lunosfer.dreamap.ui.viewmodel.DreamReelsQueueHolder

@Composable
fun DreamReelsScreen(
    onClose: () -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    val dreams = remember { DreamReelsQueueHolder.dreams }
    val startIndex = remember { DreamReelsQueueHolder.startIndex }

    if (dreams.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val pagerState = rememberPagerState(initialPage = startIndex) { dreams.size }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            DreamReelPage(
                dream = dreams[page],
                onBack = onClose,
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
private fun DreamReelPage(
    dream: Dream,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val viewModel: DreamDetailViewModel = viewModel(key = "dream_reel_${dream.id}")
    val state by viewModel.state.collectAsState()
    
    val currentUserId = remember { io.lunosfer.dreamap.supabase.supabaseClient.auth.currentUserOrNull()?.id }

    LaunchedEffect(dream.id) {
        viewModel.loadDream(dream.id)
    }

    Box(modifier = Modifier.fillMaxSize().background(io.lunosfer.dreamap.ui.theme.Void950)) {
        when (val s = state) {
            is DreamDetailUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
            }
            is DreamDetailUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = s.message, color = io.lunosfer.dreamap.ui.theme.SemanticDanger400)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadDream(dream.id) }, colors = ButtonDefaults.buttonColors(containerColor = AstralGold)) {
                        Text(stringResource(R.string.retry), color = io.lunosfer.dreamap.ui.theme.Void950)
                    }
                }
            }
            is DreamDetailUiState.Success -> {
                DreamDetailContent(
                    state = s,
                    currentUserId = currentUserId,
                    onBack = onBack,
                    showBackButton = false,
                    onUserClick = onUserClick,
                    onRefresh = { viewModel.loadDream(dream.id) },
                    onAnalyze = { viewModel.analyzeDream(dream.id, s.dream.content, s.dream.originalLanguage ?: "en") },
                    onRequestDeepAnalysis = { viewModel.requestDeepAnalysis(dream.id) },
                    onToggleLike = { viewModel.toggleLike(dream.id, currentUserId) },
                    onAddComment = { text -> viewModel.addComment(dream.id, currentUserId, text) },
                    onDeleteComment = { commentId ->
                        if (currentUserId != null) viewModel.deleteComment(commentId, currentUserId)
                    },
                    onUpdateDream = { request ->
                        viewModel.updateDream(request) { viewModel.loadDream(dream.id) }
                    },
                    onDeleteDream = { softDelete ->
                        if (currentUserId != null) {
                            viewModel.deleteDream(dream.id, currentUserId, softDelete, onSuccess = onBack)
                        }
                    },
                    onBoostDream = { viewModel.boostDream(dream.id) },
                    onAddBounty = { amount -> viewModel.addBounty(dream.id, amount) },
                    onOpenReportSheet = { viewModel.openReportSheet() },
                    onCloseReportSheet = { viewModel.closeReportSheet() },
                    onSubmitReport = { reason, note -> viewModel.submitReport(dream.id, reason, note) }
                )
            }
        }
    }
}
