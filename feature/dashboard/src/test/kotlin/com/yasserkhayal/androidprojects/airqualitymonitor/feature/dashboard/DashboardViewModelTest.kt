package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityResult
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataOrigin
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.FavoritesRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<AirQualityRepository>()
    private val selectedLocationRepository = mockk<SelectedLocationRepository>()
    private val favoritesRepository = mockk<FavoritesRepository>()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { selectedLocationRepository.getSelectedLocation() } returns Result.success(null)
        every { favoritesRepository.observeFavorites() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `requests location permission when no location is saved`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(DashboardIntent.Started)
        turbineScope {
            val uiStateTurbine = viewModel.uiState.testIn(backgroundScope)
            val effectTurbine = viewModel.effects.testIn(backgroundScope)
            val state = uiStateTurbine.awaitItem()
            assertTrue(state.isLoading)
            assertNull(state.error)
            assertEquals(
                DashboardEffect.RequestLocationPermission(1L),
                effectTurbine.awaitItem(),
            )
        }
        coVerify(exactly = 1) { selectedLocationRepository.getSelectedLocation() }
        verify { repository wasNot called }
    }

    @Test
    fun `requests current location and exposes database issue when restoring saved location fails`() = runTest {
        coEvery { selectedLocationRepository.getSelectedLocation() } returns
            Result.failure(DataError.Local(RuntimeException("database unavailable")))
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.Started)

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.locationRestoreFailed)
        viewModel.effects.test {
            assertEquals(DashboardEffect.RequestLocationPermission(1L), awaitItem())
        }
        verify { repository wasNot called }
    }

    @Test
    fun `restores saved location without requesting permission`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        val snapshot = AirQualitySnapshot(
            location = location,
            timestamp = "2026-09-25T16:00",
            europeanAqi = 35,
            pm2_5 = 9.0,
            pm10 = 12.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 15.0,
            ozone = 20.0,
        )
        coEvery { selectedLocationRepository.getSelectedLocation() } returns Result.success(location)
        coEvery { repository.getCurrentAirQuality(location) } returns Result.success(
            AirQualityResult(snapshot, DataFreshness.Fresh, DataOrigin.Remote),
        )
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.Started)

        assertEquals(snapshot, viewModel.uiState.value.snapshot)
        coVerify(exactly = 1) { repository.getCurrentAirQuality(location) }
        viewModel.effects.test {
            expectNoEvents()
        }
    }

    @Test
    fun `loads snapshot on location exists`() = runTest {
        val snapshot = AirQualitySnapshot(
            location = Location(1.0, 2.0),
            timestamp = "2026-09-25T16:00",
            europeanAqi = 35,
            pm2_5 = 9.0,
            pm10 = 12.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 15.0,
            ozone = 20.0,
        )
        val address = "Cairo, EG"
        coEvery { repository.getCurrentAirQuality(Location(1.0, 2.0, address)) } returns Result.success(
            AirQualityResult(snapshot, DataFreshness.Fresh, DataOrigin.Remote),
        )
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.OnLocationResult(Location(1.0, 2.0, address)))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(snapshot, state.snapshot)
            assertEquals(address, state.locationAddress)
            assertNull(state.error)
        }
        coVerify(exactly = 1) { repository.getCurrentAirQuality(Location(1.0, 2.0, address)) }
    }

    @Test
    fun `loads snapshot on retry if location exists`() = runTest {
        val snapshot = AirQualitySnapshot(
            location = Location(1.0, 2.0),
            timestamp = "2026-09-25T16:00",
            europeanAqi = 35,
            pm2_5 = 9.0,
            pm10 = 12.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 15.0,
            ozone = 20.0,
        )
        val address = "Cairo, EG"
        coEvery { repository.getCurrentAirQuality(Location(1.0, 2.0, address)) } returns Result.success(
            AirQualityResult(snapshot, DataFreshness.Fresh, DataOrigin.Remote),
        )
        val viewModel = createViewModel()

        viewModel.lastLocation = Location(1.0, 2.0, address)

        viewModel.onIntent(DashboardIntent.Retry)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(snapshot, state.snapshot)
            assertEquals(address, state.locationAddress)
            assertNull(state.error)
        }
        coVerify(exactly = 1) { repository.getCurrentAirQuality(Location(1.0, 2.0, address)) }
    }

    @Test
    fun `request permission on retry if location is null`() = runTest {
        val viewModel = createViewModel()

        viewModel.lastLocation = null

        viewModel.onIntent(DashboardIntent.Retry)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.snapshot)
            assertNull(state.locationAddress)
            assertNull(state.error)
            assertFalse(state.isStale)
            assertFalse(state.refreshFailed)
            assertTrue(state.isLoading)
        }

        viewModel.effects.test {
            val effect = awaitItem()
            assertTrue(effect is DashboardEffect.RequestLocationPermission)
        }
        verify { repository wasNot called }
    }

    @Test
    fun `request current location on location permission granted`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.UseCurrentLocation)
        val permissionRequest = viewModel.effects.first() as DashboardEffect.RequestLocationPermission
        viewModel.onIntent(
            DashboardIntent.OnPermissionResult(
                granted = true,
                requestId = permissionRequest.requestId,
            ),
        )

        viewModel.effects.test {
            val effect = awaitItem()
            assertEquals(DashboardEffect.RequestCurrentLocation(permissionRequest.requestId), effect)
        }
        verify { repository wasNot called }
    }

    @Test
    fun `explicit current location action requests permission`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.UseCurrentLocation)

        viewModel.effects.test {
            assertEquals(DashboardEffect.RequestLocationPermission(1L), awaitItem())
        }
    }

    @Test
    fun `persists current location before loading its air quality`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        val snapshot = AirQualitySnapshot(
            location = location,
            timestamp = "2026-09-25T16:00",
            europeanAqi = 35,
            pm2_5 = 9.0,
            pm10 = 12.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 15.0,
            ozone = 20.0,
        )
        val calls = mutableListOf<String>()
        coEvery { selectedLocationRepository.saveSelectedLocation(location) } coAnswers {
            calls += "persist"
            Result.success(Unit)
        }
        coEvery { repository.getCurrentAirQuality(location) } coAnswers {
            calls += "load"
            Result.success(AirQualityResult(snapshot, DataFreshness.Fresh, DataOrigin.Remote))
        }
        val viewModel = createViewModel()

        val requestId = requestCurrentLocation(viewModel)
        viewModel.onIntent(DashboardIntent.OnCurrentLocationResult(location, requestId))

        assertEquals(listOf("persist", "load"), calls)
        assertEquals(snapshot, viewModel.uiState.value.snapshot)
        assertFalse(viewModel.uiState.value.locationPersistenceFailed)
    }

    @Test
    fun `continues using current location when persistence fails and exposes warning`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        val snapshot = AirQualitySnapshot(
            location = location,
            timestamp = "2026-09-25T16:00",
            europeanAqi = 35,
            pm2_5 = 9.0,
            pm10 = 12.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 15.0,
            ozone = 20.0,
        )
        val persistenceFailure: Result<Unit> =
            Result.failure(DataError.Local(RuntimeException("database unavailable")))
        coEvery { selectedLocationRepository.saveSelectedLocation(location) } returnsMany listOf(
            persistenceFailure,
            Result.success(Unit),
        )
        coEvery { repository.getCurrentAirQuality(location) } returns Result.success(
            AirQualityResult(snapshot, DataFreshness.Fresh, DataOrigin.Remote),
        )
        val viewModel = createViewModel()

        val requestId = requestCurrentLocation(viewModel)
        viewModel.onIntent(DashboardIntent.OnCurrentLocationResult(location, requestId))

        assertEquals(snapshot, viewModel.uiState.value.snapshot)
        assertTrue(viewModel.uiState.value.locationPersistenceFailed)
        coVerify(exactly = 1) { repository.getCurrentAirQuality(location) }

        viewModel.onIntent(DashboardIntent.Retry)

        assertFalse(viewModel.uiState.value.locationPersistenceFailed)
        coVerify(exactly = 2) { selectedLocationRepository.saveSelectedLocation(location) }
    }

    @Test
    fun `location permission denied state on location permission denied`() = runTest {
        val viewModel = createViewModel()
        viewModel.onIntent(DashboardIntent.UseCurrentLocation)
        val permissionRequest = viewModel.effects.first() as DashboardEffect.RequestLocationPermission

        viewModel.onIntent(
            DashboardIntent.OnPermissionResult(
                granted = false,
                requestId = permissionRequest.requestId,
            ),
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.snapshot)
            assertNull(state.locationAddress)
            assertFalse(state.isLoading)
            assertEquals(DashboardError.PermissionDenied, state.error)
        }
        verify { repository wasNot called }
    }

    @Test
    fun `searched selection supersedes a pending current location result`() = runTest {
        val currentLocation = Location(1.0, 2.0, "Old current location")
        val searchedLocation = Location(30.0, 31.0, "Cairo, Egypt")
        coEvery { repository.getCurrentAirQuality(searchedLocation) } returns Result.failure(
            DataError.OfflineNoCache(RuntimeException("offline")),
        )
        val viewModel = createViewModel()
        val requestId = requestCurrentLocation(viewModel)

        viewModel.onIntent(DashboardIntent.OnLocationResult(searchedLocation))
        viewModel.onIntent(DashboardIntent.OnCurrentLocationResult(currentLocation, requestId))

        assertEquals(searchedLocation, viewModel.lastLocation)
        coVerify(exactly = 0) { selectedLocationRepository.saveSelectedLocation(any()) }
        coVerify(exactly = 1) { repository.getCurrentAirQuality(searchedLocation) }
    }

    @Test
    fun `null snapshot on null location`() = runTest {
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.OnLocationResult(null))

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.snapshot)
            assertNull(state.locationAddress)
            assertFalse(state.isLoading)
            assertEquals(DashboardError.LocationUnavailable, state.error)
        }

        verify { repository wasNot called }
    }

    @Test
    fun `handles remote fetch failure and no cache`() = runTest {
        val viewModel = createViewModel()

        coEvery {
            repository.getCurrentAirQuality(Location(3.0, 4.0))
        } returns Result.failure(DataError.OfflineNoCache(RuntimeException("nope")))
        viewModel.onIntent(DashboardIntent.OnLocationResult(Location(3.0, 4.0)))

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.snapshot)
            assertNull(state.locationAddress)
            assertFalse(state.isLoading)
            assertEquals(DashboardError.OfflineNoCache, state.error)
            assertTrue(state.dataError is DataError.OfflineNoCache)
        }
    }

    @Test
    fun `handles remote fetch failure and cache retrieval failure`() = runTest {
        val viewModel = createViewModel()

        coEvery {
            repository.getCurrentAirQuality(Location(3.0, 4.0))
        } returns Result.failure(DataError.Local(RuntimeException("nope")))
        viewModel.onIntent(DashboardIntent.OnLocationResult(Location(3.0, 4.0)))

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.snapshot)
            assertNull(state.locationAddress)
            assertFalse(state.isLoading)
            assertEquals(DashboardError.DataUnavailable, state.error)
            assertTrue(state.dataError is DataError.Local)
        }
    }

    @Test
    fun `handles remote fetch failure and server failure`() = runTest {
        val viewModel = createViewModel()

        coEvery {
            repository.getCurrentAirQuality(Location(3.0, 4.0))
        } returns Result.failure(DataError.Server(400, RuntimeException("bad request")))
        viewModel.onIntent(DashboardIntent.OnLocationResult(Location(3.0, 4.0)))

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.snapshot)
            assertNull(state.locationAddress)
            assertFalse(state.isLoading)
            assertEquals(DashboardError.DataUnavailable, state.error)
            assertTrue(state.dataError is DataError.Server)
        }
    }

    @Test
    fun `shows cached freshness and refresh failure`() = runTest {
        val location = Location(1.0, 2.0, "Cairo, EG")
        val snapshot = AirQualitySnapshot(
            location = location,
            timestamp = "2026-09-25T16:00",
            europeanAqi = 35,
            pm2_5 = 9.0,
            pm10 = 12.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 15.0,
            ozone = 20.0,
        )
        coEvery { repository.getCurrentAirQuality(location) } returns Result.success(
            AirQualityResult(
                snapshot = snapshot,
                freshness = DataFreshness.Stale,
                origin = DataOrigin.Cache,
                refreshError = DataError.Network(RuntimeException("offline")),
            ),
        )
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.OnLocationResult(location))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(snapshot, state.snapshot)
            assertEquals("Cairo, EG", state.locationAddress)
            assertEquals(true, state.isStale)
            assertEquals(true, state.refreshFailed)
            assertTrue(state.refreshError is DataError.Network)
            assertNull(state.error)
        }
    }

    @Test
    fun `retry refreshes last location without requesting permission again`() = runTest {
        val location = Location(1.0, 2.0)
        val snapshot = AirQualitySnapshot(
            location = location,
            timestamp = "2026-09-25T16:00",
            europeanAqi = 35,
            pm2_5 = 9.0,
            pm10 = 12.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 15.0,
            ozone = 20.0,
        )
        coEvery { repository.getCurrentAirQuality(location) } returns Result.success(
            AirQualityResult(snapshot, DataFreshness.Fresh, DataOrigin.Remote),
        )
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.OnLocationResult(location))
        viewModel.onIntent(DashboardIntent.Retry)

        coVerify(exactly = 2) { repository.getCurrentAirQuality(location) }
    }

    /** Adds the active Dashboard location and immediately reflects the saved state. */
    @Test
    fun `adds active location to favorites`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        coEvery { repository.getCurrentAirQuality(location) } returns Result.failure(
            DataError.OfflineNoCache(IllegalStateException("offline")),
        )
        coEvery { favoritesRepository.addFavorite(location) } returns Result.success(Unit)
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.OnLocationResult(location))
        viewModel.onIntent(DashboardIntent.AddActiveLocationToFavorites)

        assertTrue(viewModel.uiState.value.isLocationFavorite)
        assertFalse(viewModel.uiState.value.favoriteOperationFailed)
        coVerify(exactly = 1) { favoritesRepository.addFavorite(location) }
    }

    /** Favorite observation marks a newly active matching coordinate as saved. */
    @Test
    fun `observes whether active location is already favorited`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        every { favoritesRepository.observeFavorites() } returns flowOf(listOf(location.copy(address = "Cairo")))
        coEvery { repository.getCurrentAirQuality(location) } returns Result.failure(
            DataError.OfflineNoCache(IllegalStateException("offline")),
        )
        val viewModel = createViewModel()

        viewModel.onIntent(DashboardIntent.OnLocationResult(location))

        assertTrue(viewModel.uiState.value.isLocationFavorite)
    }

    private suspend fun requestCurrentLocation(viewModel: DashboardViewModel): Long {
        viewModel.onIntent(DashboardIntent.UseCurrentLocation)
        val permissionRequest = viewModel.effects.first() as DashboardEffect.RequestLocationPermission
        viewModel.onIntent(
            DashboardIntent.OnPermissionResult(
                granted = true,
                requestId = permissionRequest.requestId,
            ),
        )
        return (viewModel.effects.first() as DashboardEffect.RequestCurrentLocation).requestId
    }

    private fun createViewModel() = DashboardViewModel(
        repository,
        selectedLocationRepository,
        favoritesRepository,
    )
}
