package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import io.lunosfer.dreamap.R
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.AppNotification
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.NotificationsUiState
import io.lunosfer.dreamap.ui.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onDreamClick: (Long) -> Unit,
    onUserClick: (String) -> Unit,
    onGoalClick: (String) -> Unit = {},
    onDiaryClick: (String) -> Unit = {},
    viewModel: NotificationsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        val s = state as? NotificationsUiState.Success
        if (s?.actionError != null) {
            Toast.makeText(context, s.actionError, Toast.LENGTH_SHORT).show()
            viewModel.clearActionError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_notifications),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd), tint = Color.White)
                    }
                },
                actions = {
                    val s = state as? NotificationsUiState.Success
                    if (s != null && s.unreadCount > 0) {
                        TextButton(onClick = { viewModel.markAsRead(null) }) {
                            Text(stringResource(R.string.notifications_mark_all_read), color = AstralGold, fontSize = 12.sp)
                        }
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
                is NotificationsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
                }
                is NotificationsUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = SemanticDanger400)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadNotifications() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold)
                        ) {
                            Text(stringResource(R.string.notifications_refresh))
                        }
                    }
                }
                is NotificationsUiState.Success -> {
                    if (s.notifications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Text(stringResource(R.string.notifications_empty), color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(s.notifications, key = { it.id }) { notification ->
                                NotificationRow(
                                    notification = notification,
                                    canRespond = notification.referenceId != null &&
                                        notification.referenceId in s.actionableFriendshipIds,
                                    onClick = {
                                        if (!notification.isRead) {
                                            viewModel.markAsRead(notification.id)
                                        }
                                        // Vizyon yorumu/mana bildirimleri ilgili vizyona,
                                        // gunluk yorumu gunluge gitmeliydi; onceden hepsi
                                        // sadece profile yonlendiriyordu.
                                        when {
                                            notification.dreamId != null -> onDreamClick(notification.dreamId)
                                            notification.referenceType == "goal" && !notification.referenceId.isNullOrBlank() ->
                                                onGoalClick(notification.referenceId)
                                            notification.referenceType == "diary_entry" ->
                                                supabaseClient.auth.currentUserOrNull()?.id?.let { onDiaryClick(it) }
                                            !notification.actorId.isNullOrBlank() -> onUserClick(notification.actorId)
                                        }
                                    },
                                    onAccept = { viewModel.respondToFriendRequest(notification, "accepted") },
                                    onReject = { viewModel.respondToFriendRequest(notification, "rejected") }
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
private fun NotificationRow(
    notification: AppNotification,
    canRespond: Boolean,
    onClick: () -> Unit,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {}
) {
    val actor = notification.actor
    val isUnread = !notification.isRead

    val (icon, title, body) = when (notification.type) {
        "new_follower" -> Triple(
            Icons.Default.Person,
            stringResource(R.string.notif_new_follower_title),
            stringResource(R.string.notif_new_follower_body).format(actor?.nameOrFallback ?: stringResource(R.string.common_someone_fallback))
        )
        "friend_request" -> Triple(
            Icons.Default.PersonAdd,
            stringResource(R.string.notif_follow_request_title),
            stringResource(R.string.notif_follow_request_body).format(actor?.nameOrFallback ?: stringResource(R.string.common_someone_fallback))
        )
        "analysis_ready" -> Triple(
            Icons.Default.AutoAwesome,
            stringResource(R.string.notif_dream_analysis_ready_title),
            stringResource(R.string.notif_dream_analysis_ready_body)
        )
        "analysis_failed" -> Triple(
            Icons.Default.ErrorOutline,
            stringResource(R.string.notif_dream_analysis_failed_title),
            stringResource(R.string.notif_dream_analysis_failed_body)
        )
        // Asagidaki turlerin hepsi sunucuda uretiliyor ama uygulamada karsiligi
        // yoktu: kullaniciya "Yeni Bildirim / Yeni bir bildiriminiz var" gibi
        // bos bir metin gosteriliyordu (uretimdeki bildirimlerin cogunlugu
        // mana_received ve goal_comment).
        "friend_accepted", "follow_accepted" -> Triple(
            Icons.Default.HowToReg,
            stringResource(R.string.notif_friend_accepted_title),
            stringResource(R.string.notif_friend_accepted_body).format(actor?.nameOrFallback ?: stringResource(R.string.common_someone_fallback))
        )
        "goal_comment" -> Triple(
            Icons.Default.ChatBubbleOutline,
            stringResource(R.string.notif_goal_comment_title),
            stringResource(R.string.notif_goal_comment_body).format(actor?.nameOrFallback ?: stringResource(R.string.common_someone_fallback))
        )
        "mana_received" -> Triple(
            Icons.Default.WaterDrop,
            stringResource(R.string.notif_mana_received_title),
            stringResource(R.string.notif_mana_received_body).format(actor?.nameOrFallback ?: stringResource(R.string.common_someone_fallback))
        )
        "diary_comment" -> Triple(
            Icons.Default.ChatBubbleOutline,
            stringResource(R.string.notif_diary_comment_title),
            stringResource(R.string.notif_diary_comment_body).format(actor?.nameOrFallback ?: stringResource(R.string.common_someone_fallback))
        )
        "dream_image_gift" -> Triple(
            Icons.Default.Image,
            stringResource(R.string.notif_dream_image_gift_title),
            stringResource(R.string.notif_dream_image_gift_body)
        )
        else -> Triple(
            Icons.Default.Notifications,
            stringResource(R.string.notif_generic_title),
            stringResource(R.string.notif_generic_body)
        )
    }

    // Bug #4: bir "friend_request" bildiriminin Kabul/Reddet gösterip
    // göstermeyeceği — istek zaten yanıtlandıysa (referenceId lokal olarak
    // temizlendiyse) veya backend bir friendshipId taşımayan eski bir satırsa
    // butonlar gizlenir. Bu yüzden Card'ın tamamı artık tıklanabilir değil —
    // asıl navigasyon, buton alanıyla çakışmaması için içerik satırına taşındı
    // (bkz. AddFriendScreen'deki PendingRequestRow ile aynı desen).
    val showFriendRequestActions = notification.type == "friend_request" && canRespond

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) Void800.copy(alpha = 0.8f) else Void900
        ),
        border = BorderStroke(
            1.dp,
            if (isUnread) AstralGold.copy(alpha = 0.4f) else Void800
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Actor Avatar or Type Icon
                if (actor?.avatarUrl != null && actor.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = actor.avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnread) AstralGold.copy(alpha = 0.2f) else Void800
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isUnread) AstralGold else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Text content
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (!notification.createdAt.isNullOrBlank()) {
                            Text(
                                text = io.lunosfer.dreamap.util.RelativeTime.format(notification.createdAt),
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = body,
                        color = if (isUnread) Color(0xFFE2E8F0) else Color.Gray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }

                // Unread Dot
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AstralGold)
                    )
                }
            }

            if (showFriendRequestActions) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(stringResource(R.string.friend_accept), color = Void950, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticDanger400),
                        border = BorderStroke(1.dp, SemanticDanger400),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Text(stringResource(R.string.friend_reject), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
