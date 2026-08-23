package io.lunosfer.dreamap.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.PixabayHit
import io.lunosfer.dreamap.data.model.PixabaySelectedMedia
import io.lunosfer.dreamap.data.model.PixabayVideoHit
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun PixabayMediaPickerDialog(
    onDismissRequest: () -> Unit,
    onImageSelected: (pixabayId: Long, imageUrl: String, tags: String, user: String) -> Unit = { _, _, _, _ -> },
    onVideoSelected: ((pixabayId: Long, videoUrl: String, tags: String, user: String, durationSeconds: Int) -> Unit)? = null,
    onMultipleMediaSelected: ((List<PixabaySelectedMedia>) -> Unit)? = null,
    allowMultiple: Boolean = true,
    initialMediaType: String = "image",
    initialQuery: String = ""
) {
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf(initialQuery) }
    var mediaType by remember { mutableStateOf(initialMediaType) } // "image" or "video"
    var isLoading by remember { mutableStateOf(false) }

    var imageResults by remember { mutableStateOf<List<PixabayHit>>(emptyList()) }
    var videoResults by remember { mutableStateOf<List<PixabayVideoHit>>(emptyList()) }

    // Map of selected media: key -> PixabaySelectedMedia
    val selectedMedias = remember { mutableStateMapOf<String, PixabaySelectedMedia>() }

    fun doSearch(queryToSearch: String = searchQuery) {
        isLoading = true
        coroutineScope.launch {
            runCatching {
                val q = queryToSearch.trim()
                if (mediaType == "image") {
                    val res = NetworkModule.api.searchPixabay(q)
                    imageResults = res.hits
                } else {
                    val res = NetworkModule.api.searchPixabayVideos(q)
                    videoResults = res.hits
                }
            }
            isLoading = false
        }
    }

    fun confirmSelection() {
        val items = selectedMedias.values.toList()
        if (items.isEmpty()) return
        if (onMultipleMediaSelected != null) {
            onMultipleMediaSelected(items)
        } else {
            items.forEach { item ->
                when (item) {
                    is PixabaySelectedMedia.Image -> onImageSelected(item.id, item.imageUrl, item.tags, item.user)
                    is PixabaySelectedMedia.Video -> {
                        if (onVideoSelected != null) {
                            onVideoSelected(item.id, item.videoUrl, item.tags, item.user, item.durationSeconds)
                        } else {
                            val fallback = item.previewUrl.ifBlank { item.videoUrl }
                            onImageSelected(item.id, fallback, item.tags, item.user)
                        }
                    }
                }
            }
        }
        onDismissRequest()
    }

    LaunchedEffect(mediaType) {
        doSearch()
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = Void900,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.pixabay_title),
                        color = AstralGold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = SerifFontFamily
                    )
                    if (selectedMedias.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AstralGold)
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${selectedMedias.size}",
                                color = Void900,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.pixabay_close), tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Media Type Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Void800)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (mediaType == "image") AetherViolet else Color.Transparent)
                            .clickable { mediaType = "image" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.pixabay_image), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (mediaType == "video") AetherViolet else Color.Transparent)
                            .clickable { mediaType = "video" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.pixabay_video), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.pixabay_search_placeholder), color = Color.Gray, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        doSearch("")
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AstralGold,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    IconButton(
                        onClick = { doSearch() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AetherViolet)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.pixabay_search), tint = Color.White)
                    }
                }

                // Results Grid
                Box(modifier = Modifier.weight(1f)) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AstralGold)
                        }
                    } else if (mediaType == "image") {
                        if (imageResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.pixabay_images_not_found), color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(imageResults) { hit ->
                                    val itemKey = "img_${hit.id}"
                                    val isSelected = selectedMedias.containsKey(itemKey)
                                    val tagsStr = hit.tags.joinToString(", ")
                                    val imgUrl = hit.webformatURL.ifBlank { hit.largeImageURL ?: hit.previewURL ?: "" }

                                    Box(
                                        modifier = Modifier
                                            .height(105.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) AstralGold else Color.White.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                if (allowMultiple) {
                                                    if (isSelected) {
                                                        selectedMedias.remove(itemKey)
                                                    } else {
                                                        selectedMedias[itemKey] = PixabaySelectedMedia.Image(
                                                            id = hit.id,
                                                            imageUrl = imgUrl,
                                                            tags = tagsStr,
                                                            user = hit.user,
                                                            previewUrl = hit.previewURL ?: imgUrl
                                                        )
                                                    }
                                                } else {
                                                    onImageSelected(hit.id, imgUrl, tagsStr, hit.user)
                                                    onDismissRequest()
                                                }
                                            }
                                    ) {
                                        AsyncImage(
                                            model = hit.previewURL ?: imgUrl,
                                            contentDescription = tagsStr,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(AstralGold.copy(alpha = 0.2f))
                                            )
                                        }

                                        if (allowMultiple) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(22.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) AstralGold else Color.Black.copy(alpha = 0.45f))
                                                    .border(
                                                        1.5.dp,
                                                        if (isSelected) AstralGold else Color.White.copy(alpha = 0.7f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Void900,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (videoResults.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.pixabay_videos_not_found), color = Color.Gray, fontSize = 13.sp)
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(videoResults) { hit ->
                                    val itemKey = "vid_${hit.id}"
                                    val isSelected = selectedMedias.containsKey(itemKey)
                                    val thumb = hit.videos?.tiny?.thumbnail?.takeIf { it.isNotBlank() }
                                        ?: hit.videos?.small?.thumbnail?.takeIf { it.isNotBlank() }
                                        ?: hit.videos?.medium?.thumbnail?.takeIf { it.isNotBlank() }
                                        ?: hit.videos?.large?.thumbnail?.takeIf { it.isNotBlank() }
                                        ?: if (!hit.picture_id.isNullOrBlank()) "https://i.vimeocdn.com/video/${hit.picture_id}_295x166.jpg" else ""
                                    val videoUrl = hit.videos?.medium?.url?.takeIf { it.isNotBlank() }
                                        ?: hit.videos?.large?.url?.takeIf { it.isNotBlank() }
                                        ?: hit.videos?.small?.url?.takeIf { it.isNotBlank() }
                                        ?: hit.videos?.tiny?.url?.takeIf { it.isNotBlank() }
                                        ?: ""
                                    val tagsStr = hit.tags.joinToString(", ")

                                    Box(
                                        modifier = Modifier
                                            .height(105.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Void800)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) AstralGold else AetherViolet.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                if (allowMultiple) {
                                                    if (isSelected) {
                                                        selectedMedias.remove(itemKey)
                                                    } else {
                                                        if (videoUrl.isNotBlank()) {
                                                            selectedMedias[itemKey] = PixabaySelectedMedia.Video(
                                                                id = hit.id,
                                                                videoUrl = videoUrl,
                                                                tags = tagsStr,
                                                                user = hit.user,
                                                                durationSeconds = hit.duration,
                                                                previewUrl = thumb
                                                            )
                                                        } else if (thumb.isNotBlank()) {
                                                            selectedMedias[itemKey] = PixabaySelectedMedia.Image(
                                                                id = hit.id,
                                                                imageUrl = thumb,
                                                                tags = tagsStr,
                                                                user = hit.user,
                                                                previewUrl = thumb
                                                            )
                                                        }
                                                    }
                                                } else {
                                                    if (videoUrl.isNotBlank()) {
                                                        if (onVideoSelected != null) {
                                                            onVideoSelected(hit.id, videoUrl, tagsStr, hit.user, hit.duration)
                                                        } else {
                                                            onImageSelected(hit.id, thumb.ifBlank { videoUrl }, tagsStr, hit.user)
                                                        }
                                                    } else if (thumb.isNotBlank()) {
                                                        onImageSelected(hit.id, thumb, tagsStr, hit.user)
                                                    }
                                                    onDismissRequest()
                                                }
                                            }
                                    ) {
                                        if (thumb.isNotBlank()) {
                                            AsyncImage(
                                                model = thumb,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        // Dark gradient overlay
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            Color.Transparent,
                                                            Color.Black.copy(alpha = 0.6f)
                                                        )
                                                    )
                                                )
                                        )

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(AstralGold.copy(alpha = 0.2f))
                                            )
                                        }

                                        // Video icon & duration indicator
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = AstralGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            if (hit.duration > 0) {
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = "${hit.duration}s",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (allowMultiple) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(6.dp)
                                                    .size(22.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) AstralGold else Color.Black.copy(alpha = 0.45f))
                                                    .border(
                                                        1.5.dp,
                                                        if (isSelected) AstralGold else Color.White.copy(alpha = 0.7f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Void900,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Selection Confirmation Bar
                AnimatedVisibility(
                    visible = allowMultiple && selectedMedias.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Void800)
                            .border(1.dp, AstralGold.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.pixabay_selected_count, selectedMedias.size),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = { selectedMedias.clear() },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.pixabay_clear_selection),
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = { confirmSelection() },
                            colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Void900,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.pixabay_add_selected, selectedMedias.size),
                                    color = Void900,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}
