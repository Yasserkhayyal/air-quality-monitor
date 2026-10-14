package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.HistoricalAirQualityCacheEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.airquality.HistoricalAirQualityDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.AirQualityDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies historical Room queries, atomic window replacement, and disk persistence.
 */
@RunWith(AndroidJUnit4::class)
class HistoricalAirQualityDaoTest {
    private lateinit var context: Context
    private lateinit var database: AirQualityDatabase
    private lateinit var dao: HistoricalAirQualityDao
    private var diskDatabaseName: String? = null

    /** Creates an in-memory Room database for each DAO test. */
    @Before
    fun createDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AirQualityDatabase::class.java).build()
        dao = database.historicalAirQualityDao()
    }

    /** Closes the active database and deletes any file-backed test database. */
    @After
    fun closeDatabase() {
        database.close()
        diskDatabaseName?.let(context::deleteDatabase)
    }

    /** Replaces only rows in selected location/window and preserves outside rows. */
    @Test
    fun replacesWindowWithoutTouchingOtherLocationsOrTimes() = runTest {
        val oldInWindow = cachePoint("30.044:31.236", "2026-10-13T10:00", 10)
        val retainedOutsideWindow = cachePoint("30.044:31.236", "2026-10-13T09:00", 9)
        val retainedOtherLocation = cachePoint("51.500:-0.100", "2026-10-13T10:00", 50)
        dao.replaceWindow(
            oldInWindow.locationKey,
            oldInWindow.timestamp,
            oldInWindow.timestamp,
            listOf(oldInWindow),
        )
        dao.replaceWindow(
            retainedOutsideWindow.locationKey,
            retainedOutsideWindow.timestamp,
            retainedOutsideWindow.timestamp,
            listOf(retainedOutsideWindow),
        )
        dao.replaceWindow(
            retainedOtherLocation.locationKey,
            retainedOtherLocation.timestamp,
            retainedOtherLocation.timestamp,
            listOf(retainedOtherLocation),
        )
        val updatedInWindow = cachePoint("30.044:31.236", "2026-10-13T10:00", 20)
        dao.replaceWindow(
            updatedInWindow.locationKey,
            updatedInWindow.timestamp,
            updatedInWindow.timestamp,
            listOf(updatedInWindow),
        )

        assertEquals(
            listOf(updatedInWindow, retainedOutsideWindow),
            dao.getLatest("30.044:31.236"),
        )
        assertEquals(listOf(retainedOtherLocation), dao.getLatest("51.500:-0.100"))
    }

    /** Keeps historical observations after closing and reopening a disk-backed database. */
    @Test
    fun historicalRowsPersistAcrossDatabaseReopen() = runTest {
        val name = "historical-persistence-test.db"
        diskDatabaseName = name
        context.deleteDatabase(name)
        database.close()

        database = Room.databaseBuilder(context, AirQualityDatabase::class.java, name).build()
        dao = database.historicalAirQualityDao()
        val point = cachePoint("30.044:31.236", "2026-10-13T10:00", 10)
        dao.replaceWindow(point.locationKey, point.timestamp, point.timestamp, listOf(point))
        database.close()

        database = Room.databaseBuilder(context, AirQualityDatabase::class.java, name).build()
        dao = database.historicalAirQualityDao()

        assertEquals(listOf(point), dao.getLatest(point.locationKey))
    }

    /** Creates an hourly cache row with all supported measurements. */
    private fun cachePoint(
        locationKey: String,
        timestamp: String,
        value: Int,
    ) = HistoricalAirQualityCacheEntity(
        locationKey = locationKey,
        timestamp = timestamp,
        europeanAqi = value,
        pm2_5 = value.toDouble(),
        pm10 = value.toDouble(),
        carbonMonoxide = value.toDouble(),
        nitrogenDioxide = value.toDouble(),
        ozone = value.toDouble(),
        retrievedAtEpochMillis = 1_000L,
    )
}
