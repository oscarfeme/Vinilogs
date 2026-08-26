package app.vinilogs.core.model

/** Aggregate collection stats shown on the Stats screen (FR-B10). */
data class CollectionStats(
    val totalRecords: Int,
    val totalSpend: Double,
    val recordsByDecade: Map<Int, Int>,
    val topArtists: List<NameCount>,
    val topLabels: List<NameCount>,
)

/** One entry in a top-N-by-count list (artist or label). */
data class NameCount(
    val name: String,
    val count: Int,
)
