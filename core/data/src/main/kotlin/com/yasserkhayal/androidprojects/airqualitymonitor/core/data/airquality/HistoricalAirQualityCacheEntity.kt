package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import androidx.room.Entity

/**
 * Persisted hourly observation with retrieval time for historical freshness.
 */
@Entity(
    tableName = "historical_air_quality",
    primaryKeys = ["locationKey", "timestamp"],
)
internal data class HistoricalAirQualityCacheEntity(
    val locationKey: String,
    val timestamp: String,
    val europeanAqi: Int?,
    val pm2_5: Double?,
    val pm10: Double?,
    val carbonMonoxide: Double?,
    val nitrogenDioxide: Double?,
    val ozone: Double?,
    val retrievedAtEpochMillis: Long,
)
