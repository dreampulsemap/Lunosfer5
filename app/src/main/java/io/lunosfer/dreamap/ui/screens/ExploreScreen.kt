package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.ui.components.VisionGridCard
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.ExploreTab
import io.lunosfer.dreamap.ui.viewmodel.ExploreViewModel
import io.lunosfer.dreamap.ui.viewmodel.UiState

@Composable
fun ExploreScreen(
    onOpenReels: (List<Goal>, Int) -> Unit = { _, _ -> },
    onOpenDreamReels: (List<Dream>, Int) -> Unit = { _, _ -> },
    viewModel: ExploreViewModel = viewModel()
) {
    val activeTab by viewModel.activeTab.collectAsState()

    val dreamsState by viewModel.state.collectAsState()
    val loadingMore by viewModel.loadingMore.collectAsState()
    val visionState by viewModel.visionState.collectAsState()
    val victoryState by viewModel.victoryState.collectAsState()
    val phoenixState by viewModel.phoenixState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Void950)) {
        ScrollableTabRow(
            selectedTabIndex = activeTab.ordinal,
            containerColor = Void950,
            contentColor = AstralGold,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (activeTab.ordinal < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab.ordinal]),
                        color = AstralGold
                    )
                }
            },
            divider = {
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            }
        ) {
            val tabs = listOf(
                Triple(ExploreTab.DREAMSCAPE, stringResource(R.string.explore_tab_dreams), Icons.Filled.NightsStay),
                Triple(ExploreTab.VISION, stringResource(R.string.explore_tab_vision_board), Icons.Filled.TrackChanges),
                Triple(ExploreTab.VICTORY, stringResource(R.string.explore_tab_victory_wall), Icons.Filled.EmojiEvents),
                Triple(ExploreTab.PHOENIX, stringResource(R.string.explore_tab_phoenix_wall), Icons.Filled.AutoAwesome)
            )

            tabs.forEach { (tab, title, icon) ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(
                            text = title,
                            fontFamily = SerifFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    selectedContentColor = AstralGold,
                    unselectedContentColor = Color(0xFF94A3B8)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            when (activeTab) {
                ExploreTab.DREAMSCAPE -> {
                    when (val current = dreamsState) {
                        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AetherCyan)
                        }
                        is UiState.Error -> ExploreError(message = current.message, onRetry = { viewModel.retry(ExploreTab.DREAMSCAPE) })
                        is UiState.Success -> ExploreGrid(
                            dreams = current.data,
                            onDreamClick = onOpenDreamReels,
                            isLoadingMore = ExploreTab.DREAMSCAPE in loadingMore,
                            onLoadMore = { viewModel.loadMore(ExploreTab.DREAMSCAPE) }
                        )
                    }
                }
                ExploreTab.VISION -> {
                    when (val current = visionState) {
                        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AstralGold)
                        }
                        is UiState.Error -> ExploreError(message = current.message, onRetry = { viewModel.retry(ExploreTab.VISION) })
                        is UiState.Success -> GoalsGrid(
                            goals = current.data,
                            emptyMessage = stringResource(R.string.empty_vision),
                            onOpenReels = onOpenReels,
                            isLoadingMore = ExploreTab.VISION in loadingMore,
                            onLoadMore = { viewModel.loadMore(ExploreTab.VISION) }
                        )
                    }
                }
                ExploreTab.VICTORY -> {
                    when (val current = victoryState) {
                        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AstralGold)
                        }
                        is UiState.Error -> ExploreError(message = current.message, onRetry = { viewModel.retry(ExploreTab.VICTORY) })
                        is UiState.Success -> GoalsGrid(
                            goals = current.data,
                            emptyMessage = stringResource(R.string.empty_victory),
                            onOpenReels = onOpenReels,
                            isLoadingMore = ExploreTab.VICTORY in loadingMore,
                            onLoadMore = { viewModel.loadMore(ExploreTab.VICTORY) }
                        )
                    }
                }
                ExploreTab.PHOENIX -> {
                    when (val current = phoenixState) {
                        is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AstralGold)
                        }
                        is UiState.Error -> ExploreError(message = current.message, onRetry = { viewModel.retry(ExploreTab.PHOENIX) })
                        is UiState.Success -> GoalsGrid(
                            goals = current.data,
                            emptyMessage = stringResource(R.string.empty_phoenix),
                            onOpenReels = onOpenReels,
                            isLoadingMore = ExploreTab.PHOENIX in loadingMore,
                            onLoadMore = { viewModel.loadMore(ExploreTab.PHOENIX) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.explore_failed), color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily))
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onRetry,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AetherCyan),
            border = BorderStroke(1.dp, AetherCyan.copy(alpha = 0.4f))
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.explore_retry))
        }
    }
}

@Composable
private fun ExploreGrid(
    dreams: List<Dream>,
    onDreamClick: (List<Dream>, Int) -> Unit,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    if (dreams.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.explore_empty_state),
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, dreams.size) {
        snapshotFlow {
            (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= dreams.size - 6
        }.collect { nearEnd -> if (nearEnd) onLoadMore() }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        // Alt gezinme cubugu ve "+" butonu son satiri kapatiyordu.
        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(dreams, key = { it.id }) { dream ->
            val index = dreams.indexOf(dream)
            ExploreTile(dream, onClick = { onDreamClick(dreams, index) })
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AetherCyan, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}

@Composable
private fun ExploreTile(dream: Dream, onClick: () -> Unit) {
    // Görsel URL'si var ama yüklenemiyorsa (ör. henüz üretilmemiş/404) AsyncImage
    // sessizce boş kalıyordu — bu durumda da placeholder ikonuna geri dön.
    var imageFailed by remember(dream.aiImageUrl) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .background(Void800)
            .clickable(onClick = onClick)
    ) {
        if (dream.aiImageUrl != null && !imageFailed) {
            AsyncImage(
                model = dream.aiImageUrl,
                contentDescription = dream.displayTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { imageFailed = true }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Image, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun GoalsGrid(
    goals: List<Goal>,
    emptyMessage: String,
    onOpenReels: (List<Goal>, Int) -> Unit,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    if (goals.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = emptyMessage,
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, goals.size) {
        snapshotFlow {
            (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= goals.size - 4
        }.collect { nearEnd -> if (nearEnd) onLoadMore() }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(goals, key = { it.id }) { goal ->
            VisionGridCard(
                goal = goal,
                onClick = {
                    val index = goals.indexOfFirst { it.id == goal.id }.coerceAtLeast(0)
                    onOpenReels(goals, index)
                }
            )
        }
        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}
