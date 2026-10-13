package com.yasserkhayal.androidprojects.airqualitymonitor.core.domain

import kotlinx.coroutines.flow.Flow

/**
 * Provides locally persisted favorite locations.
 */
interface FavoritesRepository {
    /**
     * Observes favorites in their persisted insertion order, oldest first.
     */
    fun observeFavorites(): Flow<List<Location>>

    /**
     * Adds a favorite or replaces its details when coordinates already exist.
     */
    suspend fun addFavorite(location: Location): Result<Unit>

    /**
     * Removes a favorite identified by its coordinates.
     */
    suspend fun removeFavorite(location: Location): Result<Unit>
}
