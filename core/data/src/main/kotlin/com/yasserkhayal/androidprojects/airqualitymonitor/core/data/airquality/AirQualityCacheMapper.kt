package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.AirQualitySnapshot
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import java.math.BigDecimal
import java.math.RoundingMode

internal fun Location.toCacheKey(): String {
    fun roundedCoordinate(coordinate: Double): Double = BigDecimal(coordinate)
        .setScale(3, RoundingMode.HALF_UP) // Traditional mathematical rounding
        .toDouble()

    return "${roundedCoordinate(latitude)}:${roundedCoordinate(longitude)}"
}

internal fun AirQualitySnapshot.toCacheEntity(
    retrievedAtEpochMillis: Long,
) = AirQualityCacheEntity(
    locationKey = location.toCacheKey(),
    latitude = location.latitude,
    longitude = location.longitude,
    address = location.address,
    timestamp = timestamp,
    europeanAqi = europeanAqi,
    pm2_5 = pm2_5,
    pm10 = pm10,
    carbonMonoxide = carbonMonoxide,
    nitrogenDioxide = nitrogenDioxide,
    ozone = ozone,
    retrievedAtEpochMillis = retrievedAtEpochMillis,
)

internal fun AirQualityCacheEntity.toDomain() = AirQualitySnapshot(
    location = Location(latitude, longitude, address),
    timestamp = timestamp,
    europeanAqi = europeanAqi,
    pm2_5 = pm2_5,
    pm10 = pm10,
    carbonMonoxide = carbonMonoxide,
    nitrogenDioxide = nitrogenDioxide,
    ozone = ozone,
)
