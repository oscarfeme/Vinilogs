package app.vinilogs.feature.collection.model

/**
 * Shelf display mode (FR-B6: "Grid of covers and list view toggle").
 *
 * Gap note (T-15): referenced by the fixed `ShelfUiState` shape in 02-ARCHITECTURE.md §4 but
 * not defined anywhere in `core:model` yet -- see the note on [app.vinilogs.feature.collection.model.UiText]
 * for why it lives here instead.
 */
enum class ShelfLayout {
    GRID,
    LIST,
}
