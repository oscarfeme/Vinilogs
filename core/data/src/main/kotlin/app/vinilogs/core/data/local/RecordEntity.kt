package app.vinilogs.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room's on-disk representation of a [app.vinilogs.core.model.Record] (T-10,
 * 02-ARCHITECTURE.md §3). A deliberately distinct type from the domain model -- see
 * [app.vinilogs.core.data.local.toDomain]/[app.vinilogs.core.data.local.toEntity] for the
 * mapping boundary; this type never leaves `core:data`.
 *
 * Columns use plain SQL-friendly types (`String` for enums, epoch-millis `Long` for instants,
 * a comma-delimited `String` for tags) rather than relying on Room `TypeConverter`s to make the
 * schema self-explanatory when inspected directly (e.g. via `adb shell sqlite3`).
 *
 * [syncState] is the local-only sync-status column ADR-2 calls for -- Room is the source of
 * truth, and this column tracks whether a row still needs to reach Firestore. `SyncWorker`
 * lands in T-11; the column exists now so T-11 doesn't need a schema migration to add it.
 */
@Entity(tableName = "records")
data class RecordEntity(
    @PrimaryKey
    val id: String,
    val artist: String,
    val title: String,
    val year: Int?,
    val label: String?,
    val catalogNumber: String?,
    /** [app.vinilogs.core.model.Format]`.name`. */
    val format: String,
    /** [app.vinilogs.core.model.Speed]`.name`. */
    val speed: String,
    /** [app.vinilogs.core.model.Condition]`.name`. */
    val condition: String,
    val purchasePrice: Double?,
    /** Epoch millis, or null. */
    val purchaseDate: Long?,
    val rating: Int?,
    val notes: String?,
    val coverUrl: String?,
    val discogsId: Long?,
    /** Comma-delimited, wrapped in leading/trailing commas (`,jazz,favorite,`) so `LIKE
     *  '%,tag,%'` can match a single tag without partial-word false positives. Empty when
     *  there are no tags. See [tagsToColumn]/[tagsFromColumn].
     */
    val tags: String,
    /** [app.vinilogs.core.model.SyncState]`.name` -- local-only, see the class doc. */
    val syncState: String,
    /** Epoch millis. */
    val createdAt: Long,
    /** Epoch millis. */
    val updatedAt: Long,
)
