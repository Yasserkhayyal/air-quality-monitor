package com.yasserkhayal.androidprojects.airqualitymonitor.feature.favorites

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.FavoritesRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val favoritesRepository = mockk<FavoritesRepository>()
    private val selectedLocationRepository = mockk<SelectedLocationRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { favoritesRepository.observeFavorites() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Exposes the repository's saved locations in its existing order. */
    @Test
    fun `observes favorite locations`() = runTest {
        val locations = listOf(
            Location(30.0444, 31.2357, "Cairo, Egypt"),
            Location(51.5079, -0.1276, "London, UK"),
        )
        every { favoritesRepository.observeFavorites() } returns flowOf(locations)

        val viewModel = createViewModel()

        assertEquals(locations, viewModel.uiState.value.favorites)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /** Persists selected favorite as active before emitting navigation. */
    @Test
    fun `persists selected location before dashboard effect`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        val calls = mutableListOf<String>()
        every { favoritesRepository.observeFavorites() } returns flowOf(listOf(location))
        coEvery { selectedLocationRepository.saveSelectedLocation(location) } coAnswers {
            calls += "persist"
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        val effect = async { viewModel.effects.first() }

        viewModel.onIntent(FavoritesIntent.LocationSelected(location))

        assertEquals(FavoritesEffect.OpenDashboard(location), effect.await())
        assertEquals(listOf("persist"), calls)
        coVerify(exactly = 1) { selectedLocationRepository.saveSelectedLocation(location) }
        assertFalse(viewModel.uiState.value.isSelecting)
    }

    /** Retains the favorite and reports failure when active-location persistence fails. */
    @Test
    fun `reports selection persistence failure without navigating`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        every { favoritesRepository.observeFavorites() } returns flowOf(listOf(location))
        coEvery { selectedLocationRepository.saveSelectedLocation(location) } returns
            Result.failure(IllegalStateException("database unavailable"))
        val viewModel = createViewModel()

        viewModel.onIntent(FavoritesIntent.LocationSelected(location))

        assertEquals(FavoritesError.SelectionFailed, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isSelecting)
        assertEquals(listOf(location), viewModel.uiState.value.favorites)
    }

    /** Requests removal for an existing favorite. */
    @Test
    fun `removes favorite`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        every { favoritesRepository.observeFavorites() } returns flowOf(listOf(location))
        coEvery { favoritesRepository.removeFavorite(location) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(FavoritesIntent.RemoveFavorite(location))

        coVerify(exactly = 1) { favoritesRepository.removeFavorite(location) }
        assertEquals(null, viewModel.uiState.value.removingLocation)
    }

    /** Rejects stale selection intents for coordinates no longer in observed favorites. */
    @Test
    fun `ignores selection for location not in favorites`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        val viewModel = createViewModel()

        viewModel.onIntent(FavoritesIntent.LocationSelected(location))

        coVerify(exactly = 0) { selectedLocationRepository.saveSelectedLocation(any()) }
    }

    /** Retry restarts favorites observation after an upstream failure. */
    @Test
    fun `retries favorites observation`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        every { favoritesRepository.observeFavorites() } returnsMany listOf(
            flow { throw IllegalStateException("database unavailable") },
            flowOf(listOf(location)),
        )
        val viewModel = createViewModel()
        assertEquals(FavoritesError.LoadFailed, viewModel.uiState.value.error)

        viewModel.onIntent(FavoritesIntent.Retry)

        assertEquals(listOf(location), viewModel.uiState.value.favorites)
        assertEquals(null, viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /** Converts favorite-removal failures to visible presentation state. */
    @Test
    fun `reports favorite removal failure`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        every { favoritesRepository.observeFavorites() } returns flowOf(listOf(location))
        coEvery { favoritesRepository.removeFavorite(location) } returns
            Result.failure(IllegalStateException("database unavailable"))
        val viewModel = createViewModel()

        viewModel.onIntent(FavoritesIntent.RemoveFavorite(location))

        assertEquals(FavoritesError.RemoveFailed, viewModel.uiState.value.error)
        assertEquals(null, viewModel.uiState.value.removingLocation)
    }

    /** Preserves the original favorite after failed active-location persistence. */
    @Test
    fun `retains favorite after removal persistence fails`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        every { favoritesRepository.observeFavorites() } returns flowOf(listOf(location))
        coEvery { favoritesRepository.removeFavorite(location) } returns
            Result.failure(IllegalStateException("database unavailable"))
        val viewModel = createViewModel()

        viewModel.onIntent(FavoritesIntent.RemoveFavorite(location))

        assertEquals(listOf(location), viewModel.uiState.value.favorites)
    }

    /** Creates a Favorites ViewModel with the shared repository test doubles. */
    private fun createViewModel() = FavoritesViewModel(
        favoritesRepository,
        selectedLocationRepository,
    )
}
