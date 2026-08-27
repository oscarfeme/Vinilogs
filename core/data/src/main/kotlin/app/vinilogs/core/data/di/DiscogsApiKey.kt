package app.vinilogs.core.data.di

import javax.inject.Qualifier

/** Qualifies the injected Discogs personal-access-token `String` (from `BuildConfig`, T-12). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DiscogsApiKey
