package app.vinilogs.core.testing.fake

import app.vinilogs.core.data.repository.AuthRepository
import app.vinilogs.core.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [AuthRepository] fake. Starts signed out; [signUp]/[signIn] update [currentUser]
 * without touching Firebase. Call [failNextCallWith] to make the next mutating call return a
 * failure, for testing error paths.
 */
class FakeAuthRepository : AuthRepository {
    private data class StoredAccount(val user: User, val password: String)

    private val accountsByEmail = mutableMapOf<String, StoredAccount>()
    private val currentUserFlow = MutableStateFlow<User?>(null)
    private var nextFailure: Throwable? = null

    override val currentUser: Flow<User?> = currentUserFlow

    fun failNextCallWith(error: Throwable) {
        nextFailure = error
    }

    private fun <T> consumeFailureOr(onSuccess: () -> T): Result<T> {
        val failure = nextFailure
        if (failure != null) {
            nextFailure = null
            return Result.failure(failure)
        }
        return runCatching(onSuccess)
    }

    override suspend fun signUp(email: String, password: String, displayName: String): Result<User> =
        consumeFailureOr {
            check(email !in accountsByEmail) { "Duplicate email: $email" }
            val user = User(uid = "uid-${accountsByEmail.size + 1}", email = email, displayName = displayName)
            accountsByEmail[email] = StoredAccount(user, password)
            currentUserFlow.value = user
            user
        }

    override suspend fun signIn(email: String, password: String): Result<User> =
        consumeFailureOr {
            val account = requireNotNull(accountsByEmail[email]) { "No account for $email" }
            check(account.password == password) { "Wrong password for $email" }
            currentUserFlow.value = account.user
            account.user
        }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = consumeFailureOr { }

    override suspend fun signOut() {
        currentUserFlow.value = null
    }

    override suspend fun deleteAccount(): Result<Unit> =
        consumeFailureOr {
            val uid = currentUserFlow.value?.uid
            accountsByEmail.entries.removeAll { it.value.user.uid == uid }
            currentUserFlow.value = null
        }
}
