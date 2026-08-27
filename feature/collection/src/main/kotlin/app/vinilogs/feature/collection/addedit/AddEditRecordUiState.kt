package app.vinilogs.feature.collection.addedit

/**
 * Whether this screen instance is creating a new record (`AddRecordRoute`) or editing an
 * existing one (`EditRecordRoute`).
 */
internal enum class AddEditMode {
    ADD,
    EDIT,
}

/**
 * Not a fixed contract from 02-ARCHITECTURE.md §4 (only `ShelfUiState` is specified verbatim
 * there) -- shaped freely for this task, per CLAUDE.md rule 7.
 */
internal data class AddEditRecordUiState(
    val mode: AddEditMode = AddEditMode.ADD,
    val draft: RecordDraft = RecordDraft(),
    val errors: RecordDraftErrors = RecordDraftErrors(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val saved: Boolean = false,
    val notFound: Boolean = false,
)
