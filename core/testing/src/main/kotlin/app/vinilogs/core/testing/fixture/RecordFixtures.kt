package app.vinilogs.core.testing.fixture

import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.Speed
import app.vinilogs.core.model.SyncState
import java.time.Instant
import kotlin.random.Random

/**
 * A realistic, deterministic ~200-record fixture -- the same records every run (fixed seed),
 * so assertions on counts/filters/sorts stay stable across test runs and CI machines.
 */
object RecordFixtures {
    private val artists = listOf(
        "Miles Davis",
        "John Coltrane",
        "Fleetwood Mac",
        "Stevie Wonder",
        "Radiohead",
        "Pink Floyd",
        "The Beatles",
        "David Bowie",
        "Joni Mitchell",
        "Nina Simone",
        "Herbie Hancock",
        "Talking Heads",
        "Kraftwerk",
        "Aretha Franklin",
        "Bill Evans",
        "The Velvet Underground",
        "Brian Eno",
        "Alice Coltrane",
        "Sade",
        "Portishead",
    )
    private val labels = listOf(
        "Blue Note",
        "Columbia",
        "Atlantic",
        "Warner Bros.",
        "EMI",
        "Impulse!",
        "Motown",
        "4AD",
        "ECM",
        "Verve",
    )
    private val tagPool = listOf("jazz", "soul", "rock", "electronic", "favorite", "sealed", "reissue", "gift")

    /** [count] deterministic records, seeded with [seed] -- the same output every call. */
    fun records(count: Int = 200, seed: Long = 20260101L): List<Record> {
        val random = Random(seed)
        return (1..count).map { index ->
            val artist = artists.random(random)
            val createdAt = Instant.ofEpochSecond(1_700_000_000L + index * 3600L)
            val rating = if (random.nextBoolean()) random.nextInt(1, 6) else null
            Record(
                id = "fixture-$index",
                artist = artist,
                title = "$artist Session $index",
                year = random.nextInt(1955, 2024),
                label = labels.random(random),
                catalogNumber = "CAT-${1000 + index}",
                format = Format.entries.random(random),
                speed = Speed.entries.random(random),
                condition = Condition.entries.random(random),
                purchasePrice = if (random.nextBoolean()) random.nextInt(5, 80).toDouble() else null,
                purchaseDate = if (random.nextBoolean()) createdAt else null,
                rating = rating,
                notes = null,
                coverUrl = null,
                discogsId = if (random.nextBoolean()) random.nextLong(1L, 999_999L) else null,
                tags = tagPool.shuffled(random).take(random.nextInt(0, 3)),
                syncState = SyncState.SYNCED,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
        }
    }
}
