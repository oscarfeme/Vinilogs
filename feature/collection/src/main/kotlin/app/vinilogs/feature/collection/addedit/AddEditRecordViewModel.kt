package app.vinilogs.feature.collection.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.CollectionRepository
import app.vinilogs.core.model.Record
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Backs both `AddRecordScreen` and `EditRecordScreen` (T-17) -- one ViewModel, one form, per
 * `RECORD_ID_ARG` being present or absent in [savedStateHandle] (populated automatically by
 * Navigation Compose's type-safe `EditRecordRoute(recordId)` / absent for the no-arg
 * `AddRecordRoute`). Works fully offline (FR-B3/FR-B11): every read/write here goes through
 * [CollectionRepository], which is Room-backed (ADR-2) -- no network call anywhere in this
 * class, so the save path is unaffected by aeroplane mode.
 */
@HiltViewModel
internal class AddEditRecordViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val collectionRepository: CollectionRepository,
    ) : ViewModel() {
        private val recordId: String? = savedStateHandle.get<String>(RECORD_ID_ARG)
        private val mode = if (recordId == null) AddEditMode.ADD else AddEditMode.EDIT

        /** The record as last loaded from the repository, for [save] to preserve `discogsId`/`createdAt` from. */
        private var loadedRecord: Record? = null

        private val _uiState = MutableStateFlow(AddEditRecordUiState(mode = mode, isLoading = mode == AddEditMode.EDIT))
        val uiState: StateFlow<AddEditRecordUiState> = _uiState.asStateFlow()

        init {
            if (recordId != null) loadExistingRecord(recordId)
        }

        private fun loadExistingRecord(id: String) {
            viewModelScope.launch {
                // A single fetch, not a live collection: an open edit form shouldn't be
                // clobbered mid-type by a background sync updating the same record.
                val record = collectionRepository.observeRecord(id).firstOrNull()
                if (record != null) {
                    loadedRecord = record
                    _uiState.update { it.copy(draft = record.toDraft(), isLoading = false) }
                } else {
                    _uiState.update { it.copy(isLoading = false, notFound = true) }
                }
            }
        }

        fun updateDraft(transform: (RecordDraft) -> RecordDraft) {
            _uiState.update { it.copy(draft = transform(it.draft), errors = RecordDraftErrors(), saveError = null) }
        }

        fun save() {
            val draft = _uiState.value.draft
            val errors = draft.validate()
            if (!errors.isValid) {
                _uiState.update { it.copy(errors = errors) }
                return
            }
            persist(draft)
        }

        private fun persist(draft: RecordDraft) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSaving = true, saveError = null) }
                val record =
                    draft.toRecord(
                        id = recordId.orEmpty(),
                        discogsId = loadedRecord?.discogsId,
                        createdAt = loadedRecord?.createdAt ?: Instant.now(),
                    )
                val result =
                    if (recordId == null) {
                        collectionRepository.addRecord(record).map { }
                    } else {
                        collectionRepository.updateRecord(record)
                    }
                result.fold(
                    onSuccess = { _uiState.update { it.copy(isSaving = false, saved = true) } },
                    onFailure = { throwable ->
                        _uiState.update {
                            it.copy(isSaving = false, saveError = throwable.message ?: "Couldn't save this record.")
                        }
                    },
                )
            }
        }

        private companion object {
            const val RECORD_ID_ARG = "recordId"
        }
    }
