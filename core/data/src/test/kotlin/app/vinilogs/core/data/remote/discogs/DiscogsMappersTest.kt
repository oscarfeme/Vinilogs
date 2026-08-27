package app.vinilogs.core.data.remote.discogs

import app.vinilogs.core.model.CatalogResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiscogsMappersTest {
    @Test
    fun `splits Artist - Title on the first separator`() {
        val dto = discogsResult(title = "Miles Davis - Kind of Blue")

        val result = dto.toCatalogResult()

        assertEquals("Miles Davis", result.artist)
        assertEquals("Kind of Blue", result.title)
    }

    @Test
    fun `splits only on the first separator when the title itself contains one`() {
        val dto = discogsResult(title = "Artist - Title - Part Two")

        val result = dto.toCatalogResult()

        assertEquals("Artist", result.artist)
        assertEquals("Title - Part Two", result.title)
    }

    @Test
    fun `treats the whole string as the title with an empty artist when there is no separator`() {
        val dto = discogsResult(title = "Untitled")

        val result = dto.toCatalogResult()

        assertEquals("", result.artist)
        assertEquals("Untitled", result.title)
    }

    @Test
    fun `treats a missing title as an empty title with an empty artist`() {
        val dto = discogsResult(title = null)

        val result = dto.toCatalogResult()

        assertEquals("", result.artist)
        assertEquals("", result.title)
    }

    @Test
    fun `parses a numeric year string, and falls back to null for anything else`() {
        assertEquals(1969, discogsResult(year = "1969").toCatalogResult().year)
        assertEquals(null, discogsResult(year = null).toCatalogResult().year)
        assertEquals(null, discogsResult(year = "unknown").toCatalogResult().year)
    }

    @Test
    fun `uses the first label, or null when the list is missing or empty`() {
        assertEquals("Blue Note", discogsResult(label = listOf("Blue Note", "Impulse!")).toCatalogResult().label)
        assertEquals(null, discogsResult(label = emptyList()).toCatalogResult().label)
        assertEquals(null, discogsResult(label = null).toCatalogResult().label)
    }

    @Test
    fun `prefers cover_image over thumb, and falls back to thumb when cover_image is missing`() {
        val withBoth = discogsResult(coverImage = "cover.jpg", thumbnail = "thumb.jpg")
        assertEquals("cover.jpg", withBoth.toCatalogResult().coverUrl)

        val thumbOnly = discogsResult(coverImage = null, thumbnail = "thumb.jpg")
        assertEquals("thumb.jpg", thumbOnly.toCatalogResult().coverUrl)

        val neither = discogsResult(coverImage = null, thumbnail = null)
        assertEquals(null, neither.toCatalogResult().coverUrl)
    }

    @Test
    fun `maps id, catalogue number and cover through unchanged`() {
        val dto =
            discogsResult(
                id = 12345L,
                title = "Artist - Title",
                catalogNumber = "CAT-001",
                coverImage = "cover.jpg",
            )

        val result = dto.toCatalogResult()

        assertEquals(
            CatalogResult(
                discogsId = 12345L,
                artist = "Artist",
                title = "Title",
                year = null,
                label = null,
                catalogNumber = "CAT-001",
                coverUrl = "cover.jpg",
            ),
            result,
        )
    }

    private fun discogsResult(
        id: Long = 1L,
        title: String? = "Artist - Title",
        year: String? = null,
        label: List<String>? = null,
        catalogNumber: String? = null,
        coverImage: String? = null,
        thumbnail: String? = null,
    ): DiscogsSearchResult =
        DiscogsSearchResult(
            id = id,
            title = title,
            year = year,
            label = label,
            catalogNumber = catalogNumber,
            coverImage = coverImage,
            thumbnail = thumbnail,
        )
}
