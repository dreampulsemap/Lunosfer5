package io.lunosfer.dreamap.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import io.lunosfer.dreamap.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.CreateGoalRequest
import io.lunosfer.dreamap.data.model.RoadmapItemInput
import io.lunosfer.dreamap.data.repository.ProfileRepository
import io.lunosfer.dreamap.data.repository.VisionRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import io.lunosfer.dreamap.ui.components.PixabayMediaPickerDialog
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.util.VisibilityPolicy
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateVisionScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember { VisionRepository() }

    val coverImageCd = stringResource(R.string.create_vision_cover_image_cd)
    val removeCd = stringResource(R.string.common_remove_cd)
    val selectDatePlaceholder = stringResource(R.string.create_vision_select_date_placeholder)
    val selectDateCd = stringResource(R.string.create_vision_select_date_cd)
    val visibilityPublicLabel = stringResource(R.string.visibility_public)
    val visibilityFriendsLabel = stringResource(R.string.create_vision_visibility_friends)
    val visibilityPrivateLabel = stringResource(R.string.create_vision_visibility_private)
    val addStepCd = stringResource(R.string.create_vision_add_step_cd)
    val errorTitleRequired = stringResource(R.string.create_vision_error_title_required)
    val errorTitleTooLong = stringResource(R.string.create_vision_error_title_too_long)
    val errorDescriptionTooLong = stringResource(R.string.create_vision_error_description_too_long)
    val toastSuccess = stringResource(R.string.create_vision_toast_success)
    val errorTitleMissing = stringResource(R.string.create_vision_error_title_missing)
    val errorTitleTooLongServer = stringResource(R.string.create_vision_error_title_too_long_server)
    val errorDescriptionTooLongServer = stringResource(R.string.create_vision_error_description_too_long_server)
    val errorVisionCreateFailed = stringResource(R.string.vision_error_create_failed)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf<String?>(null) }
    var visibility by remember { mutableStateOf("public") }
    // Kullanıcının profil gizliliği (public/friends/private). Paylaşım gizliliği
    // seçenekleri buna göre kısıtlanır — bkz. util/VisibilityPolicy.kt.
    // Yüklenene kadar en kısıtlayıcı varsayımla (private) başlıyoruz.
    var profileVisibility by remember { mutableStateOf<String?>("private") }
    val profileRepository = remember { ProfileRepository() }

    LaunchedEffect(Unit) {
        val uid = supabaseClient.auth.currentUserOrNull()?.id ?: return@LaunchedEffect
        profileRepository.getUserProfile(uid).onSuccess { profile ->
            profileVisibility = profile.profileVisibility
        }
    }

    val allowedVisibilityOptions = remember(profileVisibility) { VisibilityPolicy.allowedOptions(profileVisibility) }
    LaunchedEffect(allowedVisibilityOptions) {
        if (visibility !in allowedVisibilityOptions) {
            visibility = allowedVisibilityOptions.first()
        }
    }
    val visibilityLockedPrivateNote = stringResource(R.string.visibility_locked_private_note)
    val visibilityRestrictedFriendsNote = stringResource(R.string.visibility_restricted_to_friends_note)
    var coverImageUrl by remember { mutableStateOf<String?>(null) }
    // Pixabay attribution — kapak görselini goal oluşturulduktan SONRA
    // /api/goals/add-image-from-pixabay ile gerçekten kalıcı hale getirmek için gerekli.
    // Sadece CreateGoalRequest.coverImageUrl göndermek görselin DB'ye yazılmasını garanti etmiyor.
    var coverImagePixabayId by remember { mutableStateOf<Long?>(null) }
    var coverImageTags by remember { mutableStateOf("") }
    var coverImagePixabayUser by remember { mutableStateOf("") }
    var showPixabayDialog by remember { mutableStateOf(false) }

    var roadmapInput by remember { mutableStateOf("") }
    var roadmapItems by remember { mutableStateOf<List<String>>(emptyList()) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            targetDate = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.create_vision_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back_cd),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SemanticDanger500.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, SemanticDanger500)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = SemanticDanger400,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Title Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_title_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        if (it.length <= 120) title = it
                    },
                    placeholder = { Text(stringResource(R.string.create_vision_title_placeholder), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text(
                            text = "${title.length} / 120",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            // Description Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_desc_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        if (it.length <= 2000) description = it
                    },
                    placeholder = { Text(stringResource(R.string.create_vision_desc_placeholder), color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    supportingText = {
                        Text(
                            text = "${description.length} / 2000",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            if (showPixabayDialog) {
                PixabayMediaPickerDialog(
                    onDismissRequest = { showPixabayDialog = false },
                    onImageSelected = { pixabayId, url, tags, user ->
                        coverImageUrl = url
                        coverImagePixabayId = pixabayId
                        coverImageTags = tags
                        coverImagePixabayUser = user
                    },
                    onVideoSelected = { pixabayId, url, tags, user, _ ->
                        coverImageUrl = url
                        coverImagePixabayId = pixabayId
                        coverImageTags = tags
                        coverImagePixabayUser = user
                    },
                    allowMultiple = false
                )
            }

            // Cover Image Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_cover_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (coverImageUrl.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { showPixabayDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AstralGold),
                        border = BorderStroke(1.dp, AstralGold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.create_vision_cover_pixabay_btn))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        AsyncImage(
                            model = coverImageUrl,
                            contentDescription = coverImageCd,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = {
                                coverImageUrl = null
                                coverImagePixabayId = null
                                coverImageTags = ""
                                coverImagePixabayUser = ""
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Void950.copy(alpha = 0.7f))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = removeCd, tint = Color.White)
                        }
                    }
                }
            }

            // Target Date Picker
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_date_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                OutlinedCard(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = Void900),
                    border = BorderStroke(1.dp, Void800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = targetDate ?: selectDatePlaceholder,
                            color = if (targetDate != null) Color.White else Color.Gray,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = selectDateCd,
                            tint = AstralGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Visibility Selection — profil gizliliğine göre kısıtlı seçenekler
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.create_vision_visibility_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val allOptions = listOf(
                        "public" to visibilityPublicLabel,
                        "friends" to visibilityFriendsLabel,
                        "private" to visibilityPrivateLabel
                    )
                    val options = allOptions.filter { (key, _) -> key in allowedVisibilityOptions }
                    options.forEach { (key, label) ->
                        val selected = visibility == key
                        FilterChip(
                            selected = selected,
                            onClick = { visibility = key },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AstralGold,
                                selectedLabelColor = Void950,
                                containerColor = Void800,
                                labelColor = Color.White
                            )
                        )
                    }
                }
                val restrictionNote = when (profileVisibility) {
                    "private" -> visibilityLockedPrivateNote
                    "friends" -> visibilityRestrictedFriendsNote
                    else -> null
                }
                if (restrictionNote != null) {
                    Text(restrictionNote, color = Color.Gray, fontSize = 11.sp)
                }
            }

            // Roadmap Items (Yol Haritası)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.create_vision_roadmap_label, roadmapItems.size),
                    color = AstralGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = roadmapInput,
                        onValueChange = { roadmapInput = it },
                        placeholder = { Text(stringResource(R.string.create_vision_roadmap_placeholder), color = Color.Gray, fontSize = 12.sp) },
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
                            if (roadmapInput.isNotBlank() && roadmapItems.size < 20) {
                                roadmapItems = roadmapItems + roadmapInput.trim()
                                roadmapInput = ""
                            }
                        },
                        enabled = roadmapInput.isNotBlank() && roadmapItems.size < 20,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (roadmapInput.isNotBlank() && roadmapItems.size < 20) AstralGold else Void800)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = addStepCd,
                            tint = Void950
                        )
                    }
                }

                if (roadmapItems.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roadmapItems.forEachIndexed { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Void900),
                                border = BorderStroke(1.dp, Void800)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${index + 1}. $item",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            roadmapItems = roadmapItems.toMutableList().apply { removeAt(index) }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = removeCd,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        errorMessage = errorTitleRequired
                        return@Button
                    }
                    if (title.length > 120) {
                        errorMessage = errorTitleTooLong
                        return@Button
                    }
                    if (description.length > 2000) {
                        errorMessage = errorDescriptionTooLong
                        return@Button
                    }

                    errorMessage = null
                    isSubmitting = true

                    coroutineScope.launch {
                        val roadmapList = roadmapItems.map { RoadmapItemInput(title = it) }
                        val req = CreateGoalRequest(
                            title = title.trim(),
                            description = description.trim().ifEmpty { null },
                            coverImageUrl = coverImageUrl,
                            coverImageSource = if (coverImageUrl != null) "pixabay" else "ai_generated",
                            targetDate = targetDate,
                            visibility = VisibilityPolicy.clamp(visibility, profileVisibility),
                            roadmap = if (roadmapList.isNotEmpty()) roadmapList else null
                        )

                        val createResult = repository.createGoal(req)
                        val newGoal = createResult.getOrNull()
                        if (newGoal != null) {
                            // Kapak görseli Pixabay'den seçildiyse, CreateGoalRequest'teki
                            // cover_image_url alanı tek başına DB'ye kalıcı yazılmayı garanti
                            // etmiyor (galeri/kapak tablosu ayrı bir endpoint bekliyor —
                            // GoalDetailScreen'deki "görsel ekle" akışıyla aynı desen). Goal
                            // artık var olduğuna göre, görseli burada gerçekten kaydediyoruz —
                            // ve ekrandan ayrılmadan ÖNCE, tamamlanmasını bekliyoruz.
                            val pixabayId = coverImagePixabayId
                            val imageUrl = coverImageUrl
                            if (!imageUrl.isNullOrBlank() && pixabayId != null) {
                                val addResult = repository.addGoalImageFromPixabay(
                                    goalId = newGoal.id,
                                    pixabayId = pixabayId,
                                    imageUrl = imageUrl,
                                    tags = coverImageTags,
                                    pixabayUser = coverImagePixabayUser
                                )
                                if (addResult.isSuccess) {
                                    repository.setGoalCover(newGoal.id, imageUrl)
                                }
                            }

                            isSubmitting = false
                            Toast.makeText(context, toastSuccess, Toast.LENGTH_SHORT).show()
                            // Vizyonun slayt/videosunu oluşturma adımı ekleme sırasında hiç
                            // yapılmıyordu — kullanıcıyı doğrudan Reels/Video editörüne
                            // yönlendirip bunu tamamlatıyoruz (mevcut mimaride "Vizyonu İzle"
                            // önce vision_video_url'e bakıyor, o yüzden burası birincil akış).
                            // create_vision ekranını geri yığından çıkarıyoruz ki editörden
                            // "kapat" ile geri dönüşte create ekranına değil, bir önceki
                            // listeye dönsün.
                            navController.navigate(Screen.VideoEditor.createRoute(newGoal.id)) {
                                popUpTo(Screen.CreateVision.route) { inclusive = true }
                            }
                        } else {
                            isSubmitting = false
                            val err = createResult.exceptionOrNull()
                            val msg = err?.message ?: ""
                            errorMessage = when {
                                msg.contains("title_required", ignoreCase = true) -> errorTitleMissing
                                msg.contains("title_too_long", ignoreCase = true) -> errorTitleTooLongServer
                                msg.contains("description_too_long", ignoreCase = true) -> errorDescriptionTooLongServer
                                else -> err?.message ?: errorVisionCreateFailed
                            }
                        }
                    }
                },
                enabled = !isSubmitting && title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Void950, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = stringResource(R.string.create_vision_submit_btn),
                        color = Void950,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
