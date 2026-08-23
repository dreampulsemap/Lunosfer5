package io.lunosfer.dreamap.ui.screens

import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import io.lunosfer.dreamap.R
import coil.compose.AsyncImage
import io.lunosfer.dreamap.data.model.GoalSlide
import io.lunosfer.dreamap.ui.components.PixabayMediaPickerDialog
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.MAX_GOAL_SLIDES
import io.lunosfer.dreamap.ui.viewmodel.SlideCreatorUiState
import io.lunosfer.dreamap.ui.viewmodel.SlideCreatorViewModel
import kotlin.math.roundToInt

/**
 * Slayt oluşturma/düzenleme — components/SlideEditor.jsx + SlideCaptionEditor.jsx
 * karşılığı. NOT: web'de bu akış artık UI'da yok (VisionVideoEditor'a
 * geçildi), ama backend (goal_slides tablosu + slides/create-update-reorder
 * endpoint'leri) tam çalışır durumda — bu ekran Android'e özgü.
 *
 * Konumlandırma serbest sürükle + iki parmak pinch (detectTransformGestures)
 * ile — sabit ön ayar yok. Font: harici Google Fonts sertifika riski
 * almamak için Android'in yerleşik ailelerinden 6 görsel olarak belirgin
 * stil (bkz. ui/theme/CaptionFonts.kt) — 3'ü (elegant/display/handwritten)
 * TAM kalıcı olması için backend'deki ALLOWED_FONTS dizisinin genişletilmesi
 * gerekiyor, bkz. o dosyadaki not.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlideCreatorScreen(
    goalId: String,
    onBack: () -> Unit
) {
    val factory = remember(goalId) { SlideCreatorViewModel.Factory(goalId) }
    val viewModel: SlideCreatorViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var showAddMenu by remember { mutableStateOf(false) }
    var showPixabayImages by remember { mutableStateOf(false) }
    var showPixabayVideos by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addSlideFromDevice(context, it) }
    }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.addSlideFromDeviceVideo(context, it) }
    }

    Scaffold(
        containerColor = Void950,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.slide_creator_title), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        }
    ) { padding ->
        when (val s = state) {
            is SlideCreatorUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AstralGold)
                }
            }
            is SlideCreatorUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(s.message, color = Color.White)
                }
            }
            is SlideCreatorUiState.Content -> {
                if (s.editingSlideId != null && s.editingSlide != null) {
                    // Düzenleme modunda tam ekran canlı önizleme
                    SlideStyleEditor(
                        slide = s.editingSlide!!,
                        onCancel = { viewModel.stopEditing() },
                        onSave = { caption, duration, font, color, x, y, size ->
                            viewModel.saveSlideStyle(s.editingSlide!!.id, caption, duration, font, color, x, y, size)
                        }
                    )
                } else {
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        Text(
                            text = "${s.slides.size} / $MAX_GOAL_SLIDES slayt",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        s.error?.let { err ->
                            Surface(
                                color = SemanticDanger500.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, SemanticDanger500.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text(err, color = SemanticDanger400, fontSize = 12.sp, modifier = Modifier.padding(10.dp))
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(s.slides, key = { it.id }) { slide ->
                                val index = s.slides.indexOf(slide)
                                SlideRow(
                                    slide = slide,
                                    index = index,
                                    total = s.slides.size,
                                    onEdit = { viewModel.startEditing(slide.id) },
                                    onDelete = { viewModel.deleteSlide(slide.id) },
                                    onMoveUp = { viewModel.moveSlide(slide.id, -1) },
                                    onMoveDown = { viewModel.moveSlide(slide.id, 1) }
                                )
                            }

                            item {
                                if (s.isUploading) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 12.dp)
                                    ) {
                                        CircularProgressIndicator(color = AstralGold, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Text(stringResource(R.string.slide_creator_uploading), color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                                    }
                                } else if (s.canAddMore) {
                                    Surface(
                                        onClick = { showAddMenu = true },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White.copy(alpha = 0.06f),
                                        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.4f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = AstralGold)
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.slide_creator_add_slide), color = AstralGold, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            containerColor = Void950,
            title = { Text(stringResource(R.string.slide_creator_add_dialog_title), color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(
                        Triple(Icons.Default.Image, stringResource(R.string.slide_creator_pick_device)) { photoPickerLauncher.launch("image/*") },
                        Triple(Icons.Default.Movie, stringResource(R.string.slide_creator_pick_device_video)) { videoPickerLauncher.launch("video/*") },
                        Triple(Icons.Default.Search, stringResource(R.string.slide_creator_pick_pixabay_image)) { showPixabayImages = true },
                        Triple(Icons.Default.Movie, stringResource(R.string.slide_creator_pick_pixabay_video)) { showPixabayVideos = true }
                    ).forEach { (icon, label, action) ->
                        Surface(
                            onClick = { showAddMenu = false; action() },
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.06f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(icon, contentDescription = null, tint = Color.White)
                                Text(label, color = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.slide_creator_cancel), color = Color.White.copy(alpha = 0.6f)) }
            }
        )
    }

    if (showPixabayImages) {
        PixabayMediaPickerDialog(
            onDismissRequest = { showPixabayImages = false },
            onImageSelected = { _, imageUrl, _, _ ->
                showPixabayImages = false
                viewModel.addSlideFromUrl(imageUrl)
            },
            onMultipleMediaSelected = { items ->
                showPixabayImages = false
                viewModel.addMultipleSlidesFromPixabay(items)
            }
        )
    }

    if (showPixabayVideos) {
        PixabayMediaPickerDialog(
            onDismissRequest = { showPixabayVideos = false },
            onImageSelected = { _, imageUrl, _, _ ->
                showPixabayVideos = false
                viewModel.addSlideFromUrl(imageUrl)
            },
            onVideoSelected = { _, videoUrl, _, _, durationSeconds ->
                showPixabayVideos = false
                viewModel.addSlideFromVideo(videoUrl, durationSeconds)
            },
            onMultipleMediaSelected = { items ->
                showPixabayVideos = false
                viewModel.addMultipleSlidesFromPixabay(items)
            },
            initialMediaType = "video"
        )
    }
}

@Composable
private fun SlideRow(
    slide: GoalSlide,
    index: Int,
    total: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box {
                AsyncImage(
                    model = slide.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))
                )
                if (slide.isVideo) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center).size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (!slide.caption.isNullOrBlank()) slide.caption else stringResource(R.string.slide_creator_no_caption),
                    color = Color.White,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${slide.durationSeconds ?: DEFAULT_IMAGE_DURATION_SECONDS_FALLBACK} sn" + if (slide.isVideo) " · video" else "",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = index > 0, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = if (index > 0) Color.White else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onMoveDown, enabled = index < total - 1, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = if (index < total - 1) Color.White else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(16.dp))
                }
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = AstralGold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = SemanticDanger400)
            }
        }
    }
}

private const val DEFAULT_IMAGE_DURATION_SECONDS_FALLBACK = 2

/**
 * Tam ekran düzenleyici: arka planda gerçek slayt görseli, üzerinde
 * sürüklenip iki parmakla büyütülüp küçültülebilen canlı altyazı önizlemesi.
 * detectTransformGestures pan+zoom'u TEK gestede birleştirir — kullanıcı
 * metni istediği yere sürükler, iki parmakla boyutunu ayarlar.
 */
