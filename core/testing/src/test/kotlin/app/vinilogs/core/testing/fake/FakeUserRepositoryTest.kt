package app.vinilogs.core.testing.fake

import app.cash.turbine.test
import app.vinilogs.core.model.ProfileUpdate
import app.vinilogs.core.model.UserProfile
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FakeUserRepositoryTest {
    private fun profile(uid: String, displayName: String, isPublic: Boolean = true) = UserProfile(
        uid = uid,
        displayName = displayName,
        avatarUrl = null,
        bio = null,
        location = null,
        isPublic = isPublic,
        recordCount = 0,
        createdAt = Instant.EPOCH,
    )

    @Test
    fun `searchUsers matches display name case-insensitively`() = runTest {
        val repository = FakeUserRepository(
            initialProfiles = listOf(profile("uid-1", "Alice Vinyl"), profile("uid-2", "Bob Beats")),
        )

        val result = repository.searchUsers("alice", page = 0)

        assertEquals(listOf("Alice Vinyl"), result.getOrThrow().map { it.displayName })
    }

    @Test
    fun `searchUsers excludes private profiles`() = runTest {
        val repository = FakeUserRepository(
            initialProfiles = listOf(profile("uid-1", "Private Pete", isPublic = false)),
        )

        val result = repository.searchUsers("Pete", page = 0)

        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `updateProfile edits the active user and is visible via observeProfile`() = runTest {
        val repository = FakeUserRepository(initialProfiles = listOf(profile("uid-1", "Old Name")))
        repository.activeUid = "uid-1"

        val result = repository.updateProfile(
            ProfileUpdate(displayName = "New Name", avatarUrl = null, bio = "hi", location = null, isPublic = true),
        )

        assertTrue(result.isSuccess)
        repository.observeProfile("uid-1").test {
            assertEquals("New Name", awaitItem()?.displayName)
        }
    }

    @Test
    fun `report records the reason for assertions`() = runTest {
        val repository = FakeUserRepository()

        repository.report("uid-2", "spam")

        assertEquals(listOf("uid-2" to "spam"), repository.reports)
    }
}
