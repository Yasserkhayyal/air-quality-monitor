package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.AirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.AirQualityDatabase
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.MIGRATION_1_2
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.MIGRATION_2_3
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.MIGRATION_3_4
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.toDomain
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies cached measurements and schema upgrades through the Room DAO boundary. */
@RunWith(AndroidJUnit4::class)
class AirQualityDaoTest {
    private lateinit var database: AirQualityDatabase
    private lateinit var dao: AirQualityDao
    private lateinit var context: Context

    @Before
    fun createDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AirQualityDatabase::class.java).build()
        dao = database.airQualityDao()
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

    /** Upgrades the v1 cache schema through current schema while preserving cached data. */
    @Test
    fun migrationFromVersionOnePreservesCacheAndCreatesNewTables() = runTest {
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
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build()
        try {
            val cachedMeasurement = migratedDatabase.airQualityDao().findByLocationKey("30.044:31.236")
            assertEquals(42, cachedMeasurement?.europeanAqi)
            assertNull(migratedDatabase.selectedLocationDao().getSelectedLocation())
            assertTrue(migratedDatabase.favoriteLocationDao().observeFavorites().first().isEmpty())
        } finally {
            migratedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    /** Applies only the v2-to-v3 migration and verifies v2 data remains intact. */
    @Test
    fun migrationFromVersionTwoPreservesCacheAndSelectedLocation() = runTest {
        val databaseName = "air-quality-v2-migration-test.db"
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
            legacyDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            )
            legacyDatabase.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$VERSION_TWO_IDENTITY_HASH')",
            )
            legacyDatabase.execSQL("PRAGMA user_version = 2")
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
            legacyDatabase.execSQL(
                """
                INSERT INTO selected_location (id, latitude, longitude, address)
                VALUES (1, 30.0444, 31.2357, 'Cairo, Egypt')
                """.trimIndent(),
            )
        }

        val migratedDatabase = Room.databaseBuilder(
            context,
            AirQualityDatabase::class.java,
            databaseName,
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4).build()
        try {
            val cachedMeasurement = migratedDatabase.airQualityDao().findByLocationKey("30.044:31.236")
            assertEquals(42, cachedMeasurement?.europeanAqi)
            assertEquals(
                SelectedLocationEntity(latitude = 30.0444, longitude = 31.2357, address = "Cairo, Egypt"),
                migratedDatabase.selectedLocationDao().getSelectedLocation(),
            )
            assertTrue(migratedDatabase.favoriteLocationDao().observeFavorites().first().isEmpty())
        } finally {
            migratedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    /** Upgrades v3 and preserves measurement cache, selected location, and favorites. */
    @Test
    fun migrationFromVersionThreePreservesExistingData() = runTest {
        val databaseName = "air-quality-v3-migration-test.db"
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
            legacyDatabase.execSQL(
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
            legacyDatabase.execSQL(
                "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)",
            )
            legacyDatabase.execSQL(
                "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$VERSION_THREE_IDENTITY_HASH')",
            )
            legacyDatabase.execSQL("PRAGMA user_version = 3")
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
            legacyDatabase.execSQL(
                """
                INSERT INTO selected_location (id, latitude, longitude, address)
                VALUES (1, 30.0444, 31.2357, 'Cairo, Egypt')
                """.trimIndent(),
            )
            legacyDatabase.execSQL(
                """
                INSERT INTO favorite_locations (latitude, longitude, address, sortOrder)
                VALUES (30.0444, 31.2357, 'Cairo, Egypt', 0)
                """.trimIndent(),
            )
        }

        val migratedDatabase = Room.databaseBuilder(
            context,
            AirQualityDatabase::class.java,
            databaseName,
        ).addMigrations(MIGRATION_3_4).build()
        try {
            assertEquals(
                42,
                migratedDatabase.airQualityDao().findByLocationKey("30.044:31.236")?.europeanAqi,
            )
            assertEquals(
                SelectedLocationEntity(latitude = 30.0444, longitude = 31.2357, address = "Cairo, Egypt"),
                migratedDatabase.selectedLocationDao().getSelectedLocation(),
            )
            assertEquals(
                listOf(Location(30.0444, 31.2357, "Cairo, Egypt")),
                migratedDatabase.favoriteLocationDao().observeFavorites().first().map { it.toDomain() },
            )
            assertTrue(migratedDatabase.historicalAirQualityDao().getLatest("30.044:31.236").isEmpty())
        } finally {
            migratedDatabase.close()
            context.deleteDatabase(databaseName)
        }
    }

    private companion object {
        const val VERSION_ONE_IDENTITY_HASH = "08fa64c14cb02d274249f36bc144bc02"
        const val VERSION_TWO_IDENTITY_HASH = "3a01314e19b5903b0f32672e2aa7de84"
        const val VERSION_THREE_IDENTITY_HASH = "a92a6f70ee012db45817f5bf35717db5"
    }
}
