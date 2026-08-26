package app.vinilogs.di

import app.vinilogs.core.data.repository.AuthRepository
import app.vinilogs.core.model.User
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

/**
 * TEMPORARY. T-08 (Track C) has not landed yet, so no real, Firebase-backed
 * `AuthRepository` exists anywhere in the repo, and `core:data` has no Hilt
 * `@Binds`/`@Provides` for the interface either -- there is currently nothing
 * for `AuthStateViewModel`'s real (non-test) `@Inject AuthRepository` to
 * resolve to. This module exists solely to keep the `app` module's Hilt graph
 * compiling and runnable in the meantime.
 *
 * [PlaceholderAuthRepository] always reports signed-out and fails every
 * mutating call with a clear message -- which is, right now, an accurate
 * description of reality: no Firebase project exists yet either
 * (CLAUDE.md "Known gotchas" / `firebase/README.md`), so there is no backend
 * for real sign-in to reach even if this module tried to call one.
 *
 * Delete this whole file the moment T-08 adds a real binding in `core:data` --
 * Hilt will fail fast with a duplicate-binding error if both exist at once,
 * which is the intended safety net.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaceholderAuthModule {
    @Provides
    @Singleton
    fun provideAuthRepository(): AuthRepository = PlaceholderAuthRepository
}

private object PlaceholderAuthRepository : AuthRepository {
    override val currentUser: Flow<User?> = flowOf(null)

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> =
        Result.failure(unavailable())

    override suspend fun signIn(email: String, password: String): Result<User> = Result.failure(unavailable())

    override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.failure(unavailable())

    override suspend fun signOut() = Unit

    override suspend fun deleteAccount(): Result<Unit> = Result.failure(unavailable())

    private fun unavailable() =
        IllegalStateException(
            "Sign-in isn't available yet -- Vinilogs' backend hasn't been connected. " +
                "See CLAUDE.md's Firebase gotcha and T-08.",
        )
}
