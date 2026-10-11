package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.freshnessAt
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import org.junit.Assert.assertEquals
import org.junit.Test

class AirQualityFreshnessTest {
    @Test
    fun `data is fresh at the thirty minute boundary`() {
        val now = 10_000_000L

        assertEquals(
            DataFreshness.Fresh,
            freshnessAt(retrievedAtEpochMillis = now - 30.minutes, nowEpochMillis = now),
        )
    }

    @Test
    fun `data is stale after the thirty minute boundary`() {
        val now = 10_000_000L

        assertEquals(
            DataFreshness.Stale,
            freshnessAt(retrievedAtEpochMillis = now - 30.minutes - 1, nowEpochMillis = now),
        )
    }

    @Test
    fun `future retrieval timestamp is stale`() {
        val now = 10_000_000L

        assertEquals(
            DataFreshness.Stale,
            freshnessAt(retrievedAtEpochMillis = now + 1, nowEpochMillis = now),
        )
    }

    private val Int.minutes: Long
        get() = this * 60_000L
}
