package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AirQualityDaoTest {
    private lateinit var database: AirQualityDatabase
    private lateinit var dao: AirQualityDao

    @Before
    fun createDatabase() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
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
}
