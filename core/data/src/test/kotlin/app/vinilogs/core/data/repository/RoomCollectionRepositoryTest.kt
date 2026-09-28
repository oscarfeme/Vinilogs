package app.vinilogs.core.data.repository

import app.vinilogs.core.data.local.RecordLocalDataSource
import app.vinilogs.core.data.remote.discogs.DiscogsCatalogClient
import app.vinilogs.core.model.CatalogResult
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
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.CapturingSlot
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class RoomCollectionRepositoryTest {
    private val localDataSource = mockk<RecordLocalDataSource>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>()
    private val firebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    private val discogsCatalogClient = mockk<DiscogsCatalogClient>()

    // Firestore->Room sync (ADR-2) is wired up in RoomCollectionRepository's init block and
    // driven by an auth-state listener -- UnconfinedTestDispatcher makes that run synchronously
    // on this thread instead of on a real background dispatcher, so every test below stays
    // deterministic without needing to advance virtual time.
    private val syncScope = TestScope(UnconfinedTestDispatcher())
    private val authListenerSlot = slot<FirebaseAuth.AuthStateListener>()
    private val repository: RoomCollectionRepository

    init {
        // Default: no existing local row for any id, unless a test overrides it for a specific
        // id -- keeps applyRemoteChange's PENDING guard from needing a stub in every test.
        every { localDataSource.observeRecord(any()) } returns flowOf(null)
        every { firebaseAuth.addAuthStateListener(capture(authListenerSlot)) } returns Unit
        repository =
            RoomCollectionRepository(localDataSource, firestore, firebaseAuth, discogsCatalogClient, syncScope)
    }

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
            coVerify(ordering = Ordering.ORDERED) {
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

    // ---- searchCatalog: delegates to DiscogsCatalogClient (T-12) ----

    @Test
    fun `searchCatalog delegates to DiscogsCatalogClient and returns its result as-is`() =
        runTest {
            val results = listOf(catalogResultFixture())
            coEvery { discogsCatalogClient.searchCatalog("Kind of Blue", 1) } returns Result.success(results)

            val result = repository.searchCatalog("Kind of Blue", 1)

            assertEquals(Result.success(results), result)
        }

    @Test
    fun `searchCatalog surfaces a DiscogsCatalogClient failure as-is`() =
        runTest {
            val failure = IOException("offline")
            coEvery { discogsCatalogClient.searchCatalog("query", 1) } returns Result.failure(failure)

            val result = repository.searchCatalog("query", 1)

            assertTrue(result.isFailure)
            assertEquals(failure, result.exceptionOrNull())
        }

    // ---- not-yet-implemented surfaces (T-13/T-27) ----

    @Test
    fun `setCoverImage and exportCsv are not yet implemented`() =
        runTest {
            assertTrue(repository.setCoverImage("r1", mockk(relaxed = true)).isFailure)
            assertTrue(repository.exportCsv().isFailure)
        }

    // ---- Firestore -> Room inbound sync (ADR-2) ----

    @Test
    fun `an ADDED remote change upserts the mapped record into Room as SYNCED`() =
        runTest {
            val handle = captureSnapshotListener("uid-1")
            signIn("uid-1")

            val change = documentChange(DocumentChange.Type.ADDED, remoteDocumentSnapshot(id = "r1"))
            handle.listenerSlot.captured.onEvent(querySnapshotOf(change), null)

            coVerify { localDataSource.save(match { it.id == "r1" && it.syncState == SyncState.SYNCED }) }
        }

    @Test
    fun `a MODIFIED remote change upserts the mapped record into Room`() =
        runTest {
            val handle = captureSnapshotListener("uid-1")
            signIn("uid-1")

            val change =
                documentChange(
                    DocumentChange.Type.MODIFIED,
                    remoteDocumentSnapshot(id = "r1", artist = "Updated Artist"),
                )
            handle.listenerSlot.captured.onEvent(querySnapshotOf(change), null)

            coVerify { localDataSource.save(match { it.id == "r1" && it.artist == "Updated Artist" }) }
        }

    @Test
    fun `a REMOVED remote change deletes the local row`() =
        runTest {
            val handle = captureSnapshotListener("uid-1")
            signIn("uid-1")

            val change = documentChange(DocumentChange.Type.REMOVED, remoteDocumentSnapshot(id = "r1"))
            handle.listenerSlot.captured.onEvent(querySnapshotOf(change), null)

            coVerify { localDataSource.delete("r1") }
        }

    @Test
    fun `an incoming snapshot does not clobber a record that is still PENDING locally`() =
        runTest {
            val handle = captureSnapshotListener("uid-1")
            signIn("uid-1")
            every { localDataSource.observeRecord("r1") } returns
                flowOf(fixture(id = "r1").copy(syncState = SyncState.PENDING))

            val change = documentChange(DocumentChange.Type.MODIFIED, remoteDocumentSnapshot(id = "r1"))
            handle.listenerSlot.captured.onEvent(querySnapshotOf(change), null)

            coVerify(exactly = 0) { localDataSource.save(match { it.id == "r1" }) }
        }

    @Test
    fun `a SYNCED local record is overwritten normally by an incoming snapshot`() =
        runTest {
            val handle = captureSnapshotListener("uid-1")
            signIn("uid-1")
            every { localDataSource.observeRecord("r1") } returns
                flowOf(fixture(id = "r1").copy(syncState = SyncState.SYNCED))

            val change = documentChange(DocumentChange.Type.MODIFIED, remoteDocumentSnapshot(id = "r1"))
            handle.listenerSlot.captured.onEvent(querySnapshotOf(change), null)

            coVerify { localDataSource.save(match { it.id == "r1" && it.syncState == SyncState.SYNCED }) }
        }

    @Test
    fun `a malformed remote document is skipped instead of crashing the listener`() =
        runTest {
            val handle = captureSnapshotListener("uid-1")
            signIn("uid-1")

            val malformed =
                mockk<QueryDocumentSnapshot> {
                    every { this@mockk.id } returns "bad"
                    every { getString("artist") } returns null
                }
            val change = documentChange(DocumentChange.Type.ADDED, malformed)
            handle.listenerSlot.captured.onEvent(querySnapshotOf(change), null)

            coVerify(exactly = 0) { localDataSource.save(any()) }
        }

    @Test
    fun `signing out detaches the previously attached records listener`() =
        runTest {
            val handle = captureSnapshotListener("uid-1")
            signIn("uid-1")

            every { firebaseAuth.currentUser } returns null
            authListenerSlot.captured.onAuthStateChanged(firebaseAuth)

            verify { handle.registration.remove() }
        }

    private fun withSignedInUser(uid: String) {
        val firebaseUser = mockk<FirebaseUser> { every { this@mockk.uid } returns uid }
        every { firebaseAuth.currentUser } returns firebaseUser
    }

    /** Like [withSignedInUser], but also fires the captured auth-state listener that drives the inbound sync. */
    private fun signIn(uid: String) {
        withSignedInUser(uid)
        authListenerSlot.captured.onAuthStateChanged(firebaseAuth)
    }

    private fun collectionReturning(uid: String): DocumentReference {
        val documentRef = mockk<DocumentReference>()
        val recordsCollection = mockk<CollectionReference> { every { document(any()) } returns documentRef }
        val userDoc = mockk<DocumentReference> { every { collection("records") } returns recordsCollection }
        val usersCollection = mockk<CollectionReference> { every { document(uid) } returns userDoc }
        every { firestore.collection("users") } returns usersCollection
        return documentRef
    }

    /** The captured `addSnapshotListener` callback plus its [ListenerRegistration], for `users/{uid}/records`. */
    private fun captureSnapshotListener(uid: String): SnapshotListenerHandle {
        val listenerSlot = slot<EventListener<QuerySnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        val recordsCollection =
            mockk<CollectionReference> { every { addSnapshotListener(capture(listenerSlot)) } returns registration }
        val userDoc = mockk<DocumentReference> { every { collection("records") } returns recordsCollection }
        val usersCollection = mockk<CollectionReference> { every { document(uid) } returns userDoc }
        every { firestore.collection("users") } returns usersCollection
        return SnapshotListenerHandle(listenerSlot, registration)
    }

    private data class SnapshotListenerHandle(
        val listenerSlot: CapturingSlot<EventListener<QuerySnapshot>>,
        val registration: ListenerRegistration,
    )

    private fun querySnapshotOf(vararg changes: DocumentChange): QuerySnapshot =
        mockk { every { documentChanges } returns changes.toList() }

    private fun documentChange(type: DocumentChange.Type, document: QueryDocumentSnapshot): DocumentChange =
        mockk {
            every { this@mockk.type } returns type
            every { this@mockk.document } returns document
        }

    /**
     * A [QueryDocumentSnapshot] matching `RecordFirestoreMappers.kt`'s `toRecord()` field reads,
     * for the given [id].
     */
    private fun remoteDocumentSnapshot(
        id: String,
        artist: String = "Remote Artist",
        title: String = "Remote Title",
        createdAt: Instant = Instant.ofEpochSecond(1_700_000_000L),
        updatedAt: Instant = createdAt,
    ): QueryDocumentSnapshot =
        mockk {
            every { this@mockk.id } returns id
            every { getString("artist") } returns artist
            every { getString("title") } returns title
            every { getString("format") } returns Format.LP.name
            every { getString("speed") } returns Speed.RPM33.name
            every { getString("condition") } returns Condition.MINT.name
            every { getLong("year") } returns 2000L
            every { getString("label") } returns null
            every { getString("catalogNumber") } returns null
            every { getDouble("purchasePrice") } returns null
            every { getDate("purchaseDate") } returns null
            every { getLong("rating") } returns null
            every { getString("notes") } returns null
            every { getString("coverUrl") } returns null
            every { getLong("discogsId") } returns null
            every { this@mockk.get("tags") } returns emptyList<String>()
            every { getDate("createdAt") } returns Date.from(createdAt)
            every { getDate("updatedAt") } returns Date.from(updatedAt)
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

    private fun catalogResultFixture(): CatalogResult =
        CatalogResult(
            discogsId = 123L,
            artist = "Miles Davis",
            title = "Kind of Blue",
            year = 1959,
            label = "Columbia",
            catalogNumber = "CL 1355",
            coverUrl = null,
        )

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
