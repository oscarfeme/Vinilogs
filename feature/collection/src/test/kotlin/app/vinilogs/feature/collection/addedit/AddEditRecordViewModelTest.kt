package app.vinilogs.feature.collection.addedit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.vinilogs.core.model.CollectionFilter
import app.vinilogs.core.model.CollectionSort
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeCollectionRepository
import app.vinilogs.core.testing.fixture.RecordFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * See ShelfViewModelTest (feature:collection/shelf) for why UnconfinedTestDispatcher is needed
 * here rather than the extension's StandardTestDispatcher default -- same viewModelScope +
 * stateIn(WhileSubscribed) shape, same two-scheduler mismatch otherwise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditRecordViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())

    private fun addViewModel(
        repository: FakeCollectionRepository = FakeCollectionRepository(),
    ): AddEditRecordViewModel = AddEditRecordViewModel(SavedStateHandle(), repository)

    private fun editViewModel(
        recordId: String,
        repository: FakeCollectionRepository,
    ): AddEditRecordViewModel = AddEditRecordViewModel(SavedStateHandle(mapOf("recordId" to recordId)), repository)

    @Test
    fun `add mode starts with an empty draft, not loading`() =
        runTest {
            val viewModel = addViewModel()

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(AddEditMode.ADD, state.mode)
                assertEquals("", state.draft.artist)
                assertFalse(state.isLoading)
            }
        }

    @Test
    fun `edit mode prefills the draft from the existing record`() =
        runTest {
            val record = RecordFixtures.records(count = 1).first()
            val repository = FakeCollectionRepository(listOf(record))
            val viewModel = editViewModel(record.id, repository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(AddEditMode.EDIT, state.mode)
                assertEquals(record.artist, state.draft.artist)
                assertEquals(record.title, state.draft.title)
            }
        }

    @Test
    fun `edit mode for a missing record reports notFound`() =
        runTest {
            val repository = FakeCollectionRepository()
            val viewModel = editViewModel("missing-id", repository)

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.notFound)
                assertFalse(state.isLoading)
            }
        }

    @Test
    fun `saving with a blank artist reports a validation error and does not call the repository`() =
        runTest {
            val repository = FakeCollectionRepository()
            val viewModel = addViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.updateDraft { it.copy(artist = "", title = "Kind of Blue") }
                awaitItem()
                viewModel.save()
                val afterSave = awaitItem()
                assertNotNull(afterSave.errors.artist)
                assertFalse(afterSave.saved)
            }
        }

    @Test
    fun `saving a valid new record adds it to the repository and marks saved`() =
        runTest {
            val repository = FakeCollectionRepository()
            val viewModel = addViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.updateDraft { it.copy(artist = "Miles Davis", title = "Kind of Blue") }
                awaitItem()
                viewModel.save()
                // UnconfinedTestDispatcher runs FakeCollectionRepository's suspend calls to
                // completion without an actual suspension point, so the isSaving=true
                // intermediate state is never separately observable -- only the settled result.
                val saved = awaitItem()
                assertTrue(saved.saved)
            }

            repository.observeCollection(CollectionFilter(), CollectionSort.ARTIST).test {
                assertEquals(1, awaitItem().size)
            }
        }

    @Test
    fun `saving a valid edit updates the existing record`() =
        runTest {
            val record = RecordFixtures.records(count = 1).first()
            val repository = FakeCollectionRepository(listOf(record))
            val viewModel = editViewModel(record.id, repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.updateDraft { it.copy(title = "A New Title") }
                awaitItem()
                viewModel.save()
                val saved = awaitItem()
                assertTrue(saved.saved)
            }

            repository.observeRecord(record.id).test {
                assertEquals("A New Title", awaitItem()?.title)
            }
        }

    @Test
    fun `a repository failure on save is surfaced as saveError, not a crash`() =
        runTest {
            val repository = FakeCollectionRepository()
            repository.failNextCallWith(IllegalStateException("offline and no local copy"))
            val viewModel = addViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.updateDraft { it.copy(artist = "Miles Davis", title = "Kind of Blue") }
                awaitItem()
                viewModel.save()
                val result = awaitItem()
                assertFalse(result.saved)
                assertNotNull(result.saveError)
            }
        }

    @Test
    fun `editing a field clears any previous save error`() =
        runTest {
            val repository = FakeCollectionRepository()
            repository.failNextCallWith(IllegalStateException("boom"))
            val viewModel = addViewModel(repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.updateDraft { it.copy(artist = "Miles Davis", title = "Kind of Blue") }
                awaitItem()
                viewModel.save()
                awaitItem() // saveError set

                viewModel.updateDraft { it.copy(title = "Kind of Blue (Deluxe)") }
                val afterEdit = awaitItem()
                assertNull(afterEdit.saveError)
            }
        }
}
