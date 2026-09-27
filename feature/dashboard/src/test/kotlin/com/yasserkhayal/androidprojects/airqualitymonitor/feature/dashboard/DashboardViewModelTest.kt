package com.yasserkhayal.androidprojects.airqualitymonitor.feature.dashboard

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Test
    fun `starts by requesting permission`() = runTest {
        Dispatchers.setMain(dispatcher)
        try {
            val repository = mockk<AirQualityRepository>()
            val viewModel = DashboardViewModel(repository)

            viewModel.onIntent(DashboardIntent.Started)

            assertEquals(true, viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `loads snapshot on location result`() = runTest {
        Dispatchers.setMain(dispatcher)
        try {
            val snapshot = AirQualitySnapshot(
                location = Location(1.0, 2.0),
                timestamp = "2026-09-25T16:00",
                europeanAqi = 35,
                pm2_5 = 9.0,
                pm10 = 12.0,
                carbonMonoxide = 250.0,
                nitrogenDioxide = 15.0,
                ozone = 20.0
            )
            val address = "Cairo, EG"
            val repository = mockk<AirQualityRepository>()
            coEvery { repository.getCurrentAirQuality(Location(1.0, 2.0, address)) } returns Result.success(snapshot)
            val viewModel = DashboardViewModel(repository)

            viewModel.onIntent(DashboardIntent.LocationResult(Location(1.0, 2.0, address)))

            assertEquals(snapshot, viewModel.uiState.value.snapshot)
            assertEquals(address, viewModel.uiState.value.locationAddress)
            assertNull(viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `handles permission denied and data failure`() = runTest {
        Dispatchers.setMain(dispatcher)
        try {
            val repository = mockk<AirQualityRepository>()
            val viewModel = DashboardViewModel(repository)

            viewModel.onIntent(DashboardIntent.PermissionResult(granted = false))

            assertEquals(DashboardError.PermissionDenied, viewModel.uiState.value.error)

            coEvery { repository.getCurrentAirQuality(Location(3.0, 4.0)) } returns Result.failure(DataError.Network(RuntimeException("nope")))
            viewModel.onIntent(DashboardIntent.LocationResult(Location(3.0, 4.0)))

            assertEquals(DashboardError.DataUnavailable, viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
