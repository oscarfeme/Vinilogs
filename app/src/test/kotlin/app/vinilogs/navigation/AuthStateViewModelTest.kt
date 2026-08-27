package app.vinilogs.navigation

import app.cash.turbine.test
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeAuthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class AuthStateViewModelTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())
    }

    @Test
    fun `reports SignedOut while AuthRepository currentUser is null`() = runTest {
        val viewModel = AuthStateViewModel(FakeAuthRepository())

        viewModel.authState.test {
            assertEquals(AuthState.SignedOut, awaitItem())
        }
    }

    @Test
    fun `reports SignedIn once AuthRepository currentUser emits a user`() = runTest {
        val authRepository = FakeAuthRepository()
        val viewModel = AuthStateViewModel(authRepository)

        viewModel.authState.test {
            assertEquals(AuthState.SignedOut, awaitItem())

            authRepository.signUp("user@example.com", "password123", "User")

            assertEquals(AuthState.SignedIn, awaitItem())
        }
    }

    @Test
    fun `reports SignedOut again after sign out`() = runTest {
        val authRepository = FakeAuthRepository()
        authRepository.signUp("user@example.com", "password123", "User")
        val viewModel = AuthStateViewModel(authRepository)

        viewModel.authState.test {
            assertEquals(AuthState.SignedIn, awaitItem())

            authRepository.signOut()

            assertEquals(AuthState.SignedOut, awaitItem())
        }
    }
}
