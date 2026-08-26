package io.lunosfer.dreamap.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.data.model.CreateDiaryInput
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.network.NetworkModule
import io.lunosfer.dreamap.data.repository.DiaryRepository
import io.lunosfer.dreamap.data.repository.ProfileRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.util.VisibilityPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DiaryComposerUiState {
    data class Content(
        val mediaType: String = "text", // "photo", "video", "text"
        val caption: String = "",
        val mediaUrl: String = "",
        val visibility: String = "private", // "public", "friends", "private"
        // Kullanıcının profil gizliliği; paylaşım gizliliği seçenekleri buna göre
        // kısıtlanır (bkz. util/VisibilityPolicy.kt).
        val profileVisibility: String? = null,
        val selectedGoalId: String? = null,
        val availableGoals: List<Goal> = emptyList(),
        val selectedImageUri: Uri? = null,
        val isUploading: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null,
        val isSuccess: Boolean = false
    ) : DiaryComposerUiState()
}

class DiaryComposerViewModel(
    private val repository: DiaryRepository = DiaryRepository(),
    private val profileRepository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val api = NetworkModule.api

    private val _state = MutableStateFlow<DiaryComposerUiState>(
        DiaryComposerUiState.Content()
    )
    val state: StateFlow<DiaryComposerUiState> = _state.asStateFlow()

    init {
        loadOwnGoals()
        loadProfileVisibility()
    }

    private fun loadOwnGoals() {
        viewModelScope.launch {
            try {
                val res = api.getGoalsFeed(mode = "own", page = 0, status = null)
                val current = _state.value as? DiaryComposerUiState.Content ?: return@launch
                _state.value = current.copy(availableGoals = res.goals)
            } catch (_: Exception) {}
        }
    }

    private fun loadProfileVisibility() {
        val uid = supabaseClient.auth.currentUserOrNull()?.id ?: return
        viewModelScope.launch {
            profileRepository.getUserProfile(uid).onSuccess { profile ->
                val current = _state.value as? DiaryComposerUiState.Content ?: return@onSuccess
                val allowed = VisibilityPolicy.allowedOptions(profile.profileVisibility)
                _state.value = current.copy(
                    profileVisibility = profile.profileVisibility,
                    visibility = if (current.visibility in allowed) current.visibility else allowed.first()
                )
            }
        }
    }

    fun setMediaType(type: String) {
        val current = _state.value as? DiaryComposerUiState.Content ?: return
        _state.value = current.copy(mediaType = type, error = null)
    }

    fun setCaption(caption: String) {
        val current = _state.value as? DiaryComposerUiState.Content ?: return
        if (caption.length <= 1000) {
            _state.value = current.copy(caption = caption, error = null)
        }
    }

    fun setMediaUrl(url: String) {
        val current = _state.value as? DiaryComposerUiState.Content ?: return
        _state.value = current.copy(mediaUrl = url, error = null)
    }

    fun setVisibility(vis: String) {
        val current = _state.value as? DiaryComposerUiState.Content ?: return
        val allowed = VisibilityPolicy.allowedOptions(current.profileVisibility)
        if (vis in allowed) {
            _state.value = current.copy(visibility = vis)
        }
    }

    fun setSelectedGoalId(goalId: String?) {
        val current = _state.value as? DiaryComposerUiState.Content ?: return
        _state.value = current.copy(selectedGoalId = goalId)
    }

    fun onImageSelected(context: Context, uri: Uri) {
        val current = _state.value as? DiaryComposerUiState.Content ?: return
        _state.value = current.copy(selectedImageUri = uri, isUploading = true, error = null)

        viewModelScope.launch {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    _state.value = current.copy(isUploading = false, error = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_image_read))
                    return@launch
                }
                val fileName = "story_${System.currentTimeMillis()}.jpg"
                repository.uploadMediaToStorage(bytes, fileName).onSuccess { uploadedUrl ->
                    val latest = _state.value as? DiaryComposerUiState.Content ?: return@onSuccess
                    _state.value = latest.copy(
                        mediaUrl = uploadedUrl,
                        isUploading = false
                    )
                }.onFailure { err ->
                    val latest = _state.value as? DiaryComposerUiState.Content ?: return@onFailure
                    _state.value = latest.copy(
                        isUploading = false,
                        error = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_upload_failed, err.message ?: "")
                    )
                }
            } catch (e: Exception) {
                val latest = _state.value as? DiaryComposerUiState.Content ?: return@launch
                _state.value = latest.copy(isUploading = false, error = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_image_upload_failed, e.message ?: ""))
            }
        }
    }

    fun submit() {
        val current = _state.value as? DiaryComposerUiState.Content ?: return

        if (current.mediaType == "text" && current.caption.isBlank()) {
            _state.value = current.copy(error = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_text_required))
            return
        }

        if (current.mediaType != "text" && current.mediaUrl.isBlank()) {
            _state.value = current.copy(error = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_media_required))
            return
        }

        _state.value = current.copy(isSubmitting = true, error = null)

        val input = CreateDiaryInput(
            mediaType = current.mediaType,
            mediaUrl = current.mediaUrl.takeIf { it.isNotBlank() },
            posterUrl = current.mediaUrl.takeIf { current.mediaType == "video" && it.isNotBlank() },
            caption = current.caption.takeIf { it.isNotBlank() },
            visibility = VisibilityPolicy.clamp(current.visibility, current.profileVisibility),
            goalId = current.selectedGoalId
        )

        viewModelScope.launch {
            repository.createEntry(input).onSuccess {
                val latest = _state.value as? DiaryComposerUiState.Content ?: return@onSuccess
                _state.value = latest.copy(isSubmitting = false, isSuccess = true)
            }.onFailure { err ->
                val latest = _state.value as? DiaryComposerUiState.Content ?: return@onFailure
                _state.value = latest.copy(
                    isSubmitting = false,
                    error = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_share_failed)
                )
            }
        }
    }

    fun clearError() {
        val current = _state.value as? DiaryComposerUiState.Content ?: return
        _state.value = current.copy(error = null)
    }
}
