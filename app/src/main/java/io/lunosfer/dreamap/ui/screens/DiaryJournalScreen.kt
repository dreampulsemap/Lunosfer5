package io.lunosfer.dreamap.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.DiaryJournalUiState
import io.lunosfer.dreamap.ui.viewmodel.DiaryJournalViewModel
import io.lunosfer.dreamap.ui.viewmodel.UNKNOWN_DATE_KEY
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Profildeki KALICI Günce ekranı. Üstteki halka/hikaye şeridi bilinçli
// olarak Instagram diliyle konuşur (bkz. DiaryRingsBar) — hızlı, günlük,
// 24 saatte söner. Burası tam tersi: her gün bir defter sayfası — büyük
// tarih başlığı, sol kenarda altın cetvel çizgisi, saat saat girdiler ve
// her girdide Düzenle / Sil.

private fun parseSupabaseTimestamp(isoTimestamp: String): Date? {
    return try {
        val withoutOffset = isoTimestamp
            .replace(Regex("[+-]\\d{2}:\\d{2}$"), "")
            .removeSuffix("Z")
            .replace(' ', 'T')
        val truncated = if (withoutOffset.contains(".")) {
            val (base, fraction) = withoutOffset.split(".", limit = 2)
            base + "." + fraction.take(3).padEnd(3, '0')
        } else {
            "$withoutOffset.000"
        }
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        format.parse(truncated)
    } catch (e: Exception) {
        null
    }
}

private fun entryTimeLabel(isoTimestamp: String?): String {
    val date = isoTimestamp?.let { parseSupabaseTimestamp(it) } ?: return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

/** dateKey: cihazın yerel günü "yyyy-MM-dd" (bkz. DiaryJournalViewModel.localDayKey). */
private fun parseDayKey(dateKey: String): Date? =
    if (dateKey == UNKNOWN_DATE_KEY) null
    else try { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey) } catch (e: Exception) { null }

private fun relativeDayLabel(date: Date, todayLabel: String, yesterdayLabel: String): String? {
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    val cal = Calendar.getInstance().apply { time = date }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(cal, today) -> todayLabel
        sameDay(cal, yesterday) -> yesterdayLabel
        else -> null
    }
}

private const val VIS_PUBLIC = "public"
private const val VIS_FRIENDS = "friends"
private const val VIS_PRIVATE = "private"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryJournalScreen(
    userId: String,
    onBack: () -> Unit,
    onGoalClick: (String) -> Unit
) {
    val factory = remember(userId) { DiaryJournalViewModel.Factory(userId) }
    val viewModel: DiaryJournalViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()

    // Silme onayi: kalici bir arsivden siliyoruz, geri alinamiyor.
    var pendingDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    var editing by remember { mutableStateOf<DiaryEntry?>(null) }

    val todayLabel = stringResource(R.string.diary_journal_today)
    val yesterdayLabel = stringResource(R.string.diary_journal_yesterday)
    val unknownLabel = stringResource(R.string.diary_journal_unknown_date)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.diary_journal_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.diary_journal_back), tint = Color.White)
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
                is DiaryJournalUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = AstralGold)
                }
                is DiaryJournalUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = s.message, color = SemanticDanger400)
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadEntries() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold)
                        ) {
                            Text(stringResource(R.string.diary_journal_retry))
                        }
                    }
                }
                is DiaryJournalUiState.Success -> {
                    if (s.groupedEntries.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.diary_journal_empty_title),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = SerifFontFamily
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.diary_journal_empty_body),
                                color = MoonSilver,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp)
                        ) {
                            s.groupedEntries.forEach { (dateKey, entries) ->
                                item(key = "day_$dateKey") {
                                    DayHeader(
                                        dateKey = dateKey,
                                        todayLabel = todayLabel,
                                        yesterdayLabel = yesterdayLabel,
                                        unknownLabel = unknownLabel
                                    )
                                }
                                // Gün içinde sabahtan akşama: defterde yazıldığı sırayla.
                                val ordered = entries.sortedBy { it.createdAt ?: "" }
                                items(ordered, key = { it.id }) { entry ->
                                    JournalEntryRow(
                                        entry = entry,
                                        canEdit = s.isSelf,
                                        isDeleting = s.deletingId == entry.id,
                                        isLast = entry.id == ordered.last().id,
                                        onEdit = { editing = entry },
                                        onDelete = { pendingDelete = entry },
                                        onGoalClick = onGoalClick
                                    )
                                }
                                item(key = "gap_$dateKey") { Spacer(Modifier.height(28.dp)) }
                            }
                        }
                    }

                    if (s.actionError != null) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Void900),
                            border = BorderStroke(1.dp, SemanticDanger400.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.actionError,
                                    color = SemanticDanger400,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { viewModel.clearActionError() }) {
                                    Text(stringResource(R.string.diary_journal_delete_cancel), color = AstralGold)
                                }
                            }
                        }
                    }

                    editing?.let { entry ->
                        EditEntryDialog(
                            entry = entry,
                            saving = s.savingId == entry.id,
                            onDismiss = { editing = null },
                            onSave = { caption, visibility ->
                                viewModel.updateEntry(entry.id, caption, visibility) { editing = null }
                            }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = Void900,
            title = {
                Text(stringResource(R.string.diary_journal_delete_title), color = Color.White)
            },
            text = {
                Text(stringResource(R.string.diary_journal_delete_body), color = MoonSilver, fontSize = 13.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteEntry(entry.id)
                    pendingDelete = null
                }) {
                    Text(stringResource(R.string.diary_journal_delete_confirm), color = SemanticDanger400, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.diary_journal_delete_cancel), color = MoonSilver)
                }
            }
        )
    }
}

