package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AirQualityCacheEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class AirQualityDatabase : RoomDatabase() {
    abstract fun airQualityDao(): AirQualityDao
}
