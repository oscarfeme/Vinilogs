package app.vinilogs.core.model

/** Goldmine-style grading scale (FR-B4: M/NM/VG+/VG/G/F/P), best to worst. */
enum class Condition {
    MINT,
    NEAR_MINT,
    VERY_GOOD_PLUS,
    VERY_GOOD,
    GOOD,
    FAIR,
    POOR,
}
