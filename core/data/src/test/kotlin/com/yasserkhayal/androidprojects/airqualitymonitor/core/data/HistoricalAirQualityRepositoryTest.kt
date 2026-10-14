package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.database.sqlite.SQLiteException
import android.util.Log
import com.google.gson.JsonParseException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityRepositoryImpl
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.HistoricalAirQualityCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.HistoricalAirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.OpenMeteoApi
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.OpenMeteoHistoricalResponse
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.OpenMeteoHourlyMeasurements
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.toCacheKey
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
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Verifies remote retrieval, full-window cache replacement, and historical fallback behavior.
 */
class HistoricalAirQualityRepositoryTest {
    private val api = mockk<OpenMeteoApi>()
    private val currentDao = mockk<AirQualityDao>(relaxed = true)
    private val historyDao = mockk<HistoricalAirQualityDao>()
    private val repository = AirQualityRepositoryImpl(api, currentDao, historyDao)

    /** Caches the newest 24 remote points by replacing the corresponding time window. */
    @Test
    fun returnsRemoteHistoryAndReplacesCachedWindow() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        val response = responseWithPoints(26)
        coEvery { api.getHistoricalAirQuality(any(), any(), any(), any(), any(), any()) } returns response
        coEvery { historyDao.replaceWindow(any(), any(), any(), any()) } returns Unit

        val result = repository.getHistoricalAirQuality(location)

        val history = requireNotNull(result.getOrNull())
        assertEquals(24, history.points.size)
        assertEquals(DataFreshness.Fresh, history.freshness)
        assertEquals(DataOrigin.Remote, history.origin)
        coVerify {
            historyDao.replaceWindow(
                location.toCacheKey(),
                history.points.first().timestamp,
                history.points.last().timestamp,
                match { it.size == 24 && it.all { point -> point.retrievedAtEpochMillis > 0L } },
            )
        }
    }

    /** Returns fresh cached history when remote request fails. */
    @Test
    fun fallsBackToFreshCacheOnNetworkFailure() = runTest {
        val location = Location(30.0444, 31.2357)
        coEvery { api.getHistoricalAirQuality(any(), any(), any(), any(), any(), any()) } throws IOException("offline")
        coEvery { historyDao.getLatest(location.toCacheKey()) } returns listOf(cachePoint(retrievedAt = System.currentTimeMillis()))

        val result = repository.getHistoricalAirQuality(location)

        val history = requireNotNull(result.getOrNull())
        assertEquals(DataFreshness.Fresh, history.freshness)
        assertEquals(DataOrigin.Cache, history.origin)
        assertTrue(history.refreshError is DataError.Network)
    }

    /** Marks cached data stale when a successful API response contains no usable metrics. */
    @Test
    fun usesStaleCacheWhenSuccessfulResponseHasNoUsableHistory() = runTest {
        val location = Location(30.0444, 31.2357)
        coEvery { api.getHistoricalAirQuality(any(), any(), any(), any(), any(), any()) } returns
            OpenMeteoHistoricalResponse(
                hourly = OpenMeteoHourlyMeasurements(
                    time = listOf("2026-10-13T23:00"),
                    europeanAqi = null,
                    pm2_5 = null,
                    pm10 = null,
                    carbonMonoxide = null,
                    nitrogenDioxide = null,
                    ozone = null,
                ),
            )
        coEvery { historyDao.getLatest(location.toCacheKey()) } returns listOf(cachePoint(retrievedAt = System.currentTimeMillis()))

        val result = repository.getHistoricalAirQuality(location)

        val history = requireNotNull(result.getOrNull())
        assertEquals(DataFreshness.Stale, history.freshness)
        assertEquals(DataOrigin.Cache, history.origin)
        assertTrue(history.refreshError is DataError.UnexpectedResponse)
        coVerify(exactly = 0) { historyDao.replaceWindow(any(), any(), any(), any()) }
    }

    /** Returns an offline-without-cache error when remote request fails and no rows exist. */
    @Test
    fun reportsOfflineWithoutCache() = runTest {
        val location = Location(30.0444, 31.2357)
        coEvery { api.getHistoricalAirQuality(any(), any(), any(), any(), any(), any()) } throws IOException("offline")
        coEvery { historyDao.getLatest(location.toCacheKey()) } returns emptyList()

        val result = repository.getHistoricalAirQuality(location)

        assertTrue(result.exceptionOrNull() is DataError.OfflineNoCache)
    }

    /** Maps malformed remote payloads and falls back to stale cached rows. */
    @Test
    fun fallsBackToCacheForMalformedRemotePayload() = runTest {
        val location = Location(30.0444, 31.2357)
        coEvery { api.getHistoricalAirQuality(any(), any(), any(), any(), any(), any()) } throws
            JsonParseException("bad payload")
        coEvery { historyDao.getLatest(location.toCacheKey()) } returns listOf(
            cachePoint(retrievedAt = System.currentTimeMillis() - 60 * 60 * 1000L - 1),
        )

        val result = repository.getHistoricalAirQuality(location)

        assertEquals(DataFreshness.Stale, result.getOrThrow().freshness)
        assertTrue(result.getOrThrow().refreshError is DataError.UnexpectedResponse)
    }

    /** Preserves successful remote data if Room cannot persist its cache. */
    @Test
    fun returnsRemoteHistoryWhenCacheWriteFails() = runTest {
        val location = Location(30.0444, 31.2357)
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        coEvery { api.getHistoricalAirQuality(any(), any(), any(), any(), any(), any()) } returns responseWithPoints(24)
        coEvery { historyDao.replaceWindow(any(), any(), any(), any()) } throws SQLiteException("write failed")

        val result = repository.getHistoricalAirQuality(location)

        assertEquals(DataOrigin.Remote, result.getOrThrow().origin)
    }

    /** Creates a full parallel-array API response with deterministic chronological points. */
    private fun responseWithPoints(count: Int) = OpenMeteoHistoricalResponse(
        hourly = OpenMeteoHourlyMeasurements(
            time = (0 until count).map { "2026-10-13T${(it % 24).toString().padStart(2, '0')}:00" },
            europeanAqi = (0 until count).toList(),
            pm2_5 = (0 until count).map(Int::toDouble),
            pm10 = (0 until count).map(Int::toDouble),
            carbonMonoxide = (0 until count).map(Int::toDouble),
            nitrogenDioxide = (0 until count).map(Int::toDouble),
            ozone = (0 until count).map(Int::toDouble),
        ),
    )

    /** Creates a persisted point for repository fallback tests. */
    private fun cachePoint(retrievedAt: Long) = HistoricalAirQualityCacheEntity(
        locationKey = "30.044:31.236",
        timestamp = "2026-10-13T23:00",
        europeanAqi = 42,
        pm2_5 = 12.5,
        pm10 = 20.0,
        carbonMonoxide = 300.0,
        nitrogenDioxide = 18.0,
        ozone = 46.0,
        retrievedAtEpochMillis = retrievedAt,
    )
}
