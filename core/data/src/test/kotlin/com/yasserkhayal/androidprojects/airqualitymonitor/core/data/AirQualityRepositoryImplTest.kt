package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.database.sqlite.SQLiteException
import android.util.Log
import com.google.gson.JsonParseException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityRepositoryImpl
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.OpenMeteoApi
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.OpenMeteoCurrent
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.OpenMeteoResponse
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.toCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.toCacheKey
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataOrigin
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class AirQualityRepositoryImplTest {
    @Test
    fun `returns mapped snapshot on success`() = runTest {
        val api = mockk<OpenMeteoApi>()
        val dao = mockk<AirQualityDao>(relaxed = true)
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } returns sampleResponse()

        val result = AirQualityRepositoryImpl(api, dao).getCurrentAirQuality(Location(1.0, 2.0))

        assertTrue(result.isSuccess)
        assertEquals(59, result.getOrNull()?.snapshot?.europeanAqi)
        assertEquals("2026-09-25 16:00", result.getOrNull()?.snapshot?.timestamp)
        assertEquals(DataFreshness.Fresh, result.getOrNull()?.freshness)
        assertEquals(DataOrigin.Remote, result.getOrNull()?.origin)
        coVerify(exactly = 1) { dao.upsert(match { it.retrievedAtEpochMillis > 0L }) }
    }

    @Test
    fun `returns offline no cache error when network fails without cache`() = runTest {
        val api = mockk<OpenMeteoApi>()
        val dao = mockk<AirQualityDao>()
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } throws IOException("offline")
        coEvery { dao.findByLocationKey(any()) } returns null

        val result = AirQualityRepositoryImpl(api, dao).getCurrentAirQuality(Location(1.0, 2.0))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is DataError.OfflineNoCache)
    }

    /** Malformed API payloads are distinct from network connectivity failures. */
    @Test
    fun `maps malformed remote response to unexpected response data error`() = runTest {
        val api = mockk<OpenMeteoApi>()
        val dao = mockk<AirQualityDao>()
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } throws
            JsonParseException("malformed response")
        coEvery { dao.findByLocationKey(any()) } returns null

        val result = AirQualityRepositoryImpl(api, dao).getCurrentAirQuality(Location(1.0, 2.0))

        assertTrue(result.exceptionOrNull() is DataError.UnexpectedResponse)
        coVerify(exactly = 1) { dao.findByLocationKey(any()) }
    }

    @Test
    fun `uses cached data and preserves refresh failure`() = runTest {
        val api = mockk<OpenMeteoApi>()
        val dao = mockk<AirQualityDao>()
        val location = Location(51.5, -0.1, "London, UK")
        val snapshot = sampleSnapshot(location)
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } throws IOException("offline")
        coEvery { dao.findByLocationKey(location.toCacheKey()) } returns snapshot.toCacheEntity(
            System.currentTimeMillis(),
        )

        val result = AirQualityRepositoryImpl(api, dao).getCurrentAirQuality(location)

        assertEquals(snapshot, result.getOrNull()?.snapshot)
        assertEquals(DataFreshness.Fresh, result.getOrNull()?.freshness)
        assertEquals(DataOrigin.Cache, result.getOrNull()?.origin)
        assertTrue(result.getOrNull()?.refreshError is DataError.Network)
    }

    @Test
    fun `marks cached data stale after freshness window`() = runTest {
        val api = mockk<OpenMeteoApi>()
        val dao = mockk<AirQualityDao>()
        val location = Location(51.5, -0.1)
        val snapshot = sampleSnapshot(location)
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } throws IOException("offline")
        coEvery { dao.findByLocationKey(location.toCacheKey()) } returns snapshot.toCacheEntity(
            System.currentTimeMillis() - 30 * 60 * 1000L - 1,
        )

        val result = AirQualityRepositoryImpl(api, dao).getCurrentAirQuality(location)

        assertTrue(result.isSuccess)
        assertEquals(DataFreshness.Stale, result.getOrNull()?.freshness)
        assertEquals(DataOrigin.Cache, result.getOrNull()?.origin)
        assertTrue(result.getOrNull()?.refreshError is DataError.Network)
    }

    @Test
    fun `returns remote data when cache persistence fails`() = runTest {
        val api = mockk<OpenMeteoApi>()
        val dao = mockk<AirQualityDao>()
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } returns sampleResponse()
        coEvery { dao.upsert(any()) } throws SQLiteException("disk error")

        val result = AirQualityRepositoryImpl(api, dao).getCurrentAirQuality(Location(1.0, 2.0))

        assertTrue(result.isSuccess)
        assertEquals(DataOrigin.Remote, result.getOrNull()?.origin)
        assertNull(result.getOrNull()?.refreshError)
    }

    @Test
    fun `maps cache read failure to local data error`() = runTest {
        val api = mockk<OpenMeteoApi>()
        val dao = mockk<AirQualityDao>()
        coEvery { api.getCurrentAirQuality(any(), any(), any(), any()) } throws IOException("offline")
        coEvery { dao.findByLocationKey(any()) } throws SQLiteException("database locked")

        val result = AirQualityRepositoryImpl(api, dao).getCurrentAirQuality(Location(1.0, 2.0))

        assertTrue(result.exceptionOrNull() is DataError.Local)
    }

    private fun sampleResponse() = OpenMeteoResponse(
        current = OpenMeteoCurrent(
            time = "2026-09-25T16:00",
            europeanAqi = 59,
            pm2_5 = 10.0,
            pm10 = 15.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 20.0,
            ozone = 30.0,
        ),
    )

    private fun sampleSnapshot(location: Location) = AirQualitySnapshot(
        location = location,
        timestamp = "2026-09-25T16:00",
        europeanAqi = 59,
        pm2_5 = 10.0,
        pm10 = 15.0,
        carbonMonoxide = 250.0,
        nitrogenDioxide = 20.0,
        ozone = 30.0,
    )
}
