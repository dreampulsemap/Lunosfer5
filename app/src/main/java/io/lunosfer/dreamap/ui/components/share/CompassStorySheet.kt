package io.lunosfer.dreamap.ui.components.share

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.CreateDiaryInput
import io.lunosfer.dreamap.data.repository.DiaryRepository
import io.lunosfer.dreamap.data.repository.GameRepository
import io.lunosfer.dreamap.data.repository.ProfileRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.MoonSilver
import io.lunosfer.dreamap.ui.theme.Void800
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.theme.Void950
import io.lunosfer.dreamap.util.GuestMode
import io.lunosfer.dreamap.util.GuestPrompt
import io.lunosfer.dreamap.util.VisibilityPolicy
import kotlinx.coroutines.launch

/**
 * Günün pusulası paylaşımı: hikâye görseli üretilir; birincil eylem kullanıcının
 * kendi Lunosfer güncesine eklemektir. İsterse aynı görseli lunosfer.com
 * filigranıyla indirip Instagram hikâyesinde (ya da başka bir uygulamada)
 * paylaşabilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassStorySheet(archetype: String?, reading: String, accentHex: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isGuest = remember { GuestMode.isGuest() }
    val storyLabel = stringResource(R.string.share_instagram_story)

    var plain by remember { mutableStateOf<Bitmap?>(null) }
    var marked by remember { mutableStateOf<Bitmap?>(null) }
    var profileVisibility by remember { mutableStateOf<String?>(null) }
    var visibility by remember { mutableStateOf(VisibilityPolicy.FRIENDS) }
    var visibilityUserSet by remember { mutableStateOf(false) }
    var posting by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val instagram = remember { SharePlatforms.storyTarget(context, storyLabel) }

    LaunchedEffect(Unit) {
        plain = ShareImages.renderCompass(context, archetype, reading, accentHex, watermark = false)
        marked = ShareImages.renderCompass(context, archetype, reading, accentHex, watermark = true)
    }
    LaunchedEffect(Unit) {
        val uid = supabaseClient.auth.currentUserOrNull()?.id ?: return@LaunchedEffect
        if (isGuest) return@LaunchedEffect
        ProfileRepository().getUserProfile(uid).onSuccess { profile ->
            profileVisibility = profile.profileVisibility
            if (!visibilityUserSet) visibility = VisibilityPolicy.defaultFor(profile.profileVisibility)
        }
    }

    fun toast(res: Int) = Toast.makeText(context, res, Toast.LENGTH_SHORT).show()

    fun postToDiary() {
        if (isGuest) {
            GuestPrompt.show()
            return
        }
        val image = plain ?: return
        if (posting) return
        posting = true
        scope.launch {
            val repo = DiaryRepository()
            val caption = if (archetype.isNullOrBlank()) {
                "🧭 " + context.getString(R.string.compass_card_title)
            } else {
                context.getString(R.string.compass_story_caption, archetype)
            }
            val result = repo.uploadMediaToStorage(ShareImages.toJpegBytes(image), "compass_${System.currentTimeMillis()}.jpg")
                .mapCatching { url ->
                    repo.createEntry(
                        CreateDiaryInput(
                            mediaType = "photo",
                            mediaUrl = url,
                            caption = caption,
                            visibility = VisibilityPolicy.clamp(visibility, profileVisibility)
                        )
                    ).getOrThrow()
                }
            posting = false
            if (result.isSuccess) {
                toast(R.string.compass_story_posted)
                onDismiss()
            } else {
                toast(R.string.share_failed)
            }
        }
    }

    fun withMarked(block: suspend (Bitmap) -> Boolean, successToast: Int?, channel: String?) {
        val image = marked ?: return
        if (busy) return
        busy = true
        scope.launch {
            val ok = runCatching { block(image) }.getOrDefault(false)
            busy = false
            when {
                !ok -> toast(R.string.share_failed)
                successToast != null -> toast(successToast)
            }
            if (ok && channel != null) GameRepository.recordShare("compass", "today", channel)
        }
    }

    fun download() = withMarked({ ShareImages.saveToGallery(context, it, "lunosfer_compass") }, R.string.share_image_saved, null)

    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) download() else toast(R.string.share_failed)
    }

    val shareBody = context.getString(R.string.compass_share_text, archetype.orEmpty(), reading)
    val chooserTitle = stringResource(R.string.compass_share_diary)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Void900) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.compass_story_title), color = AstralGold, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

            Box(
                Modifier
                    .width(180.dp)
                    .height(320.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Void950)
                    .border(1.dp, AstralGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                val preview = plain
                if (preview != null) {
                    Image(preview.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                } else {
                    CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }

            if (!isGuest) {
                val options = VisibilityPolicy.allowedOptions(profileVisibility ?: VisibilityPolicy.PUBLIC)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.reversed().forEach { option ->
                        val selected = option == visibility
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) AstralGold else Void800)
                                .clickable {
                                    visibility = option
                                    visibilityUserSet = true
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                stringResource(
                                    when (option) {
                                        VisibilityPolicy.PUBLIC -> R.string.visibility_public
                                        VisibilityPolicy.FRIENDS -> R.string.visibility_friends
                                        else -> R.string.visibility_private
                                    }
                                ),
                                color = if (selected) Void950 else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { postToDiary() },
                enabled = plain != null && !posting,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold, disabledContainerColor = Void800)
            ) {
                if (posting) {
                    CircularProgressIndicator(color = Void950, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Void950, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.compass_story_post_diary), color = Void950, fontWeight = FontWeight.Bold)
                }
            }

            Text(stringResource(R.string.compass_story_external_hint), color = MoonSilver, fontSize = 12.sp, textAlign = TextAlign.Center)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CompactAction(stringResource(R.string.compass_story_download), null, Icons.Filled.Download) {
                    val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    if (needsPermission) storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else download()
                }
                if (instagram != null) {
                    CompactAction(storyLabel, instagram.icon, null) {
                        withMarked({ bmp ->
                            SharePlatforms.launch(context, instagram, ShareImages.writeShareFile(context, bmp, "lunosfer_compass"), null)
                        }, null, "instagram_story")
                    }
                }
                CompactAction(stringResource(R.string.share_more), null, Icons.Filled.MoreHoriz) {
                    withMarked({ bmp ->
                        SharePlatforms.launchChooser(context, ShareImages.writeShareFile(context, bmp, "lunosfer_compass"), shareBody, chooserTitle)
                    }, null, "other")
                }
            }

            if (busy) CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun CompactAction(label: String, icon: ImageBitmap?, vector: ImageVector?, onClick: () -> Unit) {
    Column(
        Modifier.width(88.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(50)).background(Void800), contentAlignment = Alignment.Center) {
            when {
                icon != null -> Image(icon, contentDescription = null, modifier = Modifier.size(52.dp))
                vector != null -> Icon(vector, contentDescription = null, tint = AstralGold, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 13.sp)
    }
}