@Composable
private fun SlideStyleEditor(
    slide: GoalSlide,
    onCancel: () -> Unit,
    onSave: (caption: String, duration: Int, font: String, color: String, x: Float, y: Float, size: Float) -> Unit
) {
    var caption by remember(slide.id) { mutableStateOf(slide.caption ?: "") }
    var duration by remember(slide.id) { mutableStateOf((slide.durationSeconds ?: DEFAULT_IMAGE_DURATION_SECONDS_FALLBACK).toFloat()) }
    var font by remember(slide.id) { mutableStateOf(slide.captionFont ?: DEFAULT_CAPTION_FONT_KEY) }
    var color by remember(slide.id) { mutableStateOf(slide.captionColor ?: DEFAULT_CAPTION_COLOR_HEX) }
    // Yüzde (0-100) cinsinden konum — serbest sürükleme ile değişir.
    var posXPercent by remember(slide.id) { mutableStateOf(slide.captionX ?: 50f) }
    var posYPercent by remember(slide.id) { mutableStateOf(slide.captionY ?: 85f) }
    var size by remember(slide.id) { mutableStateOf(slide.captionSize ?: 1.2f) }
    var previewBoxSize by remember { mutableStateOf(IntSize.Zero) }

    val fontStyleInfo = captionFontStyleFor(font)
    val textColor = remember(color) {
        runCatching { Color(AndroidColor.parseColor(color)) }.getOrDefault(Color.White)
    }

    Column(modifier = Modifier.fillMaxSize().background(Void950)) {
        // --- Canlı önizleme: sürükle + pinch ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onSizeChanged { previewBoxSize = it }
        ) {
            AsyncImage(
                model = slide.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (caption.isNotBlank() && previewBoxSize != IntSize.Zero) {
                val offsetX = (previewBoxSize.width * (posXPercent / 100f))
                val offsetY = (previewBoxSize.height * (posYPercent / 100f))

                Text(
                    text = caption,
                    color = textColor,
                    fontFamily = fontStyleInfo.family,
                    fontWeight = fontStyleInfo.weight,
                    fontStyle = fontStyleInfo.style,
                    fontSize = (22 * size).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            androidx.compose.ui.unit.IntOffset(
                                x = (offsetX - (previewBoxSize.width * 0.4f)).roundToInt(),
                                y = (offsetY - 20.dp.toPx()).roundToInt()
                            )
                        }
                        .widthIn(max = 320.dp)
                        .pointerInput(previewBoxSize) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                if (previewBoxSize.width > 0 && previewBoxSize.height > 0) {
                                    posXPercent = (posXPercent + (pan.x / previewBoxSize.width) * 100f).coerceIn(0f, 100f)
                                    posYPercent = (posYPercent + (pan.y / previewBoxSize.height) * 100f).coerceIn(0f, 100f)
                                    size = (size * zoom).coerceIn(0.4f, 3.5f)
                                }
                            }
                        }
                )
            }

            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
            ) {
                Text(
                    stringResource(R.string.slide_creator_drag_hint),
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            IconButton(onClick = onCancel, modifier = Modifier.align(Alignment.TopStart).padding(4.dp)) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
            }
        }

        // --- Alt panel: metin + font + renk + süre ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Void900)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = caption,
                onValueChange = { if (it.length <= 200) caption = it },
                label = { Text("${stringResource(R.string.slide_creator_caption_hint)} (${caption.length}/200)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = AstralGold,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedLabelColor = AstralGold,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.6f)
                ),
                maxLines = 2
            )

            Text(stringResource(R.string.slide_creator_font_label), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CAPTION_FONT_STYLES.forEach { fs ->
                    val selected = font == fs.key
                    Surface(
                        onClick = { font = fs.key },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) AstralGold.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, if (selected) AstralGold else Color.White.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Aa",
                            color = if (selected) AstralGold else Color.White.copy(alpha = 0.85f),
                            fontFamily = fs.family,
                            fontWeight = fs.weight,
                            fontStyle = fs.style,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Text(stringResource(R.string.slide_creator_color_label), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_CAPTION_COLORS.forEach { hex ->
                    val selected = color.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color.White))
                            .border(if (selected) 2.dp else 1.dp, if (selected) AstralGold else Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable { color = hex }
                    )
                }
            }

            Text(stringResource(R.string.slide_creator_duration_seconds, duration.toInt()), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Slider(
                value = duration,
                onValueChange = { duration = it },
                valueRange = 1f..15f,
                steps = 13,
                colors = SliderDefaults.colors(thumbColor = AstralGold, activeTrackColor = AstralGold)
            )

            Button(
                onClick = { onSave(caption, duration.toInt(), font, color, posXPercent, posYPercent, size) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary500)
            ) {
                Text(stringResource(R.string.slide_creator_save), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val PRESET_CAPTION_COLORS = listOf(
    "#ffffff", "#04060E", DEFAULT_CAPTION_COLOR_HEX, "#F43F5E", "#22D3EE", "#A855F7"
)
