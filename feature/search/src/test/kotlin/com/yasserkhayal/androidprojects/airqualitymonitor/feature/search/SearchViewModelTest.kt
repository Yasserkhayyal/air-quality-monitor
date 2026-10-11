package com.yasserkhayal.androidprojects.airqualitymonitor.feature.search

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.LocationSearchRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private val searchRepository = mockk<LocationSearchRepository>()
    private val selectedLocationRepository = mockk<SelectedLocationRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `does not search before minimum query length`() = runTest(dispatcher) {
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged("C"))
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearching)
        coVerify(exactly = 0) { searchRepository.search(any()) }
    }

    @Test
    fun `waits for configured debounce before searching`() = runTest(dispatcher) {
        val location = Location(30.0, 31.0, "Cairo, Egypt")
        coEvery { searchRepository.search("Ca") } returns Result.success(listOf(location))
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged("Ca"))
        runCurrent()
        assertTrue(viewModel.uiState.value.isSearching)
        advanceTimeBy((SEARCH_DEBOUNCE_MILLIS - 1).milliseconds)
        runCurrent()
        coVerify(exactly = 0) { searchRepository.search(any()) }

        advanceTimeBy(1.milliseconds)
        runCurrent()

        assertEquals(listOf(location), viewModel.uiState.value.results)
        assertFalse(viewModel.uiState.value.isSearching)
        coVerify(exactly = 1) { searchRepository.search("Ca") }
    }

    @Test
    fun `cancels in flight search when query changes`() = runTest(dispatcher) {
        val stale = Location(1.0, 1.0, "Stale")
        val current = Location(30.0, 31.0, "Cairo, Egypt")
        coEvery { searchRepository.search("Ca") } coAnswers {
            delay(10_000.milliseconds)
            Result.success(listOf(stale))
        }
        coEvery { searchRepository.search("Cai") } returns Result.success(listOf(current))
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged("Ca"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS.milliseconds)
        runCurrent()
        viewModel.onIntent(SearchIntent.QueryChanged("Cai"))
        runCurrent()
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS.milliseconds)
        runCurrent()

        assertEquals(listOf(current), viewModel.uiState.value.results)
        coVerify(exactly = 1) { searchRepository.search("Ca") }
        coVerify(exactly = 1) { searchRepository.search("Cai") }
    }

    @Test
    fun `persists selected result before requesting dashboard navigation`() = runTest(dispatcher) {
        val location = Location(30.0, 31.0, "Cairo, Egypt")
        coEvery { searchRepository.search("Cairo") } returns Result.success(listOf(location))
        coEvery { selectedLocationRepository.saveSelectedLocation(location) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged("Cairo"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS.milliseconds)
        runCurrent()
        val effect = async { viewModel.effects.first() }
        viewModel.onIntent(SearchIntent.LocationSelected(location))
        advanceUntilIdle()

        assertEquals(SearchEffect.OpenDashboard(location), effect.await())
        coVerify(exactly = 1) { selectedLocationRepository.saveSelectedLocation(location) }
    }

    @Test
    fun `preserves typed search failure`() = runTest(dispatcher) {
        val failure = DataError.Network(IOException("offline"))
        coEvery { searchRepository.search("Cairo") } returns Result.failure(failure)
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged("Cairo"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS.milliseconds)
        runCurrent()

        val searchError = viewModel.uiState.value.error
        assertTrue(searchError is SearchError.SearchFailed)
        assertEquals(failure, (searchError as SearchError.SearchFailed).dataError)
        assertTrue(viewModel.uiState.value.hasSearched)
    }

    @Test
    fun `shows empty state when repository returns no matches`() = runTest(dispatcher) {
        coEvery { searchRepository.search("Unknown") } returns Result.success(emptyList())
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged("Unknown"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS.milliseconds)
        runCurrent()

        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(emptyList<Location>(), viewModel.uiState.value.results)
    }

    @Test
    fun `shows selection failure and keeps result available for retry`() = runTest(dispatcher) {
        val location = Location(30.0, 31.0, "Cairo, Egypt")
        coEvery { searchRepository.search("Cairo") } returns Result.success(listOf(location))
        coEvery { selectedLocationRepository.saveSelectedLocation(location) } returns
            Result.failure(IllegalStateException())
        val viewModel = createViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged("Cairo"))
        advanceTimeBy(SEARCH_DEBOUNCE_MILLIS.milliseconds)
        runCurrent()
        viewModel.onIntent(SearchIntent.LocationSelected(location))
        advanceUntilIdle()

        val selectionError = viewModel.uiState.value.error
        assertTrue(selectionError is SearchError.SelectionFailed)
        assertTrue((selectionError as SearchError.SelectionFailed).dataError is DataError.GenericError)
        assertEquals(listOf(location), viewModel.uiState.value.results)
        assertFalse(viewModel.uiState.value.isSelecting)
    }

    private fun createViewModel() = SearchViewModel(searchRepository, selectedLocationRepository)
}
