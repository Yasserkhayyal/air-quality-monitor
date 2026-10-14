package com.yasserkhayal.androidprojects.airqualitymonitor.feature.history

import app.cash.turbine.test
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataOrigin
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.HistoricalAirQualityPoint
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.HistoricalAirQualityResult
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies history loading, error, cache, and retry state transitions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoricalAirQualityViewModelTest {
    private val repository = mockk<AirQualityRepository>()
    private val selectedLocationRepository = mockk<SelectedLocationRepository>()

    /** Sets a deterministic Main dispatcher for ViewModel coroutines. */
    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    /** Restores the process Main dispatcher after each test. */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Loads persisted location and displays successful historical data. */
    @Test
    fun loadsHistoricalPointsForSavedLocation() = runTest {
        val location = sampleLocation()
        val result = historicalResult()
        coEvery { selectedLocationRepository.getSelectedLocation() } returns Result.success(location)
        coEvery { repository.getHistoricalAirQuality(location) } returns Result.success(result)

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isLoading)
            assertEquals(location, state.location)
            assertEquals(result.points, state.points)
            assertEquals(DataFreshness.Fresh, state.freshness)
        }
    }

    /** Represents cached stale points and remote refresh error distinctly in state. */
    @Test
    fun exposesStaleCacheAndRefreshError() = runTest {
        val location = sampleLocation()
        val refreshError = DataError.Network(IllegalStateException("offline"))
        val cached = historicalResult(
            freshness = DataFreshness.Stale,
            origin = DataOrigin.Cache,
            refreshError = refreshError,
        )
        coEvery { selectedLocationRepository.getSelectedLocation() } returns Result.success(location)
        coEvery { repository.getHistoricalAirQuality(location) } returns Result.success(cached)

        val viewModel = createViewModel()

        assertEquals(cached.points, viewModel.uiState.value.points)
        assertEquals(DataFreshness.Stale, viewModel.uiState.value.freshness)
        assertEquals(refreshError, viewModel.uiState.value.refreshError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    /** Exposes no-location state when no active location has been persisted. */
    @Test
    fun exposesLocationUnavailableWhenNothingIsSelected() = runTest {
        coEvery { selectedLocationRepository.getSelectedLocation() } returns Result.success(null)

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.locationUnavailable)
        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 0) { repository.getHistoricalAirQuality(any()) }
    }

    /** Reloads the persisted active location each time history screen is opened. */
    @Test
    fun reloadsHistoryForLatestSelectedLocationOnScreenStart() = runTest {
        val originalLocation = sampleLocation()
        val latestLocation = Location(51.5072, -0.1276, "London, United Kingdom")
        coEvery { selectedLocationRepository.getSelectedLocation() } returnsMany listOf(
            Result.success(originalLocation),
            Result.success(latestLocation),
        )
        coEvery { repository.getHistoricalAirQuality(originalLocation) } returns
            Result.success(historicalResult(aqi = 42))
        coEvery { repository.getHistoricalAirQuality(latestLocation) } returns
            Result.success(historicalResult(aqi = 17))

        val viewModel = createViewModel()
        assertEquals(originalLocation, viewModel.uiState.value.location)
        assertEquals(42, viewModel.uiState.value.points.single().europeanAqi)

        viewModel.onIntent(HistoricalAirQualityIntent.Started)

        assertEquals(latestLocation, viewModel.uiState.value.location)
        assertEquals(17, viewModel.uiState.value.points.single().europeanAqi)
        coVerify(exactly = 1) { repository.getHistoricalAirQuality(originalLocation) }
        coVerify(exactly = 1) { repository.getHistoricalAirQuality(latestLocation) }
    }

    /** Retries a failed retrieval using the restored location. */
    @Test
    fun retriesAfterHistoryFailure() = runTest {
        val location = sampleLocation()
        coEvery { selectedLocationRepository.getSelectedLocation() } returns Result.success(location)
        coEvery { repository.getHistoricalAirQuality(location) } returnsMany listOf(
            Result.failure(DataError.Network(IllegalStateException("offline"))),
            Result.success(historicalResult()),
        )
        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.error is DataError.Network)

        viewModel.onIntent(HistoricalAirQualityIntent.Retry)

        assertEquals(historicalResult().points, viewModel.uiState.value.points)
        assertEquals(null, viewModel.uiState.value.error)
        coVerify(exactly = 2) { repository.getHistoricalAirQuality(location) }
    }

    /** Creates the ViewModel with mocked application contracts. */
    private fun createViewModel() = HistoricalAirQualityViewModel(repository, selectedLocationRepository).also {
        it.onIntent(HistoricalAirQualityIntent.Started)
    }

    /** Returns a location used by the ViewModel tests. */
    private fun sampleLocation() = Location(30.0444, 31.2357, "Cairo, Egypt")

    /** Returns representative hourly result data. */
    private fun historicalResult(
        aqi: Int = 42,
        freshness: DataFreshness = DataFreshness.Fresh,
        origin: DataOrigin = DataOrigin.Remote,
        refreshError: DataError? = null,
    ) = HistoricalAirQualityResult(
        points = listOf(
            HistoricalAirQualityPoint(
                timestamp = "2026-10-13T23:00",
                europeanAqi = aqi,
                pm2_5 = 12.5,
                pm10 = null,
                carbonMonoxide = 300.0,
                nitrogenDioxide = 18.0,
                ozone = 46.0,
            ),
        ),
        freshness = freshness,
        origin = origin,
        refreshError = refreshError,
    )
}
