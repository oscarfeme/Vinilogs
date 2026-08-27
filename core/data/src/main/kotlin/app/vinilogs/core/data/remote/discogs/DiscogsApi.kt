package app.vinilogs.core.data.remote.discogs

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Discogs' public "database search" endpoint (FR-B1, ADR-3) -- catalogue lookup only, never a
 * sync source. https://www.discogs.com/developers#page:database,header:database-search .
 *
 * Retrofit interface only: request/response shapes are [DiscogsSearchResponse] and friends,
 * mapped to the domain [app.vinilogs.core.model.CatalogResult] by [DiscogsCatalogClient] --
 * this interface and its DTOs never leave the `remote.discogs` package.
 */
internal interface DiscogsApi {
    @GET("database/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int = DEFAULT_PAGE_SIZE,
        @Query("type") type: String = "release",
        @Query("token") token: String,
    ): DiscogsSearchResponse

    private companion object {
        const val DEFAULT_PAGE_SIZE = 20
    }
}
