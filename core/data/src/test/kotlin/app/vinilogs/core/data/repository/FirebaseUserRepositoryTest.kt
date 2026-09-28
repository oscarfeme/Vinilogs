package app.vinilogs.core.data.repository

import app.cash.turbine.test
import app.vinilogs.core.model.ProfileUpdate
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Date

class FirebaseUserRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val firebaseAuth = mockk<FirebaseAuth>()
    private val repository = FirebaseUserRepository(firestore, firebaseAuth)

    // ---- observeProfile ----

    @Test
    fun `observeProfile emits the mapped profile when the snapshot listener fires`() =
        runTest {
            val (documentRef, listenerSlot) = stubDocumentListener("uid-1")
            val snapshot = profileSnapshot(uid = "uid-1", displayName = "Alice", recordCount = 5L)

            repository.observeProfile("uid-1").test {
                listenerSlot.captured.onEvent(snapshot, null)
                val profile = awaitItem()

                assertEquals("uid-1", profile?.uid)
                assertEquals("Alice", profile?.displayName)
                assertEquals("A bio", profile?.bio)
                assertEquals("A city", profile?.location)
                assertTrue(profile?.isPublic == true)
                assertEquals(5, profile?.recordCount)
                assertEquals(Instant.EPOCH, profile?.createdAt)

                cancelAndIgnoreRemainingEvents()
            }
            verify { documentRef.addSnapshotListener(any<EventListener<DocumentSnapshot>>()) }
        }

    @Test
    fun `observeProfile emits null when the document doesn't exist`() =
        runTest {
            val (_, listenerSlot) = stubDocumentListener("uid-missing")
            val snapshot = mockk<DocumentSnapshot> { every { exists() } returns false }

            repository.observeProfile("uid-missing").test {
                listenerSlot.captured.onEvent(snapshot, null)
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `observeProfile emits null instead of throwing when the listener reports an error`() =
        runTest {
            val (_, listenerSlot) = stubDocumentListener("uid-1")
            val error = mockk<FirebaseFirestoreException>()

            repository.observeProfile("uid-1").test {
                listenerSlot.captured.onEvent(null, error)
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- updateProfile ----

    @Test
    fun `updateProfile merge-writes the editable fields under the signed-in uid`() =
        runTest {
            every { firebaseAuth.currentUser } returns mockk<FirebaseUser> { every { uid } returns "uid-1" }
            val documentRef = mockk<DocumentReference>()
            val collectionRef = mockk<CollectionReference> { every { document("uid-1") } returns documentRef }
            every { firestore.collection("users") } returns collectionRef
            val payloadSlot = slot<Map<String, Any?>>()
            val optionsSlot = slot<SetOptions>()
            every { documentRef.set(capture(payloadSlot), capture(optionsSlot)) } returns successfulTask(null)

            val result =
                repository.updateProfile(
                    ProfileUpdate(
                        displayName = "New Name",
                        avatarUrl = "https://example.com/avatar.jpg",
                        bio = "New bio",
                        location = "New city",
                        isPublic = false,
                    ),
                )

            assertTrue(result.isSuccess)
            assertEquals("New Name", payloadSlot.captured["displayName"])
            assertEquals("new name", payloadSlot.captured["displayNameLower"])
            assertEquals("https://example.com/avatar.jpg", payloadSlot.captured["avatarUrl"])
            assertEquals("New bio", payloadSlot.captured["bio"])
            assertEquals("New city", payloadSlot.captured["location"])
            assertEquals(false, payloadSlot.captured["isPublic"])
            // recordCount/createdAt must never be in the payload -- a non-merging write would wipe them.
            assertFalse(payloadSlot.captured.containsKey("recordCount"))
            assertFalse(payloadSlot.captured.containsKey("createdAt"))
        }

    @Test
    fun `updateProfile fails when nobody is signed in`() =
        runTest {
            every { firebaseAuth.currentUser } returns null

            val result =
                repository.updateProfile(
                    ProfileUpdate(displayName = "Name", avatarUrl = null, bio = null, location = null, isPublic = true),
                )

            assertTrue(result.isFailure)
        }

    // ---- not-yet-implemented Phase 2 methods (T-22) ----

    @Test
    fun `observePublicCollection throws NotImplementedError when collected`() {
        assertThrows(NotImplementedError::class.java) {
            runBlocking { repository.observePublicCollection("uid-1").first() }
        }
    }

    @Test
    fun `searchUsers returns a documented NotImplementedError failure`() =
        runTest {
            val result = repository.searchUsers("query", 0)
            assertTrue(result.exceptionOrNull() is NotImplementedError)
        }

    @Test
    fun `sharedRecords returns a documented NotImplementedError failure`() =
        runTest {
            val result = repository.sharedRecords("uid-1")
            assertTrue(result.exceptionOrNull() is NotImplementedError)
        }

    @Test
    fun `report returns a documented NotImplementedError failure`() =
        runTest {
            val result = repository.report("uid-1", "spam")
            assertTrue(result.exceptionOrNull() is NotImplementedError)
        }

    // ---- mapping ----

    @Test
    fun `toUserProfile defaults isPublic to true when the field is missing`() {
        val snapshot =
            mockk<DocumentSnapshot> {
                every { exists() } returns true
                every { id } returns "uid-2"
                every { getString("displayName") } returns "Bob"
                every { getString("avatarUrl") } returns null
                every { getString("bio") } returns null
                every { getString("location") } returns null
                every { getBoolean("isPublic") } returns null
                every { getLong("recordCount") } returns null
                every { getDate("createdAt") } returns null
            }

        val profile = snapshot.toUserProfile()

        assertEquals(true, profile?.isPublic)
        assertEquals(0, profile?.recordCount)
    }

    private fun stubDocumentListener(
        uid: String,
    ): Pair<DocumentReference, CapturingSlot<EventListener<DocumentSnapshot>>> {
        val documentRef = mockk<DocumentReference>()
        val collectionRef = mockk<CollectionReference> { every { document(uid) } returns documentRef }
        every { firestore.collection("users") } returns collectionRef
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        every { documentRef.addSnapshotListener(capture(listenerSlot)) } returns registration
        return documentRef to listenerSlot
    }

    private fun profileSnapshot(uid: String, displayName: String, recordCount: Long): DocumentSnapshot =
        mockk {
            every { exists() } returns true
            every { id } returns uid
            every { getString("displayName") } returns displayName
            every { getString("avatarUrl") } returns null
            every { getString("bio") } returns "A bio"
            every { getString("location") } returns "A city"
            every { getBoolean("isPublic") } returns true
            every { getLong("recordCount") } returns recordCount
            every { getDate("createdAt") } returns Date.from(Instant.EPOCH)
        }
}

/**
 * A [Task] mocked to look already-complete-and-successful, matching what
 * `kotlinx-coroutines-play-services`'s `Task<T>.await()` checks first (`isComplete` +
 * `exception == null`) so `.await()` returns [value] without needing a real Looper to dispatch
 * `addOnCompleteListener` -- same helper as `FirebaseAuthRepositoryTest`.
 */
private fun <T> successfulTask(value: T): Task<T> =
    mockk {
        every { isComplete } returns true
        every { isCanceled } returns false
        every { exception } returns null
        every { result } returns value
    }
