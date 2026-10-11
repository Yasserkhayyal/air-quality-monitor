package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationEntity

@Database(
    entities = [AirQualityCacheEntity::class, SelectedLocationEntity::class],
    version = 2,
    exportSchema = true,
)
internal abstract class AirQualityDatabase : RoomDatabase() {
    abstract fun airQualityDao(): AirQualityDao

    abstract fun selectedLocationDao(): SelectedLocationDao
}

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
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
