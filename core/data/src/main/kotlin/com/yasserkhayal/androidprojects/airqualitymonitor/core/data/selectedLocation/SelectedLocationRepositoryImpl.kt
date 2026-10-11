package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.selectedLocation

import android.database.sqlite.SQLiteException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.SelectedLocationRepository
import javax.inject.Inject

class SelectedLocationRepositoryImpl @Inject internal constructor(
    private val dao: SelectedLocationDao,
) : SelectedLocationRepository {
    override suspend fun getSelectedLocation(): Result<Location?> = try {
        Result.success(dao.getSelectedLocation()?.toDomain())
    } catch (error: SQLiteException) {
        Result.failure(DataError.Local(error))
    }

    override suspend fun saveSelectedLocation(location: Location): Result<Unit> = try {
        dao.saveSelectedLocation(location.toSelectedLocationEntity())
        Result.success(Unit)
    } catch (error: SQLiteException) {
        Result.failure(DataError.Local(error))
    }
}
