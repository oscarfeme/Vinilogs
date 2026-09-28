package app.vinilogs.feature.auth.editprofile

import android.net.Uri
import app.cash.turbine.test
import app.vinilogs.core.model.UserProfile
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeAuthRepository
import app.vinilogs.core.testing.fake.FakeAvatarUploader
import app.vinilogs.core.testing.fake.FakeUserRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class EditProfileViewModelTest {
    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())
    }

    private val profile = UserProfile(
        uid = "uid-1",
        displayName = "Alice",
        avatarUrl = "https://example.com/old.jpg",
        bio = "Collector",
        location = "London",
        isPublic = true,
        recordCount = 12,
        createdAt = Instant.EPOCH,
    )

    private suspend fun signedInFakes(): Triple<FakeAuthRepository, FakeUserRepository, FakeAvatarUploader> {
        val authRepository = FakeAuthRepository()
        val userRepository = FakeUserRepository(initialProfiles = listOf(profile))
        authRepository.signUp("alice@example.com", "password123", "Alice") // mints uid-1, matching the fixture
        return Triple(authRepository, userRepository, FakeAvatarUploader())
    }

    @Test
    fun `initial state prefills from the signed-in user's current profile`() = runTest {
        val (authRepository, userRepository, avatarUploader) = signedInFakes()
        val viewModel = EditProfileViewModel(authRepository, userRepository, avatarUploader)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("Alice", state.displayName)
            assertEquals("Collector", state.bio)
            assertEquals("London", state.location)
            assertTrue(state.isPublic)
            assertEquals("https://example.com/old.jpg", state.avatarUrl)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `saving a blank display name sets a field error without calling the repository`() = runTest {
        val (authRepository, userRepository, avatarUploader) = signedInFakes()
        val viewModel = EditProfileViewModel(authRepository, userRepository, avatarUploader)
        viewModel.onFieldChange { it.copy(displayName = "") }

        viewModel.onSaveClick()

        assertNotNull(viewModel.uiState.value.displayNameError)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `saving text fields updates the profile and emits saveSucceeded`() = runTest {
        val (authRepository, userRepository, avatarUploader) = signedInFakes()
        val viewModel = EditProfileViewModel(authRepository, userRepository, avatarUploader)
        viewModel.onFieldChange {
            it.copy(displayName = "Alice B", bio = "New bio", location = "Paris", isPublic = false)
        }

        viewModel.saveSucceeded.test {
            viewModel.onSaveClick()
            awaitItem()
        }

        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.generalError)
        val updated = userRepository.observeProfile("uid-1").first()
        assertEquals("Alice B", updated?.displayName)
        assertEquals("New bio", updated?.bio)
        assertEquals("Paris", updated?.location)
        assertFalse(updated?.isPublic == true)
        // avatarUrl carries over unchanged -- no new photo was picked in this test.
        assertEquals("https://example.com/old.jpg", updated?.avatarUrl)
    }

    @Test
    fun `saving with a newly picked photo uploads it first and saves the resolved URL`() = runTest {
        val (authRepository, userRepository, avatarUploader) = signedInFakes()
        val viewModel = EditProfileViewModel(authRepository, userRepository, avatarUploader)
        val pickedUri = mockk<Uri>()
        viewModel.onFieldChange { it.copy(pendingAvatarUri = pickedUri) }

        viewModel.saveSucceeded.test {
            viewModel.onSaveClick()
            awaitItem()
        }

        assertEquals(listOf("uid-1" to pickedUri), avatarUploader.uploads)
        val updated = userRepository.observeProfile("uid-1").first()
        assertEquals("https://fake-storage.example/avatars/uid-1.jpg", updated?.avatarUrl)
    }

    @Test
    fun `an avatar upload failure surfaces a general error and does not save`() = runTest {
        val (authRepository, userRepository, avatarUploader) = signedInFakes()
        avatarUploader.failNextCallWith(RuntimeException("upload failed"))
        val viewModel = EditProfileViewModel(authRepository, userRepository, avatarUploader)
        viewModel.onFieldChange { it.copy(pendingAvatarUri = mockk()) }

        viewModel.onSaveClick()

        assertNotNull(viewModel.uiState.value.generalError)
        assertFalse(viewModel.uiState.value.isSaving)
        val unchanged = userRepository.observeProfile("uid-1").first()
        assertEquals("https://example.com/old.jpg", unchanged?.avatarUrl)
    }

    @Test
    fun `a repository failure surfaces a general error, not a crash`() = runTest {
        val (authRepository, userRepository, avatarUploader) = signedInFakes()
        userRepository.failNextCallWith(RuntimeException("network down"))
        val viewModel = EditProfileViewModel(authRepository, userRepository, avatarUploader)

        viewModel.onSaveClick()

        assertNotNull(viewModel.uiState.value.generalError)
        assertFalse(viewModel.uiState.value.isSaving)
    }
}
