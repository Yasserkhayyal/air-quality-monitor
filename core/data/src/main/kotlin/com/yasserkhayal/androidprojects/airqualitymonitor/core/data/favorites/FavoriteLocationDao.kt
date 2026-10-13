package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location
import kotlinx.coroutines.flow.Flow

/**
 * Reads and mutates persisted favorites while preserving insertion order.
 */
@Dao
internal abstract class FavoriteLocationDao {
    /**
     * Observes favorites ordered by their original insertion position.
     */
    @Query("SELECT * FROM favorite_locations ORDER BY sortOrder ASC")
    abstract fun observeFavorites(): Flow<List<FavoriteLocationEntity>>

    /**
     * Finds a favorite by its coordinate identity.
     */
    @Query(
        "SELECT * FROM favorite_locations WHERE latitude = :latitude AND longitude = :longitude LIMIT 1",
    )
    protected abstract suspend fun findByCoordinates(
        latitude: Double,
        longitude: Double,
    ): FavoriteLocationEntity?

    /**
     * Returns the next insertion order for a new favorite.
     */
    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM favorite_locations")
    protected abstract suspend fun nextSortOrder(): Long

    /**
     * Inserts or updates the supplied favorite.
     */
    @Upsert
    protected abstract suspend fun upsert(entity: FavoriteLocationEntity)

    /**
     * Adds a favorite, replacing details for matching coordinates without changing its order.
     */
    @Transaction
    open suspend fun addFavorite(location: Location) {
        val existing = findByCoordinates(location.latitude, location.longitude)
        val sortOrder = existing?.sortOrder ?: nextSortOrder()
        upsert(location.toFavoriteEntity(sortOrder))
    }

    /**
     * Removes a favorite identified by its coordinates.
     */
    @Query("DELETE FROM favorite_locations WHERE latitude = :latitude AND longitude = :longitude")
    abstract suspend fun removeFavorite(latitude: Double, longitude: Double)
}
