package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AirQualityRepositoryImplTest {
    @Test
    fun `returns mapped snapshot on success`() = runTest {
        val api = mockk<OpenMeteoApi>()
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } returns OpenMeteoResponse(
            current = OpenMeteoCurrent(
                time = "2026-09-25T16:00",
                europeanAqi = 59,
                pm2_5 = 10.0,
                pm10 = 15.0,
                carbonMonoxide = 250.0,
                nitrogenDioxide = 20.0,
                ozone = 30.0
            )
        )

        val repo = AirQualityRepositoryImpl(api)

        val result = repo.getCurrentAirQuality(Location(1.0, 2.0))

        assertTrue(result.isSuccess)
        assertEquals(59, result.getOrNull()?.europeanAqi)
        assertEquals("2026-09-25T16:00", result.getOrNull()?.timestamp)
    }

    @Test
    fun `maps network failure to domain error`() = runTest {
        val api = mockk<OpenMeteoApi>()
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } throws java.io.IOException("offline")

        val repo = AirQualityRepositoryImpl(api)

        val result = repo.getCurrentAirQuality(Location(1.0, 2.0))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DataError.Network)
    }
}
