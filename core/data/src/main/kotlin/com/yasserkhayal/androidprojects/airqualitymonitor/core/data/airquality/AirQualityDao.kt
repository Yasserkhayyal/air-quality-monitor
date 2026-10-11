package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
internal interface AirQualityDao {
    @Upsert
    suspend fun upsert(entity: AirQualityCacheEntity)

    @Query("SELECT * FROM air_quality_cache WHERE locationKey = :locationKey")
    suspend fun findByLocationKey(locationKey: String): AirQualityCacheEntity?
}
