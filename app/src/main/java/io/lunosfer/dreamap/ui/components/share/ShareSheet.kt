package io.lunosfer.dreamap.ui.components.share

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.ShareRequestRef
import io.lunosfer.dreamap.data.repository.FriendsRepository
import io.lunosfer.dreamap.data.repository.GameRepository
import io.lunosfer.dreamap.data.repository.MessagesRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.MoonSilver
import io.lunosfer.dreamap.ui.theme.SemanticDanger400
import io.lunosfer.dreamap.ui.theme.Void800
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.theme.Void950
import io.lunosfer.dreamap.util.GuestMode
import io.lunosfer.dreamap.util.GuestPrompt
import io.lunosfer.dreamap.util.SHARE_BASE_URL
import io.lunosfer.dreamap.util.ShareContent
import io.lunosfer.dreamap.util.ShareController
import io.lunosfer.dreamap.util.ShareKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/** MainScreen'de bir kez çizilir; herhangi bir ekran ShareController.open(...) çağırır. */
@Composable
fun ShareSheetHost() {
    val content by ShareController.request.collectAsState()
    content?.let { ShareSheet(it, onDismiss = { ShareController.close() }) }
}

private data class Recipient(val id: String, val name: String, val avatarUrl: String?)

private enum class SendState { SENDING, SENT, FAILED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(content: ShareContent, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isGuest = remember { GuestMode.isGuest() }
    val myId = remember { supabaseClient.auth.currentUserOrNull()?.id }
    val storyLabel = stringResource(R.string.share_instagram_story)

    var card by remember(content) { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    var platforms by remember { mutableStateOf<List<InstalledPlatform>>(emptyList()) }
    var recipients by remember { mutableStateOf<List<Recipient>?>(null) }
    var note by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val sendStates = remember { mutableStateMapOf<String, SendState>() }

    LaunchedEffect(content) {
        card = runCatching { ShareImages.renderContent(context, content) }.getOrNull()
    }
    LaunchedEffect(Unit) {
        platforms = withContext(Dispatchers.Default) { SharePlatforms.installed(context, storyLabel) }
    }
    LaunchedEffect(myId, isGuest) {
        recipients = if (myId != null && !isGuest) loadRecipients(myId) else emptyList()
    }

    // Kart hazır değilse (ör. görsel hâlâ iniyor) bekleyip üretir.
    suspend fun ensureCard(): Bitmap? = card ?: runCatching { ShareImages.renderContent(context, content) }
        .getOrNull()?.also { card = it }

    fun toast(res: Int) = Toast.makeText(context, res, Toast.LENGTH_SHORT).show()

    fun shareTo(action: suspend (android.net.Uri) -> Boolean, channel: String) {
        if (busy) return
        busy = true
        scope.launch {
            val bitmap = ensureCard()
            val ok = bitmap != null && runCatching {
                action(ShareImages.writeShareFile(context, bitmap, "lunosfer_${content.kind.path}"))
            }.getOrDefault(false)
            busy = false
            if (ok) {
                GameRepository.recordShare(content.kind.path, content.id, channel)
                onDismiss()
            } else {
                toast(R.string.share_failed)
            }
        }
    }

    fun saveImage() {
        if (busy) return
        busy = true
        scope.launch {
            val bitmap = ensureCard()
            val ok = bitmap != null && ShareImages.saveToGallery(context, bitmap, "lunosfer_${content.kind.path}")
            busy = false
            toast(if (ok) R.string.share_image_saved else R.string.share_failed)
        }
    }

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) saveImage() else toast(R.string.share_failed)
    }

    fun send(recipient: Recipient) {
        val current = sendStates[recipient.id]
        if (current == SendState.SENDING || current == SendState.SENT) return
        sendStates[recipient.id] = SendState.SENDING
        scope.launch {
            MessagesRepository()
                .sendShare(recipient.id, ShareRequestRef(content.kind.path, content.id), note)
                .onSuccess { sendStates[recipient.id] = SendState.SENT }
                .onFailure { err ->
                    sendStates[recipient.id] = SendState.FAILED
                    toast(if (apiErrorCode(err) == "not_shareable") R.string.share_not_allowed else R.string.share_failed)
                }
        }
    }

    val text = shareText(context, content)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Void900
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SharePreview(content, card)

