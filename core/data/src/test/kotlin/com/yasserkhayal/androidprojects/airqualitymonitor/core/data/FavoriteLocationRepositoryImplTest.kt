package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.database.sqlite.SQLiteException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.FavoriteLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.FavoriteLocationEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites.FavoriteLocationRepositoryImpl
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FavoriteLocationRepositoryImplTest {
    private val dao = mockk<FavoriteLocationDao>()
    private val repository = FavoriteLocationRepositoryImpl(dao)

    /** Maps ordered persisted entities to domain locations. */
    @Test
    fun `observes favorite locations in DAO order`() = runTest {
        val entities = listOf(
            FavoriteLocationEntity(30.0444, 31.2357, "Cairo, Egypt", 0),
            FavoriteLocationEntity(51.5079, -0.1276, "London, UK", 1),
        )
        every { dao.observeFavorites() } returns flowOf(entities)

        assertEquals(
            listOf(
                Location(30.0444, 31.2357, "Cairo, Egypt"),
                Location(51.5079, -0.1276, "London, UK"),
            ),
            repository.observeFavorites().first(),
        )
    }

    /** Translates a SQLite failure during favorite observation to the domain error contract. */
    @Test
    fun `maps observation persistence failure to local error`() = runTest {
        every { dao.observeFavorites() } returns flow { throw mockk<SQLiteException>() }

        val result = runCatching { repository.observeFavorites().first() }

        assertTrue(result.exceptionOrNull() is DataError.Local)
    }

    /** Delegates coordinate-conflict upsert behavior to the transactional DAO. */
    @Test
    fun `adds favorite`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        coEvery { dao.addFavorite(location) } returns Unit

        assertTrue(repository.addFavorite(location).isSuccess)
        coVerify(exactly = 1) { dao.addFavorite(location) }
    }

    /** Translates a local SQLite failure to the domain error contract. */
    @Test
    fun `maps persistence failure to local error`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        coEvery { dao.addFavorite(location) } throws mockk<SQLiteException>()

        assertTrue(repository.addFavorite(location).exceptionOrNull() is DataError.Local)
    }

    /** Delegates removal using exact coordinate identity. */
    @Test
    fun `removes favorite by coordinates`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        coEvery { dao.removeFavorite(location.latitude, location.longitude) } returns Unit

        assertTrue(repository.removeFavorite(location).isSuccess)
        coVerify(exactly = 1) { dao.removeFavorite(location.latitude, location.longitude) }
    }
}
