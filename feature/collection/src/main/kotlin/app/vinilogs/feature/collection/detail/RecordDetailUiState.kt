package app.vinilogs.feature.collection.detail

import app.vinilogs.core.model.Record

/**
 * Not a fixed contract from 02-ARCHITECTURE.md §4 (only `ShelfUiState` is specified verbatim
 * there) -- shaped freely for this task, per CLAUDE.md rule 7.
 *
 * [showUndo] and [permanentlyDeleted] implement FR-B5's "delete asks for confirmation and
 * supports undo for 5 seconds": [RecordDetailViewModel.delete] commits the delete to
 * [app.vinilogs.core.data.repository.CollectionRepository] immediately (optimistic, per ADR-2)
 * and keeps an in-memory snapshot; undo re-adds that snapshot. [permanentlyDeleted] flips once
 * the 5-second window elapses with no undo, at which point the screen navigates back.
 */
internal data class RecordDetailUiState(
    val record: Record? = null,
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val showUndo: Boolean = false,
    val permanentlyDeleted: Boolean = false,
)