/** Günün defter sayfası başlığı: büyük gün numarası + ay / hafta günü + ince altın çizgi. */
@Composable
private fun DayHeader(dateKey: String, todayLabel: String, yesterdayLabel: String, unknownLabel: String) {
    val date = parseDayKey(dateKey)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (date == null) {
            Text(unknownLabel, color = AstralGold, fontSize = 18.sp, fontFamily = SerifFontFamily, fontWeight = FontWeight.Bold)
            return@Row
        }
        val sameYear = Calendar.getInstance().apply { time = date }.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
        Text(
            SimpleDateFormat("d", Locale.getDefault()).format(date),
            color = AstralGold,
            fontSize = 46.sp,
            lineHeight = 46.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = SerifFontFamily
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 4.dp)) {
            Text(
                SimpleDateFormat(if (sameYear) "LLLL" else "LLLL yyyy", Locale.getDefault()).format(date).uppercase(Locale.getDefault()),
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            val weekday = SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
            val relative = relativeDayLabel(date, todayLabel, yesterdayLabel)
            Text(
                if (relative != null) "$relative · $weekday" else weekday,
                color = MoonSilver,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                fontFamily = SerifFontFamily
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .padding(bottom = 10.dp)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(AstralGold.copy(alpha = 0.35f), Color.Transparent)))
        )
    }
}

@Composable
private fun VisibilityBadge(visibility: String) {
    val (label, color) = when (visibility) {
        VIS_PUBLIC -> stringResource(R.string.diary_journal_public_badge) to AetherCyan
        VIS_FRIENDS -> stringResource(R.string.diary_journal_friends_badge) to AetherViolet
        else -> stringResource(R.string.diary_journal_private_badge) to AstralGold
    }
    Text(
        label,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/**
 * Tek girdi: solda cetvel çizgisi + parlayan nokta (zaman çizelgesi),
 * sağda saat, görünürlük, Düzenle/Sil ve defter kâğıdı gibi kart.
 */
@Composable
private fun JournalEntryRow(
    entry: DiaryEntry,
    canEdit: Boolean,
    isDeleting: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onGoalClick: (String) -> Unit
) {
    val lineColor = AstralGold.copy(alpha = 0.22f)
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            Modifier
                .width(18.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = 5.dp.toPx()
                    drawLine(lineColor, Offset(x, 0f), Offset(x, if (isLast) 14.dp.toPx() else size.height), strokeWidth = 1.dp.toPx())
                }
        ) {
            Box(
                Modifier
                    .padding(top = 9.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(AstralGold)
                    .border(2.dp, AstralGold.copy(alpha = 0.35f), CircleShape)
            )
        }
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = 18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entryTimeLabel(entry.createdAt), color = MoonSilver, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                VisibilityBadge(entry.visibility)
                Spacer(Modifier.weight(1f))
                if (canEdit) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.diary_journal_edit), tint = MoonSilver, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, enabled = !isDeleting, modifier = Modifier.size(32.dp)) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SemanticDanger400, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.diary_journal_delete_confirm), tint = MoonSilver, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.015f))))
                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val thumbUrl = entry.posterUrl ?: entry.mediaUrl
                if (entry.mediaType != "text" && !thumbUrl.isNullOrBlank()) {
                    Box(contentAlignment = Alignment.Center) {
                        AsyncImage(
                            model = thumbUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp)
                                .aspectRatio(4f / 3f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Void950)
                        )
                        if (entry.mediaType == "video") {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                if (!entry.caption.isNullOrBlank()) {
                    val isText = entry.mediaType == "text"
                    Text(
                        text = if (isText) "“${entry.caption}”" else entry.caption,
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = if (isText) 17.sp else 15.sp,
                        lineHeight = if (isText) 25.sp else 22.sp,
                        fontFamily = SerifFontFamily
                    )
                }

                val goalId = entry.goalId
                if (!entry.goalTitle.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.diary_journal_goal_prefix, entry.goalTitle),
                        color = AstralGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AstralGold.copy(alpha = 0.1f))
                            .then(if (goalId != null) Modifier.clickable { onGoalClick(goalId) } else Modifier)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditEntryDialog(
    entry: DiaryEntry,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (caption: String, visibility: String) -> Unit
) {
    var caption by remember(entry.id) { mutableStateOf(entry.caption.orEmpty()) }
    var visibility by remember(entry.id) { mutableStateOf(entry.visibility) }
    val textRequired = entry.mediaType == "text" && caption.isBlank()

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = Void900,
        title = { Text(stringResource(R.string.diary_journal_edit_title), color = Color.White, fontFamily = SerifFontFamily) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(1000) },
                    placeholder = { Text(stringResource(R.string.diary_journal_edit_hint), color = MoonSilver) },
                    minLines = 4,
                    maxLines = 8,
                    textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = SerifFontFamily, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = AstralGold
                    ),
                    isError = textRequired,
                    supportingText = if (textRequired) {
                        { Text(stringResource(R.string.diary_journal_text_required), color = SemanticDanger400) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        VIS_PUBLIC to R.string.diary_journal_public_badge,
                        VIS_FRIENDS to R.string.diary_journal_friends_badge,
                        VIS_PRIVATE to R.string.diary_journal_private_badge
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = visibility == value,
                            onClick = { visibility = value },
                            label = { Text(stringResource(label), fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AstralGold.copy(alpha = 0.18f),
                                selectedLabelColor = AstralGold,
                                labelColor = MoonSilver
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(caption, visibility) }, enabled = !saving && !textRequired) {
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AstralGold, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.diary_journal_edit_save), color = AstralGold, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(stringResource(R.string.diary_journal_delete_cancel), color = MoonSilver)
            }
        }
    )
}
