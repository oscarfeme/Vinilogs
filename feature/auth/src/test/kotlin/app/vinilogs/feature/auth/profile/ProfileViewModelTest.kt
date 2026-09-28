package app.vinilogs.feature.auth.profile

import app.cash.turbine.test
import app.vinilogs.core.model.UserProfile
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeAuthRepository
import app.vinilogs.core.testing.fake.FakeUserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())
    }

    private val profile = UserProfile(
        uid = "uid-1",
        displayName = "Alice",
        avatarUrl = null,
        bio = "Collector",
        location = "London",
        isPublic = true,
        recordCount = 12,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `signed out shows no profile and is not loading`() = runTest {
        val authRepository = FakeAuthRepository()
        val userRepository = FakeUserRepository(initialProfiles = listOf(profile))
        val viewModel = ProfileViewModel(authRepository, userRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.profile)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `signed in emits the signed-in user's profile`() = runTest {
        val authRepository = FakeAuthRepository()
        val userRepository = FakeUserRepository(initialProfiles = listOf(profile))
        authRepository.signUp("alice@example.com", "password123", "Alice")
        // FakeAuthRepository.signUp mints its own uid ("uid-1" for the first account), which
        // lines up with the fixture above -- FakeUserRepository has no other notion of "the
        // signed-in user" than this shared uid.
        val viewModel = ProfileViewModel(authRepository, userRepository)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.profile != null)
            assertFalse(state.isLoading)
        }
    }
}
