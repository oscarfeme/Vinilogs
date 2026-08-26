package app.vinilogs.feature.collection.navigation

import kotlinx.serialization.Serializable

// Route shapes match the nav graph in 02-ARCHITECTURE.md §5:
//   shelf ──→ recordDetail/{id} ──→ editRecord/{id}
//        ├──→ addRecord (search | manual)
//        └──→ stats

/** Nested graph for the "Shelf" bottom-bar tab — [ShelfRoute] is its start destination. */
@Serializable
data object ShelfGraphRoute

@Serializable
data object ShelfRoute

@Serializable
data class RecordDetailRoute(val recordId: String)

@Serializable
data class EditRecordRoute(val recordId: String)

/**
 * "addRecord (search | manual)" is one screen with two entry modes (catalogue
 * search per FR-B1/T-16, manual entry per FR-B3/T-17) rather than two routes —
 * the diagram shows one node with two variants, not a branch.
 */
@Serializable
data object AddRecordRoute

@Serializable
data object StatsRoute
