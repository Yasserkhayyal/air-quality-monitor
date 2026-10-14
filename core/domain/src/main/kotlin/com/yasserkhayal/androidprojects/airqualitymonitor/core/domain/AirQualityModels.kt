package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

data class Location(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null,
)

data class AirQualitySnapshot(
    val location: Location,
    val timestamp: String,
    val europeanAqi: Int?,
    val pm2_5: Double?,
    val pm10: Double?,
    val carbonMonoxide: Double?,
    val nitrogenDioxide: Double?,
    val ozone: Double?,
) {
    val category: AqiCategory?
        get() = europeanAqi?.let(AqiCategory::from)
}

/**
 * Air-quality measurements reported for one hourly observation in UTC.
 */
data class HistoricalAirQualityPoint(
    val timestamp: String,
    val europeanAqi: Int?,
    val pm2_5: Double?,
    val pm10: Double?,
    val carbonMonoxide: Double?,
    val nitrogenDioxide: Double?,
    val ozone: Double?,
)

enum class AqiCategory {
    GOOD,
    FAIR,
    MODERATE,
    POOR,
    VERY_POOR,
    ;

    companion object {
        fun from(aqi: Int): AqiCategory = when {
            aqi <= 20 -> GOOD
            aqi <= 40 -> FAIR
            aqi <= 60 -> MODERATE
            aqi <= 80 -> POOR
            else -> VERY_POOR
        }
    }
}
