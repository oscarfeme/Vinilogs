package app.vinilogs.feature.collection.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.vinilogs.core.designsystem.component.VinylCard
import app.vinilogs.core.designsystem.theme.spacing
import app.vinilogs.core.designsystem.theme.vinilogsColors
import app.vinilogs.core.model.Record
import app.vinilogs.feature.collection.component.ConditionChip
import app.vinilogs.feature.collection.component.MetadataRow
import java.time.Instant
import java.time.ZoneOffset

@Composable
internal fun RecordDetailBody(
    record: Record,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.spacing.screenHorizontal, vertical = MaterialTheme.spacing.lg),
    ) {
        // "Large cover" (FR-B9) -- the same VinylCard the shelf grid uses, just full-width
        // instead of a grid cell. See this task's PR notes on the shared-element transition gap.
        VinylCard(
            coverUrl = record.coverUrl,
            artist = record.artist,
            title = record.title,
            catalogNumber = record.catalogNumber,
            modifier = Modifier.fillMaxWidth(),
        )
        RecordHeader(record = record, modifier = Modifier.padding(top = MaterialTheme.spacing.lg))
        RecordMetadataTable(record = record, modifier = Modifier.padding(top = MaterialTheme.spacing.xl))
        // Captured into a local: Kotlin can't smart-cast a nullable public property declared in
        // a different module (core:model) even after an explicit null/blank check.
        val notes = record.notes
        if (!notes.isNullOrBlank()) {
            RecordNotes(notes = notes, modifier = Modifier.padding(top = MaterialTheme.spacing.xl))
        }
    }
}

@Composable
private fun RecordHeader(
    record: Record,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = record.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = record.artist,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.vinilogsColors.textSecondary,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xxs),
        )
        Row(modifier = Modifier.padding(top = MaterialTheme.spacing.sm)) {
            ConditionChip(abbreviation = record.condition.abbreviation())
        }
    }
}

@Composable
private fun RecordMetadataTable(
    record: Record,
    modifier: Modifier = Modifier,
) {
    val rows = record.metadataRows()
    Column(modifier = modifier) {
        rows.forEachIndexed { index, row ->
            MetadataRow(label = row.first, value = row.second, showDivider = index != rows.lastIndex)
        }
    }
}

@Composable
private fun RecordNotes(
    notes: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Notes",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.vinilogsColors.textTertiary,
        )
        Text(
            text = notes,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = MaterialTheme.spacing.xs),
        )
    }
}

private fun Record.metadataRows(): List<Pair<String, String>> =
    buildList {
        year?.let { add("Year" to it.toString()) }
        label?.let { add("Label" to it) }
        catalogNumber?.let { add("Catalogue number" to it) }
        add("Format" to format.displayLabel())
        add("Speed" to speed.rpmLabel())
        purchasePrice?.let { add("Purchase price" to it.asCurrency()) }
        purchaseDate?.let { add("Purchase date" to it.asDate()) }
        rating?.let { add("Your rating" to "$it/5") }
        if (tags.isNotEmpty()) add("Tags" to tags.joinToString(", "))
    }

private fun Double.asCurrency(): String = if (this == toLong().toDouble()) "$${toLong()}" else "$$this"

// LocalDate.ofInstant(Instant, ZoneId) needs API 34 -- minSdk here is 26, so go via
// Instant.atZone(...).toLocalDate() instead (available since API 26).
private fun Instant.asDate(): String = atZone(ZoneOffset.UTC).toLocalDate().toString()
