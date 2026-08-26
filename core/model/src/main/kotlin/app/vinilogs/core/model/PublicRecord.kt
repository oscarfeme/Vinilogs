package app.vinilogs.core.model

/**
 * The `publicRecords` projection another user sees on someone's profile — shareable fields
 * only. A separate type from [Record] so it is structurally impossible for a discovery screen
 * to render a private field (02-ARCHITECTURE.md §3–4).
 */
data class PublicRecord(
    val id: String,
    val artist: String,
    val title: String,
    val year: Int?,
    val label: String?,
    val catalogNumber: String?,
    val format: Format,
    val speed: Speed,
    val condition: Condition,
    val coverUrl: String?,
    val tags: List<String>,
)
