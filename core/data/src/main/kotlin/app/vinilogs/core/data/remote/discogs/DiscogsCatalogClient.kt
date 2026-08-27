package app.vinilogs.core.data.remote.discogs

import app.vinilogs.core.data.di.DiscogsApiKey
import app.vinilogs.core.model.CatalogResult
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * Discogs catalogue lookup client (T-12, FR-B1). Matches
 * `CollectionRepository.searchCatalog(query, page)`'s signature exactly so T-11 can delegate to
 * this directly; debounce is the call site's job (T-16), not this client's.
 *
 * Every failure is caught and returned as `Result.failure` wrapping a [DiscogsFailure], never
 * thrown across this class's boundary -- see that type's doc for why (typed failure states so
 * "add manually" can always be offered instead of a crash).
 *
 * `internal`: like [DiscogsApi], this is an implementation detail of `core:data`. Nothing
 * outside this module should call it directly -- T-11's `CollectionRepository` implementation
 * (same module) is meant to delegate to it, and [DiscogsFailure] (public) is what propagates out
 * through `CollectionRepository.searchCatalog`'s `Result`.
 */
internal class DiscogsCatalogClient
    @Inject
    constructor(
        private val api: DiscogsApi,
        @param:DiscogsApiKey private val apiKey: String,
    ) {
        // Catching Exception broadly is the point here, not an oversight: every failure this call
        // can throw (HttpException, IOException, or anything else) must become a typed
        // DiscogsFailure so this never crashes a caller -- CancellationException is caught and
        // re-thrown in its own branch first, so structured concurrency is unaffected.
        @Suppress("TooGenericExceptionCaught")
        suspend fun searchCatalog(query: String, page: Int): Result<List<CatalogResult>> {
            if (apiKey.isBlank()) return Result.failure(DiscogsFailure.MissingApiKey)

            return try {
                val response = api.search(query = query, page = page, token = apiKey)
                Result.success(response.results.map { it.toCatalogResult() })
            } catch (cancellation: CancellationException) {
                // Structured concurrency: a cancelled coroutine must keep propagating cancellation,
                // never get reinterpreted as a normal Result.failure.
                throw cancellation
            } catch (e: Exception) {
                Result.failure(e.toDiscogsFailure())
            }
        }
    }

private fun Exception.toDiscogsFailure(): DiscogsFailure =
    when (this) {
        is DiscogsFailure -> this
        is HttpException ->
            when (code()) {
                HTTP_TOO_MANY_REQUESTS -> DiscogsFailure.RateLimited
                HTTP_NOT_FOUND -> DiscogsFailure.NotFound
                else -> DiscogsFailure.Http(code(), message())
            }
        is IOException -> DiscogsFailure.Network(this)
        else -> DiscogsFailure.Http(code = -1, message = message)
    }

private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_NOT_FOUND = 404
