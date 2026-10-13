package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.database.AirQualityDatabase
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.FavoriteLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.toDomain
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteLocationDaoTest {
    private lateinit var database: AirQualityDatabase
    private lateinit var dao: FavoriteLocationDao
    private lateinit var context: Context
    private var persistentDatabaseName: String? = null

    /** Creates an in-memory database for each favorite DAO test. */
    @Before
    fun createDatabase() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AirQualityDatabase::class.java).build()
        dao = database.favoriteLocationDao()
    }

    /** Closes the in-memory database after each favorite DAO test. */
    @After
    fun closeDatabase() {
        database.close()
        persistentDatabaseName?.let(context::deleteDatabase)
    }

    /** New favorites are observed in oldest-first order. */
    @Test
    fun newFavoritesAppendInInsertionOrder() = runTest {
        val firstLocation = Location(30.0444, 31.2357, "Cairo, Egypt")
        val secondLocation = Location(51.5079, -0.1276, "London, UK")

        dao.addFavorite(firstLocation)
        dao.addFavorite(secondLocation)

        assertEquals(listOf(firstLocation, secondLocation), dao.observeFavorites().first().map { it.toDomain() })
    }

    /** Adding matching coordinates replaces details without moving the existing favorite. */
    @Test
    fun coordinateConflictReplacesDetailsAndRetainsOrder() = runTest {
        val firstLocation = Location(30.0444, 31.2357, "Cairo, Egypt")
        val secondLocation = Location(51.5079, -0.1276, "London, UK")
        val updatedFirst = Location(30.0444, 31.2357, "Cairo, Egypt (updated)")

        dao.addFavorite(firstLocation)
        dao.addFavorite(secondLocation)
        dao.addFavorite(updatedFirst)

        assertEquals(
            listOf(updatedFirst, secondLocation),
            dao.observeFavorites().first().map { it.toDomain() },
        )
    }

    /** Removing and re-adding a favorite appends it after remaining favorites. */
    @Test
    fun removeAndReAddFavoriteMovesItToEnd() = runTest {
        val firstLocation = Location(30.0444, 31.2357, "Cairo, Egypt")
        val secondLocation = Location(51.5079, -0.1276, "London, UK")
        dao.addFavorite(firstLocation)
        dao.addFavorite(secondLocation)

        dao.removeFavorite(firstLocation.latitude, firstLocation.longitude)
        dao.addFavorite(firstLocation)

        assertEquals(
            listOf(secondLocation, firstLocation),
            dao.observeFavorites().first().map { it.toDomain() },
        )
    }

    /** Removing a favorite by coordinates leaves other favorites unchanged. */
    @Test
    fun removesFavoriteByCoordinates() = runTest {
        val firstLocation = Location(30.0444, 31.2357, "Cairo, Egypt")
        val secondLocation = Location(51.5079, -0.1276, "London, UK")
        dao.addFavorite(firstLocation)
        dao.addFavorite(secondLocation)

        dao.removeFavorite(firstLocation.latitude, firstLocation.longitude)

        assertEquals(listOf(secondLocation), dao.observeFavorites().first().map { it.toDomain() })
    }

    /** Favorites and insertion order remain persisted after closing and reopening Room. */
    @Test
    fun favoritesPersistAcrossDatabaseReopen() = runTest {
        val databaseName = "favorite-persistence-test.db"
        persistentDatabaseName = databaseName
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        dao.addFavorite(location)
        database.close()
        context.deleteDatabase(databaseName)

        database.close()
        database = Room.databaseBuilder(context, AirQualityDatabase::class.java, databaseName).build()
        dao = database.favoriteLocationDao()
        dao.addFavorite(location)
        database.close()

        database = Room.databaseBuilder(context, AirQualityDatabase::class.java, databaseName).build()
        dao = database.favoriteLocationDao()

        assertEquals(listOf(location), dao.observeFavorites().first().map { it.toDomain() })
    }
}
