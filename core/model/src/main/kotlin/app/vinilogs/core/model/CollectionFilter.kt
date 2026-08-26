package app.vinilogs.core.model

/**
 * Active shelf filters — format, condition, decade, minimum rating and tag (FR-B8). All null
 * by default, i.e. no filter applied.
 */
data class CollectionFilter(
    val format: Format? = null,
    val condition: Condition? = null,
    val decade: Int? = null,
    val minRating: Int? = null,
    val tag: String? = null,
)
