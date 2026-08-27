package app.vinilogs.core.data.di

import android.content.Context
import app.vinilogs.core.data.BuildConfig
import app.vinilogs.core.data.remote.discogs.DiscogsApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.create
import java.io.File
import javax.inject.Singleton

/**
 * Discogs Retrofit client wiring (T-12). `DiscogsApi` and
 * `app.vinilogs.core.data.remote.discogs.DiscogsCatalogClient` are both `internal` --
 * implementation details of `core:data` that T-11's `CollectionRepository` implementation
 * (same module) delegates to; only `DiscogsFailure` (public) is meant to be visible outside.
 */
@Module
@InstallIn(SingletonComponent::class)
object DiscogsModule {
    private const val BASE_URL = "https://api.discogs.com/"
    private const val CACHE_SIZE_BYTES = 10L * 1024 * 1024 // 10 MiB
    private const val CACHE_MAX_AGE_SECONDS = 24 * 60 * 60 // 24h, per this task's explicit ask

    // Discogs requires a descriptive User-Agent identifying the app on every request --
    // https://www.discogs.com/developers#page:home,header:home-general-information ("Please
    // don't use the default user agents of your programming language/toolset").
    private const val USER_AGENT = "Vinilogs/1.0 +https://github.com/oscarfeme/Vinilogs"

    @Provides
    @DiscogsApiKey
    fun provideDiscogsApiKey(): String = BuildConfig.DISCOGS_API_KEY

    @Provides
    @Singleton
    fun provideDiscogsJson(): Json = Json { ignoreUnknownKeys = true }

    /**
     * A network interceptor rewriting every response's `Cache-Control` to a 24h max-age: this
     * task calls for a 24h OkHttp response cache specifically, and Discogs' own responses don't
     * reliably send caching headers that would otherwise make [Cache] a no-op.
     */
    private val forceCacheInterceptor =
        Interceptor { chain ->
            val response = chain.proceed(chain.request())
            response
                .newBuilder()
                .header("Cache-Control", "public, max-age=$CACHE_MAX_AGE_SECONDS")
                .build()
        }

    private val userAgentInterceptor =
        Interceptor { chain ->
            val requestWithUserAgent = chain
                .request()
                .newBuilder()
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(requestWithUserAgent)
        }

    @Provides
    @Singleton
    fun provideDiscogsOkHttpClient(
        @ApplicationContext context: Context,
    ): OkHttpClient =
        OkHttpClient
            .Builder()
            .cache(Cache(File(context.cacheDir, "discogs-http-cache"), CACHE_SIZE_BYTES))
            .addInterceptor(userAgentInterceptor)
            .addNetworkInterceptor(forceCacheInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideDiscogsRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    internal fun provideDiscogsApi(retrofit: Retrofit): DiscogsApi = retrofit.create()
}
