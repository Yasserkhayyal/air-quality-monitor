package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "air_quality_cache")
internal data class AirQualityCacheEntity(
    @PrimaryKey val locationKey: String,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val timestamp: String,
    val europeanAqi: Int?,
    val pm2_5: Double?,
    val pm10: Double?,
    val carbonMonoxide: Double?,
    val nitrogenDioxide: Double?,
    val ozone: Double?,
    val retrievedAtEpochMillis: Long,
)
