package io.lunosfer.dreamap.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import io.lunosfer.dreamap.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
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
import io.lunosfer.dreamap.ui.theme.*
import io.lunosfer.dreamap.ui.viewmodel.DiaryComposerUiState
import io.lunosfer.dreamap.ui.viewmodel.DiaryComposerViewModel
import io.lunosfer.dreamap.util.VisibilityPolicy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryComposerScreen(
    onBack: () -> Unit,
    viewModel: DiaryComposerViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val contentState = state as? DiaryComposerUiState.Content ?: DiaryComposerUiState.Content()
    val sharedMessage = stringResource(R.string.diary_composer_toast_shared)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(context, it) }
    }

    LaunchedEffect(contentState.isSuccess) {
        if (contentState.isSuccess) {
            Toast.makeText(context, sharedMessage, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    LaunchedEffect(contentState.error) {
        contentState.error?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.diary_composer_title),
                        color = AstralGold,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = SerifFontFamily)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back_cd), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Void950)
            )
        },
        containerColor = Void950
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Media Type Selector Tabs
            Text(stringResource(R.string.diary_composer_content_type_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val types = listOf(
                    Triple("text", stringResource(R.string.diary_composer_type_text), Icons.Default.TextFields),
                    Triple("photo", stringResource(R.string.diary_composer_type_photo), Icons.Default.Image),
                    Triple("video", stringResource(R.string.diary_composer_type_video), Icons.Default.Videocam)
                )

                types.forEach { (type, label, icon) ->
                    val isSelected = contentState.mediaType == type
                    Button(
                        onClick = { viewModel.setMediaType(type) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) AstralGold else Void900,
                            contentColor = if (isSelected) Void950 else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isSelected) AstralGold else Void800),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Photo or Video Media Input
            if (contentState.mediaType != "text") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Void900),
                    border = BorderStroke(1.dp, Void800)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (contentState.mediaType == "photo") stringResource(R.string.diary_composer_photo_selection_label) else stringResource(R.string.diary_composer_video_selection_label),
                            color = AstralGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // File picker button
                        Button(
                            onClick = {
                                val mimeType = if (contentState.mediaType == "photo") "image/*" else "video/*"
                                photoPickerLauncher.launch(mimeType)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Void800),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = AstralGold)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.diary_composer_pick_from_gallery_camera), color = Color.White, fontSize = 13.sp)
                        }

                        // Preview or Uploading Indicator
                        if (contentState.isUploading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AstralGold)
                                Text(stringResource(R.string.diary_composer_media_uploading), color = Color.Gray, fontSize = 12.sp)
                            }
                        } else if (contentState.mediaUrl.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, AstralGold, RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = contentState.mediaUrl,
                                    contentDescription = stringResource(R.string.common_preview_cd),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Optional direct URL input
                        OutlinedTextField(
                            value = contentState.mediaUrl,
                            onValueChange = { viewModel.setMediaUrl(it) },
                            label = { Text(stringResource(R.string.diary_composer_media_url_label), color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AstralGold,
                                unfocusedBorderColor = Void800,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }

            // Caption Field
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = if (contentState.mediaType == "text") stringResource(R.string.diary_composer_text_input_required) else stringResource(R.string.diary_composer_description_optional),
                    color = AstralGold,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = contentState.caption,
                    onValueChange = { viewModel.setCaption(it) },
                    placeholder = { Text(stringResource(R.string.diary_composer_placeholder), color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AstralGold,
                        unfocusedBorderColor = Void800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Text(
                    text = "${contentState.caption.length} / 1000",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }

            // Visibility Selector — profil gizliliğine göre kısıtlı seçenekler
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.common_visibility_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                val allowedVisibilityOptions = remember(contentState.profileVisibility) {
                    VisibilityPolicy.allowedOptions(contentState.profileVisibility)
                }
                val allVisibilities = listOf(
                    "private" to stringResource(R.string.visibility_private),
                    "friends" to stringResource(R.string.visibility_friends),
                    "public" to stringResource(R.string.visibility_public)
                )
                val visibilities = allVisibilities.filter { (key, _) -> key in allowedVisibilityOptions }

                visibilities.forEach { (visKey, visLabel) ->
                    val isSelected = contentState.visibility == visKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) Void800 else Void900)
                            .clickable { viewModel.setVisibility(visKey) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { viewModel.setVisibility(visKey) },
                            colors = RadioButtonDefaults.colors(selectedColor = AstralGold)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(visLabel, color = Color.White, fontSize = 13.sp)
                    }
                }
                val diaryVisibilityNote = when {
                    contentState.profileVisibility == "private" -> stringResource(R.string.visibility_locked_private_note)
                    contentState.profileVisibility == "friends" -> stringResource(R.string.visibility_restricted_to_friends_note)
                    else -> null
                }
                if (diaryVisibilityNote != null) {
                    Text(diaryVisibilityNote, color = Color.Gray, fontSize = 11.sp)
                }
            }

            // Connect to Vision Selector (Optional)
            if (contentState.availableGoals.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.diary_composer_link_vision_label), color = AstralGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    var expanded by remember { mutableStateOf(false) }
                    val selectedGoal = contentState.availableGoals.find { it.id == contentState.selectedGoalId }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Void900,
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Void800)
                        ) {
                            Text(
                                text = selectedGoal?.title ?: stringResource(R.string.diary_composer_select_vision_placeholder),
                                color = if (selectedGoal != null) AstralGold else Color.Gray,
                                fontSize = 13.sp
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Void900)
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.diary_composer_no_vision), color = Color.Gray) },
                                onClick = {
                                    viewModel.setSelectedGoalId(null)
                                    expanded = false
                                }
                            )
                            contentState.availableGoals.forEach { goal ->
                                DropdownMenuItem(
                                    text = { Text(goal.title, color = Color.White) },
                                    onClick = {
                                        viewModel.setSelectedGoalId(goal.id)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Submit Button
            Button(
                onClick = { viewModel.submit() },
                enabled = !contentState.isSubmitting && !contentState.isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AstralGold),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (contentState.isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Void950)
                } else {
                    Text(stringResource(R.string.diary_composer_share_btn), color = Void950, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
