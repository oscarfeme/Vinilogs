package app.vinilogs.core.data.repository

import app.vinilogs.core.data.local.RecordLocalDataSource
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.model.Condition
import app.vinilogs.core.model.Format
import app.vinilogs.core.model.Record
import app.vinilogs.core.model.Speed
import app.vinilogs.core.model.SyncState
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

class RoomCollectionRepositoryTest {
    private val localDataSource = mockk<RecordLocalDataSource>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>()
    private val firebaseAuth = mockk<FirebaseAuth>()
    private val repository = RoomCollectionRepository(localDataSource, firestore, firebaseAuth)

    // ---- reads delegate straight to Room (02-ARCHITECTURE.md §1: no network wait) ----

    @Test
    fun `observeCollection delegates to the local data source`() {
        val flow = flowOf(listOf(fixture(id = "r1")))
        every { localDataSource.observeCollection(CollectionFilter(), CollectionSort.ARTIST) } returns flow

        assertEquals(flow, repository.observeCollection(CollectionFilter(), CollectionSort.ARTIST))
    }

    @Test
    fun `observeRecord delegates to the local data source`() {
        val flow = flowOf(fixture(id = "r1"))
        every { localDataSource.observeRecord("r1") } returns flow

        assertEquals(flow, repository.observeRecord("r1"))
    }

    @Test
    fun `observeSyncState delegates to the local data source`() {
        val flow = flowOf(SyncState.PENDING)
        every { localDataSource.observeSyncState() } returns flow

        assertEquals(flow, repository.observeSyncState())
    }

    // ---- addRecord: Room first, Firestore best-effort ----

    @Test
    fun `addRecord saves PENDING to Room, then flips to SYNCED once Firestore succeeds`() =
        runTest {
            withSignedInUser(uid = "uid-1")
            val documentRef = collectionReturning("uid-1")
            every { documentRef.set(any()) } returns successfulTask(null)

            val result = repository.addRecord(fixture(id = "r1"))

            assertTrue(result.isSuccess)
            assertEquals("r1", result.getOrNull())
            coVerify(ordering = io.mockk.Ordering.ORDERED) {
                localDataSource.save(match { it.id == "r1" && it.syncState == SyncState.PENDING })
                localDataSource.save(match { it.id == "r1" && it.syncState == SyncState.SYNCED })
            }
        }

    @Test
    fun `addRecord assigns a random id when none is given`() =
        runTest {
            every { firebaseAuth.currentUser } returns null

            val result = repository.addRecord(fixture(id = ""))

            assertTrue(result.isSuccess)
            assertTrue(result.getOrNull()?.isNotBlank() == true)
        }

    @Test
    fun `addRecord leaves the row PENDING when the Firestore write fails, but still succeeds`() =
        runTest {
            withSignedInUser(uid = "uid-1")
            val documentRef = collectionReturning("uid-1")
            every { documentRef.set(any()) } returns failedTask(IOException("offline"))

            val result = repository.addRecord(fixture(id = "r1"))

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { localDataSource.save(match { it.syncState == SyncState.PENDING }) }
            coVerify(exactly = 0) { localDataSource.save(match { it.syncState == SyncState.SYNCED }) }
        }

    @Test
    fun `addRecord skips the Firestore attempt entirely when nobody is signed in`() =
        runTest {
            every { firebaseAuth.currentUser } returns null

            val result = repository.addRecord(fixture(id = "r1"))

            assertTrue(result.isSuccess)
            coVerify(exactly = 1) { localDataSource.save(any()) }
            verify(exactly = 0) { firestore.collection(any()) }
        }

    // ---- updateRecord ----

    @Test
    fun `updateRecord saves PENDING then SYNCED on a successful Firestore write`() =
        runTest {
            withSignedInUser(uid = "uid-1")
            val documentRef = collectionReturning("uid-1")
            every { documentRef.set(any()) } returns successfulTask(null)

            val result = repository.updateRecord(fixture(id = "r1"))

            assertTrue(result.isSuccess)
            coVerify { localDataSource.save(match { it.syncState == SyncState.SYNCED }) }
        }

    // ---- deleteRecord ----

    @Test
    fun `deleteRecord deletes locally and succeeds even if the remote delete fails`() =
        runTest {
            withSignedInUser(uid = "uid-1")
            val documentRef = collectionReturning("uid-1")
            every { documentRef.delete() } returns failedTask(IOException("offline"))

            val result = repository.deleteRecord("r1")

            assertTrue(result.isSuccess)
            coVerify { localDataSource.delete("r1") }
        }

    @Test
    fun `deleteRecord fails when the local delete itself throws`() =
        runTest {
            every { firebaseAuth.currentUser } returns null
            coEvery { localDataSource.delete("r1") } throws IllegalStateException("db closed")

            val result = repository.deleteRecord("r1")

            assertTrue(result.isFailure)
        }

    // ---- not-yet-implemented surfaces (T-13/T-12/T-27) ----

    @Test
    fun `setCoverImage, searchCatalog and exportCsv are not yet implemented`() =
        runTest {
            assertTrue(repository.setCoverImage("r1", mockk(relaxed = true)).isFailure)
            assertTrue(repository.searchCatalog("query", 1).isFailure)
            assertTrue(repository.exportCsv().isFailure)
        }

    private fun withSignedInUser(uid: String) {
        val firebaseUser = mockk<FirebaseUser> { every { this@mockk.uid } returns uid }
        every { firebaseAuth.currentUser } returns firebaseUser
    }

    private fun collectionReturning(uid: String): DocumentReference {
        val documentRef = mockk<DocumentReference>()
        val recordsCollection = mockk<CollectionReference> { every { document(any()) } returns documentRef }
        val userDoc = mockk<DocumentReference> { every { collection("records") } returns recordsCollection }
        val usersCollection = mockk<CollectionReference> { every { document(uid) } returns userDoc }
        every { firestore.collection("users") } returns usersCollection
        return documentRef
    }

    private fun fixture(id: String): Record {
        val now = Instant.ofEpochSecond(1_700_000_000L)
        return Record(
            id = id,
            artist = "Artist",
            title = "Title",
            year = 2000,
            label = null,
            catalogNumber = null,
            format = Format.LP,
            speed = Speed.RPM33,
            condition = Condition.MINT,
            purchasePrice = null,
            purchaseDate = null,
            rating = null,
            notes = null,
            coverUrl = null,
            discogsId = null,
            tags = emptyList(),
            syncState = SyncState.SYNCED,
            createdAt = now,
            updatedAt = now,
        )
    }

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
}
