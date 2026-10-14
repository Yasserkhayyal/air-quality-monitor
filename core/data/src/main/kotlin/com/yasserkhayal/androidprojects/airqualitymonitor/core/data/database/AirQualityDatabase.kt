package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.HistoricalAirQualityCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.HistoricalAirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.FavoriteLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.FavoriteLocationEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationEntity

/** Stores measurements, the active location, and favorite locations locally. */
@Database(
    entities = [
        AirQualityCacheEntity::class,
        SelectedLocationEntity::class,
        FavoriteLocationEntity::class,
        HistoricalAirQualityCacheEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
internal abstract class AirQualityDatabase : RoomDatabase() {
    abstract fun airQualityDao(): AirQualityDao

    abstract fun selectedLocationDao(): SelectedLocationDao

    /** Exposes persisted favorites to the data layer. */
    abstract fun favoriteLocationDao(): FavoriteLocationDao

    /** Exposes persisted hourly observations to the data layer. */
    abstract fun historicalAirQualityDao(): HistoricalAirQualityDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    /** Adds selected-location persistence while preserving cached measurements. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `selected_location` (
                `id` INTEGER NOT NULL,
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `address` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    /** Adds the coordinate-keyed favorite locations table. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `favorite_locations` (
                `latitude` REAL NOT NULL,
                `longitude` REAL NOT NULL,
                `address` TEXT,
                `sortOrder` INTEGER NOT NULL,
                PRIMARY KEY(`latitude`, `longitude`)
            )
            """.trimIndent(),
        )
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    /** Adds storage for location-specific hourly air-quality observations. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `historical_air_quality` (
                `locationKey` TEXT NOT NULL,
                `timestamp` TEXT NOT NULL,
                `europeanAqi` INTEGER,
                `pm2_5` REAL,
                `pm10` REAL,
                `carbonMonoxide` REAL,
                `nitrogenDioxide` REAL,
                `ozone` REAL,
                `retrievedAtEpochMillis` INTEGER NOT NULL,
                PRIMARY KEY(`locationKey`, `timestamp`)
            )
            """.trimIndent(),
        )
    }
}
