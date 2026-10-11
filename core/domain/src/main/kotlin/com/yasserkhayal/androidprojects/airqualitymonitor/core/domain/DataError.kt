package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

sealed class DataError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Network(cause: Throwable) : DataError("Network request failed", cause)
    class Server(val code: Int, cause: Throwable) : DataError("Server request failed", cause)
    class Configuration(cause: Throwable) : DataError("Remote service configuration failed", cause)
    class RateLimited(cause: Throwable) : DataError("Remote service rate limit exceeded", cause)
    class ServiceUnavailable(cause: Throwable) : DataError("Remote service is unavailable", cause)
    class UnexpectedResponse(cause: Throwable) : DataError("Remote service returned an unexpected response", cause)
    class GenericError(cause: Throwable) : DataError("Generic Error", cause)
    class OfflineNoCache(cause: Throwable) : DataError("No cached air-quality data is available offline", cause)
    class Local(cause: Throwable) : DataError("Local air-quality storage failed", cause)
}
