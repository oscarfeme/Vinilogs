package app.vinilogs.core.data.di

import app.vinilogs.core.data.repository.AuthRepository
import app.vinilogs.core.data.repository.CollectionRepository
import app.vinilogs.core.data.repository.FirebaseAuthRepository
import app.vinilogs.core.data.repository.RoomCollectionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds `core:data`'s fixed repository contracts (02-ARCHITECTURE.md §4) to their real implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindCollectionRepository(impl: RoomCollectionRepository): CollectionRepository
}
