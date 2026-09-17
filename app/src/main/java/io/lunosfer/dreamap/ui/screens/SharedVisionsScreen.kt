package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import io.lunosfer.dreamap.data.model.GoalCollaborator
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.SharedVisionsUiState
import io.lunosfer.dreamap.ui.viewmodel.SharedVisionsViewModel

/** "Ortak Vizyonlarım" — bir arkadaşımın vizyonuna işbirlikçi olarak davet
 * edildiğim veya katıldığım vizyonlar. Kendi "Vizyonlarım" feed'inde
 * görünmezler çünkü sahibi ben değilim (bkz. goal_collaborators). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedVisionsScreen(
    onBack: () -> Unit,
    onGoalClick: (String) -> Unit,
    viewModel: SharedVisionsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.shared_visions_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { padding ->
        when (val s = state) {
            is SharedVisionsUiState.Loading -> {
                Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold)
                }
            }
            is SharedVisionsUiState.Content -> {
                if (s.invites.isEmpty() && s.accepted.isEmpty()) {
                    Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.shared_visions_empty), color = Color.Gray, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (s.invites.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.shared_visions_pending_section),
                                    color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            items(s.invites, key = { "invite_${it.id}" }) { invite ->
                                SharedVisionInviteRow(
                                    invite = invite,
                                    onAccept = { viewModel.respond(invite.id, true) },
                                    onDecline = { viewModel.respond(invite.id, false) },
                                    onClick = { onGoalClick(invite.goalId) }
                                )
                            }
                        }
                        if (s.accepted.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.shared_visions_joined_section),
                                    color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold
                                )
                            }
                            items(s.accepted, key = { "accepted_${it.id}" }) { collaboration ->
                                SharedVisionRow(collaboration = collaboration, onClick = { onGoalClick(collaboration.goalId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedVisionInviteRow(
    invite: GoalCollaborator,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, AetherViolet)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SharedVisionHeader(goalTitle = invite.goals?.title, coverImageUrl = invite.goals?.coverImageUrl)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAccept, colors = ButtonDefaults.buttonColors(containerColor = AstralGold)) {
                    Text(stringResource(R.string.vision_collaborate_accept), color = Void950, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(stringResource(R.string.vision_collaborate_decline))
                }
            }
        }
    }
}

@Composable
private fun SharedVisionRow(collaboration: GoalCollaborator, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Void900),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            SharedVisionHeader(goalTitle = collaboration.goals?.title, coverImageUrl = collaboration.goals?.coverImageUrl)
        }
    }
}

@Composable
private fun SharedVisionHeader(goalTitle: String?, coverImageUrl: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!coverImageUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(Void800),
                contentAlignment = Alignment.Center
            ) {
                Text("✨", fontSize = 18.sp)
            }
        }
        Text(
            text = goalTitle ?: "—",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}
