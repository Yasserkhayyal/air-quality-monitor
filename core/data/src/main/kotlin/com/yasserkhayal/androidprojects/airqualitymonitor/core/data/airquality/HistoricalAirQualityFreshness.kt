package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness

private const val HISTORICAL_FRESHNESS_WINDOW_MILLIS = 60 * 60 * 1000L

/**
 * Determines historical cache freshness using the agreed one-hour threshold.
 */
internal fun historicalFreshnessAt(
    retrievedAtEpochMillis: Long,
    nowEpochMillis: Long,
): DataFreshness {
    val ageMillis = nowEpochMillis - retrievedAtEpochMillis
    return if (ageMillis in 0..HISTORICAL_FRESHNESS_WINDOW_MILLIS) {
        DataFreshness.Fresh
    } else {
        DataFreshness.Stale
    }
}
