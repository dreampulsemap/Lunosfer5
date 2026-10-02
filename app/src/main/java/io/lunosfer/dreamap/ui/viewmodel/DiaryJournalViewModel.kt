package io.lunosfer.dreamap.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.lunosfer.dreamap.DreamapApp
import io.lunosfer.dreamap.R
import io.lunosfer.dreamap.data.model.DiaryEntry
import io.lunosfer.dreamap.data.model.UserProfile
import io.lunosfer.dreamap.data.repository.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// groupedEntries anahtarı olarak "yyyy-MM-dd" (createdAt'ın cihazdaki yerel günü, bkz. localDayKey)
// veya tarih yoksa UNKNOWN_DATE_KEY kullanılır — bu ham anahtar hiçbir zaman
// doğrudan ekrana yazılmaz. İnsan-okur etikete ("Bugün"/"Dün"/"10 Ağustos
// 2026") çevirme işi DiaryJournalScreen'de, stringResource ile birlikte
// yapılır; ViewModel'in kendisi yalnızca Context gerektirmeyen hata
// mesajlarında DreamapApp.instance.getString(...) kullanır (ProfileViewModel
// ile aynı desen).
const val UNKNOWN_DATE_KEY = "unknown"

/**
 * Supabase zaman damgasini cihazin yerel gunune ("yyyy-MM-dd") cevirir.
 * java.time degil SimpleDateFormat: minSdk 24 ve core library desugaring kapali.
 */
internal fun localDayKey(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return UNKNOWN_DATE_KEY
    return try {
        val base = isoTimestamp
            .replace(Regex("[+-]\\d{2}:?\\d{2}$"), "")
            .removeSuffix("Z")
            .replace(' ', 'T')
            .substringBefore('.')
        val utc = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val date = utc.parse(base) ?: return isoTimestamp.take(10)
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(date)
    } catch (e: Exception) {
        isoTimestamp.take(10)
    }
}

sealed class DiaryJournalUiState {
    object Loading : DiaryJournalUiState()
    data class Error(val message: String) : DiaryJournalUiState()
    data class Success(
        val owner: UserProfile?,
        val groupedEntries: Map<String, List<DiaryEntry>>,
        val isSelf: Boolean,
        /** Silme isteginin sonucu; gosterildikten sonra [clearActionError] ile temizlenir. */
        val actionError: String? = null,
        /** Silinmekte olan kaydin id'si — o kartin butonu bloke edilir. */
        val deletingId: String? = null,
        /** Duzenlemesi kaydedilmekte olan kaydin id'si. */
        val savingId: String? = null
    ) : DiaryJournalUiState()
}

class DiaryJournalViewModel(
    private val userId: String,
    private val repository: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<DiaryJournalUiState>(DiaryJournalUiState.Loading)
    val state: StateFlow<DiaryJournalUiState> = _state.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch {
            _state.value = DiaryJournalUiState.Loading
            repository.getEntriesForUser(userId)
                .onSuccess { response ->
                    val sorted = response.entries.sortedByDescending { it.createdAt ?: "" }
                    // Cihazin yerel gunune gore: createdAt.take(10) UTC gunuydu ve
                    // Turkiye'de 00:00-03:00 arasi yazilan girdi onceki gune dusuyordu.
                    val grouped = sorted.groupBy { entry -> localDayKey(entry.createdAt) }
                    _state.value = DiaryJournalUiState.Success(
                        owner = response.owner,
                        groupedEntries = grouped,
                        isSelf = response.isSelf
                    )
                }
                .onFailure { error ->
                    val fallback = DreamapApp.instance.getString(R.string.diary_journal_load_error)
                    _state.value = DiaryJournalUiState.Error(error.message ?: fallback)
                }
        }
    }

    /** Kalici Gunce'den bir kaydi kalici olarak sil (yalnizca kendi kayitlarim).
     *  Listeyi yeniden cekmek yerine yerel olarak cikariyoruz: kullanici uzun bir
     *  arsivin ortasindayken kaydirma konumunu kaybetmesin. */
    fun deleteEntry(entryId: String) {
        val current = _state.value as? DiaryJournalUiState.Success ?: return
        if (!current.isSelf || current.deletingId != null) return

        _state.value = current.copy(deletingId = entryId, actionError = null)
        viewModelScope.launch {
            repository.deleteEntry(entryId)
                .onSuccess {
                    val latest = _state.value as? DiaryJournalUiState.Success ?: return@onSuccess
                    val pruned = latest.groupedEntries
                        .mapValues { (_, entries) -> entries.filterNot { it.id == entryId } }
                        .filterValues { it.isNotEmpty() }
                    _state.value = latest.copy(groupedEntries = pruned, deletingId = null)
                }
                .onFailure { error ->
                    val latest = _state.value as? DiaryJournalUiState.Success ?: return@onFailure
                    _state.value = latest.copy(
                        deletingId = null,
                        actionError = io.lunosfer.dreamap.util.safeMessage(error)
                            ?: DreamapApp.instance.getString(R.string.diary_journal_delete_failed)
                    )
                }
        }
    }

    /** Aciklama + gorunurluk duzenle; liste yerinde guncellenir (kaydirma konumu korunur). */
    fun updateEntry(entryId: String, caption: String, visibility: String, onDone: () -> Unit) {
        val current = _state.value as? DiaryJournalUiState.Success ?: return
        if (!current.isSelf || current.savingId != null) return
        _state.value = current.copy(savingId = entryId, actionError = null)
        viewModelScope.launch {
            repository.updateEntry(entryId, caption, visibility)
                .onSuccess { updated ->
                    val latest = _state.value as? DiaryJournalUiState.Success ?: return@onSuccess
                    val patched = latest.groupedEntries.mapValues { (_, entries) ->
                        entries.map { if (it.id == entryId) it.copy(caption = updated.caption, visibility = updated.visibility) else it }
                    }
                    _state.value = latest.copy(groupedEntries = patched, savingId = null)
                    onDone()
                }
                .onFailure { error ->
                    val latest = _state.value as? DiaryJournalUiState.Success ?: return@onFailure
                    _state.value = latest.copy(
                        savingId = null,
                        actionError = io.lunosfer.dreamap.util.safeMessage(error)
                            ?: DreamapApp.instance.getString(R.string.diary_journal_edit_failed)
                    )
                }
        }
    }

    fun clearActionError() {
        val current = _state.value as? DiaryJournalUiState.Success ?: return
        _state.value = current.copy(actionError = null)
    }

    class Factory(private val userId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiaryJournalViewModel(userId) as T
        }
    }
}
