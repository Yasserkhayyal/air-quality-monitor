package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.historicalFreshnessAt
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the one-hour freshness policy for historical cache entries.
 */
class HistoricalAirQualityFreshnessTest {
    /** Treats cache exactly one hour old as fresh. */
    @Test
    fun cacheAtOneHourBoundaryIsFresh() {
        assertEquals(
            DataFreshness.Fresh,
            historicalFreshnessAt(1_000_000L, 1_000_000L + 60 * 60 * 1000L),
        )
    }

    /** Treats cache older than one hour as stale. */
    @Test
    fun cacheOlderThanOneHourIsStale() {
        assertEquals(
            DataFreshness.Stale,
            historicalFreshnessAt(1_000_000L, 1_000_000L + 60 * 60 * 1000L + 1),
        )
    }
}
