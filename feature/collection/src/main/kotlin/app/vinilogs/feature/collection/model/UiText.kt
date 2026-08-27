package app.vinilogs.feature.collection.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * A deferred-localisation message, per 02-ARCHITECTURE.md §4's `ShelfUiState.error: UiText?`.
 *
 * Gap note (T-15): this type is referenced by the fixed `ShelfUiState` shape but doesn't exist
 * anywhere in the repo yet. It reads as UI-layer-shared infrastructure that belongs in
 * `core:designsystem` (or `core:model`) so `feature:auth`/`feature:discovery` can reuse it too,
 * but both modules are outside Track D's boundary (CLAUDE.md rule 2), so it is defined locally
 * here for now. Flagged in the T-15 PR as a follow-up for whichever track owns that module.
 */
sealed interface UiText {
    data class Raw(val value: String) : UiText

    data class Resource(
        @param:StringRes val resId: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}

/** Resolves a [UiText] to a localised [String] at composition time. */
@Suppress("SpreadOperator") // stringResource's vararg formatArgs signature leaves no other way to forward `args`.
@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Raw -> value
        is UiText.Resource -> stringResource(resId, *args.toTypedArray())
    }
