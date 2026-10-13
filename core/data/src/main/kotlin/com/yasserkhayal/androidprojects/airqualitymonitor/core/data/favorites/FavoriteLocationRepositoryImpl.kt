package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites

import android.database.sqlite.SQLiteException
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.DataError
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.FavoritesRepository
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implements favorite persistence through Room and translates local write failures.
 */
class FavoriteLocationRepositoryImpl @Inject internal constructor(
    private val dao: FavoriteLocationDao,
) : FavoritesRepository {
    /**
     * Observes favorites in insertion order and translates SQLite failures to the domain error contract.
     */
    override fun observeFavorites(): Flow<List<Location>> = dao.observeFavorites()
        .map { favorites -> favorites.map(FavoriteLocationEntity::toDomain) }
        .catch { error ->
            if (error is SQLiteException) {
                throw DataError.Local(error)
            }
            throw error
        }

    /**
     * Adds or updates a favorite, translating local database failures.
     */
    override suspend fun addFavorite(location: Location): Result<Unit> = try {
        dao.addFavorite(location)
        Result.success(Unit)
    } catch (error: SQLiteException) {
        Result.failure(DataError.Local(error))
    }

    /**
     * Removes a favorite by its coordinate identity.
     */
    override suspend fun removeFavorite(location: Location): Result<Unit> = try {
        dao.removeFavorite(location.latitude, location.longitude)
        Result.success(Unit)
    } catch (error: SQLiteException) {
        Result.failure(DataError.Local(error))
    }
}
