package app.vinilogs.core.data.remote.discogs

/**
 * Typed failure states for [DiscogsCatalogClient.searchCatalog], per T-12's explicit scope: so
 * a caller (T-16's add-record search flow) can render "rate-limited" or "offline" as a specific
 * message with "add manually" as the primary action (02-ARCHITECTURE.md's data-flow example),
 * rather than treating every failure as an opaque crash.
 */
sealed class DiscogsFailure : Exception() {
    /** No `discogs.apiKey` in `local.properties` -- the request was never sent. */
    object MissingApiKey : DiscogsFailure()

    /** HTTP 429. Discogs' documented rate limit is 60 req/min (25 unauthenticated). */
    object RateLimited : DiscogsFailure()

    /** HTTP 404, or a search with zero results is treated as "not found" by the caller if it chooses to. */
    object NotFound : DiscogsFailure()

    /** No connectivity, timeout, DNS failure, etc. -- an [java.io.IOException] before any HTTP response arrived. */
    data class Network(override val cause: Throwable) : DiscogsFailure()

    /** Any other non-2xx HTTP response. */
    data class Http(val code: Int, override val message: String?) : DiscogsFailure()
}
