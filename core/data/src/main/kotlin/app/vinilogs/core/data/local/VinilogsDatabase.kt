package app.vinilogs.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's single Room database (ADR-2: Room is the source of truth for the user's own
 * collection). One table so far ([RecordEntity]) -- T-11 injects this via Hilt once the real
 * `CollectionRepository` lands.
 *
 * `exportSchema = false`: no migration path exists yet at version 1 (nothing to diff schema
 * history against), and enabling it needs a schema-location dir wired into the build --
 * revisit once a real migration (version 2+) is needed.
 */
@Database(entities = [RecordEntity::class], version = 1, exportSchema = false)
abstract class VinilogsDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao
}
