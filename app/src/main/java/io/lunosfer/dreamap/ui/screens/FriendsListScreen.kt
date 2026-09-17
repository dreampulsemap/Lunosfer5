package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.Friendship
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.FriendsListUiState
import io.lunosfer.dreamap.ui.viewmodel.FriendsListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsListScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    viewModel: FriendsListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.friends_list_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
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
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = state) {
                is FriendsListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
                }
                is FriendsListUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = SemanticDanger400)
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = { viewModel.load() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                is FriendsListUiState.Content -> {
                    if (s.friendships.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.People, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.friends_list_empty), color = Color.Gray, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(s.friendships, key = { it.id }) { friendship ->
                                val profile = viewModel.otherPartyOf(friendship)
                                val otherUserId = viewModel.otherUserIdOf(friendship)
                                FriendRow(
                                    name = profile?.nameOrFallback ?: "?",
                                    username = profile?.username,
                                    avatarUrl = profile?.avatarUrl,
                                    onClick = { otherUserId?.let(onUserClick) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRow(
    name: String,
    username: String?,
    avatarUrl: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Void800)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape).background(AstralGold.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = name.take(1).uppercase(), color = AstralGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column {
                Text(text = name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (!username.isNullOrBlank()) {
                    Text(text = "@$username", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}
