package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.HistoricalAirQualityPoint
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

private const val HISTORICAL_POINTS_LIMIT = 24

/**
 * Maps usable Open-Meteo hourly entries to the latest available 24 domain points.
 */
internal fun OpenMeteoHistoricalResponse.toDomainPoints(): List<HistoricalAirQualityPoint> {
    val hourly = hourly ?: return emptyList()
    return hourly.time.orEmpty().mapIndexedNotNull { index, timestamp ->
        val point = HistoricalAirQualityPoint(
            timestamp = timestamp,
            europeanAqi = hourly.europeanAqi.valueAt(index),
            pm2_5 = hourly.pm2_5.valueAt(index),
            pm10 = hourly.pm10.valueAt(index),
            carbonMonoxide = hourly.carbonMonoxide.valueAt(index),
            nitrogenDioxide = hourly.nitrogenDioxide.valueAt(index),
            ozone = hourly.ozone.valueAt(index),
        )
        point.takeIf {
            it.europeanAqi != null ||
                it.pm2_5 != null ||
                it.pm10 != null ||
                it.carbonMonoxide != null ||
                it.nitrogenDioxide != null ||
                it.ozone != null
        }
    }.sortedBy(HistoricalAirQualityPoint::timestamp).takeLast(HISTORICAL_POINTS_LIMIT)
}

/**
 * Maps a Domain observation to its persisted row for the supplied location key and retrieval time.
 */
internal fun HistoricalAirQualityPoint.toCacheEntity(
    location: Location,
    retrievedAtEpochMillis: Long,
) = HistoricalAirQualityCacheEntity(
    locationKey = location.toCacheKey(),
    timestamp = timestamp,
    europeanAqi = europeanAqi,
    pm2_5 = pm2_5,
    pm10 = pm10,
    carbonMonoxide = carbonMonoxide,
    nitrogenDioxide = nitrogenDioxide,
    ozone = ozone,
    retrievedAtEpochMillis = retrievedAtEpochMillis,
)

/**
 * Maps a persisted observation to the Domain representation.
 */
internal fun HistoricalAirQualityCacheEntity.toDomain() = HistoricalAirQualityPoint(
    timestamp = timestamp,
    europeanAqi = europeanAqi,
    pm2_5 = pm2_5,
    pm10 = pm10,
    carbonMonoxide = carbonMonoxide,
    nitrogenDioxide = nitrogenDioxide,
    ozone = ozone,
)

private fun <T> List<T?>?.valueAt(index: Int): T? = this?.getOrNull(index)