            // --- Lunosfer içinde gönder -------------------------------------
            SectionTitle(stringResource(R.string.share_send_in_app))
            when {
                isGuest || myId == null -> OutlinedButton(
                    onClick = { GuestPrompt.show() },
                    border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f))
                ) {
                    Text(stringResource(R.string.guest_sheet_cta), color = AstralGold)
                }
                recipients == null -> Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                recipients!!.isEmpty() -> Text(stringResource(R.string.share_no_people), color = MoonSilver, fontSize = 13.sp)
                else -> {
                    ShareTextField(note, { note = it.take(500) }, stringResource(R.string.share_note_hint), null)
                    val all = recipients!!
                    if (all.size > 6) {
                        ShareTextField(query, { query = it }, stringResource(R.string.share_search_people), Icons.Filled.Search)
                    }
                    val visible = if (query.isBlank()) all else all.filter { it.name.contains(query.trim(), ignoreCase = true) }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(visible, key = { it.id }) { r ->
                            RecipientItem(r, sendStates[r.id]) { send(r) }
                        }
                    }
                }
            }

            // --- Dış uygulamalar ----------------------------------------------
            SectionTitle(stringResource(R.string.share_to_apps))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(platforms, key = { it.platform.channel }) { target ->
                    PlatformItem(label = target.platform.label, icon = target.icon, vector = null) {
                        shareTo({ uri -> SharePlatforms.launch(context, target, uri, text) }, target.platform.channel)
                    }
                }
                item {
                    PlatformItem(stringResource(R.string.share_save_image), null, Icons.Filled.Download) {
                        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        if (needsPermission) storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else saveImage()
                    }
                }
                if (content.isPublic) {
                    item {
                        PlatformItem(stringResource(R.string.share_copy_link), null, Icons.Filled.Link) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Lunosfer", content.webUrl))
                            toast(R.string.share_link_copied)
                        }
                    }
                }
                item {
                    val chooserTitle = stringResource(R.string.share_sheet_title)
                    PlatformItem(stringResource(R.string.share_more), null, Icons.Filled.MoreHoriz) {
                        shareTo({ uri -> SharePlatforms.launchChooser(context, uri, text, chooserTitle) }, "other")
                    }
                }
            }

            if (busy) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    LinearProgressIndicator(color = AstralGold, trackColor = Void800, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.share_preparing), color = MoonSilver, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun SharePreview(content: ShareContent, card: Bitmap?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(92.dp)
                .height(164.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Void950)
                .border(1.dp, AstralGold.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (card != null) {
                Image(card.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            } else {
                CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    when (content.kind) {
                        ShareKind.DREAM -> R.string.share_card_dream
                        ShareKind.DIARY -> R.string.share_card_diary
                        ShareKind.VISION -> R.string.share_card_vision
                    }
                ).uppercase(),
                color = AstralGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            if (content.title.isNotBlank()) {
                Text(content.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            content.ownerName?.let { Text(it, color = MoonSilver, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (!content.isPublic) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MoonSilver, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.share_private_note), color = MoonSilver, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ShareTextField(value: String, onChange: (String) -> Unit, placeholder: String, icon: ImageVector?) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        placeholder = { Text(placeholder, color = MoonSilver, fontSize = 13.sp) },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = MoonSilver) }
        } else null,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = AstralGold.copy(alpha = 0.6f),
            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
            cursorColor = AstralGold
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RecipientItem(recipient: Recipient, state: SendState?, onSend: () -> Unit) {
    Column(Modifier.width(76.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(Void800),
            contentAlignment = Alignment.Center
        ) {
            if (!recipient.avatarUrl.isNullOrBlank()) {
                AsyncImage(recipient.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp))
            } else {
                Text(recipient.name.take(1).uppercase(), color = AstralGold, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(recipient.name, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        val (label, bg, fg) = when (state) {
            SendState.SENT -> Triple(stringResource(R.string.share_sent), Void800, MoonSilver)
            SendState.FAILED -> Triple(stringResource(R.string.retry), SemanticDanger400.copy(alpha = 0.2f), SemanticDanger400)
            else -> Triple(stringResource(R.string.share_send), AstralGold, Void950)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(bg)
                .clickable(enabled = state != SendState.SENDING && state != SendState.SENT, onClick = onSend)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                SendState.SENDING -> CircularProgressIndicator(color = Void950, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                SendState.SENT -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = fg, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                else -> Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PlatformItem(label: String, icon: ImageBitmap?, vector: ImageVector?, onClick: () -> Unit) {
    Column(
        Modifier.width(68.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Void800),
            contentAlignment = Alignment.Center
        ) {
            when {
                icon != null -> Image(icon, contentDescription = null, modifier = Modifier.size(52.dp))
                vector != null -> Icon(vector, contentDescription = null, tint = AstralGold, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

/** Konuşma geçmişi olanlar önce, sonra arkadaşlar (tekrar etmeden). */
private suspend fun loadRecipients(myId: String): List<Recipient> = coroutineScope {
    val conversations = async { MessagesRepository().loadConversations().getOrDefault(emptyList()) }
    val friends = async { FriendsRepository().getFriendsList(myId, "accepted").getOrDefault(emptyList()) }
    val byId = LinkedHashMap<String, Recipient>()
    conversations.await().forEach { c ->
        val u = c.otherUser
        byId.putIfAbsent(u.id, Recipient(u.id, u.nameOrFallback, u.avatarUrl))
    }
    friends.await().forEach { f ->
        val other = if (f.userId == myId) f.target else f.requester
        if (other != null) byId.putIfAbsent(other.id, Recipient(other.id, other.nameOrFallback, other.avatarUrl))
    }
    byId.remove(myId)
    byId.values.toList()
}

/** Dış platforma giden metin: herkese açıksa önizlemeli bağlantı, değilse yalnızca site adı. */
fun shareText(context: Context, content: ShareContent): String {
    val label = context.getString(
        when (content.kind) {
            ShareKind.DREAM -> R.string.share_text_dream
            ShareKind.DIARY -> R.string.share_text_diary
            ShareKind.VISION -> R.string.share_text_vision
        }
    )
    val title = content.title.takeIf { it.isNotBlank() }?.let { "\n“$it”" }.orEmpty()
    val link = if (content.isPublic) content.webUrl else SHARE_BASE_URL.removePrefix("https://")
    return "$label$title\n$link"
}

private fun apiErrorCode(err: Throwable): String? =
    (err as? HttpException)?.response()?.errorBody()?.string()?.let {
        Regex("\"error\"\\s*:\\s*\"([a-z_]+)\"").find(it)?.groupValues?.get(1)
    }
