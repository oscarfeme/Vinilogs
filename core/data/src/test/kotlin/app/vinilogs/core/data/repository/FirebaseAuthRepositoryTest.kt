package app.vinilogs.core.data.repository

import app.cash.turbine.test
import app.vinilogs.core.model.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FirebaseAuthRepositoryTest {
    private val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>()
    private val repository = FirebaseAuthRepository(firebaseAuth, firestore)

    // ---- currentUser ----

    @Test
    fun `currentUser emits mapped domain user when the auth-state listener fires`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-1", email = "a@b.com", displayName = "Alice")
            val listenerSlot = slot<FirebaseAuth.AuthStateListener>()
            every { firebaseAuth.addAuthStateListener(capture(listenerSlot)) } returns Unit
            every { firebaseAuth.removeAuthStateListener(any()) } returns Unit

            repository.currentUser.test {
                every { firebaseAuth.currentUser } returns firebaseUser
                listenerSlot.captured.onAuthStateChanged(firebaseAuth)
                assertEquals(User(uid = "uid-1", email = "a@b.com", displayName = "Alice"), awaitItem())

                every { firebaseAuth.currentUser } returns null
                listenerSlot.captured.onAuthStateChanged(firebaseAuth)
                assertEquals(null, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- signUp ----

    @Test
    fun `signUp creates the account, updates the display name and writes the profile document`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-1", email = "new@user.com", displayName = null)
            val authResult = mockk<AuthResult> { every { user } returns firebaseUser }

            every {
                firebaseAuth.createUserWithEmailAndPassword("new@user.com", "password123")
            } returns successfulTask(authResult)

            val profileUpdateSlot = slot<UserProfileChangeRequest>()
            every { firebaseUser.updateProfile(capture(profileUpdateSlot)) } returns successfulTask(null)

            val documentRef = mockk<DocumentReference>()
            val collectionRef = mockk<CollectionReference> { every { document("uid-1") } returns documentRef }
            every { firestore.collection("users") } returns collectionRef
            val profileSlot = slot<Map<String, Any>>()
            every { documentRef.set(capture(profileSlot)) } returns successfulTask(null)

            val result = repository.signUp("new@user.com", "password123", "New User")

            assertTrue(result.isSuccess)
            assertEquals(User(uid = "uid-1", email = "new@user.com", displayName = "New User"), result.getOrNull())
            assertEquals("New User", profileSlot.captured["displayName"])
            assertEquals("new user", profileSlot.captured["displayNameLower"])
            assertEquals(true, profileSlot.captured["isPublic"])
            assertEquals(0, profileSlot.captured["recordCount"])
            assertTrue(profileSlot.captured.containsKey("createdAt"))
        }

    @Test
    fun `signUp wraps a Firebase failure as Result failure instead of throwing`() =
        runTest {
            val exception = FirebaseAuthInvalidCredentialsException("ERROR_INVALID_EMAIL", "bad email")
            every {
                firebaseAuth.createUserWithEmailAndPassword("bad", "password123")
            } returns failedTask(exception)

            val result = repository.signUp("bad", "password123", "Someone")

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // ---- signIn ----

    @Test
    fun `signIn maps the Firebase user to the domain user on success`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-2", email = "existing@user.com", displayName = "Existing")
            val authResult = mockk<AuthResult> { every { user } returns firebaseUser }
            every {
                firebaseAuth.signInWithEmailAndPassword("existing@user.com", "secret")
            } returns successfulTask(authResult)

            val result = repository.signIn("existing@user.com", "secret")

            assertTrue(result.isSuccess)
            assertEquals(User(uid = "uid-2", email = "existing@user.com", displayName = "Existing"), result.getOrNull())
        }

    @Test
    fun `signIn returns failure on wrong credentials`() =
        runTest {
            val exception = FirebaseAuthInvalidCredentialsException("ERROR_WRONG_PASSWORD", "wrong password")
            every {
                firebaseAuth.signInWithEmailAndPassword("existing@user.com", "wrong")
            } returns failedTask(exception)

            val result = repository.signIn("existing@user.com", "wrong")

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())
        }

    // ---- sendPasswordReset ----

    @Test
    fun `sendPasswordReset succeeds regardless of whether the address exists (FR-A3)`() =
        runTest {
            every { firebaseAuth.sendPasswordResetEmail("someone@user.com") } returns successfulTask(null)

            val result = repository.sendPasswordReset("someone@user.com")

            assertTrue(result.isSuccess)
        }

    // ---- signOut ----

    @Test
    fun `signOut delegates to FirebaseAuth`() =
        runTest {
            repository.signOut()

            verify { firebaseAuth.signOut() }
        }

    // ---- deleteAccount ----

    @Test
    fun `deleteAccount deletes the signed-in Firebase user`() =
        runTest {
            val firebaseUser = firebaseUser(uid = "uid-3", email = "gone@user.com", displayName = "Gone")
            every { firebaseAuth.currentUser } returns firebaseUser
            every { firebaseUser.delete() } returns successfulTask(null)

            val result = repository.deleteAccount()

            assertTrue(result.isSuccess)
        }

    @Test
    fun `deleteAccount fails when nobody is signed in`() =
        runTest {
            every { firebaseAuth.currentUser } returns null

            val result = repository.deleteAccount()

            assertFalse(result.isSuccess)
        }

    // ---- mapping ----

    @Test
    fun `toDomainUser falls back to empty strings for null email and display name`() {
        val firebaseUser = firebaseUser(uid = "uid-4", email = null, displayName = null)

        val user = firebaseUser.toDomainUser()

        assertEquals(User(uid = "uid-4", email = "", displayName = ""), user)
    }
}

// Test-only helpers below, kept as top-level functions rather than class members so the test
// class's own function count stays under detekt's TooManyFunctions threshold -- it's ten
// @Test methods plus setUp, which is the actual signal that rule is checking for.

private fun firebaseUser(uid: String, email: String?, displayName: String?): FirebaseUser =
    mockk<FirebaseUser> {
        every { this@mockk.uid } returns uid
        every { this@mockk.email } returns email
        every { this@mockk.displayName } returns displayName
    }

/**
 * A [Task] mocked to look already-complete-and-successful, matching what
 * `kotlinx-coroutines-play-services`'s `Task<T>.await()` checks first (`isComplete` +
 * `exception == null`) so `.await()` returns [value] without needing a real Looper to
 * dispatch `addOnCompleteListener`.
 */
private fun <T> successfulTask(value: T): Task<T> =
    mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns null
        every { result } returns value
    }

private fun <T> failedTask(exception: Exception): Task<T> =
    mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { this@mockk.exception } returns exception
    }
