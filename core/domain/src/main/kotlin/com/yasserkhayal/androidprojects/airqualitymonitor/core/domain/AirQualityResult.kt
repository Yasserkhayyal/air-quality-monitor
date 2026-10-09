package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

data class AirQualityResult(
    val snapshot: AirQualitySnapshot,
    val freshness: DataFreshness,
    val origin: DataOrigin,
    val refreshError: DataError? = null,
)

enum class DataFreshness {
    Fresh,
    Stale,
}

enum class DataOrigin {
    Remote,
    Cache,
}
