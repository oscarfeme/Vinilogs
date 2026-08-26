package app.vinilogs.core.testing.fake

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FakeAuthRepositoryTest {
    @Test
    fun `starts signed out`() = runTest {
        val repository = FakeAuthRepository()

        repository.currentUser.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `signUp signs the user in and publishes it on currentUser`() = runTest {
        val repository = FakeAuthRepository()

        val result = repository.signUp("new@example.com", "password123", "New User")

        assertTrue(result.isSuccess)
        assertEquals("new@example.com", result.getOrThrow().email)
        repository.currentUser.test {
            assertEquals("new@example.com", awaitItem()?.email)
        }
    }

    @Test
    fun `signUp with a duplicate email fails`() = runTest {
        val repository = FakeAuthRepository()
        repository.signUp("dup@example.com", "password123", "First")

        val result = repository.signUp("dup@example.com", "different", "Second")

        assertTrue(result.isFailure)
    }

    @Test
    fun `signIn with the wrong password fails`() = runTest {
        val repository = FakeAuthRepository()
        repository.signUp("user@example.com", "correct", "User")

        val result = repository.signIn("user@example.com", "wrong")

        assertTrue(result.isFailure)
    }

    @Test
    fun `signOut clears currentUser`() = runTest {
        val repository = FakeAuthRepository()
        repository.signUp("user@example.com", "password123", "User")

        repository.signOut()

        repository.currentUser.test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `failNextCallWith makes the next call fail with the given error`() = runTest {
        val repository = FakeAuthRepository()
        val error = IllegalStateException("simulated network error")
        repository.failNextCallWith(error)

        val result = repository.signUp("user@example.com", "password123", "User")

        assertEquals(error, result.exceptionOrNull())
    }
}
