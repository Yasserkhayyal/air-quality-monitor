package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

interface AirQualityRepository {
    suspend fun getCurrentAirQuality(location: Location): Result<AirQualitySnapshot>
}

sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : DataError("Network request failed", cause)
    class Server(val code: Int, cause: Throwable) : DataError("Server request failed", cause)
    class InvalidResponse(cause: Throwable) : DataError("Invalid air-quality response", cause)
}
