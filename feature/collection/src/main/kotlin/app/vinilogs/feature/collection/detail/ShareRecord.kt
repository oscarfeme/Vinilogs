package app.vinilogs.feature.collection.detail

import android.content.Context
import android.content.Intent
import app.vinilogs.core.model.Record

/**
 * FR-C5: "Share action... produces a text summary plus cover image via the Android share
 * sheet." No in-app messaging (ADR-6) -- this only ever launches the system chooser.
 *
 * Text-only for now: attaching the cover image needs a `content://` URI, which means
 * downloading [Record.coverUrl] to a cache file and serving it through a `FileProvider` --
 * that requires declaring a provider + `res/xml/file_paths.xml` in the `:app` manifest, outside
 * `feature:collection`'s boundary (CLAUDE.md rule 2), and there's no image-download utility in
 * `core:data`/`core:designsystem` yet to build the bitmap from. Flagged in the PR as a
 * cross-track follow-up once T-13's cover pipeline (which already needs the same download path)
 * lands.
 */
internal fun buildShareIntent(record: Record): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, record.shareSummary())
    }

internal fun shareRecord(
    context: Context,
    record: Record,
) {
    val chooser = Intent.createChooser(buildShareIntent(record), record.title)
    context.startActivity(chooser)
}

internal fun Record.shareSummary(): String {
    val lines =
        buildList {
            add("$artist -- $title")
            year?.let { add("$it") }
            label?.let { add(it) }
        }
    return lines.joinToString("\n")
}
