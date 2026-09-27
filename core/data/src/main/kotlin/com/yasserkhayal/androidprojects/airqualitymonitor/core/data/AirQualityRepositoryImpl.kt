package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualityRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import javax.inject.Inject

class AirQualityRepositoryImpl @Inject constructor(
    private val api: OpenMeteoApi
) : AirQualityRepository {
    override suspend fun getCurrentAirQuality(location: Location): Result<AirQualitySnapshot> =
        try {
            Result.success(api.getCurrentAirQuality(location.latitude, location.longitude).toDomain(location))
        } catch (error: java.io.IOException) {
            Result.failure(DataError.Network(error))
        } catch (error: retrofit2.HttpException) {
            Result.failure(DataError.Server(error.code(), error))
        } catch (error: RuntimeException) {
            Result.failure(DataError.InvalidResponse(error))
        }
}
