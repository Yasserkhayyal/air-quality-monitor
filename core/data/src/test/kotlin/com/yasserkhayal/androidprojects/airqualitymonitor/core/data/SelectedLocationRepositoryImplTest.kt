package com.yasserkhayal.androidprojects.airqualitymonitor.core.data

import android.database.sqlite.SQLiteException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationDao
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationEntity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation.SelectedLocationRepositoryImpl
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectedLocationRepositoryImplTest {
    private val dao = mockk<SelectedLocationDao>()
    private val repository = SelectedLocationRepositoryImpl(dao)

    @Test
    fun `reads saved location`() = runTest {
        val entity = SelectedLocationEntity(
            latitude = 30.0444,
            longitude = 31.2357,
            address = "Cairo, Egypt",
        )
        coEvery { dao.getSelectedLocation() } returns entity

        assertEquals(Location(30.0444, 31.2357, "Cairo, Egypt"), repository.getSelectedLocation().getOrThrow())
    }

    @Test
    fun `saves selected location`() = runTest {
        val location = Location(30.0444, 31.2357, "Cairo, Egypt")
        coEvery { dao.saveSelectedLocation(any()) } returns Unit

        assertTrue(repository.saveSelectedLocation(location).isSuccess)
        coVerify {
            dao.saveSelectedLocation(
                SelectedLocationEntity(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = location.address,
                ),
            )
        }
    }

    @Test
    fun `maps database failure to domain error`() = runTest {
        coEvery { dao.getSelectedLocation() } throws SQLiteException("database unavailable")

        assertTrue(repository.getSelectedLocation().exceptionOrNull() is DataError.Local)
    }
}
