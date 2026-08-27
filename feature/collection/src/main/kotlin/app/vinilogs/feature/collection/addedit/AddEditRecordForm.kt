package app.vinilogs.feature.collection.addedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.vinilogs.core.designsystem.component.CoverPlaceholder
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Speed
import app.vinilogs.feature.collection.component.CollectionTextField
import app.vinilogs.feature.collection.component.OptionalChipRow
import app.vinilogs.feature.collection.component.RequiredChipRow

private val COVER_PREVIEW_SIZE = 96.dp
private const val MIN_RATING = 1
private const val MAX_RATING = 5
private val RATING_OPTIONS = (MIN_RATING..MAX_RATING).toList()

@Composable
internal fun AddEditRecordForm(
    uiState: AddEditRecordUiState,
    onDraftChange: ((RecordDraft) -> RecordDraft) -> Unit,
    onSave: () -> Unit,
) {
    val draft = uiState.draft

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.screenHorizontal, vertical = MaterialTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.lg),
    ) {
        CoverPreview(catalogNumber = draft.catalogNumber)
        IdentityFields(uiState = uiState, onChange = onDraftChange)
        PhysicalFields(draft = draft, onChange = onDraftChange)
        OwnershipFields(uiState = uiState, onChange = onDraftChange)
        NotesAndTagsFields(draft = draft, onChange = onDraftChange)
        SaveSection(uiState = uiState, onSave = onSave)
    }
}

@Composable
private fun CoverPreview(catalogNumber: String) {
    Column {
        CoverPlaceholder(
            catalogNumber = catalogNumber.ifBlank { null },
            modifier = Modifier.size(COVER_PREVIEW_SIZE),
        )
        Text(
            // FR-B13 (gallery/camera cover pick) is T-13's scope, not this task's -- see PR notes.
            text = "Cover photo -- coming soon",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.vinilogsColors.textTertiary,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
        )
    }
}

@Composable
private fun IdentityFields(
    uiState: AddEditRecordUiState,
    onChange: ((RecordDraft) -> RecordDraft) -> Unit,
) {
    val draft = uiState.draft
    val errors = uiState.errors
    CollectionTextField(
        value = draft.artist,
        onValueChange = { v -> onChange { it.copy(artist = v) } },
        label = "Artist",
        placeholder = "e.g. Miles Davis",
        isError = errors.artist != null,
        supportingText = errors.artist,
    )
    CollectionTextField(
        value = draft.title,
        onValueChange = { v -> onChange { it.copy(title = v) } },
        label = "Title",
        placeholder = "e.g. Kind of Blue",
        isError = errors.title != null,
        supportingText = errors.title,
    )
    CollectionTextField(
        value = draft.year,
        onValueChange = { v -> onChange { it.copy(year = v) } },
        label = "Year",
        keyboardType = KeyboardType.Number,
        isError = errors.year != null,
        supportingText = errors.year,
    )
    CollectionTextField(value = draft.label, onValueChange = { v -> onChange { it.copy(label = v) } }, label = "Label")
    CollectionTextField(
        value = draft.catalogNumber,
        onValueChange = { v -> onChange { it.copy(catalogNumber = v) } },
        label = "Catalogue number",
    )
}

@Composable
private fun PhysicalFields(
    draft: RecordDraft,
    onChange: ((RecordDraft) -> RecordDraft) -> Unit,
) {
    FieldLabel("Format")
    RequiredChipRow(options = Format.entries, selected = draft.format, onSelect = { v ->
        onChange { it.copy(format = v) }
    }, label = { it.label() })
    FieldLabel("Speed")
    RequiredChipRow(options = Speed.entries, selected = draft.speed, onSelect = { v ->
        onChange { it.copy(speed = v) }
    }, label = { it.rpmLabel() })
    FieldLabel("Condition")
    RequiredChipRow(
        options = Condition.entries,
        selected = draft.condition,
        onSelect = { v -> onChange { it.copy(condition = v) } },
        label = { it.abbreviation() },
    )
}

@Composable
private fun OwnershipFields(
    uiState: AddEditRecordUiState,
    onChange: ((RecordDraft) -> RecordDraft) -> Unit,
) {
    val draft = uiState.draft
    val errors = uiState.errors
    CollectionTextField(
        value = draft.purchasePrice,
        onValueChange = { v -> onChange { it.copy(purchasePrice = v) } },
        label = "Purchase price",
        keyboardType = KeyboardType.Decimal,
        isError = errors.purchasePrice != null,
        supportingText = errors.purchasePrice,
    )
    CollectionTextField(
        value = draft.purchaseDate,
        onValueChange = { v -> onChange { it.copy(purchaseDate = v) } },
        label = "Purchase date",
        placeholder = "YYYY-MM-DD",
        isError = errors.purchaseDate != null,
        supportingText = errors.purchaseDate,
    )
    FieldLabel("Your rating")
    OptionalChipRow(
        options = RATING_OPTIONS,
        selected = draft.rating,
        onSelect = { v -> onChange { it.copy(rating = v) } },
        label = { it.toString() },
    )
}

@Composable
private fun NotesAndTagsFields(
    draft: RecordDraft,
    onChange: ((RecordDraft) -> RecordDraft) -> Unit,
) {
    CollectionTextField(
        value = draft.notes,
        onValueChange = { v -> onChange { it.copy(notes = v) } },
        label = "Notes",
        singleLine = false,
    )
    CollectionTextField(
        value = draft.tags,
        onValueChange = { v -> onChange { it.copy(tags = v) } },
        label = "Tags",
        placeholder = "comma, separated, tags",
        imeAction = ImeAction.Done,
    )
}

@Composable
private fun SaveSection(
    uiState: AddEditRecordUiState,
    onSave: () -> Unit,
) {
    Button(onClick = onSave, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
        Text(if (uiState.isSaving) "Saving..." else "Save")
    }
    if (uiState.saveError != null) {
        Text(
            text = uiState.saveError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = MaterialTheme.spacing.sm),
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.vinilogsColors.textTertiary,
    )
}
