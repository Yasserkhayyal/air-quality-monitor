package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataFreshness

private const val FRESHNESS_WINDOW_MILLIS = 30 * 60 * 1000L

internal fun freshnessAt(retrievedAtEpochMillis: Long, nowEpochMillis: Long): DataFreshness {
    val ageMillis = nowEpochMillis - retrievedAtEpochMillis
    return if (ageMillis in 0..FRESHNESS_WINDOW_MILLIS) {
        DataFreshness.Fresh
    } else {
        DataFreshness.Stale
    }
}
