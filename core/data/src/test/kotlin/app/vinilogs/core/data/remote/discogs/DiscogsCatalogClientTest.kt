package app.vinilogs.core.data.remote.discogs

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

/**
 * Exercises [DiscogsCatalogClient]'s request-building and response-mapping/error-handling logic
 * against a real (local) HTTP server, rather than the real Discogs API -- no live API key
 * exists for this project yet (`00-README.md`/`firebase/README.md`-adjacent gap, but for
 * Discogs specifically: nobody has requested a personal access token).
 */
class DiscogsCatalogClientTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val server = MockWebServer()
    private lateinit var client: DiscogsCatalogClient

    @BeforeEach
    fun setUp() {
        server.start()
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
        client = DiscogsCatalogClient(retrofit.create(DiscogsApi::class.java), apiKey = "test-token")
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `builds the search request with query, page and the api token`() =
        runTest {
            server.enqueue(MockResponse().setBody("""{"results":[]}"""))

            client.searchCatalog(query = "kind of blue", page = 2)

            val request = server.takeRequest()
            val expectedPath = "/database/search?q=kind%20of%20blue&page=2&per_page=20&type=release&token=test-token"
            assertEquals("GET", request.method)
            assertEquals(expectedPath, request.path)
        }

    @Test
    fun `maps a successful response to CatalogResult`() =
        runTest {
            server.enqueue(
                MockResponse().setBody(
                    """{"results":[{"id":123,"title":"Miles Davis - Kind of Blue","year":"1959"}]}""",
                ),
            )

            val result = client.searchCatalog(query = "kind of blue", page = 1)

            assertTrue(result.isSuccess)
            val catalogResult = result.getOrThrow().single()
            assertEquals(123L, catalogResult.discogsId)
            assertEquals("Miles Davis", catalogResult.artist)
            assertEquals("Kind of Blue", catalogResult.title)
            assertEquals(1959, catalogResult.year)
        }

    @Test
    fun `does not send a request and fails with MissingApiKey when the key is blank`() =
        runTest {
            val clientWithoutKey = DiscogsCatalogClient(retrofitApi(), apiKey = "")

            val result = clientWithoutKey.searchCatalog(query = "anything", page = 1)

            assertEquals(DiscogsFailure.MissingApiKey, result.exceptionOrNull())
            assertEquals(0, server.requestCount)
        }

    @Test
    fun `maps HTTP 429 to RateLimited`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(HTTP_TOO_MANY_REQUESTS))

            val result = client.searchCatalog(query = "anything", page = 1)

            assertEquals(DiscogsFailure.RateLimited, result.exceptionOrNull())
        }

    @Test
    fun `maps HTTP 404 to NotFound`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(HTTP_NOT_FOUND))

            val result = client.searchCatalog(query = "anything", page = 1)

            assertEquals(DiscogsFailure.NotFound, result.exceptionOrNull())
        }

    @Test
    fun `maps any other non-2xx response to a typed Http failure carrying the status code`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(HTTP_SERVER_ERROR))

            val result = client.searchCatalog(query = "anything", page = 1)

            val failure = result.exceptionOrNull()
            assertTrue(failure is DiscogsFailure.Http)
            assertEquals(HTTP_SERVER_ERROR, (failure as DiscogsFailure.Http).code)
        }

    @Test
    fun `maps a connection failure to Network`() =
        runTest {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            val result = client.searchCatalog(query = "anything", page = 1)

            assertTrue(result.exceptionOrNull() is DiscogsFailure.Network)
        }

    @Test
    fun `unknown JSON fields in the response are ignored rather than failing the whole request`() =
        runTest {
            val bodyWithUnknownFields =
                """{"results":[{"id":1,"title":"A - B","some_new_field":{"nested":true}}],"pagination":{"page":1}}"""
            server.enqueue(MockResponse().setBody(bodyWithUnknownFields))

            val result = client.searchCatalog(query = "anything", page = 1)

            assertTrue(result.isSuccess)
            assertNull(result.getOrThrow().single().year)
        }

    private fun retrofitApi(): DiscogsApi =
        Retrofit
            .Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(DiscogsApi::class.java)

    private companion object {
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_NOT_FOUND = 404
        const val HTTP_SERVER_ERROR = 500
    }
}
