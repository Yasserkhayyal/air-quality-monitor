package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

interface AirQualityRepository {
    /** Retrieves current air quality for the supplied location. */
    suspend fun getCurrentAirQuality(location: Location): Result<AirQualityResult>

    /** Retrieves hourly air-quality measurements covering the latest available 24-hour period. */
    suspend fun getHistoricalAirQuality(location: Location): Result<HistoricalAirQualityResult>
}
