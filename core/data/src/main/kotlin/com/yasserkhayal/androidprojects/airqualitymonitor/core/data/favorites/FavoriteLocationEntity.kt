package com.yasserkhayal.androidprojects.airqualitymonitor.core.data.favorites

import androidx.room.Entity
import com.yasserkhayal.androidprojects.airqualitymonitor.core.domain.Location

/**
 * Room representation of a coordinate-keyed favorite and its stable insertion order.
 */
@Entity(
    tableName = "favorite_locations",
    primaryKeys = ["latitude", "longitude"],
)
internal data class FavoriteLocationEntity(
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val sortOrder: Long,
)

/**
 * Maps a persisted favorite to its domain location.
 */
internal fun FavoriteLocationEntity.toDomain(): Location = Location(
    latitude = latitude,
    longitude = longitude,
    address = address,
)

/**
 * Maps a domain location to a favorite entity at the supplied insertion order.
 */
internal fun Location.toFavoriteEntity(sortOrder: Long): FavoriteLocationEntity = FavoriteLocationEntity(
    latitude = latitude,
    longitude = longitude,
    address = address,
    sortOrder = sortOrder,
)
