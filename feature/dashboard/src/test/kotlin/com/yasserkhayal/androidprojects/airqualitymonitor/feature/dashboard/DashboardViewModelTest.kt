package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityResult
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataOrigin
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts by requesting permission`() = runTest {
        val viewModel = DashboardViewModel(repository)
        viewModel.onIntent(DashboardIntent.Started)
        turbineScope {
            val uiStateTurbine = viewModel.uiState.testIn(backgroundScope)
            val effectTurbine = viewModel.effects.testIn(backgroundScope)
            val state = uiStateTurbine.awaitItem()
            assertTrue(state.isLoading)
            assertNull(state.error)
            val receivedEffect = effectTurbine.awaitItem()
            assertEquals(DashboardEffect.RequestLocationPermission, receivedEffect)
        }
        verify { repository wasNot called }
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
        val viewModel = DashboardViewModel(repository)

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
        val viewModel = DashboardViewModel(repository)

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
        val viewModel = DashboardViewModel(repository)

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
            assertEquals(DashboardEffect.RequestLocationPermission, effect)
        }
        verify { repository wasNot called }
    }

    @Test
    fun `request current location on location permission granted`() = runTest {
        val viewModel = DashboardViewModel(repository)

        viewModel.onIntent(DashboardIntent.OnPermissionResult(granted = true))

        viewModel.effects.test {
            val effect = awaitItem()
            assertEquals(DashboardEffect.RequestCurrentLocation, effect)
        }
        verify { repository wasNot called }
    }

    @Test
    fun `location permission denied state on location permission denied`() = runTest {
        val viewModel = DashboardViewModel(repository)

        viewModel.onIntent(DashboardIntent.OnPermissionResult(granted = false))

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
    fun `null snapshot on null location`() = runTest {
        val viewModel = DashboardViewModel(repository)

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
        val viewModel = DashboardViewModel(repository)

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
        }
    }

    @Test
    fun `handles remote fetch failure and cache retrieval failure`() = runTest {
        val viewModel = DashboardViewModel(repository)

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
        }
    }

    @Test
    fun `handles remote fetch failure and server failure`() = runTest {
        val viewModel = DashboardViewModel(repository)

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
        val viewModel = DashboardViewModel(repository)

        viewModel.onIntent(DashboardIntent.OnLocationResult(location))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(snapshot, state.snapshot)
            assertEquals("Cairo, EG", state.locationAddress)
            assertEquals(true, state.isStale)
            assertEquals(true, state.refreshFailed)
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
        val viewModel = DashboardViewModel(repository)

        viewModel.onIntent(DashboardIntent.OnLocationResult(location))
        viewModel.onIntent(DashboardIntent.Retry)

        coVerify(exactly = 2) { repository.getCurrentAirQuality(location) }
    }
}
