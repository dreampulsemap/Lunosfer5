package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
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
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.GoalComment
import io.lunosfer.dreamap.data.model.GoalReportReason
import io.lunosfer.dreamap.ui.theme.AstralGold
import io.lunosfer.dreamap.ui.theme.SemanticDanger400
import io.lunosfer.dreamap.ui.theme.Void800
import io.lunosfer.dreamap.ui.theme.Void900
import io.lunosfer.dreamap.ui.theme.Void950

/**
 * Tam ekran vizyon görüntüleyicilerinin (SlidesViewerScreen,
 * VisionVideoPlayerScreen, VisionReelsScreen içindeki sayfalar) ortak UI
 * parçaları — "..." menüsü, yorumlar bottom sheet'i, bildirme bottom
 * sheet'i. Her iki görüntüleyici tipi de bu bileşenleri kullanarak BİREBİR
 * aynı davranışı sergiler; sadece kendi ViewModel'lerindeki state ve
 * fonksiyonları buraya bağlarlar.
 *
 * Bildirme akışı pages/api/goals/report.js ile birebir eşleşir: 6 sabit
 * sebep (spam/inappropriate/harassment/misinformation/hate_speech/other) ve
 * serbest metin "note" alanı — bunlar backend'deki check constraint'e sadık
 * kalınarak seçildi (bkz. GoalReportModels.kt).
 */

/** "..." menüsü — sahibi olmayan izleyiciler için "Bildir" seçeneği. */
@Composable
internal fun VisionMoreMenuButton(
    isOwner: Boolean,
    onReportClick: () -> Unit,
    tint: Color = Color.White
) {
    if (isOwner) return
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.generic_more_options_cd), tint = tint)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.vision_report_action)) },
                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = SemanticDanger400) },
                onClick = {
                    expanded = false
                    onReportClick()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VisionCommentsSheet(
    comments: List<GoalComment>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    currentUserId: String?,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Void900
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 560.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.goal_detail_comments_count, comments.size),
                color = AstralGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = AstralGold,
                        modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally).padding(vertical = 16.dp)
                    )
                } else if (comments.isEmpty()) {
                    Text(
                        text = stringResource(R.string.goal_detail_no_comments),
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    comments.forEach { comment ->
                        VisionCommentRow(
                            comment = comment,
                            currentUserId = currentUserId,
                            onDelete = { onDeleteComment(comment.id) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { if (it.length <= 1000) commentText = it },
                    placeholder = { Text(stringResource(R.string.goal_detail_comment_placeholder), color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
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
                            onAddComment(commentText)
                            commentText = ""
                        }
                    },
                    enabled = !isSubmitting && commentText.isNotBlank(),
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (commentText.isNotBlank()) AstralGold else Void800)
                ) {
                    if (isSubmitting) {
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

@Composable
private fun VisionCommentRow(
    comment: GoalComment,
    currentUserId: String?,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Void800.copy(alpha = 0.5f))
            .padding(10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val avatarUrl = comment.userProfile?.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(28.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(AstralGold.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.userProfile?.nameOrFallback?.take(1)?.uppercase() ?: "?",
                    color = AstralGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.userProfile?.nameOrFallback ?: "?",
                color = AstralGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = comment.content,
                color = Color.White,
                fontSize = 13.sp
            )
        }
        if (currentUserId != null && comment.userId == currentUserId) {
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.generic_delete_cd),
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/** reason -> string kaynağı eşlemesi, GoalReportReason enum sırasıyla aynı. */
@Composable
private fun reportReasonLabel(reason: GoalReportReason): String = when (reason) {
    GoalReportReason.SPAM -> stringResource(R.string.vision_report_reason_spam)
    GoalReportReason.INAPPROPRIATE -> stringResource(R.string.vision_report_reason_inappropriate)
    GoalReportReason.HARASSMENT -> stringResource(R.string.vision_report_reason_harassment)
    GoalReportReason.MISINFORMATION -> stringResource(R.string.vision_report_reason_misinformation)
    GoalReportReason.HATE_SPEECH -> stringResource(R.string.vision_report_reason_hate_speech)
    GoalReportReason.OTHER -> stringResource(R.string.vision_report_reason_other)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VisionReportSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (GoalReportReason, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by remember { mutableStateOf<GoalReportReason?>(null) }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Void900
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.vision_report_title),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(R.string.vision_report_subtitle),
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Sıra pages/api/goals/report.js'teki VALID_REASONS ile aynı.
            val allReasons = listOf(
                GoalReportReason.SPAM,
                GoalReportReason.INAPPROPRIATE,
                GoalReportReason.HARASSMENT,
                GoalReportReason.MISINFORMATION,
                GoalReportReason.HATE_SPEECH,
                GoalReportReason.OTHER
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                allReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { selectedReason = reason }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = AstralGold, unselectedColor = Color.Gray)
                        )
                        Text(text = reportReasonLabel(reason), color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                // Backend MAX_NOTE_LENGTH = 500 (pages/api/goals/report.js).
                onValueChange = { if (it.length <= 500) note = it },
                placeholder = { Text(stringResource(R.string.vision_report_note_placeholder), color = Color.Gray, fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Void800,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { selectedReason?.let { onSubmit(it, note.takeIf { n -> n.isNotBlank() }) } },
                enabled = selectedReason != null && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SemanticDanger400)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text(stringResource(R.string.vision_report_submit))
                }
            }
        }
    }
}
