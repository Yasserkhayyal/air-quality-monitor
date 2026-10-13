package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.AirQualityDatabase
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectedLocationDaoTest {
    private lateinit var database: AirQualityDatabase
    private lateinit var dao: SelectedLocationDao

    /** Creates an in-memory database for each selected-location DAO test. */
    @Before
    fun createDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AirQualityDatabase::class.java).build()
        dao = database.selectedLocationDao()
    }

    /** Closes the in-memory database after each selected-location DAO test. */
    @After
    fun closeDatabase() {
        database.close()
    }

    /** Saving a new active location replaces the previously stored location. */
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

        dao.saveSelectedLocation(searchedLocation)
        dao.saveSelectedLocation(currentLocation)

        assertEquals(currentLocation, dao.getSelectedLocation())
    }
}
