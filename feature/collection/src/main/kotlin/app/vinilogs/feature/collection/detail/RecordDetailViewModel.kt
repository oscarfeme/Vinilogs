package app.vinilogs.feature.collection.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vinilogs.core.data.repository.CollectionRepository
import app.vinilogs.core.model.Record
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs [RecordDetailScreen] (T-18, FR-B5/FR-B9). Observes
 * [CollectionRepository.observeRecord] -- Room-backed (ADR-2), so this never waits on network
 * to show the user their own record, own or edited.
 */
@HiltViewModel
internal class RecordDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val collectionRepository: CollectionRepository,
    ) : ViewModel() {
        private val recordId: String = requireNotNull(savedStateHandle.get<String>(RECORD_ID_ARG))

        private val deletionState = MutableStateFlow(DeletionState.NONE)
        private var deletedSnapshot: Record? = null
        private var undoJob: Job? = null

        val uiState: StateFlow<RecordDetailUiState> =
            combine(collectionRepository.observeRecord(recordId), deletionState) { record, deletion ->
                deletion.toUiState(record)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                initialValue = RecordDetailUiState(),
            )

        /**
         * FR-B5: delete is committed immediately (optimistic, ADR-2), not deferred until the
         * undo window elapses -- the snapshot kept here is what [undoDelete] re-adds if the
         * user changes their mind within 5 seconds.
         */
        fun delete() {
            val current = uiState.value.record ?: return
            deletedSnapshot = current
            viewModelScope.launch {
                collectionRepository.deleteRecord(current.id)
                deletionState.value = DeletionState.PENDING_UNDO
                undoJob =
                    launch {
                        delay(UNDO_WINDOW_MS)
                        deletionState.value = DeletionState.COMMITTED
                    }
            }
        }

        fun undoDelete() {
            val snapshot = deletedSnapshot ?: return
            undoJob?.cancel()
            viewModelScope.launch {
                collectionRepository.addRecord(snapshot)
                deletionState.value = DeletionState.NONE
                deletedSnapshot = null
            }
        }

        private enum class DeletionState {
            NONE,
            PENDING_UNDO,
            COMMITTED,
            ;

            fun toUiState(record: Record?): RecordDetailUiState =
                when {
                    record != null -> RecordDetailUiState(record = record, isLoading = false)
                    this == PENDING_UNDO -> RecordDetailUiState(isLoading = false, showUndo = true)
                    this == COMMITTED -> RecordDetailUiState(isLoading = false, permanentlyDeleted = true)
                    else -> RecordDetailUiState(isLoading = false, notFound = true)
                }
        }

        private companion object {
            const val RECORD_ID_ARG = "recordId"
            const val SUBSCRIPTION_TIMEOUT_MS = 5_000L
            const val UNDO_WINDOW_MS = 5_000L
        }
    }
