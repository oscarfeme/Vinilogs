package app.vinilogs.core.model

/**
 * Local-to-remote sync status, both per record and as the collection-wide aggregate exposed by
 * `CollectionRepository.observeSyncState()` (02-ARCHITECTURE.md §2, FR-B11).
 */
enum class SyncState {
    SYNCED,
    PENDING,
    ERROR,
}
