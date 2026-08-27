package app.vinilogs.feature.collection.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import app.vinilogs.core.testing.MainDispatcherExtension
import app.vinilogs.core.testing.fake.FakeCollectionRepository
import app.vinilogs.core.testing.fixture.RecordFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

/**
 * See ShelfViewModelTest (feature:collection/shelf, T-15) for why UnconfinedTestDispatcher is
 * needed here rather than the extension's StandardTestDispatcher default -- same viewModelScope
 * + stateIn(WhileSubscribed) shape, same two-scheduler mismatch otherwise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RecordDetailViewModelTest {
    @JvmField
    @RegisterExtension
    val mainDispatcherExtension = MainDispatcherExtension(UnconfinedTestDispatcher())

    private fun viewModel(
        recordId: String,
        repository: FakeCollectionRepository,
    ): RecordDetailViewModel = RecordDetailViewModel(SavedStateHandle(mapOf("recordId" to recordId)), repository)

    @Test
    fun `shows the record when it exists`() =
        runTest {
            val record = RecordFixtures.records(count = 1).first()
            val viewModel = viewModel(record.id, FakeCollectionRepository(listOf(record)))

            viewModel.uiState.test {
                val state = awaitItem()
                assertEquals(record.id, state.record?.id)
                assertFalse(state.isLoading)
                assertFalse(state.notFound)
            }
        }

    @Test
    fun `reports notFound for an id with no matching record`() =
        runTest {
            val viewModel = viewModel("missing-id", FakeCollectionRepository())

            viewModel.uiState.test {
                val state = awaitItem()
                assertTrue(state.notFound)
                assertNull(state.record)
            }
        }

    @Test
    fun `delete removes the record from the repository and shows the undo bar`() =
        runTest {
            val record = RecordFixtures.records(count = 1).first()
            val repository = FakeCollectionRepository(listOf(record))
            val viewModel = viewModel(record.id, repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.delete()
                val afterDelete = awaitItem()
                assertTrue(afterDelete.showUndo)
                assertNull(afterDelete.record)
            }

            repository.observeRecord(record.id).test {
                assertNull(awaitItem())
            }
        }

    @Test
    fun `undoDelete re-adds the exact record that was deleted`() =
        runTest {
            val record = RecordFixtures.records(count = 1).first()
            val repository = FakeCollectionRepository(listOf(record))
            val viewModel = viewModel(record.id, repository)

            viewModel.uiState.test {
                awaitItem()
                viewModel.delete()
                awaitItem()
                viewModel.undoDelete()
                val restored = awaitItem()
                assertEquals(record.id, restored.record?.id)
                assertEquals(record.artist, restored.record?.artist)
            }
        }
}
