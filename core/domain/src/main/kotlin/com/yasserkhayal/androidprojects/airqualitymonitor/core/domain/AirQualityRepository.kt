package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

interface AirQualityRepository {
    suspend fun getCurrentAirQuality(location: Location): Result<AirQualityResult>
}
