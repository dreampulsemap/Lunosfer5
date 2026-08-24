package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.auth.auth
import io.lunosfer.dreamap.data.model.Dream
import io.lunosfer.dreamap.data.model.Goal
import io.lunosfer.dreamap.data.model.FullUserProfile
import io.lunosfer.dreamap.data.model.PremiumStatusResponse
import io.lunosfer.dreamap.data.model.UpdateProfileRequest
import io.lunosfer.dreamap.data.repository.ProfileRepository
import io.lunosfer.dreamap.supabase.supabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Content(
        val profile: FullUserProfile,
        val premiumStatus: PremiumStatusResponse = PremiumStatusResponse(),
        val stats: io.lunosfer.dreamap.data.model.ProfileStatsResponse = io.lunosfer.dreamap.data.model.ProfileStatsResponse(),
        val dreams: List<Dream> = emptyList(),
        val visions: List<Goal> = emptyList(),
        val savedVisions: List<Goal> = emptyList(),
        val selectedTab: Int = 0, // 0: Visions, 1: Dreams, 2: Journal (navigates away, never "selected"), 3: Saved
        val isLoadingPremium: Boolean = false,
        val isSavingProfile: Boolean = false,
        val isEditModalOpen: Boolean = false,
        val isDeleteAccountDialogOpen: Boolean = false,
        val isDeletingAccount: Boolean = false,
        val accountDeleted: Boolean = false,
        val actionMessage: String? = null,
        val actionError: String? = null
    ) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(
    private val repository: ProfileRepository = ProfileRepository()
) : ViewModel() {

    private val currentUserId: String?
        get() = supabaseClient.auth.currentUserOrNull()?.id

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val uid = currentUserId
        if (uid == null) {
            _state.value = ProfileUiState.Error(io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.error_no_session))
            return
        }
        _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            val profileDef = async { repository.getUserProfile(uid).getOrNull() ?: FullUserProfile(id = uid) }
            val premiumDef = async { repository.getPremiumStatus().getOrNull() ?: PremiumStatusResponse() }
            val statsDef = async { repository.getProfileStats().getOrNull() ?: io.lunosfer.dreamap.data.model.ProfileStatsResponse() }
            val dreamsDef = async { repository.getUserDreams(uid).getOrNull() ?: emptyList() }
            val visionsDef = async { repository.getUserVisions().getOrNull() ?: emptyList() }
            val savedDef = async { repository.getSavedVisions().getOrNull() ?: emptyList() }

            _state.value = ProfileUiState.Content(
                profile = profileDef.await(),
                premiumStatus = premiumDef.await(),
                stats = statsDef.await(),
                dreams = dreamsDef.await(),
                visions = visionsDef.await(),
                savedVisions = savedDef.await()
            )
        }
    }

    fun selectTab(index: Int) {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(selectedTab = index)
    }

    fun openEditModal() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(isEditModalOpen = true)
    }

    fun closeEditModal() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(isEditModalOpen = false)
    }

    fun updateProfile(
        username: String,
        displayName: String,
        avatarUrl: String,
        isPrivate: Boolean,
        profileVisibility: String = if (isPrivate) "private" else "public",
        language: String,
        gender: String
    ) {
        val uid = currentUserId ?: return
        val current = _state.value as? ProfileUiState.Content ?: return

        if (username.isNotBlank() && (username.length < 3 || username.length > 32)) {
            _state.value = current.copy(actionError = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_username_length))
            return
        }
        if (displayName.length > 60) {
            _state.value = current.copy(actionError = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_display_name_length))
            return
        }

        // Profil tamamen gizliyse (private) tüm paylaşımlar otomatik tamamen gizli
        // olmalı; bu iş kuralı sunucu tarafında da uygulanmalı ancak istemcide de
        // tutarlılık için isPrivate bayrağını profileVisibility ile senkron tutuyoruz.
        val resolvedVisibility = profileVisibility.ifBlank { if (isPrivate) "private" else "public" }
        val resolvedIsPrivate = resolvedVisibility == "private"

        _state.value = current.copy(isSavingProfile = true)

        val req = UpdateProfileRequest(
            userId = uid,
            username = username.trim().takeIf { it.isNotBlank() },
            displayName = displayName.trim().takeIf { it.isNotBlank() },
            avatarUrl = avatarUrl.trim().takeIf { it.isNotBlank() },
            isPrivate = resolvedIsPrivate,
            profileVisibility = resolvedVisibility,
            language = language.takeIf { it.isNotBlank() },
            gender = gender.takeIf { it.isNotBlank() }
        )

        viewModelScope.launch {
            repository.updateProfile(req).onSuccess { updatedProfile ->
                val latest = _state.value as? ProfileUiState.Content ?: return@onSuccess
                _state.value = latest.copy(
                    profile = updatedProfile,
                    isSavingProfile = false,
                    isEditModalOpen = false,
                    actionMessage = io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_success_updated)
                )
            }.onFailure { err ->
                val latest = _state.value as? ProfileUiState.Content ?: return@onFailure
                _state.value = latest.copy(
                    isSavingProfile = false,
                    actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_update_failed)
                )
            }
        }
    }

    fun openDeleteAccountDialog() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(isDeleteAccountDialogOpen = true)
    }

    fun closeDeleteAccountDialog() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(isDeleteAccountDialogOpen = false)
    }

    // Google Play "Hesap Silme" politikası gereği: kullanıcı hesabını ve
    // ilişkili verilerini kalıcı olarak siler (bkz. ProfileRepository.deleteAccount,
    // pages/api/account/delete.js). Başarılı olursa yerel oturum da kapatılır;
    // ekran bunu (accountDeleted=true) izleyip kullanıcıyı login/onboarding'e yönlendirir.
    fun deleteAccount() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(isDeletingAccount = true)
        viewModelScope.launch {
            repository.deleteAccount()
                .onSuccess {
                    try {
                        supabaseClient.auth.signOut()
                    } catch (_: Exception) {
                        // Hesap sunucuda zaten silindi; yerel signOut başarısız
                        // olsa bile kullanıcıyı login'e yönlendirmeye devam ediyoruz.
                    }
                    val latest = _state.value as? ProfileUiState.Content ?: return@onSuccess
                    _state.value = latest.copy(
                        isDeletingAccount = false,
                        isDeleteAccountDialogOpen = false,
                        accountDeleted = true
                    )
                }
                .onFailure { err ->
                    val latest = _state.value as? ProfileUiState.Content ?: return@onFailure
                    _state.value = latest.copy(
                        isDeletingAccount = false,
                        isDeleteAccountDialogOpen = false,
                        actionError = err.message ?: io.lunosfer.dreamap.DreamapApp.instance.getString(io.lunosfer.dreamap.R.string.profile_error_delete_account_failed)
                    )
                }
        }
    }

    fun clearActionMessage() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(actionMessage = null)
    }

    fun clearActionError() {
        val current = _state.value as? ProfileUiState.Content ?: return
        _state.value = current.copy(actionError = null)
    }
}
