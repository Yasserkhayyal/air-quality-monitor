package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.AirQualityDatabase
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.MIGRATION_1_2
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AirQualityDaoTest {
    private lateinit var database: AirQualityDatabase
    private lateinit var dao: AirQualityDao
    private lateinit var selectedLocationDao: SelectedLocationDao
    private lateinit var context: Context

    @Before
    fun createDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AirQualityDatabase::class.java).build()
        dao = database.airQualityDao()
        selectedLocationDao = database.selectedLocationDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertAndReadReturnsCachedMeasurement() = runTest {
        val entity = AirQualityCacheEntity(
            locationKey = "51.508:-0.128",
            latitude = 51.5079,
            longitude = -0.1276,
            address = "London, UK",
            timestamp = "2026-09-25T16:00",
            europeanAqi = 59,
            pm2_5 = 10.0,
            pm10 = 15.0,
            carbonMonoxide = 250.0,
            nitrogenDioxide = 20.0,
            ozone = 30.0,
            retrievedAtEpochMillis = 1_000L,
        )

        dao.upsert(entity)

        assertEquals(entity, dao.findByLocationKey(entity.locationKey))
    }

    @Test
    fun saveLastSelectedLocationReplacesPreviousSelection() = runTest {
        val searchedLocation = SelectedLocationEntity(
            latitude = 30.0444,
            longitude = 31.2357,
            address = "Cairo, Egypt",
        )
        val currentLocation = SelectedLocationEntity(
            latitude = 51.5079,
            longitude = -0.1276,
            address = "London, UK",
        )

        selectedLocationDao.saveSelectedLocation(searchedLocation)
        selectedLocationDao.saveSelectedLocation(currentLocation)

        assertEquals(currentLocation, selectedLocationDao.getSelectedLocation())
    }

    @Test
    fun migrationFromVersionOnePreservesCacheAndCreatesSelectedLocationTable() = runTest {
        val databaseName = "air-quality-migration-test.db"
        context.deleteDatabase(databaseName)
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).use { legacyDatabase ->
            legacyDatabase.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `air_quality_cache` (
                    `locationKey` TEXT NOT NULL,
                    `latitude` REAL NOT NULL,
                    `longitude` REAL NOT NULL,
                    `address` TEXT,
                    `timestamp` TEXT NOT NULL,
                    `europeanAqi` INTEGER,
                    `pm2_5` REAL,
                    `pm10` REAL,
                    `carbonMonoxide` REAL,
                    `nitrogenDioxide` REAL,
                    `ozone` REAL,
                    `retrievedAtEpochMillis` INTEGER NOT NULL,
                    PRIMARY KEY(`locationKey`)
                )
                """.trimIndent(),
            )
            legacyDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            )
            legacyDatabase.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$VERSION_ONE_IDENTITY_HASH')",
            )
            legacyDatabase.execSQL("PRAGMA user_version = 1")
            legacyDatabase.execSQL(
                """
                INSERT INTO air_quality_cache (
                    locationKey, latitude, longitude, address, timestamp, europeanAqi,
                    pm2_5, pm10, carbonMonoxide, nitrogenDioxide, ozone, retrievedAtEpochMillis
                ) VALUES (
                    '30.044:31.236', 30.0444, 31.2357, 'Cairo, Egypt', '2026-09-25T16:00',
                    42, 12.5, 20.0, 300.0, 18.0, 46.0, 1000
                )
                """.trimIndent(),
            )
        }

        val migratedDatabase = Room.databaseBuilder(
            context,
            AirQualityDatabase::class.java,
            databaseName,
        ).addMigrations(MIGRATION_1_2).build()
        try {
            val cachedMeasurement = migratedDatabase.airQualityDao().findByLocationKey("30.044:31.236")
            assertEquals(42, cachedMeasurement?.europeanAqi)
            assertNull(migratedDatabase.selectedLocationDao().getSelectedLocation())
        } finally {
            migratedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private companion object {
        const val VERSION_ONE_IDENTITY_HASH = "08fa64c14cb02d274249f36bc144bc02"
    }
}
