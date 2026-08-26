package app.vinilogs.core.data.repository

import app.vinilogs.core.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Fixed contract per 02-ARCHITECTURE.md §4 — implement against this so feature agents can
 * work in parallel with core:testing's fakes. Real implementation lands in T-08.
 */
interface AuthRepository {
    val currentUser: Flow<User?>

    suspend fun signUp(email: String, password: String, displayName: String): Result<User>

    suspend fun signIn(email: String, password: String): Result<User>

    suspend fun sendPasswordReset(email: String): Result<Unit>

    suspend fun signOut()

    suspend fun deleteAccount(): Result<Unit>
}
