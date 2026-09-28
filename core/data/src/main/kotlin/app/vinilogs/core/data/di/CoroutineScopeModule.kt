package app.vinilogs.core.data.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Marks the process-lifetime [CoroutineScope] singletons use to launch work that outlives any
 * one screen -- currently only `RoomCollectionRepository`'s Firestore -> Room inbound sync
 * (ADR-2: "Firestore listeners write into Room"), which has to keep running for as long as the
 * app process does, not just while a particular ViewModel's `viewModelScope` is alive.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/** Provides [ApplicationScope]. */
@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {
    /**
     * `SupervisorJob` so one listener failing doesn't cancel unrelated work sharing this scope;
     * `Dispatchers.IO` because everything launched here is Firestore listener callbacks and Room
     * writes, not CPU-bound work.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
